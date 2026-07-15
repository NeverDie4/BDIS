package com.bdis.modules.training.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.file.vo.FileResourceVO;
import com.bdis.modules.notification.service.NotificationService;
import com.bdis.modules.training.entity.TrainingEvaluationEntity;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.entity.TrainingRecordEntity;
import com.bdis.modules.training.entity.TrainingReportVersionEntity;
import com.bdis.modules.training.mapper.TrainingEvaluationMapper;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.mapper.TrainingRecordMapper;
import com.bdis.modules.training.mapper.TrainingReportVersionMapper;
import com.bdis.modules.training.request.TrainingEvaluationRequest;
import com.bdis.modules.training.request.TrainingRecordReviewRequest;
import com.bdis.modules.training.request.TrainingReportSubmitRequest;
import com.bdis.modules.training.service.TrainingWorkflowService;
import com.bdis.modules.training.vo.TrainingCompletionProofVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingWorkflowServiceImpl implements TrainingWorkflowService {
    private static final String BIZ_TYPE = "edu_training_record";

    private final TrainingRecordMapper recordMapper;
    private final TrainingPlanMapper planMapper;
    private final TrainingReportVersionMapper reportMapper;
    private final TrainingEvaluationMapper evaluationMapper;
    private final FileResourceService fileResourceService;
    private final FileBusinessService fileBusinessService;

    @Autowired(required = false)
    private NotificationService notificationService;

    public TrainingWorkflowServiceImpl(
            TrainingRecordMapper recordMapper,
            TrainingPlanMapper planMapper,
            TrainingReportVersionMapper reportMapper,
            TrainingEvaluationMapper evaluationMapper,
            FileResourceService fileResourceService,
            FileBusinessService fileBusinessService) {
        this.recordMapper = recordMapper;
        this.planMapper = planMapper;
        this.reportMapper = reportMapper;
        this.evaluationMapper = evaluationMapper;
        this.fileResourceService = fileResourceService;
        this.fileBusinessService = fileBusinessService;
    }

    @Override
    @Transactional
    public TrainingReportVersionEntity submit(Long id, TrainingReportSubmitRequest request) {
        Long userId = user();
        TrainingRecordEntity record = requireRecord(id);
        if (!userId.equals(record.getUserId())) {
            throw new ForbiddenException("Only participant can submit training report");
        }
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new BusinessException("Training report content is required");
        }
        List<TrainingReportVersionEntity> all = reportMapper.selectByRecordId(id);
        LocalDateTime now = LocalDateTime.now();
        TrainingReportVersionEntity version = new TrainingReportVersionEntity();
        version.setTrainingRecordId(id);
        version.setVersionNo(all.size() + 1);
        version.setFileId(request.getFileId());
        version.setContent(request.getContent());
        version.setReportStatus("submitted");
        version.setSubmittedBy(userId);
        version.setSubmittedAt(now);
        version.setCreatedAt(now);
        version.setUpdatedAt(now);
        reportMapper.insert(version);
        record.setTrainingStatus("learning");
        record.setUpdatedAt(now);
        recordMapper.updateById(record);
        TrainingPlanEntity plan = planMapper.selectById(record.getPlanId());
        if (notificationService != null && plan != null) {
            notificationService.create(
                    plan.getOwnerId(),
                    "TRAINING_REPORT_SUBMITTED",
                    BIZ_TYPE,
                    id,
                    "培训报告已提交",
                    "请审核培训报告");
        }
        return version;
    }

    @Override
    @Transactional
    public void review(Long id, TrainingRecordReviewRequest request) {
        Long userId = user();
        TrainingRecordEntity record = requireRecord(id);
        if (!canManage(record, userId)) {
            throw new ForbiddenException("Only trainer or plan owner can review training");
        }
        if (request == null
                || !Set.of("return", "complete", "approve").contains(request.getAction())) {
            throw new BusinessException("Invalid training review action");
        }
        if ("return".equals(request.getAction())) {
            record.setTrainingStatus("makeup");
        } else {
            record.setTrainingStatus("completed");
        }
        if (request.getScore() != null) {
            record.setScore(request.getScore());
        }
        record.setResultComment(request.getComment());
        if ("complete".equals(request.getAction()) || "approve".equals(request.getAction())) {
            record.setCompletedAt(LocalDateTime.now());
            generateCompletionProof(record);
        }
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.updateById(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingEvaluationEntity> evaluations(Long id) {
        requireReadAccess(requireRecord(id));
        return evaluationMapper.selectByRecordId(id);
    }

    @Override
    @Transactional
    public void evaluate(Long id, TrainingEvaluationRequest request) {
        Long userId = user();
        TrainingRecordEntity record = requireRecord(id);
        if (!canManage(record, userId)) {
            throw new ForbiddenException("Only trainer or plan owner can evaluate training");
        }
        if (request == null
                || request.getScore() == null
                || request.getScore().signum() < 0
                || request.getScore().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Evaluation score must be between 0 and 100");
        }
        TrainingEvaluationEntity entity =
                evaluationMapper.selectOne(id, request.getDimensionCode());
        if (entity == null) {
            entity = new TrainingEvaluationEntity();
            entity.setTrainingRecordId(id);
            entity.setDimensionCode(request.getDimensionCode());
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setScore(request.getScore());
        entity.setComment(request.getComment());
        entity.setEvaluatorId(userId);
        entity.setEvaluatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        if (entity.getId() == null) {
            evaluationMapper.insert(entity);
        } else {
            evaluationMapper.updateById(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingCompletionProofVO proof(Long id) {
        TrainingRecordEntity record = requireRecord(id);
        requireReadAccess(record);
        TrainingCompletionProofVO vo = new TrainingCompletionProofVO();
        vo.setTrainingRecordId(id);
        vo.setCompleted("completed".equals(record.getTrainingStatus()));
        vo.setScore(record.getScore());
        vo.setCompletionProofFileId(record.getCompletionProofFileId());
        vo.setMessage(
                Boolean.TRUE.equals(vo.getCompleted())
                        ? "Training completed; completion proof is ready when a proof file is configured"
                        : "Training is not completed");
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingReportVersionEntity> reports(Long id) {
        requireReadAccess(requireRecord(id));
        return reportMapper.selectByRecordId(id);
    }

    private TrainingRecordEntity requireRecord(Long id) {
        TrainingRecordEntity record = recordMapper.selectActiveById(id);
        if (record == null) {
            throw new ResourceNotFoundException("Training record not found");
        }
        return record;
    }

    private Long user() {
        Long id = CurrentUserUtils.currentUserId();
        if (id == null) {
            throw new ForbiddenException("Authentication is required");
        }
        return id;
    }

    private void requireReadAccess(TrainingRecordEntity record) {
        Long userId = user();
        if (CurrentUserUtils.currentRoleCodes().isEmpty()
                || CurrentUserUtils.currentRoleCodes().stream().anyMatch("ADMIN"::equalsIgnoreCase)
                || Objects.equals(userId, record.getUserId())
                || canManage(record, userId)) {
            return;
        }
        throw new ForbiddenException("Training record is outside the current user's scope");
    }

    private boolean canManage(TrainingRecordEntity record, Long userId) {
        if (CurrentUserUtils.currentRoleCodes().stream().anyMatch("ADMIN"::equalsIgnoreCase)) {
            return true;
        }
        TrainingPlanEntity plan = planMapper.selectById(record.getPlanId());
        return plan != null
                && (Objects.equals(userId, plan.getOwnerId())
                        || Objects.equals(userId, plan.getTrainerId()));
    }

    private void generateCompletionProof(TrainingRecordEntity record) {
        if (record.getCompletionProofFileId() != null) {
            return;
        }
        if (fileResourceService == null || fileBusinessService == null) {
            throw new BusinessException("Completion proof file service is unavailable");
        }
        Path temp = null;
        try {
            TrainingPlanEntity plan = planMapper.selectById(record.getPlanId());
            temp = Files.createTempFile("training-proof-", ".txt");
            String text =
                    "培训完成证明\n培训计划："
                            + (plan == null ? record.getPlanId() : plan.getPlanName())
                            + "\n培训记录："
                            + record.getId()
                            + "\n学员："
                            + record.getUserId()
                            + "\n成绩："
                            + record.getScore()
                            + "\n完成时间："
                            + record.getCompletedAt();
            Files.writeString(temp, text, java.nio.charset.StandardCharsets.UTF_8);
            FileResourceVO file =
                    fileResourceService.importPrivate(
                            temp, "training-completion-proof-" + record.getId() + ".txt", "培训完成证明");
            FileBusinessBindDTO bind = new FileBusinessBindDTO();
            bind.setFileId(file.getId());
            bind.setBizType(BIZ_TYPE);
            bind.setBizId(record.getId());
            bind.setFileUsage("completion_proof");
            bind.setSortOrder(0);
            bind.setRemark("Generated training completion proof");
            fileBusinessService.bindSystem(bind);
            record.setCompletionProofFileId(file.getId());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("Completion proof generation failed");
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception ignored) {
                    // Temporary proof cleanup must not hide the business result.
                }
            }
        }
    }
}
