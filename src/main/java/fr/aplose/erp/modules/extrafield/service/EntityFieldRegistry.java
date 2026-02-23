package fr.aplose.erp.modules.extrafield.service;

import fr.aplose.erp.core.businessobject.BusinessObjectRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EntityFieldRegistry {

    private EntityFieldRegistry() {}

    public record FieldMeta(String name, String label, boolean mandatory) {}

    public record EntityMeta(String code, String label, String icon) {}

    /** Entity types that support extra fields: from business object registry (core). Custom types added in phase 3. */
    public static final List<EntityMeta> ENTITY_TYPES = new ArrayList<>();

    public static final Map<String, List<FieldMeta>> FIELDS = new LinkedHashMap<>();

    static {
        FIELDS.put("THIRD_PARTY", List.of(
                new FieldMeta("code", "Code", true),
                new FieldMeta("name", "Name", true),
                new FieldMeta("legalForm", "Legal Form", false),
                new FieldMeta("taxId", "Tax ID (VAT)", false),
                new FieldMeta("registrationNo", "Registration No.", false),
                new FieldMeta("website", "Website", false),
                new FieldMeta("email", "Email", false),
                new FieldMeta("phone", "Phone", false),
                new FieldMeta("fax", "Fax", false),
                new FieldMeta("addressLine1", "Address Line 1", false),
                new FieldMeta("addressLine2", "Address Line 2", false),
                new FieldMeta("city", "City", false),
                new FieldMeta("stateProvince", "State/Province", false),
                new FieldMeta("postalCode", "Postal Code", false),
                new FieldMeta("countryCode", "Country Code", false),
                new FieldMeta("currencyCode", "Currency", false),
                new FieldMeta("paymentTerms", "Payment Terms", false),
                new FieldMeta("creditLimit", "Credit Limit", false),
                new FieldMeta("balance", "Balance", false),
                new FieldMeta("salesRep", "Sales Rep", false),
                new FieldMeta("tags", "Tags", false),
                new FieldMeta("notes", "Notes", false),
                new FieldMeta("status", "Status", true)
        ));

        FIELDS.put("CONTACT", List.of(
                new FieldMeta("firstName", "First Name", true),
                new FieldMeta("lastName", "Last Name", false),
                new FieldMeta("jobTitle", "Job Title", false),
                new FieldMeta("department", "Department", false),
                new FieldMeta("email", "Email", false),
                new FieldMeta("emailSecondary", "Secondary Email", false),
                new FieldMeta("phone", "Phone", false),
                new FieldMeta("phoneMobile", "Mobile Phone", false),
                new FieldMeta("fax", "Fax", false),
                new FieldMeta("addressLine1", "Address Line 1", false),
                new FieldMeta("addressLine2", "Address Line 2", false),
                new FieldMeta("city", "City", false),
                new FieldMeta("stateProvince", "State/Province", false),
                new FieldMeta("postalCode", "Postal Code", false),
                new FieldMeta("countryCode", "Country Code", false),
                new FieldMeta("notes", "Notes", false),
                new FieldMeta("status", "Status", true)
        ));

        FIELDS.put("PRODUCT", List.of(
                new FieldMeta("code", "Code", true),
                new FieldMeta("name", "Name", true),
                new FieldMeta("type", "Type", true),
                new FieldMeta("category", "Category", false),
                new FieldMeta("description", "Description", false),
                new FieldMeta("unitOfMeasure", "Unit of Measure", false),
                new FieldMeta("salePrice", "Sale Price", true),
                new FieldMeta("purchasePrice", "Purchase Price", true),
                new FieldMeta("currencyCode", "Currency", true),
                new FieldMeta("vatRate", "VAT Rate", true),
                new FieldMeta("barcode", "Barcode", false),
                new FieldMeta("sellable", "Sellable", false),
                new FieldMeta("purchasable", "Purchasable", false),
                new FieldMeta("trackStock", "Track Stock", false),
                new FieldMeta("stockAlertLevel", "Stock Alert Level", false),
                new FieldMeta("notes", "Notes", false),
                new FieldMeta("active", "Active", true)
        ));

        FIELDS.put("PROPOSAL", List.of(
                new FieldMeta("reference", "Reference", true),
                new FieldMeta("title", "Title", false),
                new FieldMeta("thirdParty", "Client", true),
                new FieldMeta("contact", "Contact", false),
                new FieldMeta("dateIssued", "Date Issued", true),
                new FieldMeta("dateValidUntil", "Valid Until", false),
                new FieldMeta("currencyCode", "Currency", true),
                new FieldMeta("discountAmount", "Discount", false),
                new FieldMeta("salesRep", "Sales Rep", false),
                new FieldMeta("notes", "Notes", false),
                new FieldMeta("terms", "Terms", false),
                new FieldMeta("status", "Status", true)
        ));

        FIELDS.put("INVOICE", List.of(
                new FieldMeta("reference", "Reference", true),
                new FieldMeta("type", "Type", true),
                new FieldMeta("thirdParty", "Client", true),
                new FieldMeta("contact", "Contact", false),
                new FieldMeta("dateIssued", "Date Issued", true),
                new FieldMeta("dateDue", "Due Date", true),
                new FieldMeta("currencyCode", "Currency", true),
                new FieldMeta("discountAmount", "Discount", false),
                new FieldMeta("paymentMethod", "Payment Method", false),
                new FieldMeta("bankAccount", "Bank Account", false),
                new FieldMeta("notes", "Notes", false),
                new FieldMeta("terms", "Terms", false),
                new FieldMeta("status", "Status", true)
        ));

        for (BusinessObjectRegistry.CoreType t : BusinessObjectRegistry.CoreType.values()) {
            if (FIELDS.containsKey(t.getCode())) {
                ENTITY_TYPES.add(new EntityMeta(t.getCode(), t.getLabelKey(), t.getIcon()));
            }
        }
    }

    public static List<FieldMeta> getFields(String entityType) {
        return FIELDS.getOrDefault(entityType, List.of());
    }

    public static EntityMeta getEntityMeta(String code) {
        return ENTITY_TYPES.stream()
                .filter(e -> e.code().equals(code))
                .findFirst()
                .orElse(null);
    }
}
