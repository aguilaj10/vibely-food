# Vibely POS - Database Schema Documentation

## Overview

Multi-tenant SaaS PostgreSQL database with Row-Level Security (RLS) for complete data isolation.

**Architecture:**
- **Organization**: Billing entity (the "client" - could be a restaurant chain)
- **Store**: Isolation boundary (each physical location)
- **Composite Key**: `(organization_id, store_id)` on all tenant-scoped tables

## Multi-Tenancy Strategy

### Tenant Hierarchy
```
Organization (1)
└── Stores (N)
    ├── Users (M:N via users_stores)
    ├── Products
    ├── Orders
    ├── Inventory
    └── All other resources
```

### Row-Level Security (RLS)

Every tenant-scoped table has:
1. `organization_id` and `store_id` columns
2. RLS policy filtering by `current_setting('app.current_store_id')`
3. `FORCE ROW LEVEL SECURITY` to enforce even for table owners

**Application sets context per transaction:**
```sql
SET LOCAL app.current_store_id = '12345';
```

### Database Schema

See `database-schema.sql` for complete SQL implementation.

## Key Tables

### Core Multi-Tenant
- `organizations` - Billing entities
- `stores` - Physical locations
- `organization_users` - Org-level admins
- `users_stores` - Store-level permissions

### POS Operations
- `products`, `categories`, `product_modifiers`
- `orders`, `order_items`, `kitchen_orders`
- `payments`, `transactions`
- `customers`, `loyalty_transactions`

### Inventory
- `inventory` - Current stock levels
- `inventory_movements` - Stock changes (partitioned)
- `suppliers` - Supplier management

### Store Management
- `tables`, `floor_plans` - Table layout
- `shifts`, `cash_movements` - Cash management
- `settings` - Store configuration

### Offline-First
- `store_events` - Event sourcing log
- `outbox` - Reliable sync pattern

## Partitioning Strategy

High-volume tables partitioned by `store_id` using HASH partitioning (16 partitions):
- `orders`
- `transactions`
- `inventory_movements`

## Indexing Strategy

All tenant-scoped tables have:
```sql
CREATE INDEX idx_{table}_store_created ON {table}(store_id, created_at DESC);
```

Common query patterns get composite indexes:
```sql
CREATE INDEX idx_orders_store_status ON orders(store_id, status);
```

## Audit & Timestamps

All tables include:
- `created_at TIMESTAMPTZ DEFAULT NOW()`
- `updated_at TIMESTAMPTZ DEFAULT NOW()`
- Automatic `updated_at` trigger

## Migration Strategy

Use Flyway for versioned migrations:
1. Create base schema
2. Enable RLS policies
3. Create partitions
4. Add indexes (CONCURRENTLY in production)

## Connection Pooling Configuration

### HikariCP Optimization

**Critical settings for optimal performance:**

```kotlin
// JVM application configuration
HikariConfig().apply {
    // Connection pool sizing
    maximumPoolSize = (Runtime.getRuntime().availableProcessors() * 2).coerceAtLeast(10)
    minimumIdle = maximumPoolSize / 2
    
    // Connection timeout
    connectionTimeout = 30000 // 30 seconds
    idleTimeout = 600000 // 10 minutes
    maxLifetime = 1800000 // 30 minutes
    
    // Performance optimizations (30-40% improvement)
    dataSourceProperties["preparedStatementCacheQueries"] = "256"
    dataSourceProperties["preparedStatementCacheSize"] = "2048"
    dataSourceProperties["binaryTransfer"] = "true"
    dataSourceProperties["reWriteBatchedInserts"] = "true"
    
    // Health checks
    connectionTestQuery = "SELECT 1"
    validationTimeout = 5000
}
```

**Pool sizing formula:**
```
pool_size = (core_count * 2) + effective_spindle_count
```

For cloud environments:
- **Small instance (2 cores, SSD):** 10 connections
- **Medium instance (4 cores, SSD):** 12 connections
- **Large instance (8 cores, SSD):** 20 connections

### PgBouncer Configuration

**CRITICAL: Must use session pooling mode for RLS to work**

