package com.bdis.modules.training.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.query.TrainingRecordQuery;
import com.bdis.modules.training.request.TrainingRecordCreateRequest;
import com.bdis.modules.training.request.TrainingRecordUpdateRequest;
import com.bdis.modules.training.service.TrainingRecordService;
import com.bdis.modules.training.vo.TrainingRecordDetailVO;
import com.bdis.modules.training.vo.TrainingRecordListVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training-records")
public class TrainingRecordController {
    private final TrainingRecordService recordService;
    private final AuthorizationService authorizationService;

    public TrainingRecordController(
            TrainingRecordService recordService, AuthorizationService authorizationService) {
        this.recordService = recordService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<PageResult<TrainingRecordListVO>> page(
            @Valid @ModelAttribute TrainingRecordQuery query) {
        authorizationService.requirePermission("edu:training-record:list");
        return Result.success(recordService.page(query));
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    public void export(@Valid @ModelAttribute TrainingRecordQuery query, HttpServletResponse response)
            throws IOException {
        authorizationService.requirePermission("edu:training-record:list");
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=training-attendance.csv");
        response.getWriter().write(recordService.exportCsv(query));
    }

    @GetMapping("/{id}")
    public Result<TrainingRecordDetailVO> detail(@PathVariable @Positive Long id) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:training-record:detail");
        return Result.success(recordService.getDetail(id));
    }

    @PostMapping
    public Result<TrainingRecordDetailVO> create(
            @Valid @RequestBody TrainingRecordCreateRequest request) {
        authorizationService.requirePermission("edu:training-record:add");
        Long id = recordService.create(request);
        return Result.success(recordService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<TrainingRecordDetailVO> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody TrainingRecordUpdateRequest request) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:training-record:update");
        recordService.update(id, request);
        return Result.success(recordService.getDetail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable @Positive Long id) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:training-record:update");
        recordService.remove(id);
        return Result.success();
    }

    private void requirePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("Training record id must be positive");
        }
    }
}
