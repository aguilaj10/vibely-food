-- ============================================================================
-- Vibely POS - Complete PostgreSQL Database Schema
-- Multi-tenant SaaS with Row-Level Security (RLS)
-- ============================================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ENUMS
-- ============================================================================

CREATE TYPE user_role AS ENUM ('OWNER', 'MANAGER', 'CASHIER', 'WAITER', 'KITCHEN', 'VIEWER');
CREATE TYPE order_status AS ENUM ('DRAFT', 'PENDING', 'PREPARING', 'READY', 'COMPLETED', 'CANCELLED');
CREATE TYPE payment_method_type AS ENUM ('CASH', 'CARD', 'DIGITAL_WALLET', 'BANK_TRANSFER');
CREATE TYPE payment_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED');
CREATE TYPE stock_unit AS ENUM ('PIECE', 'KG', 'LITER', 'BOX', 'DOZEN');
CREATE TYPE adjustment_reason AS ENUM ('SALE', 'RESTOCK', 'DAMAGE', 'THEFT', 'CORRECTION', 'RETURN');
CREATE TYPE table_status AS ENUM ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'CLEANING');

-- ============================================================================
-- CORE TABLES
-- ============================================================================

-- Organizations (top-level tenant)
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_organizations_slug ON organizations(slug);

-- Stores (isolation boundary)
CREATE TABLE stores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(255),
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(organization_id, name)
);

CREATE INDEX idx_stores_organization ON stores(organization_id);

-- Enable RLS
ALTER TABLE stores ENABLE ROW LEVEL SECURITY;

CREATE POLICY stores_isolation ON stores
    USING (organization_id::text = current_setting('app.current_organization_id', true));

-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role user_role NOT NULL DEFAULT 'CASHIER',
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(organization_id, email)
);

CREATE INDEX idx_users_organization_store ON users(organization_id, store_id);
CREATE INDEX idx_users_email ON users(email);

ALTER TABLE users ENABLE ROW LEVEL SECURITY;

CREATE POLICY users_isolation ON users
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );


-- ============================================================================
-- PRODUCT TABLES
-- ============================================================================

-- Categories
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(organization_id, store_id, name)
);

CREATE INDEX idx_categories_org_store ON categories(organization_id, store_id);
CREATE INDEX idx_categories_parent ON categories(parent_id);

ALTER TABLE categories ENABLE ROW LEVEL SECURITY;

CREATE POLICY categories_isolation ON categories
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );

-- Products
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    sku VARCHAR(100),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price_amount DECIMAL(10, 2) NOT NULL,
    price_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    cost_amount DECIMAL(10, 2),
    cost_currency VARCHAR(3) DEFAULT 'USD',
    tax_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT true,
    image_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(organization_id, store_id, sku)
);

CREATE INDEX idx_products_org_store ON products(organization_id, store_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_sku ON products(sku);

ALTER TABLE products ENABLE ROW LEVEL SECURITY;

CREATE POLICY products_isolation ON products
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );

-- ============================================================================
-- ORDER TABLES
-- ============================================================================

-- Orders (partitioned by created_at)
CREATE TABLE orders (
    id UUID NOT NULL,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    table_id UUID REFERENCES restaurant_tables(id) ON DELETE SET NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    order_number VARCHAR(50) NOT NULL,
    status order_status NOT NULL DEFAULT 'DRAFT',
    subtotal_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE INDEX idx_orders_org_store ON orders(organization_id, store_id, created_at DESC);
CREATE INDEX idx_orders_status ON orders(status, created_at DESC);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_customer ON orders(customer_id);

ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

CREATE POLICY orders_isolation ON orders
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );

-- Create initial partitions (monthly)
CREATE TABLE orders_2026_03 PARTITION OF orders
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE TABLE orders_2026_04 PARTITION OF orders
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

CREATE TABLE orders_2026_05 PARTITION OF orders
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

-- Order Items
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL,
    unit_price_amount DECIMAL(10, 2) NOT NULL,
    unit_price_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    subtotal_amount DECIMAL(10, 2) NOT NULL,
    tax_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(10, 2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);
CREATE INDEX idx_order_items_org_store ON order_items(organization_id, store_id);

ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;

CREATE POLICY order_items_isolation ON order_items
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );


-- Order Events (for event sourcing, partitioned)
CREATE TABLE order_events (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    order_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sequence_number BIGSERIAL,
    PRIMARY KEY (id, occurred_at),
    UNIQUE(order_id, sequence_number)
) PARTITION BY RANGE (occurred_at);

