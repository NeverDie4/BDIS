package com.bdis.modules.assistant.agent.tool;

import com.bdis.modules.assistant.agent.tool.dto.DigitalLifeArchiveToolData;
import com.bdis.modules.growth.service.DigitalLifeIntegrityService;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.vo.DigitalLifeIntegrityVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeStageVO;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class DigitalLifeArchiveReadTool implements AgentBusinessTool {

    public static final String OVERVIEW = "digital_life_archive.overview";
    public static final String STAGES = "digital_life_archive.stages";
    public static final String INTEGRITY = "digital_life_archive.integrity_status";

    private final HerbDigitalLifeArchiveService archiveService;
    private final DigitalLifeIntegrityService integrityService;
    private final AgentReadToolSupport support;

    public DigitalLifeArchiveReadTool(
            HerbDigitalLifeArchiveService archiveService,
            DigitalLifeIntegrityService integrityService,
            AgentReadToolSupport support) {
        this.archiveService = archiveService;
        this.integrityService = integrityService;
        this.support = support;
    }

    public DigitalLifeArchiveToolData.Overview getDigitalLifeArchiveOverview(
            Long taskId, AgentToolExecutionContext context) {
        support.requireTaskContext(context, taskId);
        HerbDigitalLifeArchiveVO archive = archiveService.getByTaskId(taskId);
        int narrated =
                (int)
                        archive.getStages().stream()
                                .filter(stage -> StringUtils.hasText(stage.getAiNarration()))
                                .count();
        return new DigitalLifeArchiveToolData.Overview(
                taskId,
                archive.getTraceCode(),
                archive.getTaskName(),
                archive.getSpeciesName(),
                archive.getBaseName(),
                archive.getStageCount(),
                archive.getValidStageCount(),
                archive.getImageCount(),
                narrated,
                archive.getPublicVisible(),
                StringUtils.hasText(archive.getTraceCode()),
                support.status(archive.getArchiveStatus()),
                archive.getStartTime(),
                archive.getEndTime());
    }

    public DigitalLifeArchiveToolData.StageList getDigitalLifeStages(
            Long taskId, AgentToolExecutionContext context) {
        support.requireTaskContext(context, taskId);
        List<DigitalLifeArchiveToolData.Stage> stages =
                archiveService.getByTaskId(taskId).getStages().stream().map(this::toStage).toList();
        return new DigitalLifeArchiveToolData.StageList(taskId, stages);
    }

    public DigitalLifeArchiveToolData.IntegrityStatus getArchiveIntegrityStatus(
            Long taskId, AgentToolExecutionContext context) {
        support.requireTaskContext(context, taskId);
        DigitalLifeIntegrityVO integrity = integrityService.verify(taskId);
        return new DigitalLifeArchiveToolData.IntegrityStatus(
                taskId,
                integrity.verified(),
                integrity.eventCount(),
                integrity.rootHash(),
                integrity.hashVersion(),
                integrity.generatedTime(),
                integrity.failedSequence(),
                integrity.failedEventType(),
                integrity.message());
    }

    @Override
    public List<AgentToolDefinition> definitions() {
        return List.of(
                support.readDefinition(OVERVIEW, "数字生命档案概览", "读取档案阶段、解说与公开摘要"),
                support.readDefinition(STAGES, "数字生命阶段", "批量读取数字生命阶段摘要"),
                support.readDefinition(INTEGRITY, "档案完整性", "只读校验现有 SHA-256 证据链"));
    }

    private DigitalLifeArchiveToolData.Stage toStage(HerbDigitalLifeStageVO stage) {
        return new DigitalLifeArchiveToolData.Stage(
                stage.getStageId(),
                stage.getSequence(),
                stage.getBatchId(),
                stage.getBatchName(),
                stage.getGrowthRecordId(),
                stage.getGrowthStage(),
                stage.getCollectedAt(),
                stage.getCollectorName(),
                stage.getBaseName(),
                stage.getLocationName(),
                support.status(stage.getAuditStatus()),
                support.status(stage.getDataStatus()),
                stage.getImages() == null ? null : stage.getImages().size(),
                stage.getImages() == null
                        ? List.of()
                        : stage.getImages().stream()
                                .map(
                                        image ->
                                                new DigitalLifeArchiveToolData.ImageEvidence(
                                                        image.getImageId(),
                                                        support.browserUrl(image.getImageUrl()),
                                                        image.getImageType(),
                                                        image.getPrimaryImage()))
                                .toList(),
                StringUtils.hasText(stage.getAiNarration()),
                stage.getNarrationSource(),
                stage.getNarrationGeneratedTime());
    }
}
