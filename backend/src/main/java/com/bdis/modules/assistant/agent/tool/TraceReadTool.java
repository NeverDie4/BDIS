package com.bdis.modules.assistant.agent.tool;

import com.bdis.modules.assistant.agent.tool.dto.TraceToolData;
import com.bdis.modules.growth.service.DigitalLifeIntegrityService;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.DigitalLifeIntegrityVO;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.growth.vo.GrowthTraceEventVO;
import com.bdis.modules.growth.vo.GrowthTraceQrCodeVO;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class TraceReadTool implements AgentBusinessTool {

    public static final String OVERVIEW = "trace.growth_overview";
    public static final String EVENTS = "trace.events";
    public static final String PUBLIC_STATUS = "trace.public_status";

    private final GrowthRecordService growthRecordService;
    private final DigitalLifeIntegrityService integrityService;
    private final AgentReadToolSupport support;

    public TraceReadTool(
            GrowthRecordService growthRecordService,
            DigitalLifeIntegrityService integrityService,
            AgentReadToolSupport support) {
        this.growthRecordService = growthRecordService;
        this.integrityService = integrityService;
        this.support = support;
    }

    public TraceToolData.Overview getGrowthTraceOverview(
            Long recordId, AgentToolExecutionContext context) {
        GrowthRecordVO record = requireRecord(recordId, context);
        GrowthTraceQrCodeVO trace = growthRecordService.getTraceQrCode(recordId);
        DigitalLifeIntegrityVO integrity = integrityService.verify(record.getTaskId());
        return new TraceToolData.Overview(
                recordId,
                trace.getTraceCode(),
                StringUtils.hasText(trace.getQrCodeUrl()),
                Integer.valueOf(1).equals(trace.getPublicVisible()),
                support.browserUrl(trace.getTraceUrl()),
                support.status(record.getReviewStatus()),
                integrity.verified(),
                integrity.message(),
                trace.getTraceGeneratedTime());
    }

    public TraceToolData.EventList getTraceEvents(
            Long recordId, AgentToolExecutionContext context) {
        requireRecord(recordId, context);
        List<TraceToolData.Event> events =
                growthRecordService.trace(recordId).stream().map(this::toEvent).toList();
        return new TraceToolData.EventList(recordId, events);
    }

    public TraceToolData.PublicStatus getPublicTraceStatus(
            Long recordId, AgentToolExecutionContext context) {
        requireRecord(recordId, context);
        GrowthTraceQrCodeVO trace = growthRecordService.getTraceQrCode(recordId);
        return new TraceToolData.PublicStatus(
                recordId,
                trace.getTraceCode(),
                StringUtils.hasText(trace.getQrCodeUrl()),
                Integer.valueOf(1).equals(trace.getPublicVisible()),
                support.browserUrl(trace.getTraceUrl()),
                trace.getTraceGeneratedTime());
    }

    @Override
    public List<AgentToolDefinition> definitions() {
        return List.of(
                support.readDefinition(OVERVIEW, "生长溯源概览", "读取溯源、审核和档案哈希状态"),
                support.readDefinition(EVENTS, "溯源事件", "读取已脱敏的溯源事件状态变化"),
                support.readDefinition(PUBLIC_STATUS, "公开溯源状态", "读取二维码与公开开关状态"));
    }

    private GrowthRecordVO requireRecord(Long recordId, AgentToolExecutionContext context) {
        GrowthRecordVO record = growthRecordService.detail(recordId);
        support.requireTaskContext(context, record.getTaskId());
        return record;
    }

    private TraceToolData.Event toEvent(GrowthTraceEventVO event) {
        return new TraceToolData.Event(
                event.getEventType(),
                event.getEventTitle(),
                event.getAction(),
                support.status(event.getBeforeStatus()),
                support.status(event.getAfterStatus()),
                event.getEventTime());
    }
}
