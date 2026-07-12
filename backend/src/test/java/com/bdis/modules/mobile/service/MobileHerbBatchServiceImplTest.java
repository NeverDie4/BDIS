package com.bdis.modules.mobile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bdis.modules.collection.mapper.HerbBatchImageMapper;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.service.HerbBatchImageService;
import com.bdis.modules.collection.service.HerbBatchStatusService;
import com.bdis.modules.collection.service.HerbBatchSummaryService;
import com.bdis.modules.herb.service.HerbImageService;
import com.bdis.modules.herb.vo.HerbImageVO;
import com.bdis.modules.mobile.dto.MobileBatchImageUploadRequest;
import com.bdis.modules.mobile.service.impl.MobileHerbBatchServiceImpl;
import com.bdis.modules.mobile.vo.MobileImageIdentificationVO;
import com.bdis.modules.spectrum.service.HerbIdentificationService;
import com.bdis.modules.spectrum.vo.HerbIdentificationVO;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MobileHerbBatchServiceImplTest {

    @Mock private HerbBatchMapper herbBatchMapper;
    @Mock private HerbBatchImageMapper herbBatchImageMapper;
    @Mock private HerbCollectionTaskMapper herbCollectionTaskMapper;
    @Mock private HerbImageService herbImageService;
    @Mock private HerbBatchImageService herbBatchImageService;
    @Mock private HerbIdentificationService herbIdentificationService;
    @Mock private HerbBatchSummaryService herbBatchSummaryService;
    @Mock private HerbBatchStatusService herbBatchStatusService;

    @InjectMocks private MobileHerbBatchServiceImpl service;

    @Test
    void uploadRequestDoesNotEnableAutoIdentifyByDefault() {
        MobileBatchImageUploadRequest request = new MobileBatchImageUploadRequest();

        assertThat(request.getAutoIdentify()).isFalse();
    }

    @Test
    void latestIdentificationIncludesCompleteImageInformation() throws Exception {
        HerbIdentificationVO identification = new HerbIdentificationVO();
        identification.setImageId(1L);
        identification.setImageCode("IMG_1");
        identification.setImageUrl("/api/files/1/content");

        LocalDateTime collectTime = LocalDateTime.of(2026, 7, 12, 13, 38, 33);
        HerbImageVO image = new HerbImageVO();
        image.setId(1L);
        image.setImageName("wuzhimaotao.jpg");
        image.setImageType("whole");
        image.setCollectPlace("广东省河源市");
        image.setCollectTime(collectTime);

        when(herbIdentificationService.latest(1L)).thenReturn(identification);
        when(herbImageService.getById(1L)).thenReturn(image);

        MobileImageIdentificationVO result = service.latestIdentification(1L);

        assertThat(fieldValue(result, "imageName")).isEqualTo("wuzhimaotao.jpg");
        assertThat(fieldValue(result, "imageRole")).isEqualTo("whole");
        assertThat(fieldValue(result, "collectPlace")).isEqualTo("广东省河源市");
        assertThat(fieldValue(result, "collectTime")).isEqualTo(collectTime);
    }

    private Object fieldValue(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
