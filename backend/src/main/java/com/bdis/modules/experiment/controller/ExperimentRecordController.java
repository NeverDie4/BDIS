package com.bdis.modules.experiment.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.exception.BusinessException;
import com.bdis.file.vo.FileBusinessVO;
import com.bdis.modules.experiment.entity.ExperimentRecordVersionEntity;
import com.bdis.modules.experiment.query.ExperimentRecordQuery;
import com.bdis.modules.experiment.request.ExperimentRecordArchiveRequest;
import com.bdis.modules.experiment.request.ExperimentRecordAttachmentBindRequest;
import com.bdis.modules.experiment.request.ExperimentRecordCreateRequest;
import com.bdis.modules.experiment.request.ExperimentRecordGradeRequest;
import com.bdis.modules.experiment.request.ExperimentRecordReturnRequest;
import com.bdis.modules.experiment.request.ExperimentRecordSubmitRequest;
import com.bdis.modules.experiment.request.ExperimentRecordUpdateRequest;
import com.bdis.modules.experiment.request.ExperimentRecordVersionRequest;
import com.bdis.modules.experiment.service.ExperimentRecordService;
import com.bdis.modules.experiment.service.ExperimentRecordVersionService;
import com.bdis.modules.experiment.vo.ExperimentRecordDetailVO;
import com.bdis.modules.experiment.vo.ExperimentRecordListVO;
import com.bdis.modules.file.vo.FileResourceVO;
import com.bdis.modules.permission.service.AuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/experiment-records")
public class ExperimentRecordController {

    private final ExperimentRecordService recordService;
    private final AuthorizationService authorizationService;
    private final ExperimentRecordVersionService versionService;

    public ExperimentRecordController(
            ExperimentRecordService recordService, AuthorizationService authorizationService) {
        this.recordService = recordService;
        this.authorizationService = authorizationService;
        this.versionService = null;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ExperimentRecordController(
            ExperimentRecordService recordService,
            AuthorizationService authorizationService,
            ExperimentRecordVersionService versionService) {
        this.recordService = recordService;
        this.authorizationService = authorizationService;
        this.versionService = versionService;
    }

    @GetMapping
    public Result<PageResult<ExperimentRecordListVO>> page(
            @Valid @ModelAttribute ExperimentRecordQuery query) {
        authorizationService.requirePermission("edu:experiment-record:list");
        return Result.success(recordService.page(query));
    }

    @GetMapping("/{id}")
    public Result<ExperimentRecordDetailVO> detail(@PathVariable @Positive Long id) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:experiment-record:detail");
        return Result.success(recordService.getDetail(id));
    }

    @PostMapping
    public Result<ExperimentRecordDetailVO> create(
            @Valid @RequestBody ExperimentRecordCreateRequest request) {
        authorizationService.requirePermission("edu:experiment-record:add");
        Long id = recordService.create(request);
        return Result.success(recordService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<ExperimentRecordDetailVO> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ExperimentRecordUpdateRequest request) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:experiment-record:update");
        recordService.update(id, request);
        return Result.success(recordService.getDetail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @Positive Long id) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:experiment-record:delete");
        recordService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/submit")
    public Result<ExperimentRecordDetailVO> submit(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ExperimentRecordSubmitRequest request) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:experiment-record:submit");
        recordService.submit(id, request);
        return Result.success(recordService.getDetail(id));
    }

    @PostMapping("/{id}/archive")
    public Result<ExperimentRecordDetailVO> archive(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ExperimentRecordArchiveRequest request) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:experiment-record:archive");
        recordService.archive(id, request);
        return Result.success(recordService.getDetail(id));
    }

    @PostMapping("/{id}/grade")
    public Result<ExperimentRecordDetailVO> grade(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ExperimentRecordGradeRequest request) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:experiment-record:grade");
        recordService.grade(id, request);
        return Result.success(recordService.getDetail(id));
    }

    @PostMapping("/{id}/return")
    public Result<ExperimentRecordDetailVO> returnForRevision(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ExperimentRecordReturnRequest request) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:experiment-record:return");
        recordService.returnForRevision(id, request);
        return Result.success(recordService.getDetail(id));
    }

    @PostMapping("/{id}/versions")
    public Result<ExperimentRecordVersionEntity> createVersion(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ExperimentRecordVersionRequest request) {
        authorizationService.requirePermission("edu:experiment-record:version");
        return Result.success(versionService.create(id, request));
    }

    @GetMapping("/{id}/versions")
    public Result<List<ExperimentRecordVersionEntity>> versions(@PathVariable @Positive Long id) {
        authorizationService.requirePermission("edu:experiment-record:detail");
        return Result.success(versionService.list(id));
    }

    @GetMapping("/{id}/attachments")
    public Result<List<FileResourceVO>> listAttachments(
            @PathVariable @Positive Long id, @RequestParam(required = false) String fileUsage) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:experiment-attachment:list");
        return Result.success(recordService.listAttachments(id, fileUsage));
    }

    @PostMapping("/{id}/attachments")
    public Result<FileBusinessVO> bindAttachment(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ExperimentRecordAttachmentBindRequest request) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:experiment-attachment:add");
        return Result.success(recordService.bindAttachment(id, request));
    }

    @DeleteMapping("/{id}/attachments/{fileId}")
    public Result<Void> unbindAttachment(
            @PathVariable @Positive Long id, @PathVariable @Positive Long fileId) {
        requirePositiveId(id);
        if (fileId == null || fileId <= 0) {
            throw new BusinessException("File id must be positive");
        }
        authorizationService.requirePermission("edu:experiment-attachment:delete");
        recordService.unbindAttachment(id, fileId);
        return Result.success();
    }

    private void requirePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("Experiment record id must be positive");
        }
    }
}
