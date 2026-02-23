package fr.aplose.erp.modules.catalog.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Currencies are managed as a dictionary (Admin → Dictionaries → CURRENCY).
 * This controller redirects the old /currencies URL to the dictionary.
 */
@Controller
@RequestMapping("/currencies")
@PreAuthorize("hasAuthority('TENANT_READ')")
public class CurrencyController {

    @GetMapping
    public String list() {
        return "redirect:/admin/dictionaries/CURRENCY";
    }
}
