package com.bdis.modules.training.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.query.TrainingFeedbackQuery;
import com.bdis.modules.training.request.TrainingFeedbackCreateRequest;
import com.bdis.modules.training.request.TrainingFeedbackUpdateRequest;
import com.bdis.modules.training.service.TrainingFeedbackService;
import com.bdis.modules.training.vo.TrainingFeedbackDetailVO;
import com.bdis.modules.training.vo.TrainingFeedbackListVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/training-feedbacks")
public class TrainingFeedbackController {
    private final TrainingFeedbackService feedbackService;
    private final AuthorizationService authorizationService;

    public TrainingFeedbackController(
            TrainingFeedbackService feedbackService,
            AuthorizationService authorizationService) {
        this.feedbackService = feedbackService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<PageResult<TrainingFeedbackListVO>> page(
            @Valid @ModelAttribute TrainingFeedbackQuery query) {
        authorizationService.requirePermission("edu:training-feedback:list");
        return Result.success(feedbackService.page(query));
    }

    @PostMapping
    public Result<TrainingFeedbackDetailVO> create(
            @Valid @RequestBody TrainingFeedbackCreateRequest request) {
        authorizationService.requirePermission("edu:training-feedback:add");
        Long id = feedbackService.create(request);
        return Result.success(feedbackService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<TrainingFeedbackDetailVO> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody TrainingFeedbackUpdateRequest request) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:training-feedback:update");
        feedbackService.update(id, request);
        return Result.success(feedbackService.getDetail(id));
    }

    private void requirePositiveId(Long id) {
        if (id == null || id <= 0) throw new BusinessException("Training feedback id must be positive");
    }
}
