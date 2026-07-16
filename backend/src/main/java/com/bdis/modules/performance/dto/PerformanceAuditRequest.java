package com.bdis.modules.performance.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class PerformanceAuditRequest {

    @Size(max = 50, message = "认定结果不能超过 50 个字符")
    private String identifyResult;

    @Size(max = 500, message = "认定意见不能超过 500 个字符")
    private String identifyComment;

    @Size(max = 50, message = "审核决定不能超过 50 个字符")
    private String decision;

    @Size(max = 500, message = "审核意见不能超过 500 个字符")
    private String comment;

    @Size(max = 500, message = "备注不能超过 500 个字符")
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
