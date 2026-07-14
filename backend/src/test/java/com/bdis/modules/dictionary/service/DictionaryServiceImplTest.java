package com.bdis.modules.dictionary.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.dictionary.dto.DictItemRequest;
import com.bdis.modules.dictionary.entity.DictItemEntity;
import com.bdis.modules.dictionary.entity.DictTypeEntity;
import com.bdis.modules.dictionary.mapper.DictItemMapper;
import com.bdis.modules.dictionary.mapper.DictTypeMapper;
import com.bdis.modules.dictionary.mapper.RegionMapper;
import com.bdis.modules.dictionary.service.impl.DictionaryServiceImpl;
import com.bdis.modules.herb.mapper.HerbMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceImplTest {

    @Mock private DictTypeMapper dictTypeMapper;
    @Mock private DictItemMapper dictItemMapper;
    @Mock private RegionMapper regionMapper;
    @Mock private HerbMapper herbMapper;

    @Test
    void shouldRejectDeletingCategoryReferencedByHerbSpecies() {
        DictTypeEntity type = new DictTypeEntity();
        type.setId(1L);
        type.setTypeCode("herb_category");
        DictItemEntity item = new DictItemEntity();
        item.setId(2L);
        item.setTypeId(1L);
        item.setItemCode("HERB_CAT_QINGRE");

        when(dictTypeMapper.selectOne(any())).thenReturn(type);
        when(dictItemMapper.selectById(2L)).thenReturn(item);
        when(dictItemMapper.selectCount(any())).thenReturn(0L);
        when(herbMapper.countByCategoryReference(2L, "HERB_CAT_QINGRE")).thenReturn(1L);

        DictionaryServiceImpl service =
                new DictionaryServiceImpl(dictTypeMapper, dictItemMapper, regionMapper, herbMapper);

        assertThrows(BusinessException.class, () -> service.deleteItem("herb_category", 2L));
        verify(dictItemMapper, never()).deleteById(2L);
    }

    @Test
    void shouldRejectChangingCategoryCodeReferencedByHerbSpecies() {
        DictTypeEntity type = new DictTypeEntity();
        type.setId(1L);
        type.setTypeCode("herb_category");
        DictItemEntity item = new DictItemEntity();
        item.setId(2L);
        item.setTypeId(1L);
        item.setItemCode("HERB_CAT_QINGRE");

        DictItemRequest request = new DictItemRequest();
        request.setItemCode("HERB_CAT_QINGRE_NEW");
        request.setItemName("Qingre");

        when(dictTypeMapper.selectOne(any())).thenReturn(type);
        when(dictItemMapper.selectById(2L)).thenReturn(item);
        when(dictItemMapper.selectOne(any())).thenReturn(null);
        when(herbMapper.countByCategoryReference(2L, "HERB_CAT_QINGRE")).thenReturn(1L);

        DictionaryServiceImpl service =
                new DictionaryServiceImpl(dictTypeMapper, dictItemMapper, regionMapper, herbMapper);

        CurrentUser currentUser = mock(CurrentUser.class);
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUser).thenReturn(currentUser);
            assertThrows(
                    BusinessException.class,
                    () -> service.updateItem("herb_category", 2L, request));
        }
        verify(dictItemMapper, never()).updateById(any(DictItemEntity.class));
    }
}
