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

    public static LocalDate getLocalDate(Map<String, Object> m, String key) {
        String s = getString(m, key);
        if (s == null || s.isBlank()) return null;
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
