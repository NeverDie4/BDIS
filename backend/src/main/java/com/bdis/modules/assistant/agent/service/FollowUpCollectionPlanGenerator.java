package com.bdis.modules.assistant.agent.service;

import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan.RequiredImageItem;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan.RequiredMetricItem;
import com.bdis.modules.assistant.agent.tool.dto.CollectionTaskToolData;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceAnalysisVO;
import com.bdis.modules.assistant.agent.vo.AgentFindingVO;
import com.bdis.modules.assistant.agent.vo.EvidenceGapVO;
import com.bdis.modules.assistant.agent.vo.MetricTrendVO;
import com.bdis.modules.assistant.config.HerbAssistantProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FollowUpCollectionPlanGenerator {

  public static final Set<String> METRIC_WHITELIST =
      Set.of(
          "plantHeight",
          "stemDiameter",
          "temperature",
          "humidity",
          "soilMoisture",
          "soilPh",
          "light",
          "leafColor",
          "floweringStatus");
  public static final Set<String> IMAGE_WHITELIST =
      Set.of(
          "whole_plant",
          "leaf",
          "stem",
          "root",
          "flower",
          "fruit",
          "medicinal_part",
          "environment",
          "other");
  private static final Logger LOGGER =
      LoggerFactory.getLogger(FollowUpCollectionPlanGenerator.class);
  private static final String PROMPT_VERSION = "follow-up-plan-v1";
  private static final String SYSTEM_PROMPT =
      """
      你是本草数字孪生科研 Agent 的复测采集方案模块。
      只能基于输入事实和白名单生成 FollowUpCollectionPlan JSON。
      不得编造采集员、数据库 ID、疾病诊断或确定因果；时间只能是建议。
      指标、图片类型必须来自输入白名单，sourceFindingIds 只能来自输入发现项 ID。
      """;
  private static final Map<String, MetricMeta> METRIC_META = metricMeta();
  private static final Map<String, ImageMeta> IMAGE_META = imageMeta();

  private final ObjectProvider<ChatClient> chatClientProvider;
  private final HerbAssistantProperties properties;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  @Autowired
  public FollowUpCollectionPlanGenerator(
      @Qualifier("herbAssistantChatClient") ObjectProvider<ChatClient> chatClientProvider,
      HerbAssistantProperties properties,
      ObjectMapper objectMapper) {
    this(chatClientProvider, properties, objectMapper, Clock.systemDefaultZone());
  }

  FollowUpCollectionPlanGenerator(
      ObjectProvider<ChatClient> chatClientProvider,
      HerbAssistantProperties properties,
      ObjectMapper objectMapper,
      Clock clock) {
    this.chatClientProvider = chatClientProvider;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  public GeneratedPlan generate(PlanInput input) {
    FollowUpCollectionPlan fallback = ruleTemplate(input);
    if (!properties.isEnabled() || properties.isMockEnabled()) {
      return new GeneratedPlan(fallback, "RULE_TEMPLATE", PROMPT_VERSION, null);
    }
    ChatClient chatClient = chatClientProvider.getIfAvailable();
    if (chatClient == null) {
      return new GeneratedPlan(fallback, "RULE_TEMPLATE", PROMPT_VERSION, null);
    }
    try {
      FollowUpCollectionPlan candidate =
          chatClient
              .prompt()
              .system(SYSTEM_PROMPT)
              .user(prompt(input))
              .call()
              .entity(FollowUpCollectionPlan.class);
      FollowUpCollectionPlan validated = validate(input, fallback, candidate);
      return new GeneratedPlan(validated, "AI", PROMPT_VERSION, properties.getModel());
    } catch (RuntimeException exception) {
      LOGGER.warn(
          "Follow-up collection plan degraded to rules, agentTaskId={}, error={}",
          input.agentTaskId(),
          exception.getClass().getSimpleName());
      return new GeneratedPlan(fallback, "RULE_TEMPLATE", PROMPT_VERSION, null);
    }
  }

  FollowUpCollectionPlan ruleTemplate(PlanInput input) {
    LinkedHashMap<String, RequiredMetricItem> metrics = new LinkedHashMap<>();
    LinkedHashMap<String, RequiredImageItem> images = new LinkedHashMap<>();
    for (MetricTrendVO trend : input.analysis().metricTrends()) {
      if (!trend.missingStageIds().isEmpty()
          || trend.consecutiveDeclineCount() >= 2
          || trend.consecutiveRiseCount() >= 2) {
        addMetric(metrics, trend.metricCode(), reasonForTrend(trend));
      }
    }
    for (EvidenceGapVO gap : input.analysis().evidenceGaps()) {
      String imageType = imageTypeForFinding(gap.findingType());
      if (imageType != null) {
        addImage(images, imageType, gap.description());
      }
      if ("RECOGNITION_AMBIGUOUS".equals(gap.findingType())) {
        for (String type : List.of("leaf", "stem", "root", "whole_plant", "environment")) {
          addImage(images, type, "识别结果不确定，需要多角度且自然光下的可复核证据。");
        }
      }
    }
    if (input.findings().stream()
        .anyMatch(finding -> "MISSING_METRIC".equals(finding.getFindingType()))) {
      for (MetricTrendVO trend : input.analysis().metricTrends()) {
        if (!trend.missingStageIds().isEmpty()) {
          addMetric(metrics, trend.metricCode(), "既有阶段存在该指标缺失。");
        }
      }
    }
    if (input.analysis().findings().stream()
        .anyMatch(finding -> "METRIC_TREND_CHANGE".equals(finding.findingType()))) {
      for (String code : List.of("plantHeight", "soilMoisture", "temperature", "humidity")) {
        addMetric(metrics, code, "用于复核指标变化是否持续并与环境变化同时出现。");
      }
      addImage(images, "whole_plant", "保持与上一阶段相近角度拍摄，便于阶段对比。");
      addImage(images, "environment", "记录同期生长环境，仅用于证据关联。");
    }
    if (metrics.isEmpty()) {
      addMetric(metrics, "plantHeight", "形成下一阶段可比较的生长指标。");
      addMetric(metrics, "soilMoisture", "形成下一阶段可比较的环境指标。");
    }
    if (images.isEmpty()) {
      addImage(images, "whole_plant", "保留同角度整株阶段证据。");
      addImage(images, "environment", "保留现场环境证据。");
    }

    TimeWindow window = recommendedWindow(input);
    List<Long> sourceFindingIds =
        input.findings().stream()
            .map(AgentFindingVO::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    List<String> criteria =
        List.of(
            "所有必填指标均有本轮真实采集值，不以 0 代替缺失。",
            "所有必拍图片达到最小数量且图片类型标记完整。",
            "采集地点和基地与原连续观测任务一致，差异需说明。",
            "数据按现有业务流程保存，方案本身不代表审核通过。");
    return new FollowUpCollectionPlan(
        "围绕当前证据缺口完成一轮可复核复测，判断已观察到的变化是否在下一阶段持续。",
        window.type(),
        window.afterDays(),
        window.start(),
        window.end(),
        List.copyOf(metrics.values()),
        List.copyOf(images.values()),
        locationOptionalItems(input),
        criteria,
        sourceFindingIds,
        "方案由确定性规则根据指标缺失、趋势变化、图片类型覆盖和识别不确定性生成。",
        window.uncertainty() + " 当前证据只能支持复测建议，不能证明具体原因、病害或产量影响。",
        priority(input));
  }

  FollowUpCollectionPlan validate(
      PlanInput input, FollowUpCollectionPlan fallback, FollowUpCollectionPlan candidate) {
    if (candidate == null) {
      return fallback;
    }
    Map<String, RequiredMetricItem> metrics =
        filteredMetrics(candidate.requiredMetrics()).stream()
            .collect(
                Collectors.toMap(
                    RequiredMetricItem::metricCode,
                    Function.identity(),
                    (left, right) -> left,
                    LinkedHashMap::new));
    fallback.requiredMetrics().forEach(item -> metrics.putIfAbsent(item.metricCode(), item));
    Map<String, RequiredImageItem> images =
        filteredImages(candidate.requiredImages()).stream()
            .collect(
                Collectors.toMap(
                    RequiredImageItem::imageType,
                    Function.identity(),
                    (left, right) -> left,
                    LinkedHashMap::new));
    fallback.requiredImages().forEach(item -> images.putIfAbsent(item.imageType(), item));
    TimeWindow candidateWindow = validateWindow(input, candidate, fallback);
    Set<Long> allowedIds =
        input.findings().stream()
            .map(AgentFindingVO::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    List<Long> sourceIds =
        safe(candidate.sourceFindingIds()).stream()
            .filter(allowedIds::contains)
            .distinct()
            .toList();
    if (sourceIds.isEmpty()) {
      sourceIds = fallback.sourceFindingIds();
    }
    return new FollowUpCollectionPlan(
        text(candidate.objective(), fallback.objective()),
        candidateWindow.type(),
        candidateWindow.afterDays(),
        candidateWindow.start(),
        candidateWindow.end(),
        List.copyOf(metrics.values()),
        List.copyOf(images.values()),
        cleanTexts(candidate.optionalItems(), fallback.optionalItems()),
        cleanTexts(candidate.completionCriteria(), fallback.completionCriteria()),
        sourceIds,
        text(candidate.rationale(), fallback.rationale()),
        text(candidate.uncertainty(), fallback.uncertainty()),
        Set.of("LOW", "MEDIUM", "HIGH").contains(candidate.priority())
            ? candidate.priority()
            : fallback.priority());
  }

  private List<RequiredMetricItem> filteredMetrics(List<RequiredMetricItem> items) {
    return safe(items).stream()
        .filter(Objects::nonNull)
        .filter(item -> METRIC_WHITELIST.contains(item.metricCode()))
        .map(
            item -> {
              MetricMeta meta = METRIC_META.get(item.metricCode());
              return new RequiredMetricItem(
                  item.metricCode(),
                  meta.name(),
                  true,
                  text(item.reason(), "用于本轮复测。"),
                  text(item.inputHint(), meta.hint()),
                  meta.unit());
            })
        .toList();
  }

  private List<RequiredImageItem> filteredImages(List<RequiredImageItem> items) {
    return safe(items).stream()
        .filter(Objects::nonNull)
        .filter(item -> IMAGE_WHITELIST.contains(item.imageType()))
        .map(
            item -> {
              ImageMeta meta = IMAGE_META.get(item.imageType());
              int count = item.minCount() == null ? 1 : Math.max(1, Math.min(5, item.minCount()));
              return new RequiredImageItem(
                  item.imageType(),
                  meta.name(),
                  count,
                  text(item.shootingGuidance(), meta.guidance()),
                  text(item.reason(), "补充可复核图片证据。"));
            })
        .toList();
  }

  private TimeWindow validateWindow(
      PlanInput input, FollowUpCollectionPlan candidate, FollowUpCollectionPlan fallback) {
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDateTime start = candidate.recommendedStartTime();
    LocalDateTime end = candidate.recommendedEndTime();
    if (start == null
        || end == null
        || start.isBefore(now)
        || end.isBefore(start)
        || input.task().plannedEndTime() != null && end.isAfter(input.task().plannedEndTime())) {
      return new TimeWindow(
          fallback.recommendedTimeType(),
          fallback.recommendedAfterDays(),
          fallback.recommendedStartTime(),
          fallback.recommendedEndTime(),
          fallback.uncertainty());
    }
    return new TimeWindow(
        Set.of("ASAP", "DAYS_AFTER", "HISTORICAL_INTERVAL", "TASK_WINDOW")
                .contains(candidate.recommendedTimeType())
            ? candidate.recommendedTimeType()
            : fallback.recommendedTimeType(),
        Math.max(0, (int) ChronoUnit.DAYS.between(now.toLocalDate(), start.toLocalDate())),
        start,
        end,
        fallback.uncertainty());
  }

  private TimeWindow recommendedWindow(PlanInput input) {
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDateTime taskEnd = input.task().plannedEndTime();
    if (taskEnd != null && !taskEnd.isAfter(now)) {
      return new TimeWindow("TASK_WINDOW", null, null, null, "原任务计划结束时间已过，需由用户重新确定有效时间窗口。");
    }
    boolean urgentImageGap =
        input.analysis().evidenceGaps().stream()
            .anyMatch(gap -> imageTypeForFinding(gap.findingType()) != null);
    boolean trendChange =
        input.analysis().findings().stream()
            .anyMatch(finding -> "METRIC_TREND_CHANGE".equals(finding.findingType()));
    long afterDays;
    String type;
    if (trendChange) {
      afterDays = 3;
      type = "DAYS_AFTER";
    } else if (urgentImageGap) {
      afterDays = 0;
      type = "ASAP";
    } else {
      afterDays = historicalIntervalDays(input.analysis());
      type = afterDays > 0 ? "HISTORICAL_INTERVAL" : "DAYS_AFTER";
      if (afterDays <= 0) {
        afterDays = 3;
      }
    }
    LocalDateTime start = now.plusDays(afterDays).truncatedTo(ChronoUnit.HOURS);
    LocalDateTime end = trendChange ? now.plusDays(7) : start.plusDays(3);
    if (taskEnd != null && end.isAfter(taskEnd)) {
      end = taskEnd;
    }
    if (end.isBefore(start)) {
      start = now;
      end = taskEnd;
      afterDays = 0;
      type = "TASK_WINDOW";
    }
    return new TimeWindow(
        type, Math.toIntExact(afterDays), start, end, "推荐时间依据历史间隔和证据缺口生成，仅为建议，需用户结合现场条件确认。");
  }

  private long historicalIntervalDays(AgentEvidenceAnalysisVO analysis) {
    List<LocalDateTime> times =
        analysis.stages().stream()
            .map(stage -> stage.collectedAt())
            .filter(Objects::nonNull)
            .sorted()
            .toList();
    if (times.size() < 2) {
      return 0;
    }
    long totalHours = 0;
    for (int index = 1; index < times.size(); index++) {
      totalHours += Duration.between(times.get(index - 1), times.get(index)).toHours();
    }
    return Math.max(1, Math.round((double) totalHours / (times.size() - 1) / 24));
  }

  private String prompt(PlanInput input) {
    Map<String, Object> prompt = new LinkedHashMap<>();
    prompt.put("taskSummary", input.task());
    prompt.put("stageSummary", input.analysis().stages());
    prompt.put("confirmedFacts", input.analysis().explanation().confirmedFacts());
    prompt.put("findings", input.findings());
    prompt.put("evidenceGaps", input.analysis().evidenceGaps());
    prompt.put("metricWhitelist", METRIC_WHITELIST);
    prompt.put("imageTypeWhitelist", IMAGE_WHITELIST);
    prompt.put("currentTime", LocalDateTime.now(clock));
    prompt.put("timeRule", "趋势复测 3-7 天，缺图尽快，其他优先历史平均间隔且不得超过任务结束时间");
    try {
      return objectMapper.writeValueAsString(prompt);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Follow-up plan prompt serialization failed", exception);
    }
  }

  private void addMetric(
      Map<String, RequiredMetricItem> metrics, String metricCode, String reason) {
    MetricMeta meta = METRIC_META.get(metricCode);
    if (meta == null) {
      return;
    }
    metrics.putIfAbsent(
        metricCode,
        new RequiredMetricItem(metricCode, meta.name(), true, reason, meta.hint(), meta.unit()));
  }

  private void addImage(Map<String, RequiredImageItem> images, String imageType, String reason) {
    ImageMeta meta = IMAGE_META.get(imageType);
    if (meta == null) {
      return;
    }
    images.putIfAbsent(
        imageType, new RequiredImageItem(imageType, meta.name(), 1, meta.guidance(), reason));
  }

  private String reasonForTrend(MetricTrendVO trend) {
    return trend.missingStageIds().isEmpty()
        ? "该指标在连续阶段出现方向性变化，需要复测是否持续。"
        : "既有阶段存在该指标缺失，需要补充可比较数据。";
  }

  private String imageTypeForFinding(String findingType) {
    return switch (findingType) {
      case "MISSING_ROOT_IMAGE" -> "root";
      case "MISSING_LEAF_IMAGE" -> "leaf";
      case "MISSING_WHOLE_PLANT_IMAGE" -> "whole_plant";
      case "MISSING_ENVIRONMENT_IMAGE" -> "environment";
      default -> null;
    };
  }

  private List<String> locationOptionalItems(PlanInput input) {
    List<String> items = new ArrayList<>();
    if (input.analysis().evidenceGaps().stream()
        .anyMatch(
            gap ->
                Set.of("LOCATION_CHANGE_SUSPECTED", "LOCATION_MISSING")
                    .contains(gap.findingType()))) {
      items.add("确认基地和采集地点；沿用项目现有定位能力，不额外要求高精度 GPS。");
    }
    items.add("记录现场天气或异常采样条件，便于解释测量差异。");
    return List.copyOf(items);
  }

  private String priority(PlanInput input) {
    return input.findings().stream()
            .anyMatch(finding -> Set.of("HIGH", "CRITICAL").contains(finding.getSeverity()))
        ? "HIGH"
        : "MEDIUM";
  }

  private List<String> cleanTexts(List<String> values, List<String> fallback) {
    List<String> cleaned =
        safe(values).stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .limit(12)
            .toList();
    return cleaned.isEmpty() ? fallback : cleaned;
  }

  private String text(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim() : fallback;
  }

  private <T> List<T> safe(List<T> values) {
    return values == null ? List.of() : values;
  }

  private static Map<String, MetricMeta> metricMeta() {
    Map<String, MetricMeta> values = new LinkedHashMap<>();
    values.put("plantHeight", new MetricMeta("株高", "cm", "填写真实测量值"));
    values.put("stemDiameter", new MetricMeta("茎粗", "mm", "填写真实测量值"));
    values.put("temperature", new MetricMeta("温度", "°C", "记录采集时环境温度"));
    values.put("humidity", new MetricMeta("湿度", "%", "记录采集时环境湿度"));
    values.put("soilMoisture", new MetricMeta("土壤湿度", "%", "使用同口径设备测量"));
    values.put("soilPh", new MetricMeta("土壤 pH", null, "使用同口径设备测量"));
    values.put("light", new MetricMeta("光照", "lx", "记录采集时光照"));
    values.put("leafColor", new MetricMeta("叶色", null, "按现场真实观察填写"));
    values.put("floweringStatus", new MetricMeta("开花状态", null, "按现场真实观察填写"));
    return Map.copyOf(values);
  }

  private static Map<String, ImageMeta> imageMeta() {
    Map<String, ImageMeta> values = new LinkedHashMap<>();
    values.put("whole_plant", new ImageMeta("整株", "保持与上一阶段相近角度，主体完整清晰"));
    values.put("leaf", new ImageMeta("叶片", "自然光下拍摄叶片正反面和细节"));
    values.put("stem", new ImageMeta("茎部", "拍摄茎部及与叶片连接位置"));
    values.put("root", new ImageMeta("根部", "拍摄根或根茎整体与局部细节"));
    values.put("flower", new ImageMeta("花", "拍摄花部整体和细节"));
    values.put("fruit", new ImageMeta("果实", "拍摄果实整体和细节"));
    values.put("medicinal_part", new ImageMeta("药用部位", "拍摄药用部位整体和细节"));
    values.put("environment", new ImageMeta("生长环境", "自然光下拍摄周边环境和植株关系"));
    values.put("other", new ImageMeta("其他", "说明图片证据用途"));
    return Map.copyOf(values);
  }

  public record PlanInput(
      Long agentTaskId,
      CollectionTaskToolData.Overview task,
      AgentEvidenceAnalysisVO analysis,
      List<AgentFindingVO> findings) {}

  public record GeneratedPlan(
      FollowUpCollectionPlan plan, String planSource, String promptVersion, String modelName) {}

  private record MetricMeta(String name, String unit, String hint) {}

  private record ImageMeta(String name, String guidance) {}

  private record TimeWindow(
      String type, Integer afterDays, LocalDateTime start, LocalDateTime end, String uncertainty) {}
}
