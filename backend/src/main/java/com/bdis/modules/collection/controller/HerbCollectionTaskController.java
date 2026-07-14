package com.bdis.modules.collection.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.collection.dto.HerbCollectionTaskCreateRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskMyQueryRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskQueryRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskUpdateRequest;
import com.bdis.modules.collection.service.HerbCollectionTaskService;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.collection.vo.HerbCollectionTaskListVO;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.GrowthChartPointVO;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequestMapping("/herb/collection-task")
@RequirePermission("growth:record:view")
public class HerbCollectionTaskController {

    private final HerbCollectionTaskService herbCollectionTaskService;
    private final GrowthRecordService growthRecordService;
    private final CollectionAccessService collectionAccessService;

    public HerbCollectionTaskController(
            HerbCollectionTaskService herbCollectionTaskService,
            GrowthRecordService growthRecordService,
            CollectionAccessService collectionAccessService) {
        this.herbCollectionTaskService = herbCollectionTaskService;
        this.growthRecordService = growthRecordService;
        this.collectionAccessService = collectionAccessService;
    }

    @PostMapping
    @RequirePermission("growth:record:create")
    public Result<HerbCollectionTaskVO> create(
            @Valid @RequestBody HerbCollectionTaskCreateRequest request) {
        return Result.success(herbCollectionTaskService.create(request));
    }

    @GetMapping("/assignable-collectors")
    @RequirePermission("growth:record:create")
    public Result<List<CollectionAccessService.AssignableCollector>> assignableCollectors() {
        return Result.success(collectionAccessService.listAssignableCollectors());
    }

    @PutMapping("/{id}")
    @RequirePermission("growth:record:update")
    public Result<HerbCollectionTaskVO> update(
            @PathVariable Long id, @Valid @RequestBody HerbCollectionTaskUpdateRequest request) {
        return Result.success(herbCollectionTaskService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("growth:record:delete")
    public Result<Void> delete(@PathVariable Long id) {
        herbCollectionTaskService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<HerbCollectionTaskVO> getById(@PathVariable Long id) {
        return Result.success(herbCollectionTaskService.getById(id));
    }

    @GetMapping("/page")
    public Result<PageResult<HerbCollectionTaskVO>> page(
            @ModelAttribute HerbCollectionTaskQueryRequest request) {
        return Result.success(herbCollectionTaskService.page(request));
    }

    @GetMapping("/my")
    public Result<PageResult<HerbCollectionTaskVO>> myTasks(
            @ModelAttribute HerbCollectionTaskMyQueryRequest request) {
        return Result.success(herbCollectionTaskService.myTasks(request));
    }

    @GetMapping("/list")
    public Result<List<HerbCollectionTaskListVO>> list(
            @ModelAttribute HerbCollectionTaskQueryRequest request) {
        return Result.success(herbCollectionTaskService.list(request));
    }

    @PutMapping("/{id}/publish")
    @RequirePermission("growth:record:submit")
    public Result<HerbCollectionTaskVO> publish(@PathVariable Long id) {
        return Result.success(herbCollectionTaskService.publish(id));
    }

    @PutMapping("/{id}/start")
    @RequirePermission("growth:record:update")
    public Result<HerbCollectionTaskVO> start(@PathVariable Long id) {
        return Result.success(herbCollectionTaskService.start(id));
    }

    @PutMapping("/{id}/complete")
    @RequirePermission("growth:record:update")
    public Result<HerbCollectionTaskVO> complete(@PathVariable Long id) {
        return Result.success(herbCollectionTaskService.complete(id));
    }

    @PutMapping("/{id}/cancel")
    @RequirePermission("growth:record:update")
    public Result<HerbCollectionTaskVO> cancel(@PathVariable Long id) {
        return Result.success(herbCollectionTaskService.cancel(id));
    }

    @GetMapping("/{taskId}/growth-records/chart")
    public Result<List<GrowthChartPointVO>> growthChart(
            @PathVariable Long taskId, @RequestParam(defaultValue = "plantHeight") String metric) {
        return Result.success(growthRecordService.getChartByTaskId(taskId, metric));
    }
}
