package fr.aplose.erp.modules.project.entity;

import fr.aplose.erp.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_members")
@IdClass(ProjectMemberId.class)
@Getter
@Setter
@NoArgsConstructor
public class ProjectMember {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role", length = 50, nullable = false)
    private String role = "MEMBER";

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();
}
