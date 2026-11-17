package fr.aplose.erp.tenant.service;

import fr.aplose.erp.tenant.context.TenantContext;
import fr.aplose.erp.tenant.entity.Tenant;
import fr.aplose.erp.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Settings of the current company (currency, language, time zone), used as defaults instead of
 * hard-coded French values: a company of any country works in its own currency by default.
 */
@Service
@RequiredArgsConstructor
public class CurrentTenantService {

    private static final String FALLBACK_CURRENCY = "EUR";

    private final TenantRepository tenantRepository;

    public String defaultCurrency() {
        String tid = TenantContext.getCurrentTenantId();
        if (tid == null) return FALLBACK_CURRENCY;
        return tenantRepository.findById(tid)
                .map(Tenant::getDefaultCurrency)
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .orElse(FALLBACK_CURRENCY);
    }

    /** The given currency, or the company's default currency when none is given. */
    public String currencyOrDefault(String currencyCode) {
        return currencyCode != null && !currencyCode.isBlank() ? currencyCode.trim().toUpperCase() : defaultCurrency();
    }
}
