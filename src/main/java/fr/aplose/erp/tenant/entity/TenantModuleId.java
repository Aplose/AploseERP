package fr.aplose.erp.tenant.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TenantModuleId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String moduleCode;
}
