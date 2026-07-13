package com.bdis.modules.growth.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.growth.dto.GrowthAuditCommentRequest;
import com.bdis.modules.growth.dto.GrowthAuditRequest;
import com.bdis.modules.growth.dto.GrowthRecordUpsertRequest;
import com.bdis.modules.growth.query.GrowthRecordQuery;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.GrowthAuditHistoryVO;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.growth.vo.GrowthTraceQrCodeVO;
import com.bdis.modules.growth.vo.GrowthTraceEventVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/growth-records")
@RequirePermission("growth:record:view")
public class GrowthRecordController {

    private final GrowthRecordService growthRecordService;

    public GrowthRecordController(GrowthRecordService growthRecordService) {
        this.growthRecordService = growthRecordService;
    }

    @GetMapping
    public Result<PageResult<GrowthRecordVO>> page(@Valid GrowthRecordQuery query) {
        return Result.success(growthRecordService.page(query));
    }

    @GetMapping("/review/page")
    @RequirePermission("growth:record:audit")
    public Result<PageResult<GrowthRecordVO>> reviewPage(@Valid GrowthRecordQuery query) {
        return Result.success(growthRecordService.reviewPage(query));
    }

    @GetMapping("/{id}")
    public Result<GrowthRecordVO> detail(@PathVariable Long id) {
        return Result.success(growthRecordService.detail(id));
    }

    @PostMapping
    @RequirePermission("growth:record:create")
    public Result<GrowthRecordVO> create(@Valid @RequestBody GrowthRecordUpsertRequest request) {
        return Result.success(growthRecordService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("growth:record:update")
    public Result<GrowthRecordVO> update(
            @PathVariable Long id, @Valid @RequestBody GrowthRecordUpsertRequest request) {
        return Result.success(growthRecordService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("growth:record:delete")
    public Result<Void> delete(@PathVariable Long id) {
        growthRecordService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/submissions")
    @RequirePermission("growth:record:submit")
    public Result<GrowthRecordVO> submit(@PathVariable Long id) {
        return Result.success(growthRecordService.submit(id));
    }

    @PutMapping("/{id}/submit")
    @RequirePermission("growth:record:submit")
    public Result<GrowthRecordVO> submitForReview(@PathVariable Long id) {
        return Result.success(growthRecordService.submit(id));
    }

    @PutMapping("/{id}/approve")
    @RequirePermission("growth:record:audit")
    public Result<GrowthRecordVO> approve(
            @PathVariable Long id, @Valid @RequestBody GrowthAuditCommentRequest request) {
        return Result.success(growthRecordService.approve(id, request));
    }

    @PutMapping("/{id}/reject")
    @RequirePermission("growth:record:audit")
    public Result<GrowthRecordVO> reject(
            @PathVariable Long id, @Valid @RequestBody GrowthAuditCommentRequest request) {
        return Result.success(growthRecordService.reject(id, request));
    }

    @PostMapping("/{id}/audit-records")
    @RequirePermission("growth:record:audit")
    public Result<GrowthRecordVO> audit(
            @PathVariable Long id, @Valid @RequestBody GrowthAuditRequest request) {
        return Result.success(growthRecordService.audit(id, request));
    }

    @PostMapping("/{id}/archives")
    @RequirePermission("growth:record:audit")
    public Result<GrowthRecordVO> archive(@PathVariable Long id) {
        return Result.success(growthRecordService.archive(id));
    }

    @PutMapping("/{id}/archive")
    @RequirePermission("growth:record:audit")
    public Result<GrowthRecordVO> archive(
            @PathVariable Long id, @Valid @RequestBody GrowthAuditCommentRequest request) {
        return Result.success(growthRecordService.archive(id, request));
    }

    @GetMapping("/{id}/audit-history")
    public Result<List<GrowthAuditHistoryVO>> auditHistory(@PathVariable Long id) {
        return Result.success(growthRecordService.auditHistory(id));
    }

    @GetMapping("/{id}/trace-events")
    public Result<List<GrowthTraceEventVO>> trace(@PathVariable Long id) {
        return Result.success(growthRecordService.trace(id));
    }

    @PostMapping("/{id}/trace-code/generate")
    public Result<GrowthTraceQrCodeVO> generateTraceCode(@PathVariable Long id) {
        return Result.success(growthRecordService.generateTraceCode(id));
    }

    @PostMapping("/{id}/trace-qrcode/generate")
    public Result<GrowthTraceQrCodeVO> generateTraceQrCode(@PathVariable Long id) {
        return Result.success(
                growthRecordService.generateTraceQrCode(id, currentPublicBaseUrl()));
    }

    @GetMapping("/{id}/trace-qrcode")
    public Result<GrowthTraceQrCodeVO> getTraceQrCode(@PathVariable Long id) {
        return Result.success(growthRecordService.getTraceQrCode(id));
    }

    @PutMapping("/{id}/trace/public-enable")
    public Result<GrowthTraceQrCodeVO> enablePublicTrace(@PathVariable Long id) {
        return Result.success(growthRecordService.enablePublicTrace(id));
    }

    @PutMapping("/{id}/trace/public-disable")
    public Result<GrowthTraceQrCodeVO> disablePublicTrace(@PathVariable Long id) {
        return Result.success(growthRecordService.disablePublicTrace(id));
    }

    private String currentPublicBaseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
