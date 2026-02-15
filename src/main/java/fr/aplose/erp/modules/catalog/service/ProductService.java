package fr.aplose.erp.modules.catalog.service;

import fr.aplose.erp.modules.catalog.entity.Product;
import fr.aplose.erp.modules.catalog.repository.ProductCategoryRepository;
import fr.aplose.erp.modules.catalog.repository.ProductRepository;
import fr.aplose.erp.modules.catalog.web.dto.ProductDto;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repo;
    private final ProductCategoryRepository categoryRepo;

    @Transactional(readOnly = true)
    public Page<Product> findAll(String q, String type, Long categoryId, Pageable pageable) {
        String tid = TenantContext.getCurrentTenantId();
        if (q != null && !q.isBlank()) return repo.search(tid, q, pageable);
        if (type != null && !type.isBlank()) return repo.findByTenantIdAndTypeAndDeletedAtIsNull(tid, type, pageable);
        if (categoryId != null) return repo.findByTenantIdAndCategoryIdAndDeletedAtIsNull(tid, categoryId, pageable);
        return repo.findByTenantIdAndDeletedAtIsNull(tid, pageable);
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return repo.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Product> findSellable(Pageable pageable) {
        return repo.findByTenantIdAndSellableTrueAndActiveTrueAndDeletedAtIsNull(
                TenantContext.getCurrentTenantId(), pageable);
    }

    @Transactional
    public Product create(ProductDto dto) {
        String tid = TenantContext.getCurrentTenantId();
        repo.findByCodeAndTenantIdAndDeletedAtIsNull(dto.getCode().trim().toUpperCase(), tid)
            .ifPresent(p -> { throw new IllegalStateException("Product code already in use: " + dto.getCode()); });

        Product product = new Product();
        applyDto(product, dto, tid);
        return repo.save(product);
    }

    @Transactional
    public Product update(Long id, ProductDto dto) {
        Product product = findById(id);
        String tid = TenantContext.getCurrentTenantId();
        repo.findByCodeAndTenantIdAndDeletedAtIsNull(dto.getCode().trim().toUpperCase(), tid)
            .filter(p -> !p.getId().equals(id))
            .ifPresent(p -> { throw new IllegalStateException("Product code already in use: " + dto.getCode()); });
        applyDto(product, dto, tid);
        return repo.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        product.setDeletedAt(LocalDateTime.now());
        product.setActive(false);
        repo.save(product);
    }

    @Transactional(readOnly = true)
    public long count() {
        return repo.countByTenantIdAndDeletedAtIsNull(TenantContext.getCurrentTenantId());
    }

    private void applyDto(Product p, ProductDto dto, String tid) {
        p.setCode(dto.getCode().trim().toUpperCase());
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setType(dto.getType());
        p.setUnitOfMeasure(dto.getUnitOfMeasure());
        p.setSalePrice(dto.getSalePrice());
        p.setPurchasePrice(dto.getPurchasePrice());
        p.setCurrencyCode(dto.getCurrencyCode());
        p.setVatRate(dto.getVatRate());
        p.setSellable(dto.isSellable());
        p.setPurchasable(dto.isPurchasable());
        p.setTrackStock(dto.isTrackStock());
        p.setStockAlertLevel(dto.getStockAlertLevel());
        p.setBarcode(dto.getBarcode());
        p.setNotes(dto.getNotes());
        p.setActive(dto.isActive());
        if (dto.getCategoryId() != null) {
            categoryRepo.findByIdAndTenantId(dto.getCategoryId(), tid).ifPresent(p::setCategory);
        } else {
            p.setCategory(null);
        }
    }
}
