package com.bdis.modules.assistant.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.assistant.client.ArkResponsesClient;
import com.bdis.modules.assistant.config.HerbAssistantProperties;
import com.bdis.modules.assistant.dto.HerbAssistantBatchExplainRequest;
import com.bdis.modules.assistant.dto.HerbAssistantChatRequest;
import com.bdis.modules.assistant.dto.HerbAssistantChatResponse;
import com.bdis.modules.assistant.dto.HerbAssistantImageExplainRequest;
import com.bdis.modules.assistant.dto.HerbAssistantRagChatRequest;
import com.bdis.modules.assistant.knowledge.config.HerbAssistantRagProperties;
import com.bdis.modules.assistant.mapper.HerbAssistantBatchContextMapper;
import com.bdis.modules.assistant.mapper.HerbAssistantImageContextMapper;
import com.bdis.modules.assistant.service.HerbAiChatHistoryService;
import com.bdis.modules.assistant.service.HerbAssistantRagService;
import com.bdis.modules.assistant.service.HerbAssistantService;
import com.bdis.modules.assistant.tool.HerbAssistantTools;
import com.bdis.modules.assistant.vo.HerbAssistantBatchContextVO;
import com.bdis.modules.assistant.vo.HerbAssistantBatchExplainResponse;
import com.bdis.modules.assistant.vo.HerbAssistantImageContextVO;
import com.bdis.modules.assistant.vo.HerbAssistantImageExplainContextVO;
import com.bdis.modules.assistant.vo.HerbAssistantImageExplainResponse;
import com.bdis.modules.assistant.vo.HerbAssistantMatchContextVO;
import com.bdis.modules.assistant.vo.HerbAssistantRagChatResponse;
import com.bdis.modules.assistant.vo.HerbAssistantRecognitionContextVO;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Service
public class HerbAssistantServiceImpl implements HerbAssistantService {

    private static final String MOCK_ANSWER = "这是 AI 小助手模拟回答。你可以询问系统使用方法、批次识别结果、图片上传流程等。";

    private static final String SYSTEM_PROMPT =
            "你是生物医药数字信息系统的 AI 小助手，主要帮助用户理解中药材图谱识别、采集任务、批次档案、图片上传、识别结果和人工复核流程。"
                    + "回答要简洁、准确，不要编造系统中不存在的功能。如果问题涉及具体批次或图片数据，但用户没有提供编号，应提示用户提供批次 ID 或图片 ID。";

    private static final String BATCH_EXPLAIN_SYSTEM_PROMPT =
            "你是中药材图谱识别系统的 AI 小助手。现在用户询问某个批次的识别和质量评价情况。"
                    + "请根据后端提供的批次数据回答，不要编造不存在的数据。如果数据不足，请明确说明。"
                    + "回答要简洁，适合老师、管理员或采集人员阅读。";

    private static final String IMAGE_EXPLAIN_SYSTEM_PROMPT =
            "你是中药材图谱识别系统的 AI 小助手。现在用户询问某张采集图片的识别结果。"
                    + "请根据后端提供的图片、最终识别、本地图谱候选和大模型辅助识别数据回答。"
                    + "不要编造不存在的数据，不要声称已经执行复核或重新识别。"
                    + "回答要说明最终识别结果、置信度、本地图谱候选是否一致、大模型结果是否支持、是否需要人工复核和下一步建议。";

    private static final String GUIDE_RESOURCE_PATH = "assistant/herb-system-guide.md";

    private static final int MAX_GUIDE_CONTEXT_LENGTH = 12000;
    private static final Pattern POSITIVE_ID_PATTERN = Pattern.compile("\\d+");
    private static final Pattern BATCH_CODE_PATTERN = Pattern.compile("(?i)\\bBATCH[-_A-Z0-9]+\\b");

    private static final String DEFAULT_GUIDE_CONTEXT =
            "本系统用于中药材图谱识别、采集任务管理、批次档案管理和识别结果复核。" + "常见流程包括上传采集图片、本地图谱匹配、低置信度时大模型辅助识别、生成识别结论和人工复核。";

    private final HerbAssistantProperties properties;
    private final HerbAssistantRagProperties ragProperties;
    private final ObjectProvider<ChatClient> chatClientProvider;
    private final HerbAiChatHistoryService historyService;
    private final HerbAssistantRagService ragService;
    private final HerbAssistantBatchContextMapper batchContextMapper;
    private final HerbAssistantImageContextMapper imageContextMapper;
    private final HerbAssistantTools herbAssistantTools;
    private final String systemGuideContext;

