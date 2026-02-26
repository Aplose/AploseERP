package fr.aplose.erp.dictionary.service;

import fr.aplose.erp.dictionary.DictionaryType;
import fr.aplose.erp.dictionary.entity.DictionaryItem;
import fr.aplose.erp.dictionary.repository.DictionaryItemRepository;
import fr.aplose.erp.modules.catalog.entity.Currency;
import fr.aplose.erp.modules.catalog.repository.CurrencyRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final DictionaryItemRepository repository;
    private final CurrencyRepository currencyRepository;

    @Transactional(readOnly = true)
    public List<DictionaryItem> findByType(String type) {
        String tid = TenantContext.getCurrentTenantId();
        return repository.findByTenantIdAndTypeAndActiveTrueOrderBySortOrderAscCodeAsc(tid, type);
    }

    @Transactional(readOnly = true)
    public List<DictionaryItem> findByTypeIncludeInactive(String type) {
        String tid = TenantContext.getCurrentTenantId();
        return repository.findByTenantIdAndTypeOrderBySortOrderAscCodeAsc(tid, type);
    }

    @Transactional(readOnly = true)
    public List<DictionaryItem> findAllForTenantByType(String type) {
        return repository.findByTenantIdAndTypeOrderBySortOrderAscCodeAsc(
                TenantContext.getCurrentTenantId(), type);
    }

    @Transactional(readOnly = true)
    public Optional<DictionaryItem> findById(Long id) {
        String tid = TenantContext.getCurrentTenantId();
        return repository.findById(id)
                .filter(item -> tid.equals(item.getTenantId()));
    }

    @Transactional
    public DictionaryItem create(String type, String code, String label, Integer sortOrder, Boolean active) {
        String tid = TenantContext.getCurrentTenantId();
        if (repository.existsByTenantIdAndTypeAndCode(tid, type, code)) {
            throw new IllegalStateException("Dictionary item already exists: " + type + "/" + code);
        }
        DictionaryItem item = new DictionaryItem();
        item.setTenantId(tid);
        item.setType(type);
        item.setCode(code != null ? code.trim().toUpperCase() : "");
        item.setLabel(label != null ? label.trim() : "");
        item.setSortOrder(sortOrder != null ? sortOrder.shortValue() : 0);
        item.setActive(active != null ? active : true);
        DictionaryItem saved = repository.save(item);
        if (DictionaryType.CURRENCY.equals(type)) {
            ensureCurrencyInCatalog(saved.getCode(), saved.getLabel());
        }
        return saved;
    }

    @Transactional
    public DictionaryItem update(Long id, String code, String label, Integer sortOrder, Boolean active) {
        DictionaryItem item = findById(id).orElseThrow(() -> new IllegalArgumentException("Dictionary item not found: " + id));
        String tid = TenantContext.getCurrentTenantId();
        String newCode = code != null ? code.trim().toUpperCase() : item.getCode();
        if (repository.existsByTenantIdAndTypeAndCodeAndIdNot(tid, item.getType(), newCode, id)) {
            throw new IllegalStateException("Dictionary item already exists: " + item.getType() + "/" + newCode);
        }
        item.setCode(newCode);
        item.setLabel(label != null ? label.trim() : item.getLabel());
        if (sortOrder != null) item.setSortOrder(sortOrder.shortValue());
        if (active != null) item.setActive(active);
        DictionaryItem saved = repository.save(item);
        if (DictionaryType.CURRENCY.equals(item.getType())) {
            ensureCurrencyInCatalog(saved.getCode(), saved.getLabel());
        }
        return saved;
    }

    /** Ensures the currency code exists in the global currencies table (for FK integrity). */
    private void ensureCurrencyInCatalog(String code, String name) {
        if (code == null || code.isBlank()) return;
        String c = code.trim().toUpperCase();
        if (currencyRepository.findById(c).isEmpty()) {
            Currency cur = new Currency();
            cur.setCode(c);
            cur.setName(name != null && !name.isBlank() ? name.trim() : c);
            cur.setSymbol(c);
            cur.setDecimalPlaces((short) 2);
            cur.setActive(true);
            currencyRepository.save(cur);
        }
    }

    @Transactional
    public void delete(Long id) {
        DictionaryItem item = findById(id).orElseThrow(() -> new IllegalArgumentException("Dictionary item not found: " + id));
        repository.delete(item);
    }

    /**
     * Create or update a dictionary item for the given tenant (for import use case).
     * If an item with the same type and code exists, it is updated; otherwise created.
     *
     * @param tenantId must be non-null
     * @return the created or updated item
     */
    @Transactional
    public DictionaryItem createOrUpdateForTenant(String tenantId, String type, String code, String label, Integer sortOrder, Boolean active) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        String normalizedCode = code != null ? code.trim().toUpperCase() : "";
        String normalizedLabel = label != null ? label.trim() : "";
        short order = sortOrder != null ? sortOrder.shortValue() : 0;
        boolean isActive = active != null ? active : true;

        Optional<DictionaryItem> existing = repository.findByTenantIdAndTypeAndCode(tenantId, type, normalizedCode);
        if (existing.isPresent()) {
            DictionaryItem item = existing.get();
            item.setLabel(normalizedLabel);
            item.setSortOrder(order);
            item.setActive(isActive);
            DictionaryItem saved = repository.save(item);
            if (DictionaryType.CURRENCY.equals(type)) {
                ensureCurrencyInCatalog(saved.getCode(), saved.getLabel());
            }
            return saved;
        }
        DictionaryItem item = new DictionaryItem();
        item.setTenantId(tenantId);
        item.setType(type);
        item.setCode(normalizedCode);
        item.setLabel(normalizedLabel);
        item.setSortOrder(order);
        item.setActive(isActive);
        DictionaryItem saved = repository.save(item);
        if (DictionaryType.CURRENCY.equals(type)) {
            ensureCurrencyInCatalog(saved.getCode(), saved.getLabel());
        }
        return saved;
    }

    /**
     * Seed default dictionary items for a newly created tenant.
     */
    @Transactional
    public void seedDefaultsForTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) return;
        for (String type : DictionaryType.ALL) {
            if (!repository.findByTenantIdAndTypeOrderBySortOrderAscCodeAsc(tenantId, type).isEmpty()) {
                continue; // already seeded
            }
            seedDefaultsForType(tenantId, type);
        }
    }

    private void seedDefaultsForType(String tenantId, String type) {
        switch (type) {
            case DictionaryType.CIVILITY -> {
                insert(tenantId, type, "M", "M.", 1);
                insert(tenantId, type, "MME", "Mme", 2);
                insert(tenantId, type, "MLLE", "Mlle", 3);
                insert(tenantId, type, "MR", "Mr", 4);
                insert(tenantId, type, "MRS", "Mrs", 5);
                insert(tenantId, type, "MS", "Ms", 6);
            }
            case DictionaryType.COUNTRY -> {
                insert(tenantId, type, "FR", "France", 1);
                insert(tenantId, type, "BE", "Belgique", 2);
                insert(tenantId, type, "CH", "Suisse", 3);
                insert(tenantId, type, "DE", "Allemagne", 4);
                insert(tenantId, type, "ES", "Espagne", 5);
                insert(tenantId, type, "GB", "Royaume-Uni", 6);
                insert(tenantId, type, "US", "États-Unis", 7);
            }
            case DictionaryType.CURRENCY -> {
                insert(tenantId, type, "EUR", "Euro", 1);
                insert(tenantId, type, "USD", "US Dollar", 2);
                insert(tenantId, type, "GBP", "British Pound", 3);
                insert(tenantId, type, "CHF", "Swiss Franc", 4);
            }
            case DictionaryType.LEGAL_FORM -> {
                insert(tenantId, type, "SARL", "SARL", 1);
                insert(tenantId, type, "SAS", "SAS", 2);
                insert(tenantId, type, "SA", "SA", 3);
                insert(tenantId, type, "EURL", "EURL", 4);
                insert(tenantId, type, "EI", "Entreprise individuelle", 5);
            }
            case DictionaryType.PAYMENT_METHOD -> {
                insert(tenantId, type, "BANK", "Virement", 1);
                insert(tenantId, type, "CHECK", "Chèque", 2);
                insert(tenantId, type, "CARD", "Carte bancaire", 3);
            }
            default -> {}
        }
    }

    private void insert(String tenantId, String type, String code, String label, int order) {
        if (repository.existsByTenantIdAndTypeAndCode(tenantId, type, code)) return;
        DictionaryItem item = new DictionaryItem();
        item.setTenantId(tenantId);
        item.setType(type);
        item.setCode(code);
        item.setLabel(label);
        item.setSortOrder((short) order);
        item.setActive(true);
        repository.save(item);
    }
}
