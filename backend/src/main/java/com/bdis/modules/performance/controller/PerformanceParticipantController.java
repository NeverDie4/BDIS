package com.bdis.modules.performance.controller;

import com.bdis.audit.annotation.AuditLogAnnotation;
import com.bdis.common.core.Result;
import com.bdis.modules.performance.dto.PerformanceParticipantRequest;
import com.bdis.modules.performance.entity.PerformanceParticipantEntity;
import com.bdis.modules.performance.service.PerformanceParticipantService;
import com.bdis.modules.performance.vo.PerformanceParticipantUserVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/performances/{performanceId}/participants")
public class PerformanceParticipantController {

    private final PerformanceParticipantService participantService;

    @GetMapping
    public Result<List<PerformanceParticipantEntity>> list(@PathVariable Long performanceId) {
        return Result.success(participantService.listParticipants(performanceId));
    }

    @GetMapping("/participant-users")
    public Result<List<PerformanceParticipantUserVO>> listParticipantUsers(
            @PathVariable Long performanceId) {
        return Result.success(participantService.listParticipantUsers(performanceId));
    }

    @PostMapping
    @AuditLogAnnotation(
            module = "M18_PERFORMANCE",
            operationType = "ADD_PARTICIPANT",
            bizType = "perf_record")
    public Result<PerformanceParticipantEntity> add(
            @PathVariable Long performanceId,
            @Valid @RequestBody PerformanceParticipantRequest request) {
        return Result.success(participantService.addParticipant(performanceId, request));
    }

    @PutMapping("/{participantId}")
    @AuditLogAnnotation(
            module = "M18_PERFORMANCE",
            operationType = "UPDATE_PARTICIPANT",
            bizType = "perf_record")
    public Result<PerformanceParticipantEntity> update(
            @PathVariable Long performanceId,
            @PathVariable Long participantId,
            @Valid @RequestBody PerformanceParticipantRequest request) {
        return Result.success(
                participantService.updateParticipant(performanceId, participantId, request));
    }

    @DeleteMapping("/{participantId}")
    @AuditLogAnnotation(
            module = "M18_PERFORMANCE",
            operationType = "REMOVE_PARTICIPANT",
            bizType = "perf_record")
    public Result<Void> remove(@PathVariable Long performanceId, @PathVariable Long participantId) {
        participantService.removeParticipant(performanceId, participantId);
        return Result.success();
    }
}
