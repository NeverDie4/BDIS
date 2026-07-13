package com.bdis.soap.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.modules.growth.dto.GrowthRecordUpsertRequest;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbMapper;
import com.bdis.modules.map.entity.HerbBaseEntity;
import com.bdis.modules.map.entity.MapPointEntity;
import com.bdis.modules.map.mapper.HerbBaseMapper;
import com.bdis.modules.map.mapper.MapPointMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GrowthRecordSoapImportHandler implements SoapBusinessImportHandler {

    private final HerbMapper herbMapper;
    private final HerbBaseMapper herbBaseMapper;
    private final MapPointMapper mapPointMapper;
    private final GrowthRecordService growthRecordService;

    public GrowthRecordSoapImportHandler(
            HerbMapper herbMapper,
            HerbBaseMapper herbBaseMapper,
            MapPointMapper mapPointMapper,
            GrowthRecordService growthRecordService) {
        this.herbMapper = herbMapper;
        this.herbBaseMapper = herbBaseMapper;
        this.mapPointMapper = mapPointMapper;
        this.growthRecordService = growthRecordService;
    }

    @Override
    public boolean supports(String resourceType) {
        if (!StringUtils.hasText(resourceType)) {
            return false;
        }
        String normalized = resourceType.toLowerCase(Locale.ROOT);
        return normalized.equals("growth_record")
                || normalized.equals("herb_growth_record")
                || normalized.equals("growthrecord");
    }

    @Override
    public SoapBusinessImportResult importData(SoapBusinessImportContext context) {
        Map<String, Object> data = context.getParsedData();
        String externalNo = requiredText(data, "externalNo", "SOAP 外部编号不能为空");
        String herbName = requiredText(data, "herbName", "SOAP 药材名称不能为空");
        HerbEntity herb = herbMapper.selectByHerbName(herbName);
        if (herb == null) {
            throw new ResourceNotFoundException("SOAP 药材名称未匹配到品种：" + herbName);
        }

        GrowthRecordUpsertRequest request = new GrowthRecordUpsertRequest();
        request.setSpeciesId(herb.getId());
        request.setDeviceType("soap");
        request.setDataSource(defaultText(text(data, "sourceType"), "SOAP"));
        request.setCollectedAt(parseTime(text(data, "collectedAt")));
        request.setRemark("SOAP 导入，外部编号：" + externalNo);
        applyBaseAndPoint(request, herb.getId(), text(data, "baseName"));

        GrowthRecordVO record =
                growthRecordService.importFromSoap(
                        externalNo, request, text(data, "collectorName"));
        return SoapBusinessImportResult.imported(record.getId(), externalNo, "SOAP 生长采集记录已导入");
    }

    private void applyBaseAndPoint(
            GrowthRecordUpsertRequest request, Long speciesId, String baseName) {
        if (!StringUtils.hasText(baseName)) {
            return;
        }
        HerbBaseEntity herbBase =
                herbBaseMapper.selectOne(
                        new LambdaQueryWrapper<HerbBaseEntity>()
                                .eq(HerbBaseEntity::getBaseName, baseName)
                                .last("limit 1"));
        if (herbBase == null) {
            throw new ResourceNotFoundException("SOAP 基地名称未匹配到基地：" + baseName);
        }
        MapPointEntity point =
                mapPointMapper.selectOne(
                        new LambdaQueryWrapper<MapPointEntity>()
                                .eq(MapPointEntity::getBaseId, herbBase.getId())
                                .eq(MapPointEntity::getSpeciesId, speciesId)
                                .orderByDesc(MapPointEntity::getUpdatedAt)
                                .last("limit 1"));
        if (point == null) {
            throw new ResourceNotFoundException("SOAP 基地与药材尚未配置地图点位");
        }
        request.setDistributionId(point.getId());
        request.setRegionId(point.getRegionId());
        request.setLongitude(point.getLongitude());
        request.setLatitude(point.getLatitude());
    }

    private LocalDateTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "SOAP 采集时间格式不正确");
        }
    }

    private String requiredText(Map<String, Object> data, String key, String message) {
        String value = text(data, key);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, message);
        }
        return value;
    }

    private String text(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? null : value.toString().trim();
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
