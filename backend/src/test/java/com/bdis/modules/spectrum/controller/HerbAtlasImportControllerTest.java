package com.bdis.modules.spectrum.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.common.security.RequirePermission;
import org.junit.jupiter.api.Test;

class HerbAtlasImportControllerTest {

    @Test
    void importUsesDedicatedAdministrativePermission() {
        RequirePermission permission =
                HerbAtlasImportController.class.getAnnotation(RequirePermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).isEqualTo("herb:atlas:import");
    }
}
