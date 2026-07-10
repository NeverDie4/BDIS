package com.bdis.modules.map.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.growth.dto.GrowthRecordCreateRequest;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.map.dto.MapPointUpsertRequest;
import com.bdis.modules.map.query.MapPointQuery;
import com.bdis.modules.map.service.MapPointService;
import com.bdis.modules.map.vo.MapPointVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/map-points")
public class MapPointController {

    private final MapPointService mapPointService;

    private final GrowthRecordService growthRecordService;

    @GetMapping
    public Result<List<MapPointVO>> listMapPoints(MapPointQuery query) {
        return Result.success(mapPointService.listMapPoints(query));
    }

    @PostMapping
    public Result<MapPointVO> createMapPoint(@Valid @RequestBody MapPointUpsertRequest request) {
        return Result.success(mapPointService.createMapPoint(request));
    }

    @PutMapping("/{pointId}")
    public Result<MapPointVO> updateMapPoint(
            @PathVariable Long pointId, @Valid @RequestBody MapPointUpsertRequest request) {
        return Result.success(mapPointService.updateMapPoint(pointId, request));
    }

    @DeleteMapping("/{pointId}")
    public Result<Void> deleteMapPoint(@PathVariable Long pointId) {
        mapPointService.deleteMapPoint(pointId);
        return Result.success(null);
    }

    @GetMapping("/{pointId}/collections")
    public Result<List<GrowthRecordVO>> listCollectionRecords(@PathVariable Long pointId) {
        return Result.success(growthRecordService.listByPointId(pointId));
    }

    @PostMapping("/{pointId}/collections")
    public Result<GrowthRecordVO> createCollectionRecord(
            @PathVariable Long pointId, @Valid @RequestBody GrowthRecordCreateRequest request) {
        return Result.success(growthRecordService.createForPoint(pointId, request));
    }
}
