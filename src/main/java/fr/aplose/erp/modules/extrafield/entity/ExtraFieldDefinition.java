package fr.aplose.erp.modules.extrafield.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "extrafield_definitions")
@Getter
@Setter
@NoArgsConstructor
public class ExtraFieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    @Column(name = "field_code", length = 50, nullable = false)
    private String fieldCode;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "field_type", length = 30, nullable = false)
    private String fieldType;

    @Column(name = "field_options", columnDefinition = "TEXT")
    private String fieldOptions;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "is_required", nullable = false)
    private boolean required = false;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder = 0;

    @Column(name = "visible_on_list", nullable = false)
    private boolean visibleOnList = false;

    @Column(name = "visible_on_detail", nullable = false)
    private boolean visibleOnDetail = true;

    @Column(name = "visible_on_form", nullable = false)
    private boolean visibleOnForm = true;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getHtmlInputType() {
        return switch (fieldType) {
            case "INTEGER" -> "number";
            case "DECIMAL" -> "number";
            case "BOOLEAN" -> "checkbox";
            case "DATE" -> "date";
            case "DATETIME" -> "datetime-local";
            case "EMAIL" -> "email";
            case "URL" -> "url";
            case "PHONE" -> "tel";
            default -> "text";
        };
    }

    public boolean isSelectType() {
        return "SELECT".equals(fieldType);
    }

    public boolean isTextArea() {
        return "TEXT".equals(fieldType);
    }

    public boolean isBooleanType() {
        return "BOOLEAN".equals(fieldType);
    }

    public String[] getSelectOptions() {
        if (fieldOptions == null || fieldOptions.isBlank()) return new String[0];
        String clean = fieldOptions.replaceAll("[\\[\\]\"]", "");
        return clean.split(",");
    }
}
