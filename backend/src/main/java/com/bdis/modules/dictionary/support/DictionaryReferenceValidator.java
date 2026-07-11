package com.bdis.modules.dictionary.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.dictionary.entity.DictItemEntity;
import com.bdis.modules.dictionary.entity.DictTypeEntity;
import com.bdis.modules.dictionary.mapper.DictItemMapper;
import com.bdis.modules.dictionary.mapper.DictTypeMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DictionaryReferenceValidator {

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;

    public DictionaryReferenceValidator(
            DictTypeMapper dictTypeMapper, DictItemMapper dictItemMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
    }

    public void validateIfConfigured(String typeCode, String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        DictTypeEntity type =
                dictTypeMapper.selectOne(
                        new LambdaQueryWrapper<DictTypeEntity>()
                                .eq(DictTypeEntity::getTypeCode, typeCode)
                                .eq(DictTypeEntity::getStatus, 1));
        if (type == null) {
            return;
        }
        Long count =
                dictItemMapper.selectCount(
                        new LambdaQueryWrapper<DictItemEntity>()
                                .eq(DictItemEntity::getTypeId, type.getId())
                                .eq(DictItemEntity::getStatus, 1)
                                .and(
                                        item ->
                                                item.eq(DictItemEntity::getItemCode, value)
                                                        .or()
                                                        .eq(DictItemEntity::getItemValue, value)));
        if (count == null || count == 0) {
            throw new BusinessException(
                    ResultCodeEnum.VALIDATION_ERROR,
                    fieldName + "不在已启用字典 " + typeCode + " 中");
        }
    }
}
