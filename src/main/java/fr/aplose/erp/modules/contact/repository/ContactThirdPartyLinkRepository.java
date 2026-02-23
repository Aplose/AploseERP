package fr.aplose.erp.modules.contact.repository;

import fr.aplose.erp.modules.contact.entity.ContactThirdPartyLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactThirdPartyLinkRepository extends JpaRepository<ContactThirdPartyLink, Long> {

    List<ContactThirdPartyLink> findByContactId(Long contactId);

    List<ContactThirdPartyLink> findByThirdPartyId(Long thirdPartyId);

    void deleteByContactId(Long contactId);
}