CREATE INDEX idx_order_events_order ON order_events(order_id, sequence_number);
CREATE INDEX idx_order_events_org_store ON order_events(organization_id, store_id, occurred_at DESC);

ALTER TABLE order_events ENABLE ROW LEVEL SECURITY;

CREATE POLICY order_events_isolation ON order_events
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );

-- Create initial partitions
CREATE TABLE order_events_2026_03 PARTITION OF order_events
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE TABLE order_events_2026_04 PARTITION OF order_events
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

-- ============================================================================
-- PAYMENT TABLES
-- ============================================================================

-- Payments (partitioned)
CREATE TABLE payments (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    order_id UUID NOT NULL,
    payment_method payment_method_type NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status payment_status NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_org_store ON payments(organization_id, store_id, created_at DESC);
CREATE INDEX idx_payments_status ON payments(status);

ALTER TABLE payments ENABLE ROW LEVEL SECURITY;

CREATE POLICY payments_isolation ON payments
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );

-- Create initial partitions
CREATE TABLE payments_2026_03 PARTITION OF payments
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE TABLE payments_2026_04 PARTITION OF payments
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

-- ============================================================================
-- INVENTORY TABLES
-- ============================================================================

-- Inventory Items
CREATE TABLE inventory_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    current_quantity INTEGER NOT NULL DEFAULT 0,
    unit stock_unit NOT NULL DEFAULT 'PIECE',
    reorder_point INTEGER NOT NULL DEFAULT 10,
    reorder_quantity INTEGER NOT NULL DEFAULT 50,
    last_restocked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(organization_id, store_id, product_id)
);

CREATE INDEX idx_inventory_org_store ON inventory_items(organization_id, store_id);
CREATE INDEX idx_inventory_product ON inventory_items(product_id);
CREATE INDEX idx_inventory_low_stock ON inventory_items(current_quantity) WHERE current_quantity <= reorder_point;

ALTER TABLE inventory_items ENABLE ROW LEVEL SECURITY;

CREATE POLICY inventory_items_isolation ON inventory_items
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );

-- Inventory Movements (partitioned)
CREATE TABLE inventory_movements (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id) ON DELETE CASCADE,
    quantity_change INTEGER NOT NULL,
    reason adjustment_reason NOT NULL,
    reference_id UUID,
    reference_type VARCHAR(50),
    notes TEXT,
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE INDEX idx_inventory_movements_item ON inventory_movements(inventory_item_id, created_at DESC);
CREATE INDEX idx_inventory_movements_org_store ON inventory_movements(organization_id, store_id, created_at DESC);

ALTER TABLE inventory_movements ENABLE ROW LEVEL SECURITY;

CREATE POLICY inventory_movements_isolation ON inventory_movements
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );

-- Create initial partitions
CREATE TABLE inventory_movements_2026_03 PARTITION OF inventory_movements
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE TABLE inventory_movements_2026_04 PARTITION OF inventory_movements
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');


-- ============================================================================
-- CUSTOMER TABLES
-- ============================================================================

-- Customers
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(50),
    date_of_birth DATE,
    loyalty_points INTEGER NOT NULL DEFAULT 0,
    total_spent DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    visit_count INTEGER NOT NULL DEFAULT 0,
    last_visit_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_org_store ON customers(organization_id, store_id);
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_phone ON customers(phone);

ALTER TABLE customers ENABLE ROW LEVEL SECURITY;

CREATE POLICY customers_isolation ON customers
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );

-- ============================================================================
-- SUPPLIER TABLES
-- ============================================================================

-- Suppliers
CREATE TABLE suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    payment_terms TEXT,
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_suppliers_org_store ON suppliers(organization_id, store_id);

ALTER TABLE suppliers ENABLE ROW LEVEL SECURITY;

CREATE POLICY suppliers_isolation ON suppliers
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );

-- ============================================================================
-- TABLE MANAGEMENT
-- ============================================================================

-- Restaurant Tables
CREATE TABLE restaurant_tables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    table_number VARCHAR(20) NOT NULL,
    capacity INTEGER NOT NULL,
    status table_status NOT NULL DEFAULT 'AVAILABLE',
    location VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(organization_id, store_id, table_number)
);

CREATE INDEX idx_tables_org_store ON restaurant_tables(organization_id, store_id);
CREATE INDEX idx_tables_status ON restaurant_tables(status);

ALTER TABLE restaurant_tables ENABLE ROW LEVEL SECURITY;

