package com.bdis.file.support;

import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.experiment.constant.ExperimentRecordBusinessType;
import com.bdis.modules.experiment.mapper.ExperimentRecordMapper;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import org.springframework.stereotype.Component;

@Component
public class BusinessReferenceValidator {

    private final CourseMapper courseMapper;
    private final ResearchProjectMapper researchProjectMapper;
    private final ExperimentRecordMapper experimentRecordMapper;

    public BusinessReferenceValidator(
            CourseMapper courseMapper,
            ResearchProjectMapper researchProjectMapper,
            ExperimentRecordMapper experimentRecordMapper) {
        this.courseMapper = courseMapper;
        this.researchProjectMapper = researchProjectMapper;
        this.experimentRecordMapper = experimentRecordMapper;
    }

    public void validate(String bizType, Long bizId) {
        if (bizType == null || bizType.isBlank()) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "业务类型不能为空");
        }
        if (bizId == null || bizId <= 0) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "业务 ID 不合法");
        }
        if ("edu_course".equals(bizType) && courseMapper.selectById(bizId) == null) {
            throw new ResourceNotFoundException("Course not found");
        }
        if ("research_project".equals(bizType) && researchProjectMapper.selectById(bizId) == null) {
            throw new ResourceNotFoundException("Research project not found");
        }
        if (ExperimentRecordBusinessType.EXPERIMENT_RECORD.equals(bizType)
                && !experimentRecordMapper.existsActiveReferenceById(bizId)) {
            throw new ResourceNotFoundException("Experiment record not found");
        }
    }
}
