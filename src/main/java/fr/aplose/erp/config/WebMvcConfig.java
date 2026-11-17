package fr.aplose.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ModuleAccessInterceptor moduleAccessInterceptor;

    public WebMvcConfig(ModuleAccessInterceptor moduleAccessInterceptor) {
        this.moduleAccessInterceptor = moduleAccessInterceptor;
    }

    /**
     * Interface language: the one chosen in the session (language switcher) if any; otherwise the
     * logged-in user's language; otherwise the browser's language when the interface offers it;
     * otherwise English.
     */
    @Bean
    public LocaleResolver localeResolver(@org.springframework.beans.factory.annotation.Value("${erp.i18n.languages:en,fr}") java.util.List<String> languages) {
        java.util.List<Locale> supported = languages.stream().map(String::trim).map(Locale::forLanguageTag).toList();
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocaleFunction(request -> {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof fr.aplose.erp.security.service.ErpUserDetails user
                    && user.getLocale() != null && !user.getLocale().isBlank()) {
                return Locale.forLanguageTag(user.getLocale());
            }
            String accept = request.getHeader("Accept-Language");
            if (accept != null && !accept.isBlank()) {
                try {
                    Locale best = Locale.lookup(Locale.LanguageRange.parse(accept), supported);
                    if (best != null) return best;
                } catch (IllegalArgumentException malformedHeader) {
                    // fall through to English
                }
            }
            return Locale.ENGLISH;
        });
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(moduleAccessInterceptor)
                .addPathPatterns("/third-parties", "/third-parties/**",
                        "/contacts", "/contacts/**",
                        "/proposals", "/proposals/**",
                        "/orders", "/orders/**",
                        "/invoices", "/invoices/**",
                        "/products", "/products/**",
                        "/currencies", "/currencies/**",
                        "/projects", "/projects/**",
                        "/agenda", "/agenda/**");
    }
}
