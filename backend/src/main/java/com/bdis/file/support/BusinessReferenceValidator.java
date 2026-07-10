package com.bdis.file.support;

import com.bdis.common.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class BusinessReferenceValidator {

    public void validate(String bizType, Long bizId) {
        if (bizType == null || bizType.isBlank()) {
            throw new BusinessException("业务类型不能为空");
        }
        if (bizId == null || bizId <= 0) {
            throw new BusinessException("业务 ID 不合法");
        }
    }
}
