package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import fr.aplose.erp.modules.catalog.entity.Product;
import fr.aplose.erp.modules.catalog.entity.ProductCategory;
import fr.aplose.erp.modules.catalog.repository.ProductCategoryRepository;
import fr.aplose.erp.modules.catalog.repository.ProductRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DolibarrProductImporter {

    private final DolibarrApiClient client;
    private final DolibarrImportLogService logService;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final DolibarrMappingService mappingService;

    public void importProducts(Long runId) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return;

        try {
            List<Map<String, Object>> catList = client.getAll("categories", Map.of("sortfield", "t.rowid", "type", "product"));
            if (catList != null && !catList.isEmpty()) {
                for (Map<String, Object> m : catList) {
                    Long doliId = DolibarrImportHelper.getDolibarrId(m);
                    if (doliId == null || mappingService.isImported(tenantId, "categories", doliId)) continue;
                    String code = DolibarrImportHelper.getString(m, "code");
                    if (code == null || code.isBlank()) code = "CAT-" + doliId;
                    var existingCat = categoryRepository.findByCodeAndTenantId(code, tenantId);
                    if (existingCat.isPresent()) {
                        mappingService.record(tenantId, runId, "categories", doliId, "PRODUCT_CATEGORY", existingCat.get().getId());
                        continue;
                    }
                    ProductCategory cat = new ProductCategory();
                    cat.setCode(code);
                    cat.setName(DolibarrImportHelper.getString(m, "label") != null ? DolibarrImportHelper.getString(m, "label") : code);
                    cat.setDescription(DolibarrImportHelper.getString(m, "description"));
                    ProductCategory saved = categoryRepository.save(cat);
                    mappingService.record(tenantId, runId, "categories", doliId, "PRODUCT_CATEGORY", saved.getId());
                }
            }
        } catch (Exception e) {
            logService.logWarn(runId, "PRODUCTS", "categories", "Could not load categories: " + e.getMessage());
        }

        List<Map<String, Object>> list = client.getAll("products", Map.of("sortfield", "t.rowid"));
        if (list == null) return;

        int alreadyImported = 0;
        for (Map<String, Object> m : list) {
            Long doliId = DolibarrImportHelper.getDolibarrId(m);
            if (doliId == null) continue;
            if (mappingService.isImported(tenantId, "products", doliId)) {
                alreadyImported++;
                continue;
            }

            try {
                String code = DolibarrImportHelper.getString(m, "ref");
                if (code == null || code.isBlank()) code = "PRD-" + doliId;
                var existing = productRepository.findByCodeAndTenantIdAndDeletedAtIsNull(code, tenantId);
                if (existing.isPresent()) {
                    mappingService.record(tenantId, runId, "products", doliId, "PRODUCT", existing.get().getId());
                    logService.logInfo(runId, "PRODUCTS", "Dolibarr id " + doliId + " rattaché à l'existant (code " + code + ")");
                    continue;
                }

                String name = DolibarrImportHelper.getString(m, "label");
                if (name == null) name = DolibarrImportHelper.getString(m, "name");
                if (name == null || name.isBlank()) name = code;

                Product p = new Product();
                p.setCode(code);
                p.setName(name);
                p.setDescription(DolibarrImportHelper.getString(m, "description"));
                String type = DolibarrImportHelper.getString(m, "type");
                if (type != null && (type.equals("0") || type.equals("1"))) p.setType("PRODUCT");
                else if (type != null && type.equals("1")) p.setType("SERVICE");
                else p.setType("PRODUCT");
                p.setUnitOfMeasure(DolibarrImportHelper.getString(m, "unit"));
                BigDecimal price = DolibarrImportHelper.getBigDecimal(m, "price");
                p.setSalePrice(price != null ? price : BigDecimal.ZERO);
                BigDecimal priceBuy = DolibarrImportHelper.getBigDecimal(m, "price_buy");
                p.setPurchasePrice(priceBuy != null ? priceBuy : BigDecimal.ZERO);
                p.setCurrencyCode(DolibarrImportHelper.getString(m, "currency_code") != null ? DolibarrImportHelper.getString(m, "currency_code") : "EUR");
                BigDecimal tva = DolibarrImportHelper.getBigDecimal(m, "tva_tx");
                // Dolibarr stores 20 for 20 %, AploseERP stores rates as fractions (0.20)
                p.setVatRate(tva != null ? tva.movePointLeft(2) : BigDecimal.ZERO);
                p.setSellable(DolibarrImportHelper.getBoolean(m, "tosell"));
                p.setPurchasable(DolibarrImportHelper.getBoolean(m, "tobuy"));
                p.setBarcode(DolibarrImportHelper.getString(m, "barcode"));
                p.setNotes(DolibarrImportHelper.getString(m, "note"));
                p.setActive(true);

                Long fkCat = DolibarrImportHelper.getLong(m, "fk_cat");
                if (fkCat != null) {
                    Optional<Long> aploseCatId = mappingService.find(tenantId, "categories", fkCat);
                    aploseCatId.flatMap(categoryRepository::findById)
                            .filter(c -> tenantId.equals(c.getTenantId()))
                            .ifPresent(p::setCategory);
                }

                Product saved = productRepository.save(p);

                mappingService.record(tenantId, runId, "products", doliId, "PRODUCT", saved.getId());
            } catch (Exception e) {
                logService.logError(runId, "PRODUCTS", String.valueOf(doliId), e.getMessage(), null);
            }
        }
        logService.logInfo(runId, "PRODUCTS", alreadyImported + " produit(s) déjà importé(s) lors d'un import précédent");
    }
}
