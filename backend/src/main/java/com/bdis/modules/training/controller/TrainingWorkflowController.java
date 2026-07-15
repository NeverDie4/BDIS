package com.bdis.modules.training.controller;
import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.entity.*;
import com.bdis.modules.training.request.*;
import com.bdis.modules.training.service.TrainingWorkflowService;
import com.bdis.modules.training.vo.TrainingCompletionProofVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@Validated @RestController @RequestMapping("/training-records")
public class TrainingWorkflowController {
 private final TrainingWorkflowService service; private final AuthorizationService auth;
 public TrainingWorkflowController(TrainingWorkflowService s,AuthorizationService a){service=s;auth=a;}
 @PostMapping("/{id}/submit") public Result<TrainingReportVersionEntity> submit(@PathVariable @Positive Long id,@Valid @RequestBody TrainingReportSubmitRequest req){auth.requirePermission("edu:training-record:submit");return Result.success(service.submit(id,req));}
 @PostMapping("/{id}/return") public Result<Void> ret(@PathVariable @Positive Long id,@Valid @RequestBody TrainingRecordReviewRequest req){auth.requirePermission("edu:training-record:review");service.review(id,req);return Result.success();}
 @PostMapping("/{id}/review") public Result<Void> review(@PathVariable @Positive Long id,@Valid @RequestBody TrainingRecordReviewRequest req){auth.requirePermission("edu:training-record:review");service.review(id,req);return Result.success();}
 @GetMapping("/{id}/evaluations") public Result<List<TrainingEvaluationEntity>> evaluations(@PathVariable @Positive Long id){auth.requirePermission("edu:training-record:detail");return Result.success(service.evaluations(id));}
 @PostMapping("/{id}/evaluations") public Result<Void> evaluate(@PathVariable @Positive Long id,@Valid @RequestBody TrainingEvaluationRequest req){auth.requirePermission("edu:training-record:evaluation");service.evaluate(id,req);return Result.success();}
 @GetMapping("/{id}/completion-proof") public Result<TrainingCompletionProofVO> proof(@PathVariable @Positive Long id){auth.requirePermission("edu:training-record:detail");return Result.success(service.proof(id));}
 @GetMapping("/{id}/reports") public Result<List<TrainingReportVersionEntity>> reports(@PathVariable @Positive Long id){auth.requirePermission("edu:training-record:detail");return Result.success(service.reports(id));}
}