    public HerbAssistantServiceImpl(
            HerbAssistantProperties properties,
            HerbAssistantRagProperties ragProperties,
            @Qualifier("herbAssistantChatClient") ObjectProvider<ChatClient> chatClientProvider,
            HerbAiChatHistoryService historyService,
            HerbAssistantRagService ragService,
            HerbAssistantBatchContextMapper batchContextMapper,
            HerbAssistantImageContextMapper imageContextMapper,
            HerbAssistantTools herbAssistantTools) {
        this.properties = properties;
        this.ragProperties = ragProperties;
        this.chatClientProvider = chatClientProvider;
        this.historyService = historyService;
        this.ragService = ragService;
        this.batchContextMapper = batchContextMapper;
        this.imageContextMapper = imageContextMapper;
        this.herbAssistantTools = herbAssistantTools;
        this.systemGuideContext = loadSystemGuideContext();
    }

    @Override
    public HerbAssistantChatResponse chat(HerbAssistantChatRequest request) {
        validateRequest(request);
        if (!properties.isEnabled()) {
            throw new BusinessException("AI 小助手暂未启用");
        }

        boolean ragRequested = Boolean.TRUE.equals(request.getUseRag());
        if (ragRequested && ragProperties.isEnabled()) {
            HerbAssistantRagChatResponse ragResponse = ragService.chat(toRagRequest(request));
            return new HerbAssistantChatResponse(
                    ragResponse.getSessionId(),
                    ragResponse.getAnswer(),
                    true,
                    ragResponse.getReferences());
        }

        String sessionId = historyService.recordUserMessage(request);
        if (properties.isMockEnabled()) {
            String answer = appendRagDisabledTip(MOCK_ANSWER, ragRequested);
            historyService.recordAssistantMessage(sessionId, answer, "mock", request.getSource());
            return new HerbAssistantChatResponse(sessionId, answer, false, List.of());
        }

        validateModelConfig();
        try {
            String answer =
                    ArkResponsesClient.chat(
                            properties,
                            buildSystemPrompt(),
                            buildToolAugmentedUserInput(request.getMessage().trim()));
            if (!StringUtils.hasText(answer)) {
                throw new BusinessException("AI 模型返回内容为空");
            }
            String normalizedAnswer = appendRagDisabledTip(answer.trim(), ragRequested);
            historyService.recordAssistantMessage(
                    sessionId, normalizedAnswer, properties.getModel(), request.getSource());
            return new HerbAssistantChatResponse(sessionId, normalizedAnswer, false, List.of());
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            if (hasCause(exception, SocketTimeoutException.class)) {
                throw new BusinessException("AI 模型调用超时");
            }
            throw new BusinessException("AI 模型网络连接失败");
        } catch (RestClientException exception) {
            throw new BusinessException("AI 模型调用失败，请稍后重试");
        } catch (RuntimeException exception) {
            throw new BusinessException("AI 模型调用失败，请稍后重试");
        }
    }

    private HerbAssistantRagChatRequest toRagRequest(HerbAssistantChatRequest request) {
        HerbAssistantRagChatRequest ragRequest = new HerbAssistantRagChatRequest();
        ragRequest.setMessage(request.getMessage());
        ragRequest.setSessionId(request.getSessionId());
        ragRequest.setDocType(request.getDocType());
        ragRequest.setTopK(request.getTopK());
        ragRequest.setSource(request.getSource());
        return ragRequest;
    }

    private String appendRagDisabledTip(String answer, boolean ragRequested) {
        if (!ragRequested || ragProperties.isEnabled()) {
            return answer;
        }
        return answer + "\n\n知识库问答暂未启用，本次已按普通聊天回答。";
    }