CREATE POLICY restaurant_tables_isolation ON restaurant_tables
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );

-- ============================================================================
-- SHIFT MANAGEMENT
-- ============================================================================

-- Shifts
CREATE TABLE shifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMPTZ,
    opening_cash DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    closing_cash DECIMAL(10, 2),
    expected_cash DECIMAL(10, 2),
    cash_difference DECIMAL(10, 2),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shifts_org_store ON shifts(organization_id, store_id);
CREATE INDEX idx_shifts_user ON shifts(user_id, started_at DESC);
CREATE INDEX idx_shifts_started ON shifts(started_at DESC);

ALTER TABLE shifts ENABLE ROW LEVEL SECURITY;

CREATE POLICY shifts_isolation ON shifts
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );

-- ============================================================================
-- SETTINGS
-- ============================================================================

-- Store Settings
CREATE TABLE store_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    setting_key VARCHAR(100) NOT NULL,
    setting_value JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(organization_id, store_id, setting_key)
);

CREATE INDEX idx_store_settings_org_store ON store_settings(organization_id, store_id);

ALTER TABLE store_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY store_settings_isolation ON store_settings
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );

-- Tax Rates
CREATE TABLE tax_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    rate DECIMAL(5, 2) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tax_rates_org_store ON tax_rates(organization_id, store_id);

ALTER TABLE tax_rates ENABLE ROW LEVEL SECURITY;

CREATE POLICY tax_rates_isolation ON tax_rates
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );


-- ============================================================================
-- SYNC & AUDIT TABLES
-- ============================================================================

-- Sync Outbox (for reliable sync)
CREATE TABLE sync_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    operation VARCHAR(20) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_outbox_status ON sync_outbox(status, created_at);
CREATE INDEX idx_outbox_entity ON sync_outbox(entity_type, entity_id);
CREATE INDEX idx_outbox_org_store ON sync_outbox(organization_id, store_id);

ALTER TABLE sync_outbox ENABLE ROW LEVEL SECURITY;

CREATE POLICY sync_outbox_isolation ON sync_outbox
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );

-- Audit Log (partitioned)
CREATE TABLE audit_log (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    old_value JSONB,
    new_value JSONB,
    ip_address INET,
    user_agent TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);

CREATE INDEX idx_audit_log_user ON audit_log(user_id, occurred_at DESC);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id, occurred_at DESC);
CREATE INDEX idx_audit_log_org_store ON audit_log(organization_id, store_id, occurred_at DESC);

ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY audit_log_isolation ON audit_log
    USING (
        organization_id::text = current_setting('app.current_organization_id', true)
        AND store_id::text = current_setting('app.current_store_id', true)
    );

-- Create initial partitions
CREATE TABLE audit_log_2026_03 PARTITION OF audit_log
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE TABLE audit_log_2026_04 PARTITION OF audit_log
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

-- ============================================================================
-- TRIGGERS
-- ============================================================================

-- Updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to all relevant tables
CREATE TRIGGER update_organizations_updated_at BEFORE UPDATE ON organizations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_stores_updated_at BEFORE UPDATE ON stores
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_categories_updated_at BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_products_updated_at BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_order_items_updated_at BEFORE UPDATE ON order_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_inventory_items_updated_at BEFORE UPDATE ON inventory_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_suppliers_updated_at BEFORE UPDATE ON suppliers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_restaurant_tables_updated_at BEFORE UPDATE ON restaurant_tables
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_shifts_updated_at BEFORE UPDATE ON shifts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_store_settings_updated_at BEFORE UPDATE ON store_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tax_rates_updated_at BEFORE UPDATE ON tax_rates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Audit trigger function
CREATE OR REPLACE FUNCTION audit_trigger()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO audit_log (
        organization_id,
        store_id,
        user_id,
        action,
        entity_type,
        entity_id,
        old_value,
        new_value
    ) VALUES (
        COALESCE(NEW.organization_id, OLD.organization_id),
        COALESCE(NEW.store_id, OLD.store_id),
        current_setting('app.current_user_id', true)::uuid,
        TG_OP,
        TG_TABLE_NAME,
        COALESCE(NEW.id, OLD.id),
        CASE WHEN TG_OP = 'DELETE' THEN row_to_json(OLD)::jsonb ELSE NULL END,
        CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN row_to_json(NEW)::jsonb ELSE NULL END
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Apply audit trigger to sensitive tables
CREATE TRIGGER orders_audit AFTER INSERT OR UPDATE OR DELETE ON order_items
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();

CREATE TRIGGER payments_audit AFTER INSERT OR UPDATE OR DELETE ON payments
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();

CREATE TRIGGER inventory_audit AFTER INSERT OR UPDATE OR DELETE ON inventory_items
    FOR EACH ROW EXECUTE FUNCTION audit_trigger();

-- Outbox trigger function
CREATE OR REPLACE FUNCTION notify_outbox()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO sync_outbox (organization_id, store_id, entity_type, entity_id, operation, payload)
    VALUES (
        NEW.organization_id,
        NEW.store_id,
        TG_TABLE_NAME,
        NEW.id,
        TG_OP,
        row_to_json(NEW)::jsonb
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply outbox trigger to synced tables
CREATE TRIGGER products_outbox AFTER INSERT OR UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION notify_outbox();

CREATE TRIGGER inventory_outbox AFTER INSERT OR UPDATE ON inventory_items
    FOR EACH ROW EXECUTE FUNCTION notify_outbox();

-- ============================================================================
-- MATERIALIZED VIEWS
-- ============================================================================

-- Current Orders (from event sourcing)
CREATE MATERIALIZED VIEW current_orders AS
SELECT 
    order_id,
    organization_id,
    store_id,
    jsonb_build_object(
        'id', order_id,
        'items', COALESCE(jsonb_agg(items.item) FILTER (WHERE items.item IS NOT NULL), '[]'::jsonb),
        'status', status.current_status,
        'total', totals.total_amount
    ) as order_state,
    MAX(occurred_at) as last_updated
FROM order_events
LEFT JOIN LATERAL (
    SELECT event_data->'item' as item
    FROM order_events e2
    WHERE e2.order_id = order_events.order_id
    AND e2.event_type = 'ITEM_ADDED'
) items ON true
LEFT JOIN LATERAL (
    SELECT event_data->>'status' as current_status
    FROM order_events e3
    WHERE e3.order_id = order_events.order_id
    AND e3.event_type = 'STATUS_CHANGED'
    ORDER BY occurred_at DESC LIMIT 1
) status ON true
LEFT JOIN LATERAL (
    SELECT (event_data->>'total')::decimal as total_amount
    FROM order_events e4
    WHERE e4.order_id = order_events.order_id
    ORDER BY occurred_at DESC LIMIT 1
) totals ON true
GROUP BY order_id, organization_id, store_id, status.current_status, totals.total_amount;

CREATE UNIQUE INDEX idx_current_orders_id ON current_orders(order_id);
CREATE INDEX idx_current_orders_org_store ON current_orders(organization_id, store_id);

-- Refresh function
CREATE OR REPLACE FUNCTION refresh_current_orders()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY current_orders;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER order_events_refresh
AFTER INSERT ON order_events
FOR EACH STATEMENT
EXECUTE FUNCTION refresh_current_orders();

-- ============================================================================
-- MONITORING VIEWS
-- ============================================================================

-- RLS Performance Stats
CREATE VIEW rls_performance_stats AS
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    n_tup_ins as inserts,
    n_tup_upd as updates,
    n_tup_del as deletes,
    seq_scan as sequential_scans,
    idx_scan as index_scans,
    CASE 
        WHEN seq_scan + idx_scan > 0 
        THEN round(100.0 * idx_scan / (seq_scan + idx_scan), 2)
        ELSE 0 
    END as index_usage_pct
FROM pg_stat_user_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE organizations IS 'Top-level tenant entity';
COMMENT ON TABLE stores IS 'Isolation boundary for RLS - all data scoped to store';
COMMENT ON TABLE orders IS 'Partitioned by created_at for performance';
COMMENT ON TABLE order_events IS 'Event sourcing log for order state reconstruction';
COMMENT ON TABLE payments IS 'Partitioned by created_at';
COMMENT ON TABLE inventory_movements IS 'Partitioned by created_at for audit trail';
COMMENT ON TABLE audit_log IS 'Partitioned audit trail for compliance';
COMMENT ON TABLE sync_outbox IS 'Outbox pattern for reliable offline sync';

COMMENT ON POLICY stores_isolation ON stores IS 'RLS: Isolate by organization_id';
COMMENT ON POLICY users_isolation ON users IS 'RLS: Isolate by organization_id AND store_id';
COMMENT ON POLICY orders_isolation ON orders IS 'RLS: Isolate by organization_id AND store_id';

-- ============================================================================
-- END OF SCHEMA
-- ============================================================================