```ini
# /etc/pgbouncer/pgbouncer.ini

[databases]
vibely_prod = host=postgres.internal port=5432 dbname=vibely_prod

[pgbouncer]
# CRITICAL: Session pooling required for RLS
pool_mode = session

# Connection limits
max_client_conn = 1000
default_pool_size = 20
reserve_pool_size = 5
reserve_pool_timeout = 3

# Timeouts
server_idle_timeout = 600
server_lifetime = 3600
query_timeout = 60

# Logging
log_connections = 1
log_disconnections = 1
log_pooler_errors = 1
```

**Why session pooling is required:**
- RLS uses `SET LOCAL app.current_store_id` which is session-scoped
- Transaction pooling returns connection to pool after each transaction
- Session pooling keeps connection assigned to client for entire session
- **Trade-off:** Fewer concurrent clients, but RLS works correctly

**Application configuration:**
```kotlin
// Point HikariCP to PgBouncer, not PostgreSQL directly
jdbcUrl = "jdbc:postgresql://pgbouncer.internal:6432/vibely_prod"
```

---

## RLS Performance Monitoring

### Monitoring Queries

**Check for sequential scans (performance issue):**
```sql
SELECT 
    schemaname,
    tablename,
    seq_scan,
    idx_scan,
    CASE 
        WHEN seq_scan + idx_scan > 0 
        THEN round(100.0 * idx_scan / (seq_scan + idx_scan), 2)
        ELSE 0 
    END as index_usage_pct
FROM pg_stat_user_tables
WHERE schemaname = 'public'
AND seq_scan > idx_scan
ORDER BY seq_scan DESC;
```

**Explain RLS overhead:**
```sql
-- Set RLS context
SET LOCAL app.current_organization_id = 'test-org';
SET LOCAL app.current_store_id = 'test-store';

-- Analyze query with RLS
EXPLAIN (ANALYZE, BUFFERS, VERBOSE) 
SELECT * FROM orders WHERE status = 'PENDING';
```

**Monitor RLS policy execution:**
```sql
SELECT 
    schemaname || '.' || tablename as table_name,
    polname as policy_name,
    polcmd as command,
    polqual as using_expression,
    polwithcheck as with_check_expression
FROM pg_policy
WHERE schemaname = 'public'
ORDER BY tablename, polname;
```

### Performance Benchmarks

**Expected RLS overhead:**
- Simple queries (indexed): < 5ms overhead
- Complex joins: 10-20ms overhead
- Bulk operations: 5-10% overhead

**Warning signs:**
- Sequential scans on large tables
- Index usage < 90%
- Query times > 100ms for simple lookups

**Optimization checklist:**
1. ✅ Composite indexes include `store_id` as first column
2. ✅ RLS policies use indexed columns
3. ✅ Statistics are up-to-date (`ANALYZE` run regularly)
4. ✅ Partitioning reduces scan size
5. ✅ Connection pooling configured correctly

---

## Backup & Restore

### Automated Backups

**Daily full backup:**
```bash
#!/bin/bash
# /usr/local/bin/backup-vibely.sh

BACKUP_DIR="/var/backups/vibely"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/vibely_$TIMESTAMP.sql.gz"

# Full database backup with compression
pg_dump -h localhost -U vibely_prod vibely_prod \
  --format=custom \
  --compress=9 \
  --file="$BACKUP_FILE" \
  --verbose

# Upload to S3
aws s3 cp "$BACKUP_FILE" "s3://vibely-backups/daily/"

# Verify backup integrity
pg_restore --list "$BACKUP_FILE" > /dev/null
if [ $? -eq 0 ]; then
  echo "✅ Backup successful: $BACKUP_FILE"
else
  echo "❌ Backup verification failed!" | mail -s "Backup Alert" ops@vibely.com
  exit 1
fi

# Cleanup old backups (keep 7 days locally)
find "$BACKUP_DIR" -name "vibely_*.sql.gz" -mtime +7 -delete
```

**Cron schedule:**
```cron
# Daily at 2 AM
0 2 * * * /usr/local/bin/backup-vibely.sh
```

### Point-in-Time Recovery (PITR)

**Enable WAL archiving:**
```ini
# postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'aws s3 cp %p s3://vibely-backups/wal/%f'
archive_timeout = 300  # 5 minutes

# Replication settings
max_wal_senders = 3
wal_keep_size = 1GB
```

