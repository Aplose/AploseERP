package fr.aplose.erp.dictionary.repository;

import fr.aplose.erp.dictionary.entity.DictionaryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DictionaryItemRepository extends JpaRepository<DictionaryItem, Long> {

    List<DictionaryItem> findByTenantIdAndTypeAndActiveTrueOrderBySortOrderAscCodeAsc(
            String tenantId, String type);

    List<DictionaryItem> findByTenantIdAndTypeOrderBySortOrderAscCodeAsc(
            String tenantId, String type);

    Optional<DictionaryItem> findByTenantIdAndTypeAndCode(
            String tenantId, String type, String code);

    boolean existsByTenantIdAndTypeAndCode(String tenantId, String type, String code);

    boolean existsByTenantIdAndTypeAndCodeAndIdNot(
            String tenantId, String type, String code, Long id);
}
