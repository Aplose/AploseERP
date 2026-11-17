package fr.aplose.erp.dolibarr.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class DolibarrImportHelper {

    private DolibarrImportHelper() {}

    public static String getString(Map<String, Object> m, String key) {
        if (m == null || key == null) return null;
        Object v = m.get(key);
        if (v == null) return null;
        return v.toString().trim();
    }

    public static Long getLong(Map<String, Object> m, String key) {
        if (m == null || key == null) return null;
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer getInteger(Map<String, Object> m, String key) {
        if (m == null || key == null) return null;
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static BigDecimal getBigDecimal(Map<String, Object> m, String key) {
        if (m == null || key == null) return null;
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return BigDecimal.valueOf(((Number) v).doubleValue());
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse a Dolibarr date. The REST API returns most dates as Unix timestamps (seconds),
     * some (e.g. invoice payments) as "yyyy-MM-dd HH:mm:ss" strings; both are supported.
     */
    public static LocalDate getLocalDate(Map<String, Object> m, String key) {
        String s = getString(m, key);
        if (s == null || s.isBlank()) return null;
        if (s.matches("-?\\d{1,12}")) {
            long epoch = Long.parseLong(s);
            if (epoch == 0) return null;
            return java.time.Instant.ofEpochSecond(epoch).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        try {
            return LocalDate.parse(s.substring(0, Math.min(10, s.length())), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean getBoolean(Map<String, Object> m, String key) {
        if (m == null || key == null) return false;
        Object v = m.get(key);
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        return "1".equals(v.toString()) || "true".equalsIgnoreCase(v.toString()) || "y".equalsIgnoreCase(v.toString());
    }

    /** Dolibarr uses "rowid" or "id" for primary key. */
    public static Long getDolibarrId(Map<String, Object> m) {
        Long id = getLong(m, "rowid");
        if (id != null) return id;
        return getLong(m, "id");
    }

    /** Dolibarr status code: "status" in recent versions, "statut" in older ones. */
    public static Integer getStatus(Map<String, Object> m) {
        Integer s = getInteger(m, "status");
        return s != null ? s : getInteger(m, "statut");
    }

    /** Build code from code_client or code_fournisseur or rowid. */
    public static String thirdPartyCode(Map<String, Object> m) {
        String c = getString(m, "code_client");
        if (c != null && !c.isBlank()) return c;
        c = getString(m, "code_fournisseur");
        if (c != null && !c.isBlank()) return c;
        Long id = getDolibarrId(m);
        return id != null ? "DOLI-" + id : "DOLI-UNK";
    }
}
