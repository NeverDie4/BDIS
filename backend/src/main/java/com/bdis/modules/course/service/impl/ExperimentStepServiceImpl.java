package com.bdis.modules.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.course.constant.CoursePublishStatus;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.entity.ExperimentStepEntity;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.course.mapper.ExperimentStepMapper;
import com.bdis.modules.course.request.ExperimentStepCreateRequest;
import com.bdis.modules.course.request.ExperimentStepUpdateRequest;
import com.bdis.modules.course.service.ExperimentStepService;
import com.bdis.modules.course.vo.ExperimentStepVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExperimentStepServiceImpl implements ExperimentStepService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentStepServiceImpl.class);
    private static final String BIZ_TYPE = "edu_experiment_step";
    private static final String AUDIT_MODULE = "M12_COURSE";

    private final ExperimentStepMapper stepMapper;
    private final CourseMapper courseMapper;
    private final AuditLogService auditLogService;

    public ExperimentStepServiceImpl(
            ExperimentStepMapper stepMapper,
            CourseMapper courseMapper,
            AuditLogService auditLogService) {
        this.stepMapper = stepMapper;
        this.courseMapper = courseMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<ExperimentStepVO> listByCourseId(Long courseId) {
        requireCourse(courseId);
        return stepMapper
                .selectList(
                        new LambdaQueryWrapper<ExperimentStepEntity>()
                                .eq(ExperimentStepEntity::getCourseId, courseId)
                                .eq(ExperimentStepEntity::getStatus, 1)
                                .orderByAsc(ExperimentStepEntity::getSortOrder)
                                .orderByAsc(ExperimentStepEntity::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public ExperimentStepVO create(Long courseId, ExperimentStepCreateRequest request) {
        CourseEntity course = requireEditableCourse(courseId);
        validateRequest(request);
        String stepNo = request.getStepNo().trim();
        if (stepMapper.selectByCourseIdAndStepNoIncludingDeleted(courseId, stepNo) != null) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Step number already exists");
        }
        LocalDateTime now = LocalDateTime.now();
        ExperimentStepEntity entity = new ExperimentStepEntity();
        entity.setCourseId(course.getId());
        entity.setStepNo(stepNo);
        entity.setStepTitle(request.getStepTitle().trim());
        entity.setStepContent(request.getStepContent());
        entity.setExpectedResult(request.getExpectedResult());
        entity.setSortOrder(defaultSortOrder(request.getSortOrder()));
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(CurrentUserUtils.currentUserId());
        entity.setUpdatedBy(CurrentUserUtils.currentUserId());
        entity.setRemark(request.getRemark());
        entity.setVersion(0);
        stepMapper.insert(entity);
        recordAudit("CREATE", entity.getId());
        return toVO(entity);
    }

    @Override
    @Transactional
    public ExperimentStepVO update(
            Long courseId, Long stepId, ExperimentStepUpdateRequest request) {
        requireEditableCourse(courseId);
        validateRequest(request);
        ExperimentStepEntity entity = requireStep(courseId, stepId);
        checkVersion(entity, request.getVersion());
        ExperimentStepEntity duplicate =
                stepMapper.selectByCourseIdAndStepNoIncludingDeleted(
                        courseId, request.getStepNo().trim());
        if (duplicate != null && !Objects.equals(duplicate.getId(), stepId)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Step number already exists");
        }
        entity.setStepNo(request.getStepNo().trim());
        entity.setStepTitle(request.getStepTitle().trim());
        entity.setStepContent(request.getStepContent());
        entity.setExpectedResult(request.getExpectedResult());
        entity.setSortOrder(defaultSortOrder(request.getSortOrder()));
        entity.setRemark(request.getRemark());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(CurrentUserUtils.currentUserId());
        if (stepMapper.updateById(entity) == 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Step update failed");
        }
        recordAudit("UPDATE", stepId);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long courseId, Long stepId) {
        requireEditableCourse(courseId);
        ExperimentStepEntity entity = requireStep(courseId, stepId);
        if (stepMapper.deleteById(entity.getId()) == 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Step delete failed");
        }
        recordAudit("DELETE", stepId);
    }

    private CourseEntity requireCourse(Long courseId) {
        if (courseId == null || courseId <= 0) {
            throw new BusinessException("Course id is required");
        }
        CourseEntity course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found");
        }
        return course;
    }

    private CourseEntity requireEditableCourse(Long courseId) {
        CourseEntity course = requireCourse(courseId);
        CoursePublishStatus.requireEditable(course.getPublishStatus());
        return course;
    }

    private ExperimentStepEntity requireStep(Long courseId, Long stepId) {
        if (stepId == null || stepId <= 0) {
            throw new BusinessException("Step id is required");
        }
        ExperimentStepEntity entity = stepMapper.selectById(stepId);
        if (entity == null) {
            throw new ResourceNotFoundException("Step not found");
        }
        if (!Objects.equals(entity.getCourseId(), courseId)) {
            throw new ResourceNotFoundException("Step does not belong to course");
        }
        return entity;
    }

    private void validateRequest(ExperimentStepCreateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getStepNo())
                || !StringUtils.hasText(request.getStepTitle())) {
            throw new BusinessException("Step number and title are required");
        }
    }

    private void validateRequest(ExperimentStepUpdateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getStepNo())
                || !StringUtils.hasText(request.getStepTitle())
                || request.getVersion() == null) {
            throw new BusinessException("Step number, title and version are required");
        }
    }

    private void checkVersion(ExperimentStepEntity entity, Integer version) {
        if (!Objects.equals(entity.getVersion(), version)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Step version conflict");
        }
    }

    private Integer defaultSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }

    private ExperimentStepVO toVO(ExperimentStepEntity entity) {
        ExperimentStepVO vo = new ExperimentStepVO();
        vo.setId(entity.getId());
        vo.setCourseId(entity.getCourseId());
        vo.setStepNo(entity.getStepNo());
        vo.setStepTitle(entity.getStepTitle());
        vo.setStepContent(entity.getStepContent());
        vo.setExpectedResult(entity.getExpectedResult());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setRemark(entity.getRemark());
        return vo;
    }

    private void recordAudit(String operationType, Long stepId) {
        AuditRecordDTO audit = new AuditRecordDTO();
        audit.setOperationModule(AUDIT_MODULE);
        audit.setOperationType(operationType);
        audit.setBizType(BIZ_TYPE);
        audit.setBizId(stepId);
        try {
            auditLogService.record(audit);
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to persist experiment step audit log", exception);
        }
    }
}
