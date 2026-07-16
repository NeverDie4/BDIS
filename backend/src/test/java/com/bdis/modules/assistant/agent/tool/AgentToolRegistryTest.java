package com.bdis.modules.assistant.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.modules.assistant.agent.service.HerbDigitalTwinAgentTaskService;
import com.bdis.modules.collection.service.HerbBatchImageService;
import com.bdis.modules.collection.service.HerbBatchService;
import com.bdis.modules.collection.service.HerbCollectionTaskService;
import com.bdis.modules.growth.service.DigitalLifeIntegrityService;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.spectrum.service.HerbRecognitionService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class AgentToolRegistryTest {

    @Test
    void registersOnlyTwentyReadOnlyBusinessOperations() {
        AgentReadToolSupport support =
                new AgentReadToolSupport(mock(HerbDigitalTwinAgentTaskService.class));
        HerbCollectionTaskService taskService = mock(HerbCollectionTaskService.class);
        HerbDigitalLifeArchiveService archiveService = mock(HerbDigitalLifeArchiveService.class);
        HerbBatchService batchService = mock(HerbBatchService.class);
        HerbBatchImageService imageService = mock(HerbBatchImageService.class);
        GrowthRecordService growthService = mock(GrowthRecordService.class);
        DigitalLifeIntegrityService integrityService = mock(DigitalLifeIntegrityService.class);
        HerbRecognitionService recognitionService = mock(HerbRecognitionService.class);

        AgentToolRegistry registry =
                new AgentToolRegistry(
                        List.of(
                                new CollectionTaskReadTool(taskService, archiveService, support),
                                new HerbBatchReadTool(
                                        batchService,
                                        imageService,
                                        taskService,
                                        archiveService,
                                        support),
                                new GrowthRecordReadTool(growthService, archiveService, support),
                                new RecognitionReadTool(
                                        recognitionService, imageService, batchService, support),
                                new TraceReadTool(growthService, integrityService, support),
                                new DigitalLifeArchiveReadTool(
                                        archiveService, integrityService, support)));

        assertThat(registry.listReadOnlyDefinitions()).hasSize(20);
        assertThat(registry.listReadOnlyDefinitions())
                .allMatch(
                        definition ->
                                definition.toolMode() == AgentToolDefinition.ToolMode.READ_ONLY)
                .allMatch(definition -> definition.timeoutSeconds() == 15);
    }

    private <T> T mock(Class<T> type) {
        return Mockito.mock(type);
    }
}