    @Override
    public HerbAssistantBatchExplainResponse explainBatch(
            Long batchId, HerbAssistantBatchExplainRequest request) {
        validateBatchExplainRequest(batchId, request);
        if (!properties.isEnabled()) {
            throw new BusinessException("AI 小助手暂未启用");
        }

        CurrentUser currentUser = SecurityUtils.currentUser();
        boolean dataScopeAll = hasAllAssistantDataScope(currentUser);
        HerbAssistantBatchContextVO context =
                batchContextMapper.selectBatchContextById(
                        batchId, currentUser.getUserId(), dataScopeAll);
        if (context == null) {
            throw new BusinessException("批次不存在");
        }
        List<HerbAssistantImageContextVO> images =
                batchContextMapper.selectImageContextsByBatchId(
                        batchId, currentUser.getUserId(), dataScopeAll);
        context.setImages(images);

        String question = normalizeQuestion(request.getQuestion());
        HerbAssistantChatRequest chatRequest = new HerbAssistantChatRequest();
        chatRequest.setMessage("批次 " + batchId + " 解释：" + question);
        chatRequest.setSessionId(request.getSessionId());
        chatRequest.setSource(request.getSource());
        String sessionId = historyService.recordUserMessage(chatRequest);

        if (properties.isMockEnabled()) {
            String answer = buildMockBatchAnswer(context);
            historyService.recordAssistantMessage(sessionId, answer, "mock", request.getSource());
            return new HerbAssistantBatchExplainResponse(batchId, sessionId, answer);
        }

        validateModelConfig();
        try {
            String answer =
                    ArkResponsesClient.chat(
                            properties,
                            BATCH_EXPLAIN_SYSTEM_PROMPT,
                            buildBatchExplainPrompt(context, question));
            if (!StringUtils.hasText(answer)) {
                throw new BusinessException("AI 模型返回内容为空");
            }
            String normalizedAnswer = answer.trim();
            historyService.recordAssistantMessage(
                    sessionId, normalizedAnswer, properties.getModel(), request.getSource());
            return new HerbAssistantBatchExplainResponse(batchId, sessionId, normalizedAnswer);
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            if (hasCause(exception, SocketTimeoutException.class)) {
                throw new BusinessException("AI 模型调用超时");
            }
            throw new BusinessException("AI 模型网络连接失败");
        } catch (RestClientException exception) {
            throw new BusinessException("AI 模型调用失败，请稍后重试");
        } catch (RuntimeException exception) {
            throw new BusinessException("AI 模型调用失败，请稍后重试");
        }
    }

    @Override
    public HerbAssistantImageExplainResponse explainImage(
            Long imageId, HerbAssistantImageExplainRequest request) {
        validateImageExplainRequest(imageId, request);
        if (!properties.isEnabled()) {
            throw new BusinessException("AI 小助手暂未启用");
        }

        CurrentUser currentUser = SecurityUtils.currentUser();
        boolean dataScopeAll = hasAllAssistantDataScope(currentUser);
        HerbAssistantImageExplainContextVO context =
                imageContextMapper.selectImageContextById(
                        imageId, currentUser.getUserId(), dataScopeAll);
        if (context == null) {
            throw new BusinessException("图片不存在");
        }
        context.setMatches(
                imageContextMapper.selectTopMatchesByImageId(
                        imageId, currentUser.getUserId(), dataScopeAll));
        context.setRecognition(
                imageContextMapper.selectLatestRecognitionByImageId(
                        imageId, currentUser.getUserId(), dataScopeAll));

        String question = normalizeImageQuestion(request.getQuestion());
        HerbAssistantChatRequest chatRequest = new HerbAssistantChatRequest();
        chatRequest.setMessage("图片 " + imageId + " 解释：" + question);
        chatRequest.setSessionId(request.getSessionId());
        chatRequest.setSource(request.getSource());
        String sessionId = historyService.recordUserMessage(chatRequest);

        if (properties.isMockEnabled()) {
            String answer = buildMockImageAnswer(context);
            historyService.recordAssistantMessage(sessionId, answer, "mock", request.getSource());
            return new HerbAssistantImageExplainResponse(imageId, sessionId, answer);
        }

        validateModelConfig();
        try {
            String answer =
                    ArkResponsesClient.chat(
                            properties,
                            IMAGE_EXPLAIN_SYSTEM_PROMPT,
                            buildImageExplainPrompt(context, question));
            if (!StringUtils.hasText(answer)) {
                throw new BusinessException("AI 模型返回内容为空");
            }
            String normalizedAnswer = answer.trim();
            historyService.recordAssistantMessage(
                    sessionId, normalizedAnswer, properties.getModel(), request.getSource());
            return new HerbAssistantImageExplainResponse(imageId, sessionId, normalizedAnswer);
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            if (hasCause(exception, SocketTimeoutException.class)) {
                throw new BusinessException("AI 模型调用超时");
            }
            throw new BusinessException("AI 模型网络连接失败");
        } catch (RestClientException exception) {
            throw new BusinessException("AI 模型调用失败，请稍后重试");
        } catch (RuntimeException exception) {
            throw new BusinessException("AI 模型调用失败，请稍后重试");
        }
    }

