package com.bdis.modules.herb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.herb.dto.HerbSpeciesCreateRequest;
import com.bdis.modules.herb.dto.HerbSpeciesQueryRequest;
import com.bdis.modules.herb.dto.HerbSpeciesUpdateRequest;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
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

    private HerbSpeciesService herbSpeciesService;

    @BeforeEach
    void setUp() {
        herbSpeciesService = new HerbSpeciesServiceImpl(herbSpeciesMapper);
    }

    @Test
    void createSpeciesSetsDefaultStatusAndReturnsCreatedObject() {
        HerbSpeciesCreateRequest request = new HerbSpeciesCreateRequest();
        request.setHerbCode("HERB_HUANGLIAN");
        request.setHerbName("Huanglian");

        when(herbSpeciesMapper.selectByHerbCode("HERB_HUANGLIAN")).thenReturn(null);

        HerbSpeciesVO result = herbSpeciesService.create(request);

        ArgumentCaptor<HerbEntity> captor = ArgumentCaptor.forClass(HerbEntity.class);
        verify(herbSpeciesMapper).insertSpecies(captor.capture());
        HerbEntity inserted = captor.getValue();
        assertThat(inserted.getStatus()).isEqualTo(1);
        assertThat(inserted.getDeleted()).isZero();
        assertThat(inserted.getCreateTime()).isNotNull();
        assertThat(inserted.getUpdateTime()).isNotNull();
        assertThat(result.getHerbCode()).isEqualTo("HERB_HUANGLIAN");
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
    void deleteSpeciesUsesLogicalDelete() {
        HerbEntity existing = new HerbEntity();
        existing.setId(1L);
        existing.setHerbCode("HERB_HUANGLIAN");
        existing.setHerbName("Huanglian");
        when(herbSpeciesMapper.selectActiveById(1L)).thenReturn(existing);
        when(herbSpeciesMapper.logicalDeleteById(any(HerbEntity.class))).thenReturn(1);

        herbSpeciesService.delete(1L);

        ArgumentCaptor<HerbEntity> captor = ArgumentCaptor.forClass(HerbEntity.class);
        verify(herbSpeciesMapper).logicalDeleteById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getUpdateTime()).isNotNull();
    }

    @Test
    void pageSpeciesNormalizesInvalidPageParameters() {
        HerbSpeciesQueryRequest request = new HerbSpeciesQueryRequest();
        request.setPageNum(0);
        request.setPageSize(0);
        when(herbSpeciesMapper.countPage(request)).thenReturn(1L);
        when(herbSpeciesMapper.selectPage(request, 0L, 10)).thenReturn(List.of(new HerbEntity()));

        PageResult<HerbSpeciesVO> result = herbSpeciesService.page(request);

        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
    }
}
