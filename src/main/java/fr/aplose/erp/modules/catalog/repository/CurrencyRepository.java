package fr.aplose.erp.modules.catalog.repository;

import fr.aplose.erp.modules.catalog.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {

    List<Currency> findByActiveTrueOrderByCodeAsc();
}
