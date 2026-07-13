package com.bdis.modules.herb.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.modules.collection.support.CollectionAccessScope;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import com.bdis.modules.growth.mapper.GrowthRecordMapper;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbImageAccessServiceTest {

    @Mock private CollectionAccessService collectionAccessService;

    @Mock private HerbImageMapper herbImageMapper;

    @Mock private GrowthRecordMapper growthRecordMapper;

    private HerbImageAccessService herbImageAccessService;

    @BeforeEach
    void setUp() {
        herbImageAccessService =
                new HerbImageAccessService(
                        collectionAccessService, herbImageMapper, growthRecordMapper);
    }

    @Test
    void allowsImageUploader() {
        HerbImageEntity image = image(9L, null);
        when(collectionAccessService.currentScope())
                .thenReturn(new CollectionAccessScope(false, List.of(9L)));

        herbImageAccessService.requireAccess(image);
    }

    @Test
    void allowsLinkedGrowthRecordCollector() {
        HerbImageEntity image = image(20L, 30L);
        GrowthRecordEntity growthRecord = new GrowthRecordEntity();
        growthRecord.setId(30L);
        growthRecord.setCollectorId(9L);
        growthRecord.setIsDeleted(0);
        when(growthRecordMapper.selectById(30L)).thenReturn(growthRecord);
        when(collectionAccessService.currentScope())
                .thenReturn(new CollectionAccessScope(false, List.of(9L)));

        herbImageAccessService.requireAccess(image);
    }

    @Test
    void rejectsImageOutsideDataScope() {
        HerbImageEntity image = image(20L, null);
        when(collectionAccessService.currentScope())
                .thenReturn(new CollectionAccessScope(false, List.of(9L)));

        assertThatThrownBy(() -> herbImageAccessService.requireAccess(image))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("图片超出当前数据范围");
    }

    @Test
    void allowsAllDataScope() {
        when(collectionAccessService.currentScope())
                .thenReturn(new CollectionAccessScope(true, List.of(9L)));

        herbImageAccessService.requireAccess(image(20L, null));
    }

    private HerbImageEntity image(Long uploaderId, Long growthRecordId) {
        HerbImageEntity image = new HerbImageEntity();
        image.setId(1L);
        image.setUploaderId(uploaderId);
        image.setGrowthRecordId(growthRecordId);
        return image;
    }
}
