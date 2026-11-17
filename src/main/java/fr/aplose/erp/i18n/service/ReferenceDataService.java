package fr.aplose.erp.i18n.service;

import fr.aplose.erp.modules.catalog.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * International reference data: countries (ISO 3166), currencies (ISO 4217), interface languages
 * and time zones, with names in the user's language.
 * <p>
 * Countries come from the JDK, so every country can be chosen, named in the user's language.
 * Currencies come from the currencies table (all ISO currencies since migration V15), named in the
 * user's language. Usable from templates as {@code ${@referenceData.countries()}}.
 */
@Service("referenceData")
@RequiredArgsConstructor
public class ReferenceDataService {

    /** An entry of a drop-down list. {@code name} is an alias of {@code label} for older templates. */
    public record Option(String code, String label) {
        public String name() {
            return label;
        }
    }

    /** Defaults suggested for a company, deduced from its country. */
    public record CountryDefaults(String currency, String language, String timezone) {}

    private final CurrencyRepository currencyRepository;

    @Value("${erp.i18n.languages:en,fr}")
    private List<String> supportedLanguages;

    private final Map<Locale, List<Option>> countriesCache = new ConcurrentHashMap<>();

    private static Locale userLocale() {
        return LocaleContextHolder.getLocale();
    }

    private static Comparator<Option> byLabel(Locale locale) {
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.PRIMARY);
        return Comparator.comparing(Option::label, collator);
    }

    /** All ISO 3166 countries, named and sorted in the user's language. */
    public List<Option> countries() {
        Locale locale = userLocale();
        return countriesCache.computeIfAbsent(locale, l -> Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2)
                .stream()
                .map(cc -> new Option(cc, Locale.of("", cc).getDisplayCountry(l)))
                .sorted(byLabel(l))
                .toList());
    }

    public String countryName(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) return "";
        return Locale.of("", countryCode.trim().toUpperCase()).getDisplayCountry(userLocale());
    }

    /** Active currencies of the currencies table, named in the user's language, sorted by code. */
    public List<Option> currencies() {
        Locale locale = userLocale();
        return currencyRepository.findByActiveTrueOrderByCodeAsc().stream()
                .map(c -> new Option(c.getCode(), currencyName(c.getCode(), c.getName(), locale)))
                .toList();
    }

    private static String currencyName(String code, String fallback, Locale locale) {
        try {
            String name = Currency.getInstance(code.trim()).getDisplayName(locale);
            return name.equals(code) ? fallback : name;
        } catch (IllegalArgumentException unknownToThisJdk) {
            return fallback;
        }
    }

    /** Interface languages available (one messages file each), named in their own language. */
    public List<Option> languages() {
        return supportedLanguages.stream()
                .map(tag -> {
                    Locale l = Locale.forLanguageTag(tag.trim());
                    String name = l.getDisplayLanguage(l);
                    return new Option(tag.trim(), name.substring(0, 1).toUpperCase(l) + name.substring(1));
                })
                .toList();
    }

    public boolean isSupportedLanguage(String tag) {
        return tag != null && supportedLanguages.stream().anyMatch(s -> s.trim().equalsIgnoreCase(tag.trim()));
    }

    /** All IANA time zones (Europe/Paris, America/New_York...), sorted. */
    public List<String> timezones() {
        return ZoneId.getAvailableZoneIds().stream()
                .filter(z -> z.contains("/") && !z.startsWith("Etc/") && !z.startsWith("SystemV/"))
                .sorted()
                .toList();
    }

    public boolean isValidTimezone(String zone) {
        try {
            return zone != null && !zone.isBlank() && ZoneId.of(zone) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isActiveCurrency(String code) {
        return code != null && currencyRepository.findByActiveTrueOrderByCodeAsc().stream()
                .anyMatch(c -> c.getCode().trim().equalsIgnoreCase(code.trim()));
    }

    /**
     * Currency, interface language and time zone suggested for a company of the given country.
     * The language is the first supported language spoken in the country (English otherwise);
     * the time zone is only suggested when the country has a single main zone known here,
     * otherwise UTC (the signup form proposes the browser's zone).
     */
    public CountryDefaults defaultsFor(String countryCode) {
        String cc = countryCode != null ? countryCode.trim().toUpperCase() : "";
        String currency = "EUR";
        try {
            Currency c = cc.isEmpty() ? null : Currency.getInstance(Locale.of("", cc));
            if (c != null && isActiveCurrency(c.getCurrencyCode())) currency = c.getCurrencyCode();
        } catch (IllegalArgumentException ignored) {
            // not a country with a currency (e.g. Antarctica)
        }
        String language = Optional.of(cc).filter(s -> !s.isEmpty())
                .flatMap(country -> java.util.Arrays.stream(Locale.getAvailableLocales())
                        .filter(l -> country.equals(l.getCountry()))
                        .map(Locale::getLanguage)
                        .filter(this::isSupportedLanguage)
                        .findFirst())
                .orElse("en");
        String timezone = MAIN_ZONES.getOrDefault(cc, "UTC");
        return new CountryDefaults(currency, language, timezone);
    }

    /** Main time zone of countries with a single (or a clearly dominant) zone. */
    private static final Map<String, String> MAIN_ZONES = Map.ofEntries(
            Map.entry("FR", "Europe/Paris"), Map.entry("BE", "Europe/Brussels"), Map.entry("CH", "Europe/Zurich"),
            Map.entry("LU", "Europe/Luxembourg"), Map.entry("DE", "Europe/Berlin"), Map.entry("ES", "Europe/Madrid"),
            Map.entry("IT", "Europe/Rome"), Map.entry("GB", "Europe/London"), Map.entry("IE", "Europe/Dublin"),
            Map.entry("PT", "Europe/Lisbon"), Map.entry("NL", "Europe/Amsterdam"), Map.entry("AT", "Europe/Vienna"),
            Map.entry("PL", "Europe/Warsaw"), Map.entry("SE", "Europe/Stockholm"), Map.entry("NO", "Europe/Oslo"),
            Map.entry("DK", "Europe/Copenhagen"), Map.entry("FI", "Europe/Helsinki"), Map.entry("GR", "Europe/Athens"),
            Map.entry("RO", "Europe/Bucharest"), Map.entry("CZ", "Europe/Prague"), Map.entry("HU", "Europe/Budapest"),
            Map.entry("MC", "Europe/Monaco"), Map.entry("MA", "Africa/Casablanca"), Map.entry("DZ", "Africa/Algiers"),
            Map.entry("TN", "Africa/Tunis"), Map.entry("SN", "Africa/Dakar"), Map.entry("CI", "Africa/Abidjan"),
            Map.entry("CM", "Africa/Douala"), Map.entry("JP", "Asia/Tokyo"), Map.entry("CN", "Asia/Shanghai"),
            Map.entry("IN", "Asia/Kolkata"), Map.entry("IL", "Asia/Jerusalem"), Map.entry("AE", "Asia/Dubai"),
            Map.entry("SG", "Asia/Singapore"), Map.entry("KR", "Asia/Seoul"), Map.entry("NZ", "Pacific/Auckland"),
            Map.entry("ZA", "Africa/Johannesburg"), Map.entry("RE", "Indian/Reunion"), Map.entry("GP", "America/Guadeloupe"),
            Map.entry("MQ", "America/Martinique"), Map.entry("GF", "America/Cayenne"), Map.entry("NC", "Pacific/Noumea"),
            Map.entry("PF", "Pacific/Tahiti"), Map.entry("YT", "Indian/Mayotte"), Map.entry("TR", "Europe/Istanbul"),
            Map.entry("AR", "America/Argentina/Buenos_Aires"), Map.entry("CO", "America/Bogota"),
            Map.entry("PE", "America/Lima"), Map.entry("CL", "America/Santiago")
    );
}
