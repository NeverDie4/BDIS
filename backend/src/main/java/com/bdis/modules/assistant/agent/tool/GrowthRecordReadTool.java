package com.bdis.modules.assistant.agent.tool;

import com.bdis.modules.assistant.agent.tool.dto.GrowthRecordToolData;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.vo.GrowthAuditHistoryVO;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeMetricsVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeStageVO;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class GrowthRecordReadTool implements AgentBusinessTool {

    public static final String OVERVIEW = "growth_record.overview";
    public static final String BY_BATCH = "growth_record.by_batch";
    public static final String LIST_BY_TASK = "growth_record.list_by_task";
    public static final String AUDIT_HISTORY = "growth_record.audit_history";

    private final GrowthRecordService growthRecordService;
    private final HerbDigitalLifeArchiveService archiveService;
    private final AgentReadToolSupport support;

    public GrowthRecordReadTool(
            GrowthRecordService growthRecordService,
            HerbDigitalLifeArchiveService archiveService,
            AgentReadToolSupport support) {
        this.growthRecordService = growthRecordService;
        this.archiveService = archiveService;
        this.support = support;
    }

    public GrowthRecordToolData.Overview getGrowthRecordOverview(
            Long recordId, AgentToolExecutionContext context) {
        GrowthRecordVO record = growthRecordService.detail(recordId);
        support.requireTaskContext(context, record.getTaskId());
        return toOverview(record, record.getImages() == null ? null : record.getImages().size());
    }

    public GrowthRecordToolData.Overview getGrowthRecordByBatch(
            Long batchId, AgentToolExecutionContext context) {
        GrowthRecordVO record = growthRecordService.getByBatchId(batchId);
        support.requireTaskContext(context, record.getTaskId());
        return toOverview(record, record.getImages() == null ? null : record.getImages().size());
    }

    public GrowthRecordToolData.RecordList listTaskGrowthRecords(
            Long taskId, AgentToolExecutionContext context) {
        support.requireTaskContext(context, taskId);
        HerbDigitalLifeArchiveVO archive = archiveService.getByTaskId(taskId);
        List<GrowthRecordToolData.Overview> records =
                archive.getStages().stream()
                        .filter(stage -> stage.getGrowthRecordId() != null)
                        .map(stage -> toOverview(archive, stage))
                        .toList();
        return new GrowthRecordToolData.RecordList(taskId, records);
    }

    public GrowthRecordToolData.AuditHistory getGrowthAuditHistory(
            Long recordId, AgentToolExecutionContext context) {
        GrowthRecordVO record = growthRecordService.detail(recordId);
        support.requireTaskContext(context, record.getTaskId());
        List<GrowthRecordToolData.AuditEvent> events =
                growthRecordService.auditHistory(recordId).stream()
                        .map(this::toAuditEvent)
                        .toList();
        return new GrowthRecordToolData.AuditHistory(recordId, events);
    }

    @Override
    public List<AgentToolDefinition> definitions() {
        return List.of(
                support.readDefinition(OVERVIEW, "生长记录概览", "读取生长记录、指标和字段空值摘要"),
                support.readDefinition(BY_BATCH, "批次生长记录", "按批次读取唯一生长记录"),
                support.readDefinition(LIST_BY_TASK, "任务生长记录", "批量读取任务下生长阶段"),
                support.readDefinition(AUDIT_HISTORY, "生长审核历史", "读取已脱敏的审核状态变化"));
    }

    private GrowthRecordToolData.Overview toOverview(GrowthRecordVO record, Integer imageCount) {
        return new GrowthRecordToolData.Overview(
                record.getId(),
                record.getTaskId(),
                record.getTaskName(),
                record.getBatchId(),
                record.getBatchName(),
                record.getSpeciesName(),
                record.getBaseName(),
                record.getCollectorName(),
                record.getCollectedAt(),
                support.status(record.getReviewStatus()),
                new GrowthRecordToolData.EnvironmentMetrics(
                        record.getTemperature(),
                        record.getHumidity(),
                        record.getSoilMoisture(),
                        record.getSoilPh(),
                        record.getLight(),
                        record.getSoilType(),
                        record.getWeather(),
                        record.getLongitude(),
                        record.getLatitude()),
                new GrowthRecordToolData.GrowthMetrics(
                        record.getGrowthStage(),
                        record.getPlantHeight(),
                        record.getStemDiameter(),
                        record.getLeafColor(),
                        record.getFloweringStatus(),
                        record.getSampleWeight(),
                        record.getGrowthEvaluation()),
                imageCount,
                emptyFields(record));
    }

    private GrowthRecordToolData.Overview toOverview(
            HerbDigitalLifeArchiveVO archive, HerbDigitalLifeStageVO stage) {
        HerbDigitalLifeMetricsVO metrics = stage.getMetrics();
        return new GrowthRecordToolData.Overview(
                stage.getGrowthRecordId(),
                archive.getTaskId(),
                archive.getTaskName(),
                stage.getBatchId(),
                stage.getBatchName(),
                archive.getSpeciesName(),
                stage.getBaseName(),
                stage.getCollectorName(),
                stage.getCollectedAt(),
                support.status(stage.getAuditStatus()),
                new GrowthRecordToolData.EnvironmentMetrics(
                        value(metrics, Metric.TEMPERATURE),
                        value(metrics, Metric.HUMIDITY),
                        value(metrics, Metric.SOIL_MOISTURE),
                        value(metrics, Metric.SOIL_PH),
                        value(metrics, Metric.LIGHT),
                        null,
                        null,
                        stage.getLongitude(),
                        stage.getLatitude()),
                new GrowthRecordToolData.GrowthMetrics(
                        stage.getGrowthStage(),
                        value(metrics, Metric.PLANT_HEIGHT),
                        value(metrics, Metric.STEM_DIAMETER),
                        metrics == null ? null : metrics.getLeafColor(),
                        metrics == null ? null : metrics.getFloweringStatus(),
                        null,
                        metrics == null ? null : metrics.getGrowthEvaluation()),
                stage.getImages() == null ? null : stage.getImages().size(),
                emptyFields(stage));
    }

    private java.math.BigDecimal value(HerbDigitalLifeMetricsVO metrics, Metric metric) {
        if (metrics == null) {
            return null;
        }
        return switch (metric) {
            case TEMPERATURE -> metrics.getTemperature();
            case HUMIDITY -> metrics.getHumidity();
            case SOIL_MOISTURE -> metrics.getSoilMoisture();
            case SOIL_PH -> metrics.getSoilPh();
            case LIGHT -> metrics.getLight();
            case PLANT_HEIGHT -> metrics.getPlantHeight();
            case STEM_DIAMETER -> metrics.getStemDiameter();
        };
    }

    private GrowthRecordToolData.AuditEvent toAuditEvent(GrowthAuditHistoryVO event) {
        return new GrowthRecordToolData.AuditEvent(
                event.getActionType(),
                support.status(event.getBeforeStatus()),
                support.status(event.getAfterStatus()),
                event.getOperatorRole(),
                event.getOperateTime());
    }

    private List<String> emptyFields(GrowthRecordVO record) {
        List<String> empty = new ArrayList<>();
        addEmpty(empty, "growthStage", record.getGrowthStage());
        addEmpty(empty, "plantHeight", record.getPlantHeight());
        addEmpty(empty, "temperature", record.getTemperature());
        addEmpty(empty, "humidity", record.getHumidity());
        addEmpty(empty, "soilMoisture", record.getSoilMoisture());
        addEmpty(empty, "soilPh", record.getSoilPh());
        addEmpty(empty, "longitude", record.getLongitude());
        addEmpty(empty, "latitude", record.getLatitude());
        return empty;
    }

    private List<String> emptyFields(HerbDigitalLifeStageVO stage) {
        List<String> empty = new ArrayList<>();
        HerbDigitalLifeMetricsVO metrics = stage.getMetrics();
        addEmpty(empty, "growthStage", stage.getGrowthStage());
        addEmpty(empty, "metrics", metrics);
        addEmpty(empty, "longitude", stage.getLongitude());
        addEmpty(empty, "latitude", stage.getLatitude());
        return empty;
    }

    private void addEmpty(List<String> empty, String field, Object value) {
        if (value == null || value instanceof String text && !StringUtils.hasText(text)) {
            empty.add(field);
        }
    }

    private enum Metric {
        TEMPERATURE,
        HUMIDITY,
        SOIL_MOISTURE,
        SOIL_PH,
        LIGHT,
        PLANT_HEIGHT,
        STEM_DIAMETER
    }
}
