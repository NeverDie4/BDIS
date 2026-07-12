package com.bdis.modules.performance.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class PerformanceAuditRequest {

    private String identifyResult;
    private String identifyComment;
    private String decision;
    private String comment;
    private String remark;

    public String resolvedAction() {
        return "approved".equals(resolvedResult()) ? "approve" : "reject";
    }

    public String resolvedResult() {
        return StringUtils.hasText(identifyResult) ? identifyResult : decision;
    }

    public String resolvedComment() {
        return StringUtils.hasText(identifyComment) ? identifyComment : comment;
    }
}
