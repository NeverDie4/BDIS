package com.bdis.modules.training.service;
import com.bdis.modules.training.entity.TrainingEvaluationEntity;
import com.bdis.modules.training.entity.TrainingReportVersionEntity;
import com.bdis.modules.training.request.TrainingEvaluationRequest;
import com.bdis.modules.training.request.TrainingRecordReviewRequest;
import com.bdis.modules.training.request.TrainingReportSubmitRequest;
import com.bdis.modules.training.vo.TrainingCompletionProofVO;
import java.util.List;
public interface TrainingWorkflowService { TrainingReportVersionEntity submit(Long id, TrainingReportSubmitRequest request); void review(Long id, TrainingRecordReviewRequest request); List<TrainingEvaluationEntity> evaluations(Long id); void evaluate(Long id, TrainingEvaluationRequest request); TrainingCompletionProofVO proof(Long id); List<TrainingReportVersionEntity> reports(Long id); }
