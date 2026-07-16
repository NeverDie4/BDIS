package com.bdis.modules.assistant.agent.support;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class AgentTaskNoGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    public String temporaryTaskNo() {
        return "AGENT-TMP-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    public String generate(LocalDateTime createTime, Long taskId) {
        if (createTime == null || taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("生成 Agent 任务编号所需参数无效");
        }
        return "AGENT-DT-"
                + DATE_FORMAT.format(createTime.toLocalDate())
                + "-"
                + String.format("%06d", taskId);
    }
}
