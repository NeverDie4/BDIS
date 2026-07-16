package com.bdis.modules.assistant.agent.constant;

import java.util.Arrays;

public enum AgentTaskStatus {
    CREATED("已创建", false),
    PLANNING("规划中", false),
    RUNNING("执行中", false),
    WAITING_CONFIRMATION("等待确认", false),
    WAITING_FIELD_DATA("等待现场数据", false),
    REANALYZING("重新分析中", false),
    COMPLETED("已完成", true),
    FAILED("执行失败", true),
    CANCELLED("已取消", true);

    private final String label;
    private final boolean terminal;

    AgentTaskStatus(String label, boolean terminal) {
        this.label = label;
        this.terminal = terminal;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public static AgentTaskStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(status -> status.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Agent 任务状态无效：" + code));
    }
}
