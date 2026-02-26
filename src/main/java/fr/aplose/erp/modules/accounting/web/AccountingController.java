package fr.aplose.erp.modules.accounting.web;

import fr.aplose.erp.modules.accounting.entity.AccountingEntry;
import fr.aplose.erp.modules.accounting.entity.AccountingJournal;
import fr.aplose.erp.modules.accounting.service.AccountingService;
import fr.aplose.erp.security.service.ErpUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/accounting")
@PreAuthorize("hasAuthority('ACCOUNTING_READ')")
@RequiredArgsConstructor
public class AccountingController {

    private final AccountingService accountingService;

    @GetMapping
    public String index(Model model) {
        List<AccountingJournal> journals = accountingService.findAllJournals();
        model.addAttribute("journals", journals);
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "entryDate"));
        model.addAttribute("recentEntries", accountingService.findEntries(pageable));
        return "modules/accounting/index";
    }

    @GetMapping("/accounts")
    public String chartOfAccounts(Model model) {
        model.addAttribute("accounts", accountingService.findAllAccounts());
        return "modules/accounting/account-list";
    }

    @GetMapping("/entries")
    public String entries(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) Long journalId,
                          Model model) {
        var pageable = PageRequest.of(page, 25, Sort.by(Sort.Direction.DESC, "entryDate"));
        var entries = journalId != null
                ? accountingService.findEntriesByJournal(journalId, pageable)
                : accountingService.findEntries(pageable);
        model.addAttribute("entries", entries);
        model.addAttribute("journals", accountingService.findAllJournals());
        model.addAttribute("journalId", journalId);
        return "modules/accounting/entry-list";
    }

    @GetMapping("/entries/{id}")
    public String entryDetail(@PathVariable Long id, Model model) {
        AccountingEntry entry = accountingService.findEntryById(id);
        model.addAttribute("entry", entry);
        return "modules/accounting/entry-detail";
    }

    @GetMapping("/entries/new")
    @PreAuthorize("hasAuthority('ACCOUNTING_CREATE')")
    public String newEntryForm(Model model) {
        AccountingEntry entry = new AccountingEntry();
        entry.setEntryDate(LocalDate.now());
        model.addAttribute("entry", entry);
        model.addAttribute("journals", accountingService.findAllJournals());
        model.addAttribute("accounts", accountingService.findActiveAccounts());
        return "modules/accounting/entry-form";
    }

    @PostMapping("/entries")
    @PreAuthorize("hasAuthority('ACCOUNTING_CREATE')")
    public String createEntry(@ModelAttribute AccountingEntry entry,
                              @RequestParam(required = false) Long journalId,
                              @RequestParam(required = false) List<Long> accountIds,
                              @RequestParam(required = false) List<java.math.BigDecimal> debits,
                              @RequestParam(required = false) List<java.math.BigDecimal> credits,
                              @AuthenticationPrincipal ErpUserDetails user,
                              RedirectAttributes redirectAttributes) {
        if (journalId == null) {
            redirectAttributes.addFlashAttribute("error", "Journal is required.");
            return "redirect:/accounting/entries/new";
        }
        entry.setJournal(accountingService.findJournalById(journalId));
        if (accountIds != null && !accountIds.isEmpty()) {
            for (int i = 0; i < accountIds.size(); i++) {
                if (accountIds.get(i) == null) continue;
                var line = new fr.aplose.erp.modules.accounting.entity.AccountingEntryLine();
                line.setAccount(accountingService.findAccountById(accountIds.get(i)));
                line.setDebit(debits != null && i < debits.size() && debits.get(i) != null ? debits.get(i) : java.math.BigDecimal.ZERO);
                line.setCredit(credits != null && i < credits.size() && credits.get(i) != null ? credits.get(i) : java.math.BigDecimal.ZERO);
                line.setSortOrder(entry.getLines().size());
                entry.getLines().add(line);
            }
        }
        accountingService.saveEntry(entry, user.getUserId());
        redirectAttributes.addFlashAttribute("message", "Entry created.");
        return "redirect:/accounting/entries";
    }
}
