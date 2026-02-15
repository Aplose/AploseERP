package fr.aplose.erp.tenant.context;

/**
 * Thread-local holder for the current tenant ID.
 * Must be set at the start of every request and cleared in a finally block.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static String getCurrentTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }
}
