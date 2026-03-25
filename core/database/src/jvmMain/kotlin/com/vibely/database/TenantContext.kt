package com.vibely.database

import com.vibely.domain.tenant.OrganizationId
import com.vibely.domain.tenant.StoreId
import com.vibely.domain.tenant.UserId

/**
 * Carries tenant identity context for a single database transaction.
 *
 * Created per-request from validated JWT claims and passed to
 * [DatabaseFactory.withTenantContext]. Discarded after the transaction returns.
 *
 * All three RLS session variables are set from this context:
 * - `app.current_organization_id` — always set
 * - `app.current_user_id` — always set (required for audit trail)
 * - `app.current_store_id` — set only when [storeId] is non-null
 *
 * @property organizationId Top-level tenant identifier (required).
 * @property storeId Store-level isolation boundary. `null` for org-level operations
 *   (e.g., reading organisation metadata). When null, `app.current_store_id` is
 *   NOT set — setting it to an empty string would silently corrupt RLS.
 * @property userId Authenticated user identifier, required for the audit log trigger.
 */
data class TenantContext(
    val organizationId: OrganizationId,
    val storeId: StoreId?,
    val userId: UserId,
)
