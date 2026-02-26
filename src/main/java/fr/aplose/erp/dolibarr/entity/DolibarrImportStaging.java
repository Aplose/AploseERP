package fr.aplose.erp.dolibarr.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dolibarr_import_staging")
@Getter
@Setter
@NoArgsConstructor
public class DolibarrImportStaging {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_run_id", nullable = false)
    private Long importRunId;

    @Column(name = "entity_dolibarr", length = 50, nullable = false)
    private String entityDolibarr;

    @Column(name = "external_id", nullable = false)
    private Long externalId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;
}
