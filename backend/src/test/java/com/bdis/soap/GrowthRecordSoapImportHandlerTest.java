package com.bdis.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.modules.growth.dto.GrowthRecordUpsertRequest;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbMapper;
import com.bdis.modules.map.entity.HerbBaseEntity;
import com.bdis.modules.map.entity.MapPointEntity;
import com.bdis.modules.map.mapper.HerbBaseMapper;
import com.bdis.modules.map.mapper.MapPointMapper;
import com.bdis.soap.integration.GrowthRecordSoapImportHandler;
import com.bdis.soap.integration.SoapBusinessImportContext;
import com.bdis.soap.integration.SoapBusinessImportResult;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GrowthRecordSoapImportHandlerTest {

    @Mock private HerbMapper herbMapper;
    @Mock private HerbBaseMapper herbBaseMapper;
    @Mock private MapPointMapper mapPointMapper;
    @Mock private GrowthRecordService growthRecordService;

    @Test
    void importsParsedGrowthRecordThroughGrowthService() {
        HerbEntity herb = new HerbEntity();
        herb.setId(12L);
        when(herbMapper.selectByHerbName("黄连")).thenReturn(herb);
        HerbBaseEntity base = new HerbBaseEntity();
        base.setId(24L);
        when(herbBaseMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(base);
        MapPointEntity point = new MapPointEntity();
        point.setId(48L);
        point.setRegionId(72L);
        point.setLongitude(new BigDecimal("108.113"));
        point.setLatitude(new BigDecimal("29.998"));
        when(mapPointMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(point);
        GrowthRecordVO imported = new GrowthRecordVO();
        imported.setId(36L);
        when(growthRecordService.importFromSoap(
                        eq("SOAP-001"), org.mockito.ArgumentMatchers.any(), eq("外部采集员")))
                .thenReturn(imported);
        SoapBusinessImportContext context = new SoapBusinessImportContext();
        context.setResourceType("GROWTH_RECORD");
        context.setParsedData(
                Map.of(
                        "externalNo", "SOAP-001",
                        "herbName", "黄连",
                        "baseName", "重庆示范基地",
                        "collectorName", "外部采集员",
                        "collectedAt", "2026-07-11T09:30:00",
                        "sourceType", "PC"));
        GrowthRecordSoapImportHandler handler =
                new GrowthRecordSoapImportHandler(
                        herbMapper, herbBaseMapper, mapPointMapper, growthRecordService);

        SoapBusinessImportResult result = handler.importData(context);

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getBusinessId()).isEqualTo(36L);
        ArgumentCaptor<GrowthRecordUpsertRequest> requestCaptor =
                ArgumentCaptor.forClass(GrowthRecordUpsertRequest.class);
        verify(growthRecordService)
                .importFromSoap(eq("SOAP-001"), requestCaptor.capture(), eq("外部采集员"));
        assertThat(requestCaptor.getValue().getSpeciesId()).isEqualTo(12L);
        assertThat(requestCaptor.getValue().getDeviceType()).isEqualTo("soap");
        assertThat(requestCaptor.getValue().getDataSource()).isEqualTo("SOAP");
        assertThat(requestCaptor.getValue().getDistributionId()).isEqualTo(48L);
        assertThat(requestCaptor.getValue().getCollectedAt()).hasToString("2026-07-11T09:30");
    }

    @Test
    void supportsDocumentedGrowthResourceNames() {
        GrowthRecordSoapImportHandler handler =
                new GrowthRecordSoapImportHandler(
                        herbMapper, herbBaseMapper, mapPointMapper, growthRecordService);

        assertThat(handler.supports("GROWTH_RECORD")).isTrue();
        assertThat(handler.supports("herb_growth_record")).isTrue();
        assertThat(handler.supports("growthRecord")).isTrue();
        assertThat(handler.supports("course")).isFalse();
    }
}