**Restore procedure:**
```bash
# 1. Stop application servers
systemctl stop vibely-api

# 2. Restore base backup
pg_restore \
  --dbname=vibely_prod \
  --clean \
  --if-exists \
  --verbose \
  /path/to/base_backup.sql.gz

# 3. Create recovery configuration
cat > /var/lib/postgresql/data/recovery.signal

# 4. Configure recovery target
cat >> /var/lib/postgresql/data/postgresql.auto.conf <<EOF
restore_command = 'aws s3 cp s3://vibely-backups/wal/%f %p'
recovery_target_time = '2026-03-20 14:30:00'
recovery_target_action = 'promote'
EOF

# 5. Start PostgreSQL (applies WAL files)
systemctl start postgresql

# 6. Verify recovery
psql -U vibely_prod -d vibely_prod -c "SELECT NOW();"

# 7. Resume application
systemctl start vibely-api
```

### Disaster Recovery Plan

**Recovery Time Objective (RTO):** 1 hour
**Recovery Point Objective (RPO):** 15 minutes

**Incident Response:**

1. **Detect (0-5 min)**
   - Monitoring alerts trigger
   - Database connectivity fails
   - Data corruption detected

2. **Assess (5-15 min)**
   - Determine failure type (hardware, corruption, deletion)
   - Identify last known good state
   - Calculate data loss window

3. **Isolate (15-20 min)**
   - Stop application servers
   - Prevent further writes
   - Preserve evidence for post-mortem

4. **Restore (20-50 min)**
   - Restore from backup
   - Apply WAL files to recovery point
   - Verify data integrity

5. **Resume (50-60 min)**
   - Start application servers
   - Run smoke tests
   - Monitor for issues

6. **Communicate (ongoing)**
   - Notify users of incident
   - Provide status updates
   - Document data loss (if any)

**Post-Incident:**
- Root cause analysis within 24 hours
- Update runbooks
- Implement preventive measures

---

## Security Hardening

### Database User Roles

```sql
-- Application user (limited permissions)
CREATE ROLE vibely_app WITH LOGIN PASSWORD 'secure_password';
GRANT CONNECT ON DATABASE vibely_prod TO vibely_app;
GRANT USAGE ON SCHEMA public TO vibely_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO vibely_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO vibely_app;

-- Read-only user (for analytics)
CREATE ROLE vibely_readonly WITH LOGIN PASSWORD 'readonly_password';
GRANT CONNECT ON DATABASE vibely_prod TO vibely_readonly;
GRANT USAGE ON SCHEMA public TO vibely_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO vibely_readonly;

-- Backup user
CREATE ROLE vibely_backup WITH LOGIN PASSWORD 'backup_password';
GRANT CONNECT ON DATABASE vibely_prod TO vibely_backup;
ALTER ROLE vibely_backup WITH REPLICATION;
```

### SSL/TLS Configuration

```ini
# postgresql.conf
ssl = on
ssl_cert_file = '/etc/ssl/certs/postgresql.crt'
ssl_key_file = '/etc/ssl/private/postgresql.key'
ssl_ca_file = '/etc/ssl/certs/ca.crt'

# Require SSL for all connections
ssl_min_protocol_version = 'TLSv1.2'
ssl_ciphers = 'HIGH:MEDIUM:+3DES:!aNULL'
```

```ini
# pg_hba.conf
# Require SSL for all remote connections
hostssl all all 0.0.0.0/0 md5
hostssl all all ::/0 md5
```

### Audit Logging

```ini
# postgresql.conf
log_connections = on
log_disconnections = on
log_duration = on
log_statement = 'mod'  # Log all modifications
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '

# Log slow queries
log_min_duration_statement = 1000  # 1 second
```

---

## Maintenance Procedures

### Regular Maintenance

**Daily:**
```sql
-- Update statistics for query planner
ANALYZE;
```

**Weekly:**
```sql
-- Vacuum to reclaim space
VACUUM ANALYZE;

-- Reindex heavily-updated tables
REINDEX TABLE CONCURRENTLY orders;
REINDEX TABLE CONCURRENTLY inventory;
```

**Monthly:**
```sql
-- Full vacuum (requires downtime)
VACUUM FULL ANALYZE;

-- Check for bloat
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    n_dead_tup as dead_tuples
FROM pg_stat_user_tables
WHERE n_dead_tup > 10000
ORDER BY n_dead_tup DESC;
```

