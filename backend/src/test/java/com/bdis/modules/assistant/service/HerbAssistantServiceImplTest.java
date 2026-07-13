package com.bdis.modules.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.assistant.client.ArkResponsesClient;
import com.bdis.modules.assistant.config.HerbAssistantProperties;
import com.bdis.modules.assistant.dto.HerbAssistantBatchExplainRequest;
import com.bdis.modules.assistant.dto.HerbAssistantChatRequest;
import com.bdis.modules.assistant.dto.HerbAssistantChatResponse;
import com.bdis.modules.assistant.dto.HerbAssistantImageExplainRequest;
import com.bdis.modules.assistant.knowledge.config.HerbAssistantRagProperties;
import com.bdis.modules.assistant.mapper.HerbAssistantBatchContextMapper;
import com.bdis.modules.assistant.mapper.HerbAssistantImageContextMapper;
import com.bdis.modules.assistant.service.impl.HerbAssistantServiceImpl;
import com.bdis.modules.assistant.tool.HerbAssistantTools;
import com.bdis.modules.assistant.vo.HerbAssistantBatchContextVO;
import com.bdis.modules.assistant.vo.HerbAssistantBatchExplainResponse;
import com.bdis.modules.assistant.vo.HerbAssistantImageContextVO;
import com.bdis.modules.assistant.vo.HerbAssistantImageExplainContextVO;
import com.bdis.modules.assistant.vo.HerbAssistantImageExplainResponse;
import com.bdis.modules.assistant.vo.HerbAssistantMatchContextVO;
import com.bdis.modules.assistant.vo.HerbAssistantRecognitionContextVO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class HerbAssistantServiceImplTest {

    @Mock private ObjectProvider<ChatClient> chatClientProvider;

    @Mock private HerbAiChatHistoryService historyService;

    @Mock private HerbAssistantBatchContextMapper batchContextMapper;

    @Mock private HerbAssistantImageContextMapper imageContextMapper;

    @Mock private HerbAssistantTools herbAssistantTools;

    @Mock private HerbAssistantRagService ragService;

    private HerbAssistantProperties properties;
    private HerbAssistantRagProperties ragProperties;
    private HerbAssistantService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new CurrentUser(
                                        1L,
                                        "admin",
                                        "admin",
                                        null,
                                        null,
                                        java.util.Set.of("ADMIN"),
                                        java.util.Set.of(1L),
                                        java.util.Set.of("*")),
                                null,
                                List.of()));
        properties = new HerbAssistantProperties();
        ragProperties = new HerbAssistantRagProperties();
        ragProperties.setEnabled(false);
        service =
                new HerbAssistantServiceImpl(
                        properties,
                        ragProperties,
                        chatClientProvider,
                        historyService,
                        ragService,
                        batchContextMapper,
                        imageContextMapper,
                        herbAssistantTools);
        lenient()
                .when(historyService.recordUserMessage(any()))
                .thenAnswer(
                        invocation -> {
                            HerbAssistantChatRequest request = invocation.getArgument(0);
                            return request.getSessionId() == null
                                    ? "generated-session"
                                    : request.getSessionId();
                        });
    }

    @Test
    void mockChatReturnsFixedAnswerAndKeepsSessionId() {
        HerbAssistantChatRequest request = request("这个系统怎么进行批次采集？");
        request.setSessionId("test-session-001");

        HerbAssistantChatResponse response = service.chat(request);

        assertThat(response.getSessionId()).isEqualTo("test-session-001");
        assertThat(response.getAnswer()).contains("AI 小助手模拟回答");
        verify(historyService)
                .recordAssistantMessage(
                        "test-session-001", response.getAnswer(), "mock", request.getSource());
        verify(chatClientProvider, never()).getObject();
    }

    @Test
    void mockChatGeneratesSessionIdWhenMissing() {
        HerbAssistantChatResponse response = service.chat(request("如何上传图片？"));

        assertThat(response.getSessionId()).isEqualTo("generated-session");
    }

    @Test
    void chatRejectsBlankMessage() {
        assertThatThrownBy(() -> service.chat(request(" ")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("消息不能为空");
    }

    @Test
    void chatRejectsDisabledAssistant() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> service.chat(request("如何上传图片？")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 小助手暂未启用");
    }

    @Test
    void realChatRejectsMissingModelConfiguration() {
        properties.setMockEnabled(false);

        assertThatThrownBy(() -> service.chat(request("如何上传图片？")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 模型 baseUrl 未配置");
        verify(chatClientProvider, never()).getObject();
    }

    @Test
    void realChatAddsSystemGuideContextToPrompt() {
        properties.setMockEnabled(false);
        properties.setBaseUrl("https://example.com/v1");
        properties.setModel("test-model");
        properties.setApiKey("test-key");
        try (MockedStatic<ArkResponsesClient> mocked = mockStatic(ArkResponsesClient.class)) {
            mocked.when(() -> ArkResponsesClient.chat(eq(properties), anyString(), anyString()))
                    .thenReturn("请先在 PC 端创建采集任务。");

            HerbAssistantChatResponse response = service.chat(request("批次采集流程是什么？"));

            mocked.verify(
                    () ->
                            ArkResponsesClient.chat(
                                    eq(properties),
                                    argThat(
                                            prompt ->
                                                    prompt.contains("请优先调用系统工具查询真实业务数据")
                                                            && prompt.contains(
                                                                    "不要编造系统中不存在的页面、接口、数据或功能")),
                                    eq("批次采集流程是什么？")));
            assertThat(response.getAnswer()).isEqualTo("请先在 PC 端创建采集任务。");
        }
    }

    @Test
    void mockBatchExplainUsesBatchDataAndKeepsSessionId() {
        HerbAssistantBatchExplainRequest request = batchExplainRequest("这个批次为什么需要复核？");
        request.setSessionId("test-session-001");
        when(batchContextMapper.selectBatchContextById(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(batchContext());
        when(batchContextMapper.selectImageContextsByBatchId(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(List.of(imageContext()));

        HerbAssistantBatchExplainResponse response = service.explainBatch(1L, request);

        assertThat(response.getBatchId()).isEqualTo(1L);
        assertThat(response.getSessionId()).isEqualTo("test-session-001");
        assertThat(response.getAnswer())
                .contains("共绑定 4 张图片")
                .contains("已识别 4 张")
                .contains("待复核 2 张")
                .contains("黄连")
                .contains("normal");
        verify(historyService)
                .recordAssistantMessage(
                        "test-session-001", response.getAnswer(), "mock", request.getSource());
        verify(chatClientProvider, never()).getObject();
    }

    @Test
    void batchExplainRejectsMissingBatch() {
        when(batchContextMapper.selectBatchContextById(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(null);

        assertThatThrownBy(() -> service.explainBatch(404L, batchExplainRequest(null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("批次不存在");
    }

    @Test
    void realBatchExplainAddsBatchContextToPrompt() {
        properties.setMockEnabled(false);
        properties.setBaseUrl("https://example.com/v1");
        properties.setModel("test-model");
        properties.setApiKey("test-key");
        when(batchContextMapper.selectBatchContextById(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(batchContext());
        when(batchContextMapper.selectImageContextsByBatchId(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(List.of(imageContext()));
        try (MockedStatic<ArkResponsesClient> mocked = mockStatic(ArkResponsesClient.class)) {
            mocked.when(() -> ArkResponsesClient.chat(eq(properties), anyString(), anyString()))
                    .thenReturn("该批次仍有 2 张图片需要复核。");

            HerbAssistantBatchExplainResponse response =
                    service.explainBatch(1L, batchExplainRequest("这个批次为什么需要复核？"));

            mocked.verify(
                    () ->
                            ArkResponsesClient.chat(
                                    eq(properties),
                                    argThat(prompt -> prompt.contains("请根据后端提供的批次数据回答")),
                                    argThat(
                                            prompt ->
                                                    prompt.contains("待复核数量：2")
                                                            && prompt.contains("图片ID 10")
                                                            && prompt.contains("这个批次为什么需要复核？")
                                                            && !prompt.contains("featureVector"))));
            assertThat(response.getAnswer()).isEqualTo("该批次仍有 2 张图片需要复核。");
        }
    }

    @Test
    void mockImageExplainUsesImageDataAndKeepsSessionId() {
        HerbAssistantImageExplainRequest request = imageExplainRequest("这张图片为什么识别为黄连？");
        request.setSessionId("test-session-001");
        when(imageContextMapper.selectImageContextById(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(imageExplainContext());
        when(imageContextMapper.selectTopMatchesByImageId(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(List.of(matchContext()));
        when(imageContextMapper.selectLatestRecognitionByImageId(
                        anyLong(), anyLong(), anyBoolean()))
                .thenReturn(recognitionContext());

        HerbAssistantImageExplainResponse response = service.explainImage(12L, request);

        assertThat(response.getImageId()).isEqualTo(12L);
        assertThat(response.getSessionId()).isEqualTo("test-session-001");
        assertThat(response.getAnswer())
                .contains("最终识别结果为 黄连")
                .contains("置信度为 91.23%")
                .contains("结果来源为 local_match")
                .contains("复核状态为 pending");
        verify(historyService)
                .recordAssistantMessage(
                        "test-session-001", response.getAnswer(), "mock", request.getSource());
        verify(chatClientProvider, never()).getObject();
    }

    @Test
    void mockImageExplainSupportsMissingMatchesAndRecognition() {
        when(imageContextMapper.selectImageContextById(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(imageExplainContext());
        when(imageContextMapper.selectTopMatchesByImageId(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(List.of());
        when(imageContextMapper.selectLatestRecognitionByImageId(
                        anyLong(), anyLong(), anyBoolean()))
                .thenReturn(null);

        HerbAssistantImageExplainResponse response =
                service.explainImage(12L, imageExplainRequest(null));

        assertThat(response.getAnswer())
                .contains("最终识别结果为 黄连")
                .contains("置信度为 91.23%")
                .contains("建议管理员结合 TopK 图谱候选和大模型辅助结果进行确认");
        verify(chatClientProvider, never()).getObject();
    }

    @Test
    void imageExplainRejectsMissingImage() {
        when(imageContextMapper.selectImageContextById(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(null);

        assertThatThrownBy(() -> service.explainImage(404L, imageExplainRequest(null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("图片不存在");
    }

    @Test
    void realImageExplainAddsImageContextToPrompt() {
        properties.setMockEnabled(false);
        properties.setBaseUrl("https://example.com/v1");
        properties.setModel("test-model");
        properties.setApiKey("test-key");
        when(imageContextMapper.selectImageContextById(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(imageExplainContext());
        when(imageContextMapper.selectTopMatchesByImageId(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(List.of(matchContext()));
        when(imageContextMapper.selectLatestRecognitionByImageId(
                        anyLong(), anyLong(), anyBoolean()))
                .thenReturn(recognitionContext());
        try (MockedStatic<ArkResponsesClient> mocked = mockStatic(ArkResponsesClient.class)) {
            mocked.when(() -> ArkResponsesClient.chat(eq(properties), anyString(), anyString()))
                    .thenReturn("这张图片最终识别为黄连，建议人工复核。");

            HerbAssistantImageExplainResponse response =
                    service.explainImage(12L, imageExplainRequest("这张图片为什么需要复核？"));

            mocked.verify(
                    () ->
                            ArkResponsesClient.chat(
                                    eq(properties),
                                    argThat(prompt -> prompt.contains("不要声称已经执行复核或重新识别")),
                                    argThat(
                                            prompt ->
                                                    prompt.contains("图片编号：IMG-012")
                                                            && prompt.contains("最终药材：黄连")
                                                            && prompt.contains("大模型辅助")
                                                            && !prompt.contains("featureVector"))));
            assertThat(response.getAnswer()).isEqualTo("这张图片最终识别为黄连，建议人工复核。");
        }
    }

    private HerbAssistantChatRequest request(String message) {
        HerbAssistantChatRequest request = new HerbAssistantChatRequest();
        request.setMessage(message);
        return request;
    }

    private HerbAssistantBatchExplainRequest batchExplainRequest(String question) {
        HerbAssistantBatchExplainRequest request = new HerbAssistantBatchExplainRequest();
        request.setQuestion(question);
        return request;
    }

    private HerbAssistantImageExplainRequest imageExplainRequest(String question) {
        HerbAssistantImageExplainRequest request = new HerbAssistantImageExplainRequest();
        request.setQuestion(question);
        return request;
    }

    private HerbAssistantBatchContextVO batchContext() {
        HerbAssistantBatchContextVO context = new HerbAssistantBatchContextVO();
        context.setBatchId(1L);
        context.setBatchCode("BATCH-001");
        context.setBatchName("石柱黄连手机端采集第 001 批");
        context.setBatchStatus("reviewing");
        context.setSpeciesName("黄连");
        context.setImageCount(4);
        context.setIdentifiedCount(4);
        context.setReviewedCount(2);
        context.setNeedReviewCount(2);
        context.setFinalSpeciesName("黄连");
        context.setAvgSimilarity(new BigDecimal("0.7325"));
        context.setQualityLevel("normal");
        context.setQualityScore(new BigDecimal("78.5"));
        context.setEvaluationSummary("该批次仍有图片需要复核。");
        return context;
    }

    private HerbAssistantImageContextVO imageContext() {
        HerbAssistantImageContextVO image = new HerbAssistantImageContextVO();
        image.setImageId(10L);
        image.setImageRole("root");
        image.setFinalSpeciesName("黄连");
        image.setFinalConfidence(new BigDecimal("0.65"));
        image.setNeedReview(1);
        image.setReviewStatus("pending");
        image.setResultSource("local_match");
        image.setMatchResult("low_confidence");
        image.setSuggestion("建议人工复核");
        return image;
    }

    private HerbAssistantImageExplainContextVO imageExplainContext() {
        HerbAssistantImageExplainContextVO context = new HerbAssistantImageExplainContextVO();
        context.setImageId(12L);
        context.setImageCode("IMG-012");
        context.setImageType("leaf");
        context.setGrowthStage("mature");
        context.setHealthStatus("normal");
        context.setCollectPlace("石柱基地");
        context.setFinalSpeciesName("黄连");
        context.setFinalConfidence(new BigDecimal("0.9123"));
        context.setResultSource("local_match");
        context.setMatchResult("high_confidence");
        context.setNeedReview(1);
        context.setReviewStatus("pending");
        context.setSuggestion("建议结合图谱候选复核");
        return context;
    }

    private HerbAssistantMatchContextVO matchContext() {
        HerbAssistantMatchContextVO match = new HerbAssistantMatchContextVO();
        match.setRank(1);
        match.setSpeciesName("黄连");
        match.setSimilarity(new BigDecimal("0.9123"));
        match.setAtlasName("ATLAS-001");
        return match;
    }

    private HerbAssistantRecognitionContextVO recognitionContext() {
        HerbAssistantRecognitionContextVO recognition = new HerbAssistantRecognitionContextVO();
        recognition.setSpeciesName("黄连");
        recognition.setConfidence(new BigDecimal("0.88"));
        recognition.setReason("叶片纹理和黄连样本相似");
        recognition.setSuggestion("建议人工复核确认");
        return recognition;
    }
}
