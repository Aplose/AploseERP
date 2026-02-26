package fr.aplose.erp.modules.commerce.repository;

import fr.aplose.erp.modules.commerce.entity.BusinessContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessContractRepository extends JpaRepository<BusinessContract, Long> {

    List<BusinessContract> findByTenantIdOrderByStartDateDesc(String tenantId);

    List<BusinessContract> findByTenantIdAndThirdPartyIdOrderByStartDateDesc(String tenantId, Long thirdPartyId);

    Optional<BusinessContract> findByIdAndTenantId(Long id, String tenantId);
}
