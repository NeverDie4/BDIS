package com.bdis.modules.growth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.security.RequirePermission;
import com.bdis.modules.growth.service.DigitalLifeIntegrityService;
import com.bdis.modules.growth.service.DigitalLifeNarrationService;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.vo.DigitalLifeIntegrityVO;
import com.bdis.modules.growth.vo.DigitalLifeNarrationGenerationVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifePublicArchiveVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbDigitalLifeArchiveControllerTest {

    @Mock private HerbDigitalLifeArchiveService archiveService;
    @Mock private DigitalLifeNarrationService narrationService;
    @Mock private DigitalLifeIntegrityService integrityService;

    @Test
    void managementControllerRequiresGrowthViewPermissionAndDelegates() {
        HerbDigitalLifeArchiveVO archive = new HerbDigitalLifeArchiveVO();
        when(archiveService.getByTaskId(9L)).thenReturn(archive);
        HerbDigitalLifeArchiveController controller =
                new HerbDigitalLifeArchiveController(
                        archiveService, narrationService, integrityService);

        assertThat(controller.getByTaskId(9L).getData()).isSameAs(archive);
        assertThat(
                        HerbDigitalLifeArchiveController.class
                                .getAnnotation(RequirePermission.class)
                                .value())
                .isEqualTo("growth:record:view");
        verify(archiveService).getByTaskId(9L);
    }

    @Test
    void managementControllerDelegatesNarrationGeneration() {
        DigitalLifeNarrationGenerationVO generation = new DigitalLifeNarrationGenerationVO(2, 1, 0);
        when(narrationService.generateForTask(9L)).thenReturn(generation);
        HerbDigitalLifeArchiveController controller =
                new HerbDigitalLifeArchiveController(
                        archiveService, narrationService, integrityService);

        assertThat(controller.generateNarrations(9L).getData()).isSameAs(generation);
        verify(narrationService).generateForTask(9L);
    }

    @Test
    void integrityEndpointsDelegateToIntegrityService() {
        DigitalLifeIntegrityVO integrity =
                new DigitalLifeIntegrityVO(
                        true, 3, "root", "sha256-v1:1", null, null, null, "校验通过");
        when(integrityService.generate(9L)).thenReturn(integrity);
        when(integrityService.verify(9L)).thenReturn(integrity);
        HerbDigitalLifeArchiveController controller =
                new HerbDigitalLifeArchiveController(
                        archiveService, narrationService, integrityService);

        assertThat(controller.generateIntegrity(9L).getData()).isSameAs(integrity);
        assertThat(controller.verifyIntegrity(9L).getData()).isSameAs(integrity);
        verify(integrityService).generate(9L);
        verify(integrityService).verify(9L);
    }

    @Test
    void publicControllerDelegatesWithoutPermissionAnnotation() {
        HerbDigitalLifePublicArchiveVO archive = new HerbDigitalLifePublicArchiveVO();
        when(archiveService.publicArchive("DL-009")).thenReturn(archive);
        HerbDigitalLifePublicArchiveController controller =
                new HerbDigitalLifePublicArchiveController(archiveService, integrityService);

        assertThat(controller.publicArchive("DL-009").getData()).isSameAs(archive);
        assertThat(
                        HerbDigitalLifePublicArchiveController.class.getAnnotation(
                                RequirePermission.class))
                .isNull();
        verify(archiveService).publicArchive("DL-009");
    }

    @Test
    void publicIntegrityEndpointDelegatesWithoutExposingPayload() {
        DigitalLifeIntegrityVO integrity =
                new DigitalLifeIntegrityVO(
                        true, 3, "root", "sha256-v1:1", null, null, null, "校验通过");
        when(integrityService.verifyPublic("DL-009")).thenReturn(integrity);
        HerbDigitalLifePublicArchiveController controller =
                new HerbDigitalLifePublicArchiveController(archiveService, integrityService);

        assertThat(controller.publicIntegrity("DL-009").getData()).isSameAs(integrity);
        verify(integrityService).verifyPublic("DL-009");
    }
}
