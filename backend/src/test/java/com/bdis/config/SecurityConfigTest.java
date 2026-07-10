package com.bdis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.modules.collection.mapper.HerbBatchImageMapper;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.herb.controller.HerbSpeciesController;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.mapper.HerbMapper;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.herb.service.HerbSpeciesService;
import com.bdis.modules.knowledge.mapper.KnowledgeNodeMapper;
import com.bdis.modules.knowledge.mapper.KnowledgeRelationMapper;
import com.bdis.modules.knowledge.mapper.KnowledgeTripleMapper;
import com.bdis.modules.spectrum.mapper.AiModelVersionMapper;
import com.bdis.modules.spectrum.mapper.HerbAtlasMapper;
import com.bdis.modules.spectrum.mapper.HerbAtlasFeatureMapper;
import com.bdis.modules.spectrum.mapper.HerbAtlasTagMapper;
import com.bdis.modules.spectrum.mapper.HerbIdentificationResultMapper;
import com.bdis.modules.spectrum.mapper.HerbImageFeatureMapper;
import com.bdis.modules.spectrum.mapper.HerbImageMatchMapper;
import com.bdis.modules.spectrum.mapper.ImageRecognitionMapper;
import com.bdis.modules.spectrum.mapper.SpectrumComparisonMapper;
import com.bdis.modules.spectrum.mapper.SpectrumMapper;
import com.bdis.modules.spectrum.mapper.SpectrumTagMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HerbSpeciesController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private HerbSpeciesService herbSpeciesService;

    @MockBean private HerbBatchImageMapper herbBatchImageMapper;

    @MockBean private HerbBatchMapper herbBatchMapper;

    @MockBean private HerbCollectionTaskMapper herbCollectionTaskMapper;

    @MockBean private HerbImageMapper herbImageMapper;

    @MockBean private HerbMapper herbMapper;

    @MockBean private HerbSpeciesMapper herbSpeciesMapper;

    @MockBean private KnowledgeNodeMapper knowledgeNodeMapper;

    @MockBean private KnowledgeRelationMapper knowledgeRelationMapper;

    @MockBean private KnowledgeTripleMapper knowledgeTripleMapper;

    @MockBean private AiModelVersionMapper aiModelVersionMapper;

    @MockBean private HerbAtlasMapper herbAtlasMapper;

    @MockBean private HerbAtlasFeatureMapper herbAtlasFeatureMapper;

    @MockBean private HerbAtlasTagMapper herbAtlasTagMapper;

    @MockBean private HerbIdentificationResultMapper herbIdentificationResultMapper;

    @MockBean private HerbImageFeatureMapper herbImageFeatureMapper;

    @MockBean private HerbImageMatchMapper herbImageMatchMapper;

    @MockBean private ImageRecognitionMapper imageRecognitionMapper;

    @MockBean private SpectrumComparisonMapper spectrumComparisonMapper;

    @MockBean private SpectrumMapper spectrumMapper;

    @MockBean private SpectrumTagMapper spectrumTagMapper;

    @Test
    void herbApiAllowsAnonymousAccessWithoutRedirectingToLogin() throws Exception {
        when(herbSpeciesService.listEnabled()).thenReturn(List.of());

        mockMvc.perform(get("/herb/species/list")).andExpect(status().isOk());
    }

    @Test
    void herbPostApiAllowsAnonymousAccessWithoutCsrfToken() throws Exception {
        mockMvc.perform(
                        post("/herb/atlas/feature/batch-extract")
                                .contentType("application/json")
                                .content("{\"speciesId\":null,\"forceRefresh\":false}"))
                .andExpect(
                        result ->
                                assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void mobileHerbApiAllowsAnonymousAccessWithoutRedirectingToLogin() throws Exception {
        mockMvc.perform(get("/mobile/herb/tasks?collectorId=1001"))
                .andExpect(
                        result ->
                                assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }
}
