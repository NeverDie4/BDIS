package com.bdis.modules.collection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.collection.service.HerbCollectionTaskService;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.GrowthChartPointVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class HerbCollectionTaskControllerTest {

    private MockMvc mockMvc;

    @Mock private HerbCollectionTaskService herbCollectionTaskService;

    @Mock private GrowthRecordService growthRecordService;

    @Mock private CollectionAccessService collectionAccessService;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HerbCollectionTaskController(
                                        herbCollectionTaskService,
                                        growthRecordService,
                                        collectionAccessService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
        authenticate(2L, "TEACHER");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void collectorCannotAccessTaskManagementEndpoints() throws Exception {
        authenticate(7L, "COLLECTOR");

        mockMvc.perform(get("/herb/collection-task/assignable-collectors"))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        post("/herb/collection-task")
                                .contentType("application/json")
                                .content(
                                        "{\"taskCode\":\"TASK_COLLECTOR_FORBIDDEN\","
                                                + "\"taskName\":\"Forbidden task\","
                                                + "\"collectorId\":7}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/herb/collection-task/1/publish"))
                .andExpect(status().isForbidden());
    }
    @Test
    void createTaskReturnsUnifiedResult() throws Exception {
        HerbCollectionTaskVO vo = activeVO();
        when(herbCollectionTaskService.create(any())).thenReturn(vo);

        mockMvc.perform(
                        post("/herb/collection-task")
                                .contentType("application/json")
                                .content(
                                        "{\"taskCode\":\"TASK_20260710_001\","
                                                + "\"taskName\":\"Huanglian collection task\","
                                                + "\"collectorId\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.taskCode").value("TASK_20260710_001"));
    }

    @Test
    void getTaskByIdReturnsUnifiedResult() throws Exception {
        HerbCollectionTaskVO vo = activeVO();
        when(herbCollectionTaskService.getById(1L)).thenReturn(vo);

        mockMvc.perform(get("/herb/collection-task/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void publishTaskReturnsLatestDetail() throws Exception {
        HerbCollectionTaskVO vo = activeVO();
        vo.setTaskStatus("published");
        when(herbCollectionTaskService.publish(1L)).thenReturn(vo);

        mockMvc.perform(put("/herb/collection-task/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.taskStatus").value("published"));
    }

    @Test
    void getGrowthChartReturnsTaskPoints() throws Exception {
        GrowthChartPointVO point = new GrowthChartPointVO();
        point.setRecordId(11L);
        point.setBatchId(2L);
        point.setValue(new BigDecimal("18.50"));
        when(growthRecordService.getChartByTaskId(1L, "plantHeight")).thenReturn(List.of(point));

        mockMvc.perform(
                        get("/herb/collection-task/1/growth-records/chart")
                                .param("metric", "plantHeight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].recordId").value(11))
                .andExpect(jsonPath("$.data[0].value").value(18.5));
    }

    private void authenticate(Long userId, String roleCode) {
        CurrentUser user =
                new CurrentUser(
                        userId,
                        roleCode.toLowerCase(),
                        roleCode,
                        1L,
                        1L,
                        Set.of(roleCode),
                        Set.of(4L),
                        Set.of("growth:record:create", "growth:record:submit"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
    private HerbCollectionTaskVO activeVO() {
        HerbCollectionTaskVO vo = new HerbCollectionTaskVO();
        vo.setId(1L);
        vo.setTaskCode("TASK_20260710_001");
        vo.setTaskName("Huanglian collection task");
        return vo;
    }
}
