package com.bdis.modules.growth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.growth.entity.DigitalLifeNarrationEntity;
import com.bdis.modules.growth.mapper.DigitalLifeNarrationMapper;
import com.bdis.modules.growth.service.DigitalLifeNarrationGenerator;
import com.bdis.modules.growth.service.DigitalLifeNarrationService;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.vo.DigitalLifeNarrationGenerationVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeMetricsVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeRecognitionVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeStageVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DigitalLifeNarrationServiceImpl implements DigitalLifeNarrationService {

    static final String NARRATION_TYPE = "stage_summary";
    static final String PROMPT_VERSION = "stage-v1";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DigitalLifeNarrationServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy年M月d日");

    private final HerbDigitalLifeArchiveService archiveService;
    private final DigitalLifeNarrationMapper narrationMapper;
    private final DigitalLifeNarrationGenerator narrationGenerator;
    private final ObjectMapper objectMapper;

    public DigitalLifeNarrationServiceImpl(
            HerbDigitalLifeArchiveService archiveService,
            DigitalLifeNarrationMapper narrationMapper,
            DigitalLifeNarrationGenerator narrationGenerator,
            ObjectMapper objectMapper) {
        this.archiveService = archiveService;
        this.narrationMapper = narrationMapper;
        this.narrationGenerator = narrationGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public DigitalLifeNarrationGenerationVO generateForTask(Long taskId) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        if (currentUser.getRoleCodes().stream()
                .noneMatch(role -> Set.of("ADMIN", "TEACHER", "REVIEWER").contains(role))) {
            throw new ForbiddenException("仅管理端角色可以生成数字生命阶段解说");
        }
        HerbDigitalLifeArchiveVO archive = archiveService.getByTaskId(taskId);
        List<HerbDigitalLifeStageVO> validStages =
                archive.getStages().stream()
                        .filter(stage -> stage.getGrowthRecordId() != null)
                        .toList();
        if (validStages.isEmpty()) {
            return new DigitalLifeNarrationGenerationVO(0, 0, 0);
        }

        List<Long> growthRecordIds =
                validStages.stream().map(HerbDigitalLifeStageVO::getGrowthRecordId).toList();
        Map<Long, DigitalLifeNarrationEntity> existingByRecordId =
                narrationMapper
                        .selectList(
                                new LambdaQueryWrapper<DigitalLifeNarrationEntity>()
                                        .in(
                                                DigitalLifeNarrationEntity::getGrowthRecordId,
                                                growthRecordIds)
                                        .eq(
                                                DigitalLifeNarrationEntity::getNarrationType,
                                                NARRATION_TYPE)
                                        .eq(
                                                DigitalLifeNarrationEntity::getPromptVersion,
                                                PROMPT_VERSION))
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        DigitalLifeNarrationEntity::getGrowthRecordId,
                                        Function.identity(),
                                        (left, right) -> left));

        int generated = 0;
        int skipped = 0;
        int failed = 0;
        for (int index = 0; index < validStages.size(); index++) {
            HerbDigitalLifeStageVO stage = validStages.get(index);
            HerbDigitalLifeStageVO previous = index > 0 ? validStages.get(index - 1) : null;
            try {
                String canonicalInput = canonicalInput(stage, previous);
                String snapshot = sha256(canonicalInput);
                DigitalLifeNarrationEntity existing =
                        existingByRecordId.get(stage.getGrowthRecordId());
                if (existing != null && snapshot.equals(existing.getInputSnapshot())) {
                    skipped++;
                    continue;
                }

                String template = buildTemplate(stage, previous);
                DigitalLifeNarrationGenerator.GeneratedNarration narration =
                        narrationGenerator.generate(canonicalInput, template);
                saveNarration(archive, stage, existing, snapshot, narration, currentUser);
                generated++;
            } catch (RuntimeException exception) {
                failed++;
                LOGGER.warn(
                        "Failed to cache digital life narration, taskId={}, growthRecordId={}",
                        taskId,
                        stage.getGrowthRecordId(),
                        exception);
            }
        }
        return new DigitalLifeNarrationGenerationVO(generated, skipped, failed);
    }

    private String canonicalInput(HerbDigitalLifeStageVO stage, HerbDigitalLifeStageVO previous) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("sequence", stage.getSequence());
        input.put("collectedAt", Objects.toString(stage.getCollectedAt(), null));
        input.put("growthStage", stage.getGrowthStage());
        input.put("metrics", metricsSnapshot(stage.getMetrics()));
        input.put(
                "previousMetrics",
                metricsSnapshot(previous == null ? null : previous.getMetrics()));
        input.put("imageCount", stage.getImages() == null ? 0 : stage.getImages().size());
        input.put("recognition", recognitionSnapshot(stage.getRecognition()));
        input.put("auditStatus", stage.getAuditStatus());
        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("数字生命阶段快照序列化失败", exception);
        }
    }

    private Map<String, Object> metricsSnapshot(HerbDigitalLifeMetricsVO metrics) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (metrics == null) {
            return values;
        }
        values.put("plantHeight", metrics.getPlantHeight());
        values.put("stemDiameter", metrics.getStemDiameter());
        values.put("leafColor", metrics.getLeafColor());
        values.put("floweringStatus", metrics.getFloweringStatus());
        values.put("temperature", metrics.getTemperature());
        values.put("humidity", metrics.getHumidity());
        values.put("soilMoisture", metrics.getSoilMoisture());
        values.put("soilPh", metrics.getSoilPh());
        values.put("light", metrics.getLight());
        return values;
    }

    private Map<String, Object> recognitionSnapshot(HerbDigitalLifeRecognitionVO recognition) {
        if (recognition == null) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("speciesName", recognition.getSpeciesName());
        values.put("confidence", recognition.getConfidence());
        values.put("similarity", recognition.getSimilarity());
        values.put("needReview", recognition.getNeedReview());
        values.put("recognitionSource", recognition.getRecognitionSource());
        values.put("conclusion", recognition.getConclusion());
        return values;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前环境不支持 SHA-256", exception);
        }
    }

    private String buildTemplate(HerbDigitalLifeStageVO stage, HerbDigitalLifeStageVO previous) {
        List<String> sentences = new ArrayList<>();
        if (stage.getCollectedAt() != null) {
            sentences.add("本阶段记录于" + DATE_FORMATTER.format(stage.getCollectedAt()) + "。");
        } else {
            sentences.add("本阶段为连续观测档案中的第" + stage.getSequence() + "次记录。");
        }
        if (StringUtils.hasText(stage.getGrowthStage())) {
            sentences.add("记录的生长阶段为" + stage.getGrowthStage() + "。");
        }
        HerbDigitalLifeMetricsVO metrics = stage.getMetrics();
        if (metrics != null) {
            addMetricSentence(sentences, "株高", metrics.getPlantHeight(), " cm");
            addMetricSentence(sentences, "环境温度", metrics.getTemperature(), " ℃");
            addMetricSentence(sentences, "环境湿度", metrics.getHumidity(), "%");
        }
        int imageCount = stage.getImages() == null ? 0 : stage.getImages().size();
        sentences.add("本阶段收录现场图片" + imageCount + "张。");
        addDifferenceSentence(sentences, metrics, previous == null ? null : previous.getMetrics());
        if (stage.getRecognition() != null
                && StringUtils.hasText(stage.getRecognition().getConclusion())) {
            sentences.add("已有识别结论记录。" + stage.getRecognition().getConclusion().trim() + "。");
        }
        if (StringUtils.hasText(stage.getAuditStatus())) {
            sentences.add("审核状态为" + auditStatusLabel(stage.getAuditStatus()) + "。");
        }
        sentences.add("本说明仅整理已记录的连续观测事实，未记录字段不作推断。");
        sentences.add("内容不涉及医疗功效判断。");

        StringBuilder result = new StringBuilder();
        for (String sentence : sentences) {
            if (result.length() + sentence.length() <= 160) {
                result.append(sentence);
            }
        }
        return result.toString();
    }

    private void addMetricSentence(
            List<String> sentences, String label, BigDecimal value, String unit) {
        if (value != null) {
            sentences.add(label + "为" + decimal(value) + unit + "。");
        }
    }

    private void addDifferenceSentence(
            List<String> sentences,
            HerbDigitalLifeMetricsVO current,
            HerbDigitalLifeMetricsVO previous) {
        if (current == null || previous == null) {
            return;
        }
        List<String> changes = new ArrayList<>();
        addDifference(changes, "株高", current.getPlantHeight(), previous.getPlantHeight(), " cm");
        addDifference(changes, "温度", current.getTemperature(), previous.getTemperature(), " ℃");
        if (!changes.isEmpty()) {
            sentences.add("较上一阶段，" + String.join("，", changes) + "。");
        }
    }

    private void addDifference(
            List<String> changes,
            String label,
            BigDecimal current,
            BigDecimal previous,
            String unit) {
        if (current == null || previous == null) {
            return;
        }
        BigDecimal difference = current.subtract(previous);
        String sign = difference.signum() > 0 ? "+" : "";
        changes.add(label + sign + decimal(difference) + unit);
    }

    private String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String auditStatusLabel(String status) {
        return switch (status) {
            case "approved", "已通过" -> "已通过";
            case "submitted", "待审核" -> "待审核";
            case "rejected", "已驳回" -> "已驳回";
            case "draft", "草稿" -> "草稿";
            default -> "状态已记录";
        };
    }

    private void saveNarration(
            HerbDigitalLifeArchiveVO archive,
            HerbDigitalLifeStageVO stage,
            DigitalLifeNarrationEntity existing,
            String snapshot,
            DigitalLifeNarrationGenerator.GeneratedNarration narration,
            CurrentUser currentUser) {
        LocalDateTime now = LocalDateTime.now();
        DigitalLifeNarrationEntity entity =
                existing == null ? new DigitalLifeNarrationEntity() : existing;
        entity.setTaskId(archive.getTaskId());
        entity.setBatchId(stage.getBatchId());
        entity.setGrowthRecordId(stage.getGrowthRecordId());
        entity.setNarrationType(NARRATION_TYPE);
        entity.setNarrationText(narration.text());
        entity.setNarrationSource(narration.source());
        entity.setInputSnapshot(snapshot);
        entity.setPromptVersion(PROMPT_VERSION);
        entity.setModelName(narration.modelName());
        entity.setGeneratedBy(currentUser.getUserId());
        entity.setGeneratedTime(now);
        entity.setUpdatedBy(currentUser.getUserId());
        entity.setUpdatedAt(now);
        if (existing == null) {
            entity.setStatus(1);
            entity.setIsDeleted(0);
            entity.setVersion(0);
            entity.setCreatedBy(currentUser.getUserId());
            entity.setCreatedAt(now);
            narrationMapper.insert(entity);
        } else {
            narrationMapper.updateById(entity);
        }
    }
}
