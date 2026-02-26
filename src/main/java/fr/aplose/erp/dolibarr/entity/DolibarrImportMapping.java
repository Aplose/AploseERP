package fr.aplose.erp.dolibarr.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dolibarr_import_mapping")
@Getter
@Setter
@NoArgsConstructor
public class DolibarrImportMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "import_run_id", nullable = false)
    private Long importRunId;

    @Column(name = "dolibarr_entity", length = 50, nullable = false)
    private String dolibarrEntity;

    @Column(name = "dolibarr_id", nullable = false)
    private Long dolibarrId;

    @Column(name = "aplose_entity", length = 50, nullable = false)
    private String aploseEntity;

    @Column(name = "aplose_id", nullable = false)
    private Long aploseId;
}
