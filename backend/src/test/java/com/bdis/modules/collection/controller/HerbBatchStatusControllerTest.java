package com.bdis.modules.collection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.service.HerbBatchStatusService;
import com.bdis.modules.collection.vo.HerbBatchStatusVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class HerbBatchStatusControllerTest {

    private MockMvc mockMvc;

    @Mock private HerbBatchStatusService herbBatchStatusService;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HerbBatchStatusController(herbBatchStatusService))
                        .build();
    }

    @Test
    void startCollectionReturnsUpdatedBatch() throws Exception {
        when(herbBatchStatusService.startCollection(1L))
                .thenReturn(batch(HerbBatchStatusConstants.COLLECTING));

        mockMvc.perform(put("/herb/batch/1/start-collection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.batchStatus").value(HerbBatchStatusConstants.COLLECTING));
    }

    @Test
    void confirmStatusAcceptsJsonBody() throws Exception {
        when(herbBatchStatusService.confirmStatus(any(), any()))
                .thenReturn(batch(HerbBatchStatusConstants.CONFIRMED));

        mockMvc.perform(
                        put("/herb/batch/1/confirm-status")
                                .contentType("application/json")
                                .content("{\"force\":true,\"remark\":\"人工强制确认批次结果\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchStatus").value(HerbBatchStatusConstants.CONFIRMED));
    }

    @Test
    void statusReturnsAllowedActions() throws Exception {
        HerbBatchStatusVO status = new HerbBatchStatusVO();
        status.setBatchId(1L);
        status.setBatchCode("BATCH_20260710_001");
        status.setBatchName("Huanglian batch 001");
        status.setBatchStatus(HerbBatchStatusConstants.COLLECTING);
        status.setImageCount(3);
        status.setIdentifiedCount(0);
        status.setReviewedCount(0);
        status.setNeedReviewCount(0);
        status.setAllowedActions(List.of("submit", "cancel"));
        when(herbBatchStatusService.status(1L)).thenReturn(status);

        mockMvc.perform(get("/herb/batch/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchId").value(1))
                .andExpect(jsonPath("$.data.allowedActions[0]").value("submit"));
    }

    private HerbBatchVO batch(String status) {
        HerbBatchVO vo = new HerbBatchVO();
        vo.setId(1L);
        vo.setBatchCode("BATCH_20260710_001");
        vo.setBatchName("Huanglian batch 001");
        vo.setBatchStatus(status);
        return vo;
    }
}
