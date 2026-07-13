package com.bdis.modules.map.service.impl;

import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbMapper;
import com.bdis.modules.map.dto.MapPointUpsertRequest;
import com.bdis.modules.map.entity.MapPointEntity;
import com.bdis.modules.map.mapper.MapPointMapper;
import com.bdis.modules.map.query.MapPointQuery;
import com.bdis.modules.map.service.MapPointService;
import com.bdis.modules.map.vo.MapPointVO;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MapPointServiceImpl implements MapPointService {

    private static final DateTimeFormatter HERB_NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MapPointMapper mapPointMapper;

    private final HerbMapper herbMapper;

    @Override
    public List<MapPointVO> listMapPoints(MapPointQuery query) {
        return mapPointMapper.selectMapPoints(
                query.getKeyword(), query.getDistrict(), query.getSpeciesId(), query.getBaseId());
    }

    @Override
    @Transactional
    public MapPointVO createMapPoint(MapPointUpsertRequest request) {
        Long speciesId = resolveSpeciesId(request);
        MapPointEntity entity = new MapPointEntity();
        fillMapPoint(entity, request, speciesId);
        entity.setCreatedBy(SecurityUtils.currentUser().getUserId());
        mapPointMapper.insert(entity);
        return findCreatedOrUpdated(entity.getId());
    }

    @Override
    @Transactional
    public MapPointVO updateMapPoint(Long pointId, MapPointUpsertRequest request) {
        MapPointEntity entity = mapPointMapper.selectById(pointId);
        if (entity == null) {
            throw new ResourceNotFoundException("地图点位不存在");
        }
        Long speciesId = resolveSpeciesId(request);
        fillMapPoint(entity, request, speciesId);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        mapPointMapper.updateById(entity);
        return findCreatedOrUpdated(pointId);
    }

    @Override
    @Transactional
    public MapPointVO updateMapPointStatus(Long pointId, Integer status) {
        MapPointEntity entity = mapPointMapper.selectById(pointId);
        if (entity == null) {
            throw new ResourceNotFoundException("地图点位不存在");
        }
        entity.setStatus(status);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        mapPointMapper.updateById(entity);
        return findCreatedOrUpdated(pointId);
    }

    @Override
    @Transactional
    public void deleteMapPoint(Long pointId) {
        MapPointEntity entity = mapPointMapper.selectById(pointId);
        if (entity == null) {
            throw new ResourceNotFoundException("地图点位不存在");
        }
        Long operatorId = SecurityUtils.currentUser().getUserId();
        entity.setDeletedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        mapPointMapper.updateById(entity);
        if (mapPointMapper.deleteById(pointId) == 0) {
            throw new ResourceNotFoundException("地图点位不存在");
        }
    }

    private Long resolveSpeciesId(MapPointUpsertRequest request) {
        if (request.getSpeciesId() != null) {
            updateHerbIfNeeded(request.getSpeciesId(), request);
            return request.getSpeciesId();
        }

        HerbEntity existing = herbMapper.selectByHerbName(request.getHerbName());
        if (existing != null) {
            updateHerbIfNeeded(existing.getId(), request);
            return existing.getId();
        }

        HerbEntity herb = new HerbEntity();
        herb.setHerbNo(generateHerbNo());
        herb.setHerbName(request.getHerbName());
        herb.setAliasName(request.getAliasName());
        herb.setLatinName(request.getLatinName());
        herb.setMedicinalPart(request.getMedicinalPart());
        herb.setEfficacy(request.getEfficacy());
        herb.setGrowthEnvironment(request.getGrowthEnvironment());
        herb.setOriginArea(request.getOriginArea());
        herb.setGrowthCycle(request.getGrowthCycle());
        herb.setDescription(request.getHerbDescription());
        herb.setCreatedBy(SecurityUtils.currentUser().getUserId());
        herbMapper.insert(herb);
        return herb.getId();
    }

    private void updateHerbIfNeeded(Long speciesId, MapPointUpsertRequest request) {
        HerbEntity herb = herbMapper.selectById(speciesId);
        if (herb == null) {
            throw new ResourceNotFoundException("药材品种不存在");
        }
        boolean changed = false;
        changed =
                applyTextUpdate(request.getHerbName(), herb.getHerbName(), herb::setHerbName)
                        || changed;
        changed =
                applyTextUpdate(request.getAliasName(), herb.getAliasName(), herb::setAliasName)
                        || changed;
        changed =
                applyTextUpdate(request.getLatinName(), herb.getLatinName(), herb::setLatinName)
                        || changed;
        changed =
                applyTextUpdate(
                                request.getMedicinalPart(),
                                herb.getMedicinalPart(),
                                herb::setMedicinalPart)
                        || changed;
        changed =
                applyTextUpdate(request.getEfficacy(), herb.getEfficacy(), herb::setEfficacy)
                        || changed;
        changed =
                applyTextUpdate(
                                request.getGrowthEnvironment(),
                                herb.getGrowthEnvironment(),
                                herb::setGrowthEnvironment)
                        || changed;
        changed =
                applyTextUpdate(request.getOriginArea(), herb.getOriginArea(), herb::setOriginArea)
                        || changed;
        changed =
                applyTextUpdate(
                                request.getGrowthCycle(),
                                herb.getGrowthCycle(),
                                herb::setGrowthCycle)
                        || changed;
        changed =
                applyTextUpdate(
                                request.getHerbDescription(),
                                herb.getDescription(),
                                herb::setDescription)
                        || changed;
        if (changed) {
            herb.setUpdatedBy(SecurityUtils.currentUser().getUserId());
            herbMapper.updateById(herb);
        }
    }

    private boolean applyTextUpdate(
            String nextValue, String currentValue, java.util.function.Consumer<String> setter) {
        if (!StringUtils.hasText(nextValue) || nextValue.equals(currentValue)) {
            return false;
        }
        setter.accept(nextValue);
        return true;
    }

    private void fillMapPoint(
            MapPointEntity entity, MapPointUpsertRequest request, Long speciesId) {
        entity.setSpeciesId(speciesId);
        entity.setBaseId(request.getBaseId());
        entity.setRegionId(request.getRegionId());
        entity.setLocationName(request.getLocationName());
        entity.setLongitude(request.getLongitude());
        entity.setLatitude(request.getLatitude());
        entity.setProvince(defaultText(request.getProvince(), "重庆市"));
        entity.setCity(defaultText(request.getCity(), "重庆市"));
        entity.setDistrict(request.getDistrict());
        entity.setAddress(request.getAddress());
        entity.setAltitude(request.getAltitude());
        entity.setDistributionType(defaultText(request.getDistributionType(), "cultivated"));
        entity.setDistributionLevel(request.getDistributionLevel());
        entity.setDistributionDesc(request.getDistributionDesc());
        entity.setCoverImageUrl(request.getCoverImageUrl());
        entity.setLastCollectedAt(request.getLastCollectedAt());
        entity.setSourceType(defaultText(request.getSourceType(), "pc"));
        entity.setDataSource(defaultText(request.getDataSource(), "map"));
        entity.setRemark(request.getRemark());
    }

    private MapPointVO findCreatedOrUpdated(Long pointId) {
        List<MapPointVO> points = mapPointMapper.selectMapPoints(null, null, null, null);
        return points.stream()
                .filter(point -> pointId.equals(point.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("地图点位不存在"));
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String generateHerbNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return "HERB-" + java.time.LocalDateTime.now().format(HERB_NO_TIME_FORMAT) + "-" + suffix;
    }
}
