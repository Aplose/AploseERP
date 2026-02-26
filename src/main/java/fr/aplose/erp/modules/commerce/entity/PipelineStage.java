package fr.aplose.erp.modules.commerce.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "sales_pipeline_stages")
@Getter
@Setter
@NoArgsConstructor
public class PipelineStage extends BaseEntity {

    @NotBlank
    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @NotBlank
    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "probability", precision = 5, scale = 2, nullable = false)
    private BigDecimal probability = BigDecimal.ZERO;

    @Column(name = "is_closed", nullable = false)
    private boolean closed = false;
}
