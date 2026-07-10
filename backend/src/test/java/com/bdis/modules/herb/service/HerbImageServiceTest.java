package com.bdis.modules.herb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.herb.dto.HerbImageQueryRequest;
import com.bdis.modules.herb.dto.HerbImageUpdateRequest;
import com.bdis.common.storage.LocalFileStorage;
import com.bdis.modules.herb.dto.HerbImageUploadRequest;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.herb.service.impl.HerbImageServiceImpl;
import com.bdis.modules.herb.vo.HerbImageVO;
import com.bdis.modules.spectrum.service.HerbFeatureService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class HerbImageServiceTest {

    @TempDir private Path uploadRoot;

    @Mock private HerbImageMapper herbImageMapper;

    @Mock private HerbSpeciesMapper herbSpeciesMapper;

    @Mock private HerbFeatureService herbFeatureService;

    private HerbImageService herbImageService;

    @BeforeEach
    void setUp() {
        LocalFileStorage localFileStorage = new LocalFileStorage(uploadRoot.toString());
        herbImageService =
                new HerbImageServiceImpl(
                        herbImageMapper, herbSpeciesMapper, localFileStorage, herbFeatureService);
    }

    @Test
    void uploadImageSavesFileAndInsertsUploadedRecord() {
        HerbImageUploadRequest request = new HerbImageUploadRequest();
        request.setSpeciesId(1L);
        request.setCollectorId(9L);
        request.setCollectPlace("Shizhu base");
        request.setImageType("leaf");
        request.setGrowthStage("growth");
        request.setHealthStatus("unknown");
        HerbEntity species = new HerbEntity();
        species.setId(1L);
        species.setHerbName("Huanglian");
        when(herbSpeciesMapper.selectActiveById(1L)).thenReturn(species);
        doAnswer(
                        invocation -> {
                            HerbImageEntity image = invocation.getArgument(0);
                            image.setId(11L);
                            return 1;
                        })
                .when(herbImageMapper)
                .insertImage(any(HerbImageEntity.class));
        MockMultipartFile file =
                new MockMultipartFile("file", "huanglian_leaf.jpg", "image/jpeg", new byte[] {1});

        HerbImageVO result = herbImageService.upload(file, request);

        ArgumentCaptor<HerbImageEntity> imageCaptor = ArgumentCaptor.forClass(HerbImageEntity.class);
        verify(herbImageMapper).insertImage(imageCaptor.capture());
        HerbImageEntity inserted = imageCaptor.getValue();
        assertThat(inserted.getImageCode()).startsWith("IMG_");
        assertThat(inserted.getImageUrl()).startsWith("/herb/image/");
        assertThat(inserted.getImageUrl()).endsWith(".jpg");
        assertThat(inserted.getImageName()).isEqualTo("huanglian_leaf.jpg");
        assertThat(inserted.getUploadSource()).isEqualTo("mobile");
        assertThat(inserted.getProcessStatus()).isEqualTo("uploaded");
        assertThat(inserted.getCollectTime()).isNotNull();
        assertThat(inserted.getCreateTime()).isNotNull();
        assertThat(inserted.getUpdateTime()).isNotNull();
        assertThat(inserted.getDeleted()).isZero();
        assertThat(Files.exists(uploadRoot.resolve(inserted.getImageUrl().substring(1)))).isTrue();
        assertThat(result.getImageCode()).isEqualTo(inserted.getImageCode());
        assertThat(result.getSpeciesName()).isEqualTo("Huanglian");
        assertThat(result.getProcessStatus()).isEqualTo("uploaded");
        verify(herbFeatureService).extractImageFeature(11L);
    }

    @Test
    void getByIdRejectsMissingImage() {
        when(herbImageMapper.selectDetailById(1L)).thenReturn(null);

        assertThatThrownBy(() -> herbImageService.getById(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Herb image not found");
    }

    @Test
    void getByIdReturnsImageDetail() {
        HerbImageVO detail = new HerbImageVO();
        detail.setId(1L);
        detail.setImageCode("IMG_1");
        detail.setSpeciesName("Huanglian");
        when(herbImageMapper.selectDetailById(1L)).thenReturn(detail);

        HerbImageVO result = herbImageService.getById(1L);

        assertThat(result.getImageCode()).isEqualTo("IMG_1");
        assertThat(result.getSpeciesName()).isEqualTo("Huanglian");
    }

    @Test
    void deleteImageUsesLogicalDelete() {
        HerbImageEntity existing = imageEntity();
        when(herbImageMapper.selectActiveById(1L)).thenReturn(existing);
        when(herbImageMapper.logicalDeleteById(any(HerbImageEntity.class))).thenReturn(1);

        herbImageService.delete(1L);

        ArgumentCaptor<HerbImageEntity> captor = ArgumentCaptor.forClass(HerbImageEntity.class);
        verify(herbImageMapper).logicalDeleteById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getUpdateTime()).isNotNull();
    }

    @Test
    void updateImageValidatesSpeciesWhenProvided() {
        HerbImageEntity existing = imageEntity();
        HerbImageUpdateRequest request = new HerbImageUpdateRequest();
        request.setSpeciesId(2L);
        request.setProcessStatus("recognized");
        when(herbImageMapper.selectActiveById(1L)).thenReturn(existing);
        when(herbSpeciesMapper.selectActiveById(2L)).thenReturn(null);

        assertThatThrownBy(() -> herbImageService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Herb species not found");
    }

    @Test
    void updateImageUpdatesBusinessFields() {
        HerbImageEntity existing = imageEntity();
        HerbImageUpdateRequest request = new HerbImageUpdateRequest();
        request.setSpeciesId(2L);
        request.setCollectPlace("New place");
        request.setProcessStatus("recognized");
        HerbEntity species = new HerbEntity();
        species.setId(2L);
        species.setHerbName("Dangshen");
        when(herbImageMapper.selectActiveById(1L)).thenReturn(existing);
        when(herbSpeciesMapper.selectActiveById(2L)).thenReturn(species);

        HerbImageVO result = herbImageService.update(1L, request);

        ArgumentCaptor<HerbImageEntity> captor = ArgumentCaptor.forClass(HerbImageEntity.class);
        verify(herbImageMapper).updateImage(captor.capture());
        assertThat(captor.getValue().getSpeciesId()).isEqualTo(2L);
        assertThat(captor.getValue().getCollectPlace()).isEqualTo("New place");
        assertThat(captor.getValue().getProcessStatus()).isEqualTo("recognized");
        assertThat(result.getSpeciesName()).isEqualTo("Dangshen");
    }

    @Test
    void pageImageNormalizesInvalidPageParameters() {
        HerbImageQueryRequest request = new HerbImageQueryRequest();
        request.setPageNum(0);
        request.setPageSize(0);
        when(herbImageMapper.countPage(request)).thenReturn(1L);
        when(herbImageMapper.selectPage(request, 0L, 10)).thenReturn(List.of(new HerbImageVO()));

        PageResult<HerbImageVO> result = herbImageService.page(request);

        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    void myImagesRequiresCollectorIdAndPaginates() {
        when(herbImageMapper.countByCollectorId(9L)).thenReturn(1L);
        when(herbImageMapper.selectByCollectorId(9L, 0L, 10)).thenReturn(List.of(new HerbImageVO()));

        PageResult<HerbImageVO> result = herbImageService.my(9L, 0, 0);

        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    private HerbImageEntity imageEntity() {
        HerbImageEntity image = new HerbImageEntity();
        image.setId(1L);
        image.setImageCode("IMG_1");
        image.setImageUrl("/herb/image/2026/07/08/IMG_1.jpg");
        image.setImageName("leaf.jpg");
        image.setSpeciesId(1L);
        image.setUploadSource("mobile");
        image.setCollectTime(LocalDateTime.now());
        image.setProcessStatus("uploaded");
        image.setDeleted(0);
        return image;
    }
}
