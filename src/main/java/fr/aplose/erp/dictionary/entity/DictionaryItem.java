package fr.aplose.erp.dictionary.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dictionary_items")
@Getter
@Setter
@NoArgsConstructor
public class DictionaryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "type", length = 50, nullable = false)
    private String type;

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
