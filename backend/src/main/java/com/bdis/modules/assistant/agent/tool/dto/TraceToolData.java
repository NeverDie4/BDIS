package com.bdis.modules.assistant.agent.tool.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class TraceToolData {

    private TraceToolData() {}

    public record Overview(
            Long recordId,
            String traceCode,
            Boolean qrCodeGenerated,
            Boolean publicVisible,
            String publicUrl,
            StatusValue reviewStatus,
            Boolean hashVerified,
            String hashMessage,
            LocalDateTime traceGeneratedTime) {}

    public record EventList(Long recordId, List<Event> events) {}

    /** 仅保留事件类型和状态变化，过滤内部正文、审核意见和元数据。 */
    public record Event(
            String eventType,
            String eventTitle,
            String action,
            StatusValue beforeStatus,
            StatusValue afterStatus,
            LocalDateTime eventTime) {}

    public record PublicStatus(
            Long recordId,
            String traceCode,
            Boolean qrCodeGenerated,
            Boolean publicVisible,
            String publicUrl,
            LocalDateTime traceGeneratedTime) {}
}
