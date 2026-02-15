package fr.aplose.erp.modules.catalog.service;

import fr.aplose.erp.modules.catalog.entity.ProductCategory;
import fr.aplose.erp.modules.catalog.repository.ProductCategoryRepository;
import fr.aplose.erp.modules.catalog.web.dto.ProductCategoryDto;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCategoryService {

    private final ProductCategoryRepository repo;

    @Transactional(readOnly = true)
    public List<ProductCategory> findAll() {
        return repo.findByTenantIdOrderBySortOrderAscNameAsc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public ProductCategory findById(Long id) {
        return repo.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
    }

    @Transactional
    public ProductCategory create(ProductCategoryDto dto) {
        String tid = TenantContext.getCurrentTenantId();
        repo.findByCodeAndTenantId(dto.getCode().trim().toUpperCase(), tid)
            .ifPresent(c -> { throw new IllegalStateException("Category code already in use: " + dto.getCode()); });

        ProductCategory cat = new ProductCategory();
        applyDto(cat, dto, tid);
        return repo.save(cat);
    }

    @Transactional
    public ProductCategory update(Long id, ProductCategoryDto dto) {
        ProductCategory cat = findById(id);
        String tid = TenantContext.getCurrentTenantId();
        repo.findByCodeAndTenantId(dto.getCode().trim().toUpperCase(), tid)
            .filter(c -> !c.getId().equals(id))
            .ifPresent(c -> { throw new IllegalStateException("Category code already in use: " + dto.getCode()); });
        applyDto(cat, dto, tid);
        return repo.save(cat);
    }

    @Transactional
    public void delete(Long id) {
        ProductCategory cat = findById(id);
        repo.delete(cat);
    }

    private void applyDto(ProductCategory cat, ProductCategoryDto dto, String tid) {
        cat.setCode(dto.getCode().trim().toUpperCase());
        cat.setName(dto.getName());
        cat.setDescription(dto.getDescription());
        cat.setSortOrder(dto.getSortOrder());
        if (dto.getParentId() != null) {
            repo.findByIdAndTenantId(dto.getParentId(), tid).ifPresent(cat::setParent);
        } else {
            cat.setParent(null);
        }
    }
}
