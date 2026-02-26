package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import fr.aplose.erp.dolibarr.entity.DolibarrImportMapping;
import fr.aplose.erp.dolibarr.repository.DolibarrImportMappingRepository;
import fr.aplose.erp.modules.contact.entity.Contact;
import fr.aplose.erp.modules.contact.entity.ContactThirdPartyLink;
import fr.aplose.erp.modules.contact.repository.ContactRepository;
import fr.aplose.erp.modules.contact.repository.ContactThirdPartyLinkRepository;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DolibarrContactImporter {

    private final DolibarrApiClient client;
    private final DolibarrImportLogService logService;
    private final ContactRepository contactRepository;
    private final ContactThirdPartyLinkRepository linkRepository;
    private final ThirdPartyRepository thirdPartyRepository;
    private final DolibarrImportMappingRepository mappingRepository;

    public void importContacts(Long runId) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return;

        List<Map<String, Object>> list = client.getList("contacts", Map.of("limit", "10000", "sortfield", "rowid"));
        if (list == null) return;

        for (Map<String, Object> m : list) {
            Long doliId = DolibarrImportHelper.getDolibarrId(m);
            if (doliId == null) continue;

            try {
                String firstName = DolibarrImportHelper.getString(m, "firstname");
                if (firstName == null) firstName = DolibarrImportHelper.getString(m, "prenom");
                if (firstName == null || firstName.isBlank()) firstName = "?";
                String lastName = DolibarrImportHelper.getString(m, "lastname");
                if (lastName == null) lastName = DolibarrImportHelper.getString(m, "nom");

                Contact c = new Contact();
                c.setFirstName(firstName);
                c.setLastName(lastName);
                c.setCivility(DolibarrImportHelper.getString(m, "civility"));
                c.setJobTitle(DolibarrImportHelper.getString(m, "poste"));
                c.setDepartment(DolibarrImportHelper.getString(m, "department"));
                c.setEmail(DolibarrImportHelper.getString(m, "email"));
                c.setPhone(DolibarrImportHelper.getString(m, "phone"));
                c.setPhoneMobile(DolibarrImportHelper.getString(m, "phone_mobile"));
                c.setFax(DolibarrImportHelper.getString(m, "fax"));
                c.setAddressLine1(DolibarrImportHelper.getString(m, "address"));
                c.setAddressLine2(DolibarrImportHelper.getString(m, "address2"));
                c.setCity(DolibarrImportHelper.getString(m, "town"));
                c.setStateProvince(DolibarrImportHelper.getString(m, "state"));
                c.setPostalCode(DolibarrImportHelper.getString(m, "zip"));
                c.setCountryCode(DolibarrImportHelper.getString(m, "country_code"));
                c.setNotes(DolibarrImportHelper.getString(m, "note_private"));
                c.setStatus("ACTIVE");

                Contact saved = contactRepository.save(c);

                DolibarrImportMapping mapping = new DolibarrImportMapping();
                mapping.setTenantId(tenantId);
                mapping.setImportRunId(runId);
                mapping.setDolibarrEntity("contacts");
                mapping.setDolibarrId(doliId);
                mapping.setAploseEntity("CONTACT");
                mapping.setAploseId(saved.getId());
                mappingRepository.save(mapping);

                Long fkSoc = DolibarrImportHelper.getLong(m, "socid");
                if (fkSoc != null) {
                    Optional<Long> aploseTpId = mappingRepository.findAploseId(tenantId, runId, "thirdparties", fkSoc);
                    aploseTpId.flatMap(thirdPartyRepository::findById)
                            .filter(tp -> tenantId.equals(tp.getTenantId()))
                            .ifPresent(tp -> {
                                ContactThirdPartyLink link = new ContactThirdPartyLink();
                                link.setTenantId(tenantId);
                                link.setContact(saved);
                                link.setThirdParty(tp);
                                link.setLinkTypeCode("SALARIE");
                                linkRepository.save(link);
                            });
                }
            } catch (Exception e) {
                logService.logError(runId, "CONTACTS", String.valueOf(doliId), e.getMessage(), null);
            }
        }
    }
}
