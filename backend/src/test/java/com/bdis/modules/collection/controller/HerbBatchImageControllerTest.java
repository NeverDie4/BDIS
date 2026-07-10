package com.bdis.modules.collection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.modules.collection.service.HerbBatchImageService;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class HerbBatchImageControllerTest {

    private MockMvc mockMvc;

    @Mock private HerbBatchImageService herbBatchImageService;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new HerbBatchImageController(herbBatchImageService))
                        .build();
    }

    @Test
    void bindImageReturnsUnifiedResult() throws Exception {
        HerbBatchImageVO vo = activeVO();
        when(herbBatchImageService.bind(any(), any())).thenReturn(vo);

        mockMvc.perform(
                        post("/herb/batch/1/image")
                                .contentType("application/json")
                                .content("{\"imageId\":12,\"imageRole\":\"leaf\",\"isPrimary\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.imageId").value(12));
    }

    @Test
    void listBatchImagesReturnsUnifiedResult() throws Exception {
        when(herbBatchImageService.listByBatch(any(), any()))
                .thenReturn(java.util.List.of(activeVO()));

        mockMvc.perform(get("/herb/batch/1/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].imageId").value(12));
    }

    @Test
    void setPrimaryReturnsUnifiedResult() throws Exception {
        HerbBatchImageVO vo = activeVO();
        vo.setIsPrimary(1);
        when(herbBatchImageService.setPrimary(1L, 12L)).thenReturn(vo);

        mockMvc.perform(put("/herb/batch/1/image/12/primary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.isPrimary").value(1));
    }

    private HerbBatchImageVO activeVO() {
        HerbBatchImageVO vo = new HerbBatchImageVO();
        vo.setId(1L);
        vo.setBatchId(1L);
        vo.setImageId(12L);
        vo.setImageRole("leaf");
        vo.setIsPrimary(0);
        return vo;
    }
}
