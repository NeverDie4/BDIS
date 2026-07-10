package com.bdis.modules.collection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.service.HerbBatchSummaryService;
import com.bdis.modules.collection.vo.HerbBatchIdentificationItemVO;
import com.bdis.modules.collection.vo.HerbBatchSummaryVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class HerbBatchSummaryControllerTest {

    private MockMvc mockMvc;

    @Mock private HerbBatchSummaryService herbBatchSummaryService;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HerbBatchSummaryController(herbBatchSummaryService))
                        .build();
    }

    @Test
    void refreshSummaryReturnsUnifiedResult() throws Exception {
        when(herbBatchSummaryService.refreshSummary(1L)).thenReturn(summary());

        mockMvc.perform(post("/herb/batch/1/summary/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(
                        jsonPath("$.data.batchStatus").value(HerbBatchStatusConstants.CONFIRMED));
    }

    @Test
    void getSummaryReturnsUnifiedResult() throws Exception {
        when(herbBatchSummaryService.getSummary(1L)).thenReturn(summary());

        mockMvc.perform(get("/herb/batch/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.batchId").value(1));
    }

    @Test
    void listIdentificationItemsReturnsUnifiedResult() throws Exception {
        HerbBatchIdentificationItemVO item = new HerbBatchIdentificationItemVO();
        item.setImageId(12L);
        item.setFinalSpeciesName("Huanglian");
        when(herbBatchSummaryService.listIdentificationItems(1L)).thenReturn(List.of(item));

        mockMvc.perform(get("/herb/batch/1/identification-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].imageId").value(12));
    }

    @Test
    void confirmBatchReturnsUnifiedResult() throws Exception {
        when(herbBatchSummaryService.confirm(any(), any())).thenReturn(summary());

        mockMvc.perform(
                        put("/herb/batch/1/confirm")
                                .contentType("application/json")
                                .content(
                                        "{\"finalSpeciesId\":1,\"qualityLevel\":\"good\","
                                                + "\"qualityScore\":88.75}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(
                        jsonPath("$.data.batchStatus").value(HerbBatchStatusConstants.CONFIRMED));
    }

    private HerbBatchSummaryVO summary() {
        HerbBatchSummaryVO vo = new HerbBatchSummaryVO();
        vo.setBatchId(1L);
        vo.setBatchCode("BATCH_20260710_001");
        vo.setBatchName("Huanglian batch");
        vo.setBatchStatus(HerbBatchStatusConstants.CONFIRMED);
        return vo;
    }
}
