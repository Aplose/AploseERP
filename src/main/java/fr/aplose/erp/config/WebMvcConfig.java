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

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
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
