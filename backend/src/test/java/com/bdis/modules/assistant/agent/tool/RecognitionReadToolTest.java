package com.bdis.modules.assistant.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bdis.modules.assistant.agent.tool.dto.RecognitionToolData;
import com.bdis.modules.assistant.agent.tool.dto.StatusValue;
import com.bdis.modules.collection.service.HerbBatchImageService;
import com.bdis.modules.collection.service.HerbBatchService;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import com.bdis.modules.spectrum.service.HerbRecognitionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class RecognitionReadToolTest {

    private HerbRecognitionService recognitionService;
    private HerbBatchImageService batchImageService;
    private HerbBatchService batchService;
    private AgentReadToolSupport support;
    private RecognitionReadTool tool;
    private AgentToolExecutionContext context;

    @BeforeEach
    void setUp() {
        recognitionService = mock(HerbRecognitionService.class);
        batchImageService = mock(HerbBatchImageService.class);
        batchService = mock(HerbBatchService.class);
        support = mock(AgentReadToolSupport.class);
        when(support.status(any()))
                .thenAnswer(
                        invocation -> {
                            String code = invocation.getArgument(0);
                            return new StatusValue(code, code);
                        });
        tool =
                new RecognitionReadTool(
                        recognitionService, batchImageService, batchService, support);
        context = mock(AgentToolExecutionContext.class);
    }

    @Test
    void taskRecognitionListsUseSingleBatchServiceQueryAndHandleMissingResult() {
        HerbBatchImageVO unrecognized = new HerbBatchImageVO();
        unrecognized.setImageId(1L);
        HerbBatchImageVO recognizedWithoutConfidence = new HerbBatchImageVO();
        recognizedWithoutConfidence.setImageId(2L);
        recognizedWithoutConfidence.setIdentificationResultId(22L);
        when(batchImageService.listByTask(12L))
                .thenReturn(List.of(unrecognized, recognizedWithoutConfidence));

        RecognitionToolData.TaskImageList result = tool.listUnrecognizedImages(12L, context);

        assertThat(result.images()).hasSize(1);
        assertThat(result.images().get(0).imageId()).isEqualTo(1L);
        assertThat(result.images().get(0).highestSimilarity()).isNull();
        verify(batchImageService).listByTask(12L);
        verifyNoInteractions(recognitionService);
    }

    @Test
    void batchOverviewDoesNotQueryRecognitionPerImage() {
        HerbBatchVO batch = new HerbBatchVO();
        batch.setId(5L);
        batch.setTaskId(12L);
        HerbBatchImageVO image = new HerbBatchImageVO();
        image.setImageId(1L);
        when(batchService.getById(5L)).thenReturn(batch);
        when(batchImageService.listByBatch(eqLong(5L), any())).thenReturn(List.of(image));

        RecognitionToolData.BatchOverview result = tool.getBatchRecognitionOverview(5L, context);

        assertThat(result.imageCount()).isEqualTo(1);
        assertThat(result.unrecognizedCount()).isEqualTo(1);
        verifyNoInteractions(recognitionService);
    }

    private Long eqLong(long value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
