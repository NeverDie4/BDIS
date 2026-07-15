package com.bdis.modules.herb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.dictionary.support.DictionaryReferenceValidator;
import com.bdis.modules.herb.dto.HerbSpeciesCreateRequest;
import com.bdis.modules.herb.dto.HerbSpeciesQueryRequest;
import com.bdis.modules.herb.dto.HerbSpeciesUpdateRequest;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.herb.service.HerbSpeciesCoverFileService;
import com.bdis.modules.herb.service.impl.HerbSpeciesServiceImpl;
import com.bdis.modules.herb.vo.HerbSpeciesVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbSpeciesServiceTest {

    @Mock private HerbSpeciesMapper herbSpeciesMapper;

    @Mock private DictionaryReferenceValidator dictionaryReferenceValidator;

    @Mock private HerbSpeciesCoverFileService coverFileService;

    private HerbSpeciesService herbSpeciesService;

    @BeforeEach
    void setUp() {
        herbSpeciesService =
                new HerbSpeciesServiceImpl(
                        herbSpeciesMapper, dictionaryReferenceValidator, coverFileService);
    }

    @Test
    void createSpeciesSetsDefaultStatusAndReturnsCreatedObject() {
        HerbSpeciesCreateRequest request = new HerbSpeciesCreateRequest();
        request.setHerbCode("HERB_HUANGLIAN");
        request.setHerbName("Huanglian");
        request.setCoverImageUrl("/api/files/10/content");

        when(herbSpeciesMapper.selectByHerbCode("HERB_HUANGLIAN")).thenReturn(null);
        doAnswer(
                        invocation -> {
                            HerbEntity entity = invocation.getArgument(0);
                            entity.setId(6L);
                            return 1;
                        })
                .when(herbSpeciesMapper)
                .insertSpecies(any(HerbEntity.class));
        when(coverFileService.replaceCover(6L, null, "/api/files/10/content"))
                .thenReturn("/api/public-files/10/content");

        HerbSpeciesVO result = herbSpeciesService.create(request);

        ArgumentCaptor<HerbEntity> insertCaptor = ArgumentCaptor.forClass(HerbEntity.class);
        verify(herbSpeciesMapper).insertSpecies(insertCaptor.capture());
        HerbEntity inserted = insertCaptor.getValue();
        assertThat(inserted.getStatus()).isEqualTo(1);
        assertThat(inserted.getIsDeleted()).isZero();
        assertThat(inserted.getCoverImageUrl()).isEqualTo("/api/public-files/10/content");
        assertThat(inserted.getCreatedAt()).isNotNull();
        assertThat(inserted.getUpdatedAt()).isNotNull();
        ArgumentCaptor<HerbEntity> updateCaptor = ArgumentCaptor.forClass(HerbEntity.class);
        verify(herbSpeciesMapper).updateSpecies(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getCoverImageUrl())
                .isEqualTo("/api/public-files/10/content");
        assertThat(result.getHerbCode()).isEqualTo("HERB_HUANGLIAN");
        assertThat(result.getCoverImageUrl()).isEqualTo("/api/public-files/10/content");
    }

    @Test
    void createSpeciesRejectsDuplicateHerbCode() {
        HerbSpeciesCreateRequest request = new HerbSpeciesCreateRequest();
        request.setHerbCode("HERB_HUANGLIAN");
        request.setHerbName("Huanglian");
        when(herbSpeciesMapper.selectByHerbCode("HERB_HUANGLIAN")).thenReturn(new HerbEntity());

        assertThatThrownBy(() -> herbSpeciesService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Herb code already exists");
    }

    @Test
    void updateSpeciesRejectsDeletedOrMissingData() {
        HerbSpeciesUpdateRequest request = new HerbSpeciesUpdateRequest();
        request.setHerbName("Updated");
        when(herbSpeciesMapper.selectActiveById(1L)).thenReturn(null);

        assertThatThrownBy(() -> herbSpeciesService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Herb species not found");
    }

    @Test
    void updateSpeciesPreservesStatusWhenRequestOmitsIt() {
        HerbEntity existing = new HerbEntity();
        existing.setId(1L);
        existing.setHerbNo("HERB_HUANGLIAN");
        existing.setHerbName("Huanglian");
        existing.setStatus(1);
        when(herbSpeciesMapper.selectActiveById(1L)).thenReturn(existing);

        HerbSpeciesUpdateRequest request = new HerbSpeciesUpdateRequest();
        request.setHerbName("Updated Huanglian");
        request.setCoverImageUrl("/api/files/22/content");
        when(coverFileService.replaceCover(1L, null, "/api/files/22/content"))
                .thenReturn("/api/public-files/22/content");

        herbSpeciesService.update(1L, request);

        ArgumentCaptor<HerbEntity> captor = ArgumentCaptor.forClass(HerbEntity.class);
        verify(herbSpeciesMapper).updateSpecies(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getCoverImageUrl()).isEqualTo("/api/public-files/22/content");
    }

    @Test
    void deleteSpeciesUsesLogicalDelete() {
        HerbEntity existing = new HerbEntity();
        existing.setId(1L);
        existing.setHerbNo("HERB_HUANGLIAN");
        existing.setHerbName("Huanglian");
        when(herbSpeciesMapper.selectActiveById(1L)).thenReturn(existing);
        when(herbSpeciesMapper.logicalDeleteById(any(HerbEntity.class))).thenReturn(1);

        herbSpeciesService.delete(1L);

        ArgumentCaptor<HerbEntity> captor = ArgumentCaptor.forClass(HerbEntity.class);
        verify(herbSpeciesMapper).logicalDeleteById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void pageSpeciesReturnsDisplayFieldsFromDatabaseContract() {
        HerbSpeciesQueryRequest request = new HerbSpeciesQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        request.setMedicinalPart("根茎");

        HerbEntity entity = new HerbEntity();
        entity.setId(7L);
        entity.setHerbNo("HERB_HUANGLIAN");
        entity.setHerbName("黄连");
        entity.setAliasName("川连");
        entity.setCategoryCode("root");
        entity.setCategoryName("根及根茎类");
        entity.setMedicinalPart("根茎");
        entity.setCoverImageUrl("/api/files/7/content");
        entity.setStatus(1);
        entity.setDistributionRegionText("重庆南川、重庆石柱、重庆石柱");

        when(herbSpeciesMapper.countPage(request)).thenReturn(1L);
        when(herbSpeciesMapper.selectPage(request, 0L, 10)).thenReturn(List.of(entity));

        PageResult<HerbSpeciesVO> result = herbSpeciesService.page(request);

        HerbSpeciesVO vo = result.getRecords().getFirst();
        assertThat(vo.getHerbCode()).isEqualTo("HERB_HUANGLIAN");
        assertThat(vo.getCategory()).isEqualTo("root");
        assertThat(vo.getCategoryName()).isEqualTo("根及根茎类");
        assertThat(vo.getMedicinalPart()).isEqualTo("根茎");
        assertThat(vo.getStatus()).isEqualTo(1);
        assertThat(vo.getCoverImageUrl()).isEqualTo("/api/files/7/content");
        assertThat(vo.getStatusText()).isEqualTo("启用");
        assertThat(vo.getDistributionRegionText()).isEqualTo("重庆南川、重庆石柱、重庆石柱");
        assertThat(vo.getDistributionRegions()).containsExactly("重庆南川", "重庆石柱");
    }

    @Test
    void pageSpeciesNormalizesInvalidPageParameters() {
        HerbSpeciesQueryRequest request = new HerbSpeciesQueryRequest();
        request.setPageNum(0);
        request.setPageSize(0);
        when(herbSpeciesMapper.countPage(request)).thenReturn(1L);
        when(herbSpeciesMapper.selectPage(request, 0L, 10)).thenReturn(List.of(new HerbEntity()));

        PageResult<HerbSpeciesVO> result = herbSpeciesService.page(request);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
    }
}
