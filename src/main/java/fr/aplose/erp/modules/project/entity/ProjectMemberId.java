package fr.aplose.erp.modules.project.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ProjectMemberId implements Serializable {

    private Long project;
    private Long user;
}
