package com.bdis.modules.training.controller;

import com.bdis.common.core.Result;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.service.TrainingSummaryService;
import com.bdis.modules.training.vo.TrainingSummaryVO;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training-plans")
public class TrainingSummaryController {
    private final TrainingSummaryService summaryService;
    private final AuthorizationService authorizationService;

    public TrainingSummaryController(
            TrainingSummaryService summaryService,
            AuthorizationService authorizationService) {
        this.summaryService = summaryService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/{id}/summary")
    public Result<TrainingSummaryVO> summary(@PathVariable @Positive Long id) {
        if (id == null || id <= 0) throw new BusinessException("Training plan id must be positive");
        authorizationService.requirePermission("edu:training-summary:view");
        return Result.success(summaryService.getSummary(id));
    }
}
