package com.bdis.modules.training.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.training.entity.*;
import com.bdis.modules.training.mapper.*;
import com.bdis.modules.training.request.*;
import com.bdis.modules.training.service.TrainingWorkflowService;
import com.bdis.modules.training.vo.TrainingCompletionProofVO;
import com.bdis.modules.notification.service.NotificationService;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.file.vo.FileResourceVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingWorkflowServiceImpl implements TrainingWorkflowService {
 private final TrainingRecordMapper recordMapper; private final TrainingPlanMapper planMapper; private final TrainingReportVersionMapper reportMapper; private final TrainingEvaluationMapper evaluationMapper;
 @org.springframework.beans.factory.annotation.Autowired(required=false) private NotificationService notificationService;
 private final FileResourceService fileResourceService;
 public TrainingWorkflowServiceImpl(TrainingRecordMapper r,TrainingPlanMapper p,TrainingReportVersionMapper rv,TrainingEvaluationMapper e,FileResourceService fs){recordMapper=r;planMapper=p;reportMapper=rv;evaluationMapper=e;fileResourceService=fs;}
 @Override @Transactional public TrainingReportVersionEntity submit(Long id,TrainingReportSubmitRequest req){Long uid=user(); TrainingRecordEntity record=requireRecord(id); if(!uid.equals(record.getUserId())) throw new ForbiddenException("Only participant can submit training report"); if(req==null||req.getContent()==null||req.getContent().isBlank()) throw new BusinessException("Training report content is required"); List<TrainingReportVersionEntity> all=reportMapper.selectByRecordId(id); TrainingReportVersionEntity last=new TrainingReportVersionEntity(); last.setTrainingRecordId(id);last.setVersionNo(all.size()+1);last.setFileId(req.getFileId());last.setContent(req.getContent());last.setReportStatus("submitted");last.setSubmittedBy(uid);last.setSubmittedAt(LocalDateTime.now());last.setCreatedAt(LocalDateTime.now());last.setUpdatedAt(LocalDateTime.now());reportMapper.insert(last); record.setTrainingStatus("learning");record.setUpdatedAt(LocalDateTime.now());recordMapper.updateById(record);TrainingPlanEntity plan=planMapper.selectById(record.getPlanId());if(notificationService!=null&&plan!=null)notificationService.create(plan.getOwnerId(),"TRAINING_REPORT_SUBMITTED","training_record",id,"培训报告已提交","请审核培训报告");return last;}
 @Override @Transactional public void review(Long id,TrainingRecordReviewRequest req){Long uid=user();TrainingRecordEntity record=requireRecord(id);if(!canManage(record,uid))throw new ForbiddenException("Only trainer or plan owner can review training");if(req==null||!java.util.Set.of("return","complete","approve").contains(req.getAction()))throw new BusinessException("Invalid training review action");if("return".equals(req.getAction()))record.setTrainingStatus("makeup");else record.setTrainingStatus("completed");if(req.getScore()!=null)record.setScore(req.getScore());record.setResultComment(req.getComment());if("complete".equals(req.getAction())||"approve".equals(req.getAction())){record.setCompletedAt(LocalDateTime.now());generateCompletionProof(record);}record.setUpdatedAt(LocalDateTime.now());recordMapper.updateById(record);}
 @Override @Transactional(readOnly=true) public List<TrainingEvaluationEntity> evaluations(Long id){requireRecord(id);return evaluationMapper.selectByRecordId(id);}
 @Override @Transactional public void evaluate(Long id,TrainingEvaluationRequest req){Long uid=user();TrainingRecordEntity record=requireRecord(id);if(!canManage(record,uid))throw new ForbiddenException("Only trainer or plan owner can evaluate training");if(req==null||req.getScore()==null||req.getScore().signum()<0||req.getScore().compareTo(java.math.BigDecimal.valueOf(100))>0)throw new BusinessException("Evaluation score must be between 0 and 100");TrainingEvaluationEntity e=evaluationMapper.selectOne(id,req.getDimensionCode());if(e==null){e=new TrainingEvaluationEntity();e.setTrainingRecordId(id);e.setDimensionCode(req.getDimensionCode());e.setCreatedAt(LocalDateTime.now());}e.setScore(req.getScore());e.setComment(req.getComment());e.setEvaluatorId(uid);e.setEvaluatedAt(LocalDateTime.now());e.setUpdatedAt(LocalDateTime.now());if(e.getId()==null)evaluationMapper.insert(e);else evaluationMapper.updateById(e);}
 @Override @Transactional(readOnly=true) public TrainingCompletionProofVO proof(Long id){TrainingRecordEntity r=requireRecord(id);TrainingCompletionProofVO vo=new TrainingCompletionProofVO();vo.setTrainingRecordId(id);vo.setCompleted("completed".equals(r.getTrainingStatus()));vo.setScore(r.getScore());vo.setCompletionProofFileId(r.getCompletionProofFileId());vo.setMessage(Boolean.TRUE.equals(vo.getCompleted())?"Training completed; completion proof is ready when a proof file is configured":"Training is not completed");return vo;}
 @Override @Transactional(readOnly=true) public List<TrainingReportVersionEntity> reports(Long id){requireRecord(id);return reportMapper.selectByRecordId(id);}
 private TrainingRecordEntity requireRecord(Long id){TrainingRecordEntity r=recordMapper.selectById(id);if(r==null)throw new ResourceNotFoundException("Training record not found");return r;}
 private Long user(){Long id=CurrentUserUtils.currentUserId();if(id==null)throw new ForbiddenException("Authentication is required");return id;}
 private boolean canManage(TrainingRecordEntity r,Long uid){if(CurrentUserUtils.currentRoleCodes().stream().anyMatch(x->"ADMIN".equalsIgnoreCase(x)))return true;TrainingPlanEntity p=planMapper.selectById(r.getPlanId());return p!=null&&(uid.equals(p.getOwnerId())||uid.equals(p.getTrainerId()));}
 private void generateCompletionProof(TrainingRecordEntity record){if(record.getCompletionProofFileId()!=null||fileResourceService==null)return;try{TrainingPlanEntity plan=planMapper.selectById(record.getPlanId());Path temp=Files.createTempFile("training-proof-", ".txt");String text="培训完成证明\n培训计划："+(plan==null?record.getPlanId():plan.getPlanName())+"\n培训记录："+record.getId()+"\n学员："+record.getUserId()+"\n成绩："+record.getScore()+"\n完成时间："+record.getCompletedAt();Files.writeString(temp,text,java.nio.charset.StandardCharsets.UTF_8);FileResourceVO file=fileResourceService.importPrivate(temp,"training-completion-proof-"+record.getId()+".txt","培训完成证明");record.setCompletionProofFileId(file.getId());Files.deleteIfExists(temp);}catch(Exception ex){throw new BusinessException("Completion proof generation failed");}}
}