    private String buildSystemPrompt() {
        return SYSTEM_PROMPT
                + "\n\n# 系统使用说明上下文\n"
                + systemGuideContext
                + "\n\n当用户询问具体批次、图片或采集任务状态时，请优先调用系统工具查询真实业务数据，再基于工具结果回答。"
                + "如果缺少必要的 ID 或编码，请提示用户补充。工具仅用于查询，不得声称已修改识别结果、确认批次、归档批次或执行人工复核。"
                + "如果没有调用工具，不得直接断言具体业务状态，应提示用户补充 ID 或编码，或说明当前未能查询。"
                + "请只基于以上系统说明、工具结果和用户问题回答，不要编造系统中不存在的页面、接口、数据或功能。";
    }

    private String buildToolAugmentedUserInput(String message) {
        String toolContext = buildToolContext(message);
        if (!StringUtils.hasText(toolContext)) {
            return message;
        }
        return "业务工具查询结果：\n"
                + toolContext
                + "\n\n用户问题：\n"
                + message
                + "\n\n请优先基于业务工具查询结果回答；如果工具结果显示未找到或无权限，请明确说明无法查询到对应业务数据。";
    }

    private String buildToolContext(String message) {
        StringBuilder builder = new StringBuilder();
        Long id = firstPositiveId(message);
        if (id != null && containsAny(message, "批次", "batch", "BATCH")) {
            builder.append("批次查询：").append(herbAssistantTools.getBatchSummaryById(id)).append('\n');
        }
        String batchCode = firstBatchCode(message);
        if (StringUtils.hasText(batchCode)) {
            builder.append("批次编码查询：")
                    .append(herbAssistantTools.getBatchSummaryByCode(batchCode))
                    .append('\n');
        }
        if (id != null && containsAny(message, "图片", "图像", "image", "IMG")) {
            builder.append("图片查询：")
                    .append(herbAssistantTools.getImageIdentificationById(id))
                    .append('\n');
        }
        if (id != null && containsAny(message, "任务", "task", "TASK")) {
            builder.append("任务查询：").append(herbAssistantTools.getTaskSummaryById(id)).append('\n');
        }
        if (containsAny(message, "我的任务", "我负责的任务")) {
            builder.append("我的任务查询：").append(herbAssistantTools.listMyTasks()).append('\n');
        }
        if (containsAny(message, "复核中批次", "待复核批次")) {
            builder.append("复核中批次查询：")
                    .append(herbAssistantTools.listReviewingBatches())
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private Long firstPositiveId(String message) {
        Matcher matcher = POSITIVE_ID_PATTERN.matcher(message);
        while (matcher.find()) {
            try {
                long id = Long.parseLong(matcher.group());
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }

    private String firstBatchCode(String message) {
        Matcher matcher = BATCH_CODE_PATTERN.matcher(message);
        return matcher.find() ? matcher.group().trim() : null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String loadSystemGuideContext() {
        ClassPathResource resource = new ClassPathResource(GUIDE_RESOURCE_PATH);
        if (!resource.exists()) {
            return DEFAULT_GUIDE_CONTEXT;
        }
        try {
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            if (!StringUtils.hasText(content)) {
                return DEFAULT_GUIDE_CONTEXT;
            }
            String normalizedContent = content.trim();
            if (normalizedContent.length() > MAX_GUIDE_CONTEXT_LENGTH) {
                return normalizedContent.substring(0, MAX_GUIDE_CONTEXT_LENGTH);
            }
            return normalizedContent;
        } catch (IOException exception) {
            return DEFAULT_GUIDE_CONTEXT;
        }
    }

    private void validateImageExplainRequest(
            Long imageId, HerbAssistantImageExplainRequest request) {
        if (imageId == null || imageId <= 0) {
            throw new BusinessException("图片 ID 无效");
        }
        if (request == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (request.getQuestion() != null && request.getQuestion().length() > 1000) {
            throw new BusinessException("问题长度不能超过1000字");
        }
    }

    private void validateBatchExplainRequest(
            Long batchId, HerbAssistantBatchExplainRequest request) {
        if (batchId == null || batchId <= 0) {
            throw new BusinessException("批次 ID 无效");
        }
        if (request == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (request.getQuestion() != null && request.getQuestion().length() > 1000) {
            throw new BusinessException("问题长度不能超过1000字");
        }
    }

    private String normalizeQuestion(String question) {
        return StringUtils.hasText(question) ? question.trim() : "请解释该批次当前识别和质量评价情况。";
    }

    private String normalizeImageQuestion(String question) {
        return StringUtils.hasText(question) ? question.trim() : "请解释该图片当前识别结果和复核建议。";
    }

    private String buildMockBatchAnswer(HerbAssistantBatchContextVO context) {
        return "该批次共绑定 "
                + safeNumber(context.getImageCount())
                + " 张图片，已识别 "
                + safeNumber(context.getIdentifiedCount())
                + " 张，待复核 "
                + safeNumber(context.getNeedReviewCount())
                + " 张。当前主要识别结果为 "
                + safeText(context.getFinalSpeciesName())
                + "，质量等级为 "
                + safeText(context.getQualityLevel())
                + "。建议先完成人工复核后再归档。";
    }

    private String buildMockImageAnswer(HerbAssistantImageExplainContextVO context) {
        return "该图片当前最终识别结果为 "
                + safeText(context.getFinalSpeciesName())
                + "，置信度为 "
                + safePercentage(context.getFinalConfidence())
                + "。系统判断结果来源为 "
                + safeText(context.getResultSource())
                + "，复核状态为 "
                + safeText(context.getReviewStatus())
                + "。如果该图片仍需复核，建议管理员结合 TopK 图谱候选和大模型辅助结果进行确认。";
    }

    private String buildBatchExplainPrompt(HerbAssistantBatchContextVO context, String question) {
        StringBuilder builder = new StringBuilder();
        builder.append("批次编号：").append(safeText(context.getBatchCode())).append('\n');
        builder.append("批次名称：").append(safeText(context.getBatchName())).append('\n');
        builder.append("批次状态：").append(safeText(context.getBatchStatus())).append('\n');
        builder.append("目标药材：").append(safeText(context.getSpeciesName())).append('\n');
        builder.append("图片数量：").append(safeNumber(context.getImageCount())).append('\n');
        builder.append("已识别数量：").append(safeNumber(context.getIdentifiedCount())).append('\n');
        builder.append("已复核数量：").append(safeNumber(context.getReviewedCount())).append('\n');
        builder.append("待复核数量：").append(safeNumber(context.getNeedReviewCount())).append('\n');
        builder.append("最终药材：").append(safeText(context.getFinalSpeciesName())).append('\n');
        builder.append("平均置信度：").append(safeDecimal(context.getAvgSimilarity())).append('\n');
        builder.append("质量等级：").append(safeText(context.getQualityLevel())).append('\n');
        builder.append("质量分：").append(safeDecimal(context.getQualityScore())).append('\n');
        builder.append("评价摘要：").append(safeText(context.getEvaluationSummary())).append('\n');
        builder.append("\n图片明细：\n");
        if (context.getImages() == null || context.getImages().isEmpty()) {
            builder.append("暂无绑定图片。\n");
        } else {
            int index = 1;
            for (HerbAssistantImageContextVO image : context.getImages()) {
                builder.append(index++)
                        .append(". 图片ID ")
                        .append(image.getImageId())
                        .append("，角色 ")
                        .append(safeText(image.getImageRole()))
                        .append("，识别为 ")
                        .append(safeText(image.getFinalSpeciesName()))
                        .append("，置信度 ")
                        .append(safeDecimal(image.getFinalConfidence()))
                        .append("，是否需复核 ")
                        .append(formatNeedReview(image.getNeedReview()))
                        .append("，复核状态 ")
                        .append(safeText(image.getReviewStatus()))
                        .append("，结果来源 ")
                        .append(safeText(image.getResultSource()))
                        .append("，匹配结果 ")
                        .append(safeText(image.getMatchResult()))
                        .append("，建议 ")
                        .append(safeText(image.getSuggestion()))
                        .append('\n');
            }
        }
        builder.append("\n用户问题：\n").append(question);
        return builder.toString();
    }

    private String buildImageExplainPrompt(
            HerbAssistantImageExplainContextVO context, String question) {
        StringBuilder builder = new StringBuilder();
        builder.append("图片编号：").append(safeText(context.getImageCode())).append('\n');
        builder.append("图片类型：").append(safeText(context.getImageType())).append('\n');
        builder.append("生长阶段：").append(safeText(context.getGrowthStage())).append('\n');
        builder.append("健康状态：").append(safeText(context.getHealthStatus())).append('\n');
        builder.append("采集地点：").append(safeText(context.getCollectPlace())).append('\n');
        builder.append("采集时间：").append(safeObject(context.getCollectTime())).append('\n');
        builder.append("\n最终识别：\n");
        builder.append("最终药材：").append(safeText(context.getFinalSpeciesName())).append('\n');
        builder.append("最终置信度：").append(safePercentage(context.getFinalConfidence())).append('\n');
        builder.append("结果来源：").append(safeText(context.getResultSource())).append('\n');
        builder.append("匹配结果：").append(safeText(context.getMatchResult())).append('\n');
        builder.append("是否需复核：").append(formatNeedReview(context.getNeedReview())).append('\n');
        builder.append("复核状态：").append(safeText(context.getReviewStatus())).append('\n');
        builder.append("系统建议：").append(safeText(context.getSuggestion())).append('\n');
        builder.append("\n本地图谱候选：\n");
        if (context.getMatches() == null || context.getMatches().isEmpty()) {
            builder.append("暂无本地图谱候选记录。\n");
        } else {
            for (HerbAssistantMatchContextVO match : context.getMatches()) {
                builder.append(safeObject(match.getRank()))
                        .append(". 药材 ")
                        .append(safeText(match.getSpeciesName()))
                        .append("，相似度 ")
                        .append(safePercentage(match.getSimilarity()))
                        .append("，图谱 ")
                        .append(safeText(match.getAtlasName()))
                        .append('\n');
            }
        }
        builder.append("\n大模型辅助：\n");
        HerbAssistantRecognitionContextVO recognition = context.getRecognition();
        if (recognition == null) {
            builder.append("暂无大模型辅助识别记录。\n");
        } else {
            builder.append("识别药材：").append(safeText(recognition.getSpeciesName())).append('\n');
            builder.append("置信度：").append(safePercentage(recognition.getConfidence())).append('\n');
            builder.append("理由：").append(safeText(recognition.getReason())).append('\n');
            builder.append("建议：").append(safeText(recognition.getSuggestion())).append('\n');
        }
        builder.append("\n用户问题：\n").append(question);
        return builder.toString();
    }

    private void validateRequest(HerbAssistantChatRequest request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new BusinessException("消息不能为空");
        }
        if (request.getMessage().length() > 1000) {
            throw new BusinessException("消息长度不能超过1000字");
        }
    }

    private void validateModelConfig() {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new BusinessException("AI 模型 baseUrl 未配置");
        }
        if (!StringUtils.hasText(properties.getModel())) {
            throw new BusinessException("AI 模型名称未配置");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException("AI 模型 API Key 未配置");
        }
        if (properties.getTimeoutSeconds() == null || properties.getTimeoutSeconds() <= 0) {
            throw new BusinessException("AI 模型超时时间配置无效");
        }
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "暂无";
    }

    private String safeNumber(Integer value) {
        return value == null ? "0" : value.toString();
    }

    private String safeDecimal(BigDecimal value) {
        return value == null ? "暂无" : value.stripTrailingZeros().toPlainString();
    }

    private String safePercentage(BigDecimal value) {
        if (value == null) {
            return "暂无";
        }
        BigDecimal percentage =
                value.compareTo(BigDecimal.ONE) <= 0 ? value.movePointRight(2) : value;
        return percentage.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                + "%";
    }

    private String safeObject(Object value) {
        return value == null ? "暂无" : value.toString();
    }

    private String formatNeedReview(Integer needReview) {
        if (needReview == null) {
            return "未知";
        }
        return needReview == 1 ? "是" : "否";
    }

    private boolean hasCause(Throwable exception, Class<? extends Throwable> causeType) {
        Throwable current = exception;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean hasAllAssistantDataScope(CurrentUser currentUser) {
        return currentUser.getRoleCodes().contains("ADMIN")
                || currentUser.getPermissions().contains("*")
                || currentUser.getPermissions().contains("herb:assistant:data:all");
    }
}
