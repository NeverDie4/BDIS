package com.bdis.modules.collection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.modules.collection.service.HerbBatchService;
import com.bdis.modules.collection.vo.HerbBatchVO;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class HerbBatchControllerTest {

    private MockMvc mockMvc;

    @Mock private HerbBatchService herbBatchService;

    @Mock private GrowthRecordService growthRecordService;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HerbBatchController(herbBatchService, growthRecordService))
                        .build();
    }

    @Test
    void createBatchReturnsUnifiedResult() throws Exception {
        HerbBatchVO vo = new HerbBatchVO();
        vo.setId(1L);
        vo.setBatchCode("BATCH_20260710_001");
        vo.setBatchName("Huanglian batch 001");
        when(herbBatchService.create(any())).thenReturn(vo);

        mockMvc.perform(
                        post("/herb/batch")
                                .contentType("application/json")
                                .content(
                                        "{\"batchCode\":\"BATCH_20260710_001\","
                                                + "\"batchName\":\"Huanglian batch 001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.batchCode").value("BATCH_20260710_001"));
    }

    @Test
    void getBatchByIdReturnsUnifiedResult() throws Exception {
        HerbBatchVO vo = new HerbBatchVO();
        vo.setId(1L);
        vo.setBatchCode("BATCH_20260710_001");
        vo.setBatchName("Huanglian batch 001");
        when(herbBatchService.getById(1L)).thenReturn(vo);

        mockMvc.perform(get("/herb/batch/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getGrowthRecordByBatchReturnsUnifiedResult() throws Exception {
        GrowthRecordVO vo = new GrowthRecordVO();
        vo.setId(11L);
        vo.setBatchId(1L);
        when(growthRecordService.getByBatchId(1L)).thenReturn(vo);

        mockMvc.perform(get("/herb/batch/1/growth-record"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.batchId").value(1));
    }

    @Test
    void createGrowthRecordForBatchUsesBatchPath() throws Exception {
        GrowthRecordVO vo = new GrowthRecordVO();
        vo.setId(11L);
        vo.setBatchId(1L);
        when(growthRecordService.createForBatch(any(), any())).thenReturn(vo);

        mockMvc.perform(
                        post("/herb/batch/1/growth-record")
                                .contentType("application/json")
                                .content("{\"plantHeight\":18.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchId").value(1));
    }

    @Test
    void updateGrowthRecordForBatchUsesBatchAndRecordPath() throws Exception {
        GrowthRecordVO vo = new GrowthRecordVO();
        vo.setId(11L);
        vo.setBatchId(1L);
        when(growthRecordService.updateForBatch(any(), any(), any())).thenReturn(vo);

        mockMvc.perform(
                        put("/herb/batch/1/growth-record/11")
                                .contentType("application/json")
                                .content("{\"plantHeight\":20.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(11));
    }
}
