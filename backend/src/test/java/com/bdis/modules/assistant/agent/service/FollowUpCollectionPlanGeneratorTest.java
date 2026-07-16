package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan.RequiredImageItem;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan.RequiredMetricItem;
import com.bdis.modules.assistant.agent.service.FollowUpCollectionPlanGenerator.GeneratedPlan;
import com.bdis.modules.assistant.agent.service.FollowUpCollectionPlanGenerator.PlanInput;
import com.bdis.modules.assistant.agent.tool.dto.CollectionTaskToolData;
import com.bdis.modules.assistant.agent.tool.dto.StatusValue;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceAnalysisVO;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceExplanation;
import com.bdis.modules.assistant.agent.vo.AgentFindingVO;
import com.bdis.modules.assistant.agent.vo.CrossModalFindingVO;
import com.bdis.modules.assistant.agent.vo.EvidenceGapVO;
import com.bdis.modules.assistant.agent.vo.MetricTrendVO;
import com.bdis.modules.assistant.agent.vo.StageEvidenceSummaryVO;
import com.bdis.modules.assistant.config.HerbAssistantProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

class FollowUpCollectionPlanGeneratorTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-16T02:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Test
    void springCanSelectProductionConstructor() {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.registerBean(HerbAssistantProperties.class, HerbAssistantProperties::new);
            context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            context.register(FollowUpCollectionPlanGenerator.class);

            context.refresh();

            assertThat(context.getBean(FollowUpCollectionPlanGenerator.class)).isNotNull();
        }
    }

    @Test
    void ruleTemplateIncludesMissingSoilPhRootAndAmbiguousRecognitionViews() {
        GeneratedPlan result = generator(false, null).generate(input());

        assertThat(result.planSource()).isEqualTo("RULE_TEMPLATE");
        assertThat(result.plan().requiredMetrics())
                .extracting(RequiredMetricItem::metricCode)
                .contains("soilPh");
        assertThat(result.plan().requiredImages())
                .extracting(RequiredImageItem::imageType)
                .contains("root", "leaf", "stem", "whole_plant", "environment");
    }

    @Test
    void invalidModelFieldsAreFilteredAndPastTimeFallsBack() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        FollowUpCollectionPlan candidate =
                new FollowUpCollectionPlan(
                        "AI 草稿",
                        "DAYS_AFTER",
                        -3,
                        LocalDateTime.of(2026, 7, 1, 8, 0),
                        LocalDateTime.of(2026, 7, 2, 8, 0),
                        List.of(
                                new RequiredMetricItem("fakeMetric", "伪造指标", true, "x", "x", null),
                                new RequiredMetricItem(
                                        "sampleWeight", "采样重量", true, "无法填写", "称重", "g"),
                                new RequiredMetricItem("soilPh", "错误名称", true, "复测", "记录", null)),
                        List.of(
                                new RequiredImageItem("fake_image", "伪造", 99, "x", "x"),
                                new RequiredImageItem("root", "错误名称", 99, "拍摄", "复测")),
                        List.of(),
                        List.of("完成复测"),
                        List.of(1L, 999L),
                        "规则依据",
                        "存在不确定性",
                        "MEDIUM");
        when(client.prompt()
                        .system(anyString())
                        .user(anyString())
                        .call()
                        .entity(FollowUpCollectionPlan.class))
                .thenReturn(candidate);

        GeneratedPlan result = generator(true, client).generate(input());

        assertThat(result.planSource()).isEqualTo("AI");
        assertThat(result.plan().requiredMetrics())
                .extracting(RequiredMetricItem::metricCode)
                .doesNotContain("fakeMetric", "sampleWeight")
                .contains("soilPh");
        assertThat(result.plan().requiredImages())
                .extracting(RequiredImageItem::imageType)
                .doesNotContain("fake_image")
                .contains("root");
        assertThat(
                        result.plan().requiredImages().stream()
                                .filter(image -> "root".equals(image.imageType()))
                                .findFirst()
                                .orElseThrow()
                                .minCount())
                .isEqualTo(5);
        assertThat(result.plan().recommendedStartTime()).isAfterOrEqualTo(LocalDateTime.now(CLOCK));
        assertThat(result.plan().sourceFindingIds()).containsExactly(1L);
    }

    @Test
    void modelFailureUsesRuleTemplateWithoutCollectorOrBusinessIds() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt()
                        .system(anyString())
                        .user(anyString())
                        .call()
                        .entity(FollowUpCollectionPlan.class))
                .thenThrow(new IllegalStateException("model failed"));

        GeneratedPlan result = generator(true, client).generate(input());

        assertThat(result.planSource()).isEqualTo("RULE_TEMPLATE");
        assertThat(result.plan().toString()).doesNotContain("collector", "采集员", "baseId");
    }

    @SuppressWarnings("unchecked")
    private FollowUpCollectionPlanGenerator generator(boolean useModel, ChatClient client) {
        ObjectProvider<ChatClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(client);
        HerbAssistantProperties properties = new HerbAssistantProperties();
        properties.setEnabled(useModel);
        properties.setMockEnabled(false);
        properties.setModel("test-model");
        return new FollowUpCollectionPlanGenerator(
                provider, properties, new ObjectMapper().findAndRegisterModules(), CLOCK);
    }

    private PlanInput input() {
        AgentFindingVO finding = new AgentFindingVO();
        finding.setId(1L);
        finding.setFindingType("MISSING_METRIC");
        finding.setSeverity("MEDIUM");
        return new PlanInput(1L, task(), analysis(), List.of(finding));
    }

    private CollectionTaskToolData.Overview task() {
        return new CollectionTaskToolData.Overview(
                12L,
                "TASK-12",
                "黄连任务",
                2L,
                "黄连",
                3L,
                "基地",
                "地点",
                null,
                new StatusValue("in_progress", "进行中"),
                LocalDateTime.of(2026, 7, 1, 8, 0),
                LocalDateTime.of(2026, 8, 31, 18, 0),
                3,
                3,
                3,
                2,
                0,
                3,
                0,
                new StatusValue("not_ready", "未就绪"));
    }

    private AgentEvidenceAnalysisVO analysis() {
        MetricTrendVO soilPh =
                new MetricTrendVO(
                        "soilPh",
                        "土壤 pH",
                        null,
                        0,
                        "ADJACENT_ONLY",
                        List.of(),
                        List.of(1L),
                        List.of(),
                        0,
                        0,
                        null,
                        null,
                        List.of());
        EvidenceGapVO root =
                new EvidenceGapVO("MISSING_ROOT_IMAGE", "LOW", 11L, "BATCH", "缺少根部图片", "补拍根部图片");
        EvidenceGapVO ambiguous =
                new EvidenceGapVO(
                        "RECOGNITION_AMBIGUOUS", "MEDIUM", 99L, "IMAGE", "识别不确定", "补充多角度图片");
        CrossModalFindingVO trend =
                new CrossModalFindingVO(
                        "METRIC_TREND_CHANGE",
                        "MEDIUM",
                        List.of(1L),
                        List.of("指标变化"),
                        List.of("实测值"),
                        "时间上同时出现",
                        "不能证明因果");
        AgentEvidenceExplanation explanation =
                new AgentEvidenceExplanation(
                        "规则分析完成",
                        List.of("三个阶段"),
                        List.of(),
                        List.of("不能证明因果"),
                        List.of("缺少根部图片"),
                        List.of("建议复测"),
                        "LOW");
        return new AgentEvidenceAnalysisVO(
                1L,
                12L,
                1,
                List.of(
                        new StageEvidenceSummaryVO(
                                1L,
                                11L,
                                1,
                                LocalDateTime.of(2026, 7, 15, 8, 0),
                                "地点",
                                "approved",
                                Map.of("plantHeight", new BigDecimal("10")),
                                1,
                                Map.of("leaf", 1),
                                List.of())),
                List.of(soilPh),
                List.of(trend),
                List.of(root, ambiguous),
                explanation,
                LocalDateTime.of(2026, 7, 16, 9, 0));
    }
}
