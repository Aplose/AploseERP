package fr.aplose.erp.core.businessobject;

/**
 * Optional marker for entities that represent a business object type.
 * The type code can be used with {@link BusinessObjectRegistry}.
 */
public interface BusinessObject {

    /**
     * Returns the business object type code (e.g. THIRD_PARTY, INVOICE).
     */
    String getBusinessObjectTypeCode();
}
