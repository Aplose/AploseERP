package fr.aplose.erp.i18n.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Controller
public class LocaleController {

    private final LocaleResolver localeResolver;

    public LocaleController(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @GetMapping("/locale")
    public String changeLocale(@RequestParam("lang") String lang,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        localeResolver.setLocale(request, response, Locale.forLanguageTag(lang));
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/dashboard");
    }
}
