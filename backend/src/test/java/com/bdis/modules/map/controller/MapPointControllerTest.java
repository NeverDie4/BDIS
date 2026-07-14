package com.bdis.modules.map.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.common.security.RequirePermission;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.map.dto.MapPointStatusRequest;
import com.bdis.modules.map.service.MapPointService;
import com.bdis.modules.map.vo.MapPointVO;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MapPointControllerTest {

    @Mock private MapPointService mapPointService;
    @Mock private GrowthRecordService growthRecordService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new MapPointController(mapPointService, growthRecordService))
                        .build();
    }

    @Test
    void updateStatusAcceptsValidatedJsonBody() throws Exception {
        MapPointVO point = new MapPointVO();
        point.setId(7L);
        point.setStatus(0);
        when(mapPointService.updateMapPointStatus(7L, 0)).thenReturn(point);

        mockMvc.perform(
                        patch("/map-points/7/status")
                                .contentType("application/json")
                                .content("{\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.status").value(0));
    }

    @Test
    void updateStatusUsesMapUpdatePermission() throws Exception {
        Method method =
                MapPointController.class.getMethod(
                        "updateMapPointStatus", Long.class, MapPointStatusRequest.class);

        assertThat(method.getAnnotation(RequirePermission.class).value())
                .isEqualTo("map:point:update");
    }
}
