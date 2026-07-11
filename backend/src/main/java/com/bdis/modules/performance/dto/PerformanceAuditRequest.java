package com.bdis.modules.performance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class PerformanceAuditRequest {

    @NotNull(message = "认定人ID不能为空")
    private Long identifierId;

    private String identifyAction;

    private String identifyResult;

    private String identifyComment;

    private String decision;

    private String comment;

    private String remark;

    public String resolvedAction() {
        if (StringUtils.hasText(identifyAction)) {
            return identifyAction;
        }
        String result = resolvedResult();
        if ("approved".equals(result)) {
            return "approve";
        }
        if ("rejected".equals(result)) {
            return "reject";
        }
        return "identify";
    }

    public String resolvedResult() {
        return StringUtils.hasText(identifyResult) ? identifyResult : decision;
    }

    public String resolvedComment() {
        return StringUtils.hasText(identifyComment) ? identifyComment : comment;
    }
}
