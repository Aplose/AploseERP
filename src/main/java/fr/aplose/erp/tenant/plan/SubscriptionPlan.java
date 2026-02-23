package fr.aplose.erp.tenant.plan;

import lombok.Getter;

/**
 * Subscription plans for tenant registration.
 * Used on the public pricing page and during signup.
 */
@Getter
public enum SubscriptionPlan {

    DISCOVERY("discovery", "0", 1, "pricing.discovery.features"),
    PREMIUM("premium", "9", 10, "pricing.premium.features"),
    PRIVILEGE("privilege", "19", -1, "pricing.privilege.features");

    private final String code;
    private final String pricePerUserPerMonthEur;
    private final int maxUsers; // -1 = unlimited
    private final String featuresMessageKey;

    SubscriptionPlan(String code, String pricePerUserPerMonthEur, int maxUsers, String featuresMessageKey) {
        this.code = code;
        this.pricePerUserPerMonthEur = pricePerUserPerMonthEur;
        this.maxUsers = maxUsers;
        this.featuresMessageKey = featuresMessageKey;
    }

    public boolean isFree() {
        return "0".equals(pricePerUserPerMonthEur);
    }

    public static SubscriptionPlan fromCode(String code) {
        if (code == null || code.isBlank()) return DISCOVERY;
        for (SubscriptionPlan p : values()) {
            if (p.getCode().equalsIgnoreCase(code.trim())) return p;
        }
        return DISCOVERY;
    }
}
