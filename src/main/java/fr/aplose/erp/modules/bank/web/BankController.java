package fr.aplose.erp.modules.bank.web;

import fr.aplose.erp.modules.bank.entity.BankAccount;
import fr.aplose.erp.modules.bank.entity.BankMovement;
import fr.aplose.erp.modules.bank.service.BankAccountService;
import fr.aplose.erp.modules.catalog.repository.CurrencyRepository;
import fr.aplose.erp.security.service.ErpUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/bank")
@PreAuthorize("hasAuthority('BANK_READ')")
@RequiredArgsConstructor
public class BankController {

    private final BankAccountService bankAccountService;
    private final CurrencyRepository currencyRepository;

    @GetMapping
    public String index(Model model) {
        List<BankAccount> accounts = bankAccountService.findAllAccounts();
        model.addAttribute("accounts", accounts);
        return "modules/bank/account-list";
    }

    @GetMapping("/accounts/new")
    @PreAuthorize("hasAuthority('BANK_CREATE')")
    public String newAccountForm(Model model) {
        model.addAttribute("account", new BankAccount());
        model.addAttribute("currencies", currencyRepository.findByActiveTrueOrderByCodeAsc());
        return "modules/bank/account-form";
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasAuthority('BANK_CREATE')")
    public String createAccount(@Valid @ModelAttribute("account") BankAccount account,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("currencies", currencyRepository.findByActiveTrueOrderByCodeAsc());
            return "modules/bank/account-form";
        }
        bankAccountService.saveAccount(account);
        redirectAttributes.addFlashAttribute("message", "Bank account created.");
        return "redirect:/bank";
    }

    @GetMapping("/accounts/{id}")
    public String viewAccount(@PathVariable Long id, Model model,
                              @RequestParam(defaultValue = "0") int page) {
        BankAccount account = bankAccountService.findAccountById(id);
        var pageable = PageRequest.of(page, 25, Sort.by(Sort.Direction.DESC, "movementDate"));
        var movements = bankAccountService.findMovementsByAccount(id, pageable);
        java.math.BigDecimal balance = bankAccountService.getAccountBalance(id);
        model.addAttribute("account", account);
        model.addAttribute("movements", movements);
        model.addAttribute("balance", balance);
        return "modules/bank/account-detail";
    }

    @GetMapping("/accounts/{id}/edit")
    @PreAuthorize("hasAuthority('BANK_UPDATE')")
    public String editAccountForm(@PathVariable Long id, Model model) {
        BankAccount account = bankAccountService.findAccountById(id);
        model.addAttribute("account", account);
        model.addAttribute("currencies", currencyRepository.findByActiveTrueOrderByCodeAsc());
        return "modules/bank/account-form";
    }

    @PostMapping("/accounts/{id}")
    @PreAuthorize("hasAuthority('BANK_UPDATE')")
    public String updateAccount(@PathVariable Long id,
                               @Valid @ModelAttribute("account") BankAccount account,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("currencies", currencyRepository.findByActiveTrueOrderByCodeAsc());
            return "modules/bank/account-form";
        }
        BankAccount existing = bankAccountService.findAccountById(id);
        existing.setName(account.getName());
        existing.setIban(account.getIban());
        existing.setBic(account.getBic());
        existing.setCurrencyCode(account.getCurrencyCode());
        existing.setActive(account.isActive());
        bankAccountService.saveAccount(existing);
        redirectAttributes.addFlashAttribute("message", "Bank account updated.");
        return "redirect:/bank";
    }

    @GetMapping("/movements")
    public String movements(@RequestParam(defaultValue = "0") int page, Model model) {
        var pageable = PageRequest.of(page, 25, Sort.by(Sort.Direction.DESC, "movementDate"));
        model.addAttribute("movements", bankAccountService.findMovements(pageable));
        model.addAttribute("accounts", bankAccountService.findAllAccounts());
        return "modules/bank/movement-list";
    }

    @GetMapping("/movements/new")
    @PreAuthorize("hasAuthority('BANK_CREATE')")
    public String newMovementForm(@RequestParam(required = false) Long accountId, Model model) {
        BankMovement movement = new BankMovement();
        movement.setMovementDate(LocalDate.now());
        movement.setAmount(BigDecimal.ZERO);
        if (accountId != null) {
            movement.setAccount(bankAccountService.findAccountById(accountId));
            movement.setCurrencyCode(movement.getAccount().getCurrencyCode());
        }
        model.addAttribute("movement", movement);
        model.addAttribute("accounts", bankAccountService.findAllActiveAccounts());
        model.addAttribute("currencies", currencyRepository.findByActiveTrueOrderByCodeAsc());
        return "modules/bank/movement-form";
    }

    @PostMapping("/movements")
    @PreAuthorize("hasAuthority('BANK_CREATE')")
    public String createMovement(@Valid @ModelAttribute("movement") BankMovement movement,
                                BindingResult result,
                                @RequestParam(required = false) Long accountId,
                                @AuthenticationPrincipal ErpUserDetails user,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("accounts", bankAccountService.findAllActiveAccounts());
            model.addAttribute("currencies", currencyRepository.findByActiveTrueOrderByCodeAsc());
            return "modules/bank/movement-form";
        }
        if (accountId == null) {
            result.rejectValue("account", "required", "Account is required");
            model.addAttribute("accounts", bankAccountService.findAllActiveAccounts());
            model.addAttribute("currencies", currencyRepository.findByActiveTrueOrderByCodeAsc());
            return "modules/bank/movement-form";
        }
        movement.setAccount(bankAccountService.findAccountById(accountId));
        bankAccountService.saveMovement(movement, user.getUserId());
        redirectAttributes.addFlashAttribute("message", "Movement recorded.");
        return "redirect:/bank/movements";
    }
}
