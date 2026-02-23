package fr.aplose.erp.dictionary;

/**
 * Dictionary types (reference data lists) per tenant.
 */
public final class DictionaryType {

    private DictionaryType() {}

    public static final String CIVILITY = "CIVILITY";
    public static final String COUNTRY = "COUNTRY";
    public static final String CURRENCY = "CURRENCY";
    public static final String LEGAL_FORM = "LEGAL_FORM";
    public static final String PAYMENT_METHOD = "PAYMENT_METHOD";
    public static final String CONTACT_THIRD_PARTY_LINK_TYPE = "CONTACT_THIRD_PARTY_LINK_TYPE";

    public static final String[] ALL = {
            CIVILITY, COUNTRY, CURRENCY, LEGAL_FORM, PAYMENT_METHOD, CONTACT_THIRD_PARTY_LINK_TYPE
    };
}
