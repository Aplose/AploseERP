package fr.aplose.erp.modules.commerce.web;

import fr.aplose.erp.modules.commerce.entity.BusinessContract;
import fr.aplose.erp.modules.commerce.entity.Proposal;
import fr.aplose.erp.modules.commerce.service.BusinessContractService;
import fr.aplose.erp.modules.commerce.service.ProposalService;
import fr.aplose.erp.modules.contact.repository.ContactRepository;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class BusinessContractController {

    private final BusinessContractService contractService;
    private final ThirdPartyRepository thirdPartyRepository;
    private final ContactRepository contactRepository;
    private final ProposalService proposalService;

    @GetMapping
    @PreAuthorize("hasAuthority('BUSINESS_CONTRACT_READ')")
    public String list(@RequestParam(required = false) Long thirdPartyId, Model model) {
        List<BusinessContract> contracts = thirdPartyId != null
                ? contractService.findByThirdParty(thirdPartyId)
                : contractService.findAll();
        model.addAttribute("contracts", contracts);
        model.addAttribute("thirdPartyId", thirdPartyId);
        if (thirdPartyId != null) {
            thirdPartyRepository.findByIdAndTenantIdAndDeletedAtIsNull(thirdPartyId, TenantContext.getCurrentTenantId())
                    .ifPresent(tp -> model.addAttribute("thirdParty", tp));
        }
        return "modules/commerce/contract-list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('BUSINESS_CONTRACT_CREATE')")
    public String newForm(@RequestParam(required = false) Long thirdPartyId,
                          @RequestParam(required = false) Long proposalId,
                          Model model) {
        BusinessContract contract = new BusinessContract();
        if (thirdPartyId != null) {
            thirdPartyRepository.findByIdAndTenantIdAndDeletedAtIsNull(thirdPartyId, TenantContext.getCurrentTenantId())
                    .ifPresent(contract::setThirdParty);
        }
        if (proposalId != null) {
            try {
                Proposal p = proposalService.findById(proposalId);
                contract.setProposal(p);
                if (contract.getThirdParty() == null && p.getThirdParty() != null) contract.setThirdParty(p.getThirdParty());
            } catch (Exception ignored) {}
        }
        model.addAttribute("contract", contract);
        addFormRefs(model);
        return "modules/commerce/contract-form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BUSINESS_CONTRACT_CREATE')")
    public String create(@ModelAttribute @Valid BusinessContract contract,
                         @RequestParam(required = false) Long thirdPartyId,
                         @RequestParam(required = false) Long contactId,
                         @RequestParam(required = false) Long proposalId,
                         BindingResult result, Model model, RedirectAttributes ra) {
        if (thirdPartyId == null) result.rejectValue("thirdParty", "required", "Third party is required");
        if (result.hasErrors()) {
            addFormRefs(model);
            return "modules/commerce/contract-form";
        }
        thirdPartyRepository.findByIdAndTenantIdAndDeletedAtIsNull(thirdPartyId, TenantContext.getCurrentTenantId()).ifPresent(contract::setThirdParty);
        if (contactId != null) contactRepository.findByIdAndTenantIdAndDeletedAtIsNull(contactId, TenantContext.getCurrentTenantId()).ifPresent(contract::setContact);
        if (proposalId != null) {
            try {
                contract.setProposal(proposalService.findById(proposalId));
            } catch (Exception ignored) {}
        }
        contractService.save(contract);
        ra.addFlashAttribute("message", "Contract created.");
        return "redirect:/contracts";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BUSINESS_CONTRACT_READ')")
    public String detail(@PathVariable Long id, Model model) {
        return contractService.findById(id)
                .map(c -> {
                    model.addAttribute("contract", c);
                    return "modules/commerce/contract-detail";
                })
                .orElse("redirect:/contracts");
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('BUSINESS_CONTRACT_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        return contractService.findById(id)
                .map(c -> {
                    model.addAttribute("contract", c);
                    addFormRefs(model);
                    return "modules/commerce/contract-form";
                })
                .orElse("redirect:/contracts");
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('BUSINESS_CONTRACT_UPDATE')")
    public String update(@PathVariable Long id,
                          @ModelAttribute @Valid BusinessContract contract,
                          @RequestParam(required = false) Long thirdPartyId,
                          @RequestParam(required = false) Long contactId,
                          @RequestParam(required = false) Long proposalId,
                          BindingResult result, Model model, RedirectAttributes ra) {
        BusinessContract existing = contractService.findById(id).orElse(null);
        if (existing == null) return "redirect:/contracts";
        if (result.hasErrors()) {
            contract.setId(id);
            model.addAttribute("contract", contract);
            addFormRefs(model);
            return "modules/commerce/contract-form";
        }
        contract.setId(id);
        contract.setTenantId(existing.getTenantId());
        contract.setCreatedAt(existing.getCreatedAt());
        if (thirdPartyId != null) thirdPartyRepository.findByIdAndTenantIdAndDeletedAtIsNull(thirdPartyId, TenantContext.getCurrentTenantId()).ifPresent(contract::setThirdParty);
        if (contactId != null) contactRepository.findByIdAndTenantIdAndDeletedAtIsNull(contactId, TenantContext.getCurrentTenantId()).ifPresent(contract::setContact);
        else contract.setContact(null);
        if (proposalId != null) {
            try { contract.setProposal(proposalService.findById(proposalId)); } catch (Exception e) { contract.setProposal(null); }
        } else contract.setProposal(null);
        contractService.save(contract);
        ra.addFlashAttribute("message", "Contract updated.");
        return "redirect:/contracts/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('BUSINESS_CONTRACT_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        contractService.delete(id);
        ra.addFlashAttribute("message", "Contract deleted.");
        return "redirect:/contracts";
    }

    private void addFormRefs(Model model) {
        String tid = TenantContext.getCurrentTenantId();
        model.addAttribute("thirdParties", thirdPartyRepository.findByTenantIdAndDeletedAtIsNull(tid, PageRequest.of(0, 500, Sort.by("name"))).getContent());
        model.addAttribute("contacts", contactRepository.findByTenantIdAndDeletedAtIsNull(tid, PageRequest.of(0, 500, Sort.by("lastName"))).getContent());
        model.addAttribute("proposals", proposalService.findAll(null, null, PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "dateIssued"))).getContent());
    }
}
