package com.bdis.modules.growth.service.impl;

import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.growth.dto.GrowthRecordCreateRequest;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import com.bdis.modules.growth.mapper.GrowthRecordMapper;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.map.entity.MapPointEntity;
import com.bdis.modules.map.mapper.MapPointMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class GrowthRecordServiceImpl implements GrowthRecordService {

    private final GrowthRecordMapper growthRecordMapper;

    private final MapPointMapper mapPointMapper;

    @Override
    public List<GrowthRecordVO> listByPointId(Long pointId) {
        if (mapPointMapper.selectById(pointId) == null) {
            throw new ResourceNotFoundException("地图点位不存在");
        }
        return growthRecordMapper.selectByPointId(pointId);
    }

    @Override
    @Transactional
    public GrowthRecordVO createForPoint(Long pointId, GrowthRecordCreateRequest request) {
        MapPointEntity point = mapPointMapper.selectById(pointId);
        if (point == null) {
            throw new ResourceNotFoundException("地图点位不存在");
        }

        Long operatorId = SecurityUtils.currentUser().getUserId();

        LocalDateTime collectedAt =
                request.getCollectedAt() == null ? LocalDateTime.now() : request.getCollectedAt();

        GrowthRecordEntity record = new GrowthRecordEntity();
        record.setSpeciesId(point.getSpeciesId());
        record.setDistributionId(pointId);
        record.setCollectorId(operatorId);
        record.setCollectorNameSnapshot(request.getCollectorName());
        record.setRegionId(point.getRegionId());
        record.setLongitude(point.getLongitude());
        record.setLatitude(point.getLatitude());
        record.setGrowthStage(request.getGrowthStage());
        record.setSoilType(request.getSoilType());
        record.setSoilPh(request.getSoilPh());
        record.setTemperature(request.getTemperature());
        record.setHumidity(request.getHumidity());
        record.setWeather(request.getWeather());
        record.setSampleWeight(request.getSampleWeight());
        record.setDeviceType("pc");
        record.setDataSource(defaultText(request.getDataSource(), "map"));
        record.setReviewStatus("draft");
        record.setCollectedAt(collectedAt);
        record.setCreatedBy(operatorId);
        record.setRemark(request.getRemark());
        growthRecordMapper.insert(record);

        point.setLastCollectedAt(collectedAt);
        point.setUpdatedBy(operatorId);
        mapPointMapper.updateById(point);

        return growthRecordMapper.selectByPointId(pointId).stream()
                .filter(item -> record.getId().equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("采集记录不存在"));
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
