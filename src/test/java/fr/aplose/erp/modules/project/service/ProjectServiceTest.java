package fr.aplose.erp.modules.project.service;

import fr.aplose.erp.modules.project.repository.ProjectRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepo;

    @Mock
    private fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository thirdPartyRepo;

    @Mock
    private fr.aplose.erp.security.repository.UserRepository userRepo;

    private static final String TENANT_ID = "00000000-0000-0000-0000-000000000001";

    @Test
    void countActive_returnsCountFromRepository() {
        try (MockedStatic<TenantContext> tenantMock = mockStatic(TenantContext.class)) {
            tenantMock.when(TenantContext::getCurrentTenantId).thenReturn(TENANT_ID);
            when(projectRepo.countByTenantIdAndStatusIn(eq(TENANT_ID), anySet())).thenReturn(3L);

            ProjectService service = new ProjectService(projectRepo, thirdPartyRepo, userRepo);
            long count = service.countActive();

            assertThat(count).isEqualTo(3L);
            verify(projectRepo).countByTenantIdAndStatusIn(eq(TENANT_ID), anySet());
        }
    }
}
