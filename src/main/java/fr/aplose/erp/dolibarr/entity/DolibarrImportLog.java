package fr.aplose.erp.dolibarr.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dolibarr_import_log")
@Getter
@Setter
@NoArgsConstructor
public class DolibarrImportLog {

    public static final String LEVEL_INFO = "INFO";
    public static final String LEVEL_WARN = "WARN";
    public static final String LEVEL_ERROR = "ERROR";
    public static final String LEVEL_SKIP = "SKIP";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_run_id", nullable = false)
    private Long importRunId;

    @Column(name = "step", length = 80, nullable = false)
    private String step;

    @Column(name = "level", length = 10, nullable = false)
    private String level;

    @Column(name = "external_id", length = 50)
    private String externalId;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "message", length = 1000, nullable = false)
    private String message;

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;
}