### Partition Management

**Create new monthly partition:**
```sql
-- For orders table (partitioned by created_at)
CREATE TABLE orders_2026_04 PARTITION OF orders
FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

-- Create indexes on new partition
CREATE INDEX CONCURRENTLY idx_orders_2026_04_store_status 
ON orders_2026_04(store_id, status);
```

**Drop old partitions (after archival):**
```sql
-- Archive to cold storage first
pg_dump -t orders_2024_01 > orders_2024_01.sql
aws s3 cp orders_2024_01.sql s3://vibely-archives/

-- Then drop partition
DROP TABLE orders_2024_01;
```

---

## Scaling Strategy

### Vertical Scaling

**Current (0-1000 stores):**
- PostgreSQL 16 on single instance
- 4 vCPU, 16GB RAM
- 500GB SSD storage
- Cost: ~$500/month

**Growth (1000-5000 stores):**
- 8 vCPU, 32GB RAM
- 1TB SSD storage
- Read replicas for analytics
- Cost: ~$1500/month

### Horizontal Scaling (5000+ stores)

**Sharding by organization_id:**
```sql
-- Shard 1: organizations A-M
-- Shard 2: organizations N-Z

-- Use Citus or manual sharding
CREATE EXTENSION citus;

SELECT create_distributed_table('orders', 'organization_id');
SELECT create_distributed_table('products', 'organization_id');
```

**Read replicas for analytics:**
```ini
# On primary
wal_level = replica
max_wal_senders = 5

# On replica
hot_standby = on
max_standby_streaming_delay = 30s
```

---

## Troubleshooting

### Common Issues

**Issue: RLS not working (seeing other stores' data)**
```sql
-- Check if RLS is enabled
SELECT tablename, rowsecurity FROM pg_tables WHERE schemaname = 'public';

-- Check current settings
SHOW app.current_store_id;
SHOW app.current_organization_id;

-- Verify policy exists
SELECT * FROM pg_policy WHERE tablename = 'orders';
```

**Issue: Slow queries**
```sql
-- Find slow queries
SELECT 
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

-- Check missing indexes
SELECT 
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation
FROM pg_stats
WHERE schemaname = 'public'
AND n_distinct > 100
AND correlation < 0.1;
```

**Issue: Connection pool exhausted**
```sql
-- Check active connections
SELECT 
    datname,
    usename,
    application_name,
    state,
    COUNT(*)
FROM pg_stat_activity
GROUP BY datname, usename, application_name, state;

-- Kill idle connections
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'idle'
AND state_change < NOW() - INTERVAL '10 minutes';
```

---

## Performance Tuning

### PostgreSQL Configuration

```ini
# postgresql.conf - Production settings

# Memory
shared_buffers = 4GB
effective_cache_size = 12GB
maintenance_work_mem = 1GB
work_mem = 64MB

# Parallelism
max_parallel_workers_per_gather = 4
max_parallel_workers = 8
max_worker_processes = 8

# WAL
wal_buffers = 16MB
min_wal_size = 1GB
max_wal_size = 4GB
checkpoint_completion_target = 0.9

# Query planning
random_page_cost = 1.1  # For SSD
effective_io_concurrency = 200
default_statistics_target = 100
```

### Index Optimization

**Monitor index usage:**
```sql
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY pg_relation_size(indexrelid) DESC;
```

**Remove unused indexes:**
```sql
-- Indexes with zero scans and > 1MB size
SELECT 
    'DROP INDEX CONCURRENTLY ' || indexrelid::regclass || ';' as drop_statement
FROM pg_stat_user_indexes
WHERE idx_scan = 0
AND pg_relation_size(indexrelid) > 1048576;
```

---

## Summary

This database schema provides:

✅ **Complete data isolation** via RLS
✅ **High performance** with proper indexing and partitioning
✅ **Scalability** from 0 to 10,000+ stores
✅ **Reliability** with PITR and automated backups
✅ **Security** with SSL, audit logging, and role-based access
✅ **Observability** with comprehensive monitoring

**Next Steps:**
1. Review `database-schema.sql` for complete SQL implementation
2. Set up HikariCP and PgBouncer with configurations above
3. Implement monitoring queries in application
4. Schedule automated backups
5. Test disaster recovery procedure
