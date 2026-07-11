package com.bdis.modules.spectrum.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.spectrum.service.HerbIdentificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class HerbIdentificationControllerTest {

    @Mock private HerbIdentificationService herbIdentificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HerbIdentificationController(herbIdentificationService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void reviewRejectsEmptyBody() throws Exception {
        mockMvc.perform(
                        put("/herb/identification/9/review")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(herbIdentificationService);
    }

    @Test
    void reviewRejectsUnknownStatus() throws Exception {
        mockMvc.perform(
                        put("/herb/identification/9/review")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"finalSpeciesId\":2,"
                                                + "\"reviewStatus\":\"confirmd\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.reviewStatus").exists());
        verifyNoInteractions(herbIdentificationService);
    }
}
