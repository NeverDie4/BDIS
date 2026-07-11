package com.bdis.modules.dictionary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.DuplicateResourceException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.dictionary.dto.DictItemRequest;
import com.bdis.modules.dictionary.dto.DictTypeRequest;
import com.bdis.modules.dictionary.dto.RegionRequest;
import com.bdis.modules.dictionary.entity.DictItemEntity;
import com.bdis.modules.dictionary.entity.DictTypeEntity;
import com.bdis.modules.dictionary.entity.RegionEntity;
import com.bdis.modules.dictionary.mapper.DictItemMapper;
import com.bdis.modules.dictionary.mapper.DictTypeMapper;
import com.bdis.modules.dictionary.mapper.RegionMapper;
import com.bdis.modules.dictionary.service.DictionaryService;
import com.bdis.modules.dictionary.vo.DictItemVO;
import com.bdis.modules.dictionary.vo.DictTypeVO;
import com.bdis.modules.dictionary.vo.RegionVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DictionaryServiceImpl implements DictionaryService {

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;
    private final RegionMapper regionMapper;

    public DictionaryServiceImpl(
            DictTypeMapper dictTypeMapper,
            DictItemMapper dictItemMapper,
            RegionMapper regionMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
        this.regionMapper = regionMapper;
    }

    @Override
    public PageResult<DictTypeVO> pageTypes(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<DictTypeEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(
                    item ->
                            item.like(DictTypeEntity::getTypeCode, keyword)
                                    .or()
                                    .like(DictTypeEntity::getTypeName, keyword));
        }
        wrapper.eq(status != null, DictTypeEntity::getStatus, status)
                .orderByAsc(DictTypeEntity::getSortOrder)
                .orderByAsc(DictTypeEntity::getId);
        Page<DictTypeEntity> result = dictTypeMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords().stream().map(this::toTypeVO).toList(), result);
    }

    @Override
    @Transactional
    public Long createType(DictTypeRequest request) {
        ensureTypeCodeAvailable(request.getTypeCode(), null);
        DictTypeEntity entity = new DictTypeEntity();
        applyType(entity, request);
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setCreatedBy(SecurityUtils.currentUser().getUserId());
        dictTypeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateType(Long typeId, DictTypeRequest request) {
        DictTypeEntity entity = requireType(typeId);
        ensureTypeCodeAvailable(request.getTypeCode(), typeId);
        applyType(entity, request);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        dictTypeMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void deleteType(Long typeId) {
        requireType(typeId);
        if (dictItemMapper.selectCount(
                        new LambdaQueryWrapper<DictItemEntity>()
                                .eq(DictItemEntity::getTypeId, typeId))
                > 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "字典类型下仍有字典项，不能删除");
        }
        dictTypeMapper.deleteById(typeId);
    }

    @Override
    public List<DictItemVO> items(String typeCode, Integer status) {
        DictTypeEntity type = requireType(typeCode);
        if (Integer.valueOf(1).equals(status) && !Integer.valueOf(1).equals(type.getStatus())) {
            return List.of();
        }
        List<DictItemEntity> entities =
                dictItemMapper.selectList(
                        new LambdaQueryWrapper<DictItemEntity>()
                                .eq(DictItemEntity::getTypeId, type.getId())
                                .eq(status != null, DictItemEntity::getStatus, status)
                                .orderByAsc(DictItemEntity::getSortOrder)
                                .orderByAsc(DictItemEntity::getId));
        return buildItemTree(entities);
    }

    @Override
    @Transactional
    public Long createItem(String typeCode, DictItemRequest request) {
        DictTypeEntity type = requireType(typeCode);
        ensureItemCodeAvailable(type.getId(), request.getItemCode(), null);
        validateItemParent(type.getId(), request.getParentId(), null);
        DictItemEntity entity = new DictItemEntity();
        entity.setTypeId(type.getId());
        applyItem(entity, request);
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setCreatedBy(SecurityUtils.currentUser().getUserId());
        dictItemMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateItem(String typeCode, Long itemId, DictItemRequest request) {
        DictTypeEntity type = requireType(typeCode);
        DictItemEntity entity = requireItem(type.getId(), itemId);
        ensureItemCodeAvailable(type.getId(), request.getItemCode(), itemId);
        validateItemParent(type.getId(), request.getParentId(), itemId);
        applyItem(entity, request);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        dictItemMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void deleteItem(String typeCode, Long itemId) {
        DictTypeEntity type = requireType(typeCode);
        requireItem(type.getId(), itemId);
        if (dictItemMapper.selectCount(
                        new LambdaQueryWrapper<DictItemEntity>()
                                .eq(DictItemEntity::getParentId, itemId))
                > 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "字典项仍有子项，不能删除");
        }
        dictItemMapper.deleteById(itemId);
    }

    @Override
    public List<RegionVO> regionTree(Integer status) {
        List<RegionEntity> regions =
                regionMapper.selectList(
                        new LambdaQueryWrapper<RegionEntity>()
                                .eq(status != null, RegionEntity::getStatus, status)
                                .orderByAsc(RegionEntity::getSortOrder)
                                .orderByAsc(RegionEntity::getId));
        Map<Long, RegionVO> byId = new LinkedHashMap<>();
        regions.forEach(region -> byId.put(region.getId(), toRegionVO(region)));
        List<RegionVO> roots = new ArrayList<>();
        for (RegionVO region : byId.values()) {
            if (region.getParentId() == null
                    || region.getParentId() == 0
                    || !byId.containsKey(region.getParentId())) {
                roots.add(region);
            } else {
                byId.get(region.getParentId()).getChildren().add(region);
            }
        }
        return roots;
    }

    @Override
    @Transactional
    public Long createRegion(RegionRequest request) {
        ensureRegionCodeAvailable(request.getRegionCode(), null);
        validateRegionParent(request.getParentId(), null);
        RegionEntity entity = new RegionEntity();
        applyRegion(entity, request);
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setCreatedBy(SecurityUtils.currentUser().getUserId());
        regionMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateRegion(Long regionId, RegionRequest request) {
        RegionEntity entity = requireRegion(regionId);
        ensureRegionCodeAvailable(request.getRegionCode(), regionId);
        validateRegionParent(request.getParentId(), regionId);
        applyRegion(entity, request);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        regionMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void deleteRegion(Long regionId) {
        requireRegion(regionId);
        if (regionMapper.selectCount(
                        new LambdaQueryWrapper<RegionEntity>()
                                .eq(RegionEntity::getParentId, regionId))
                > 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "区域仍有下级节点，不能删除");
        }
        regionMapper.deleteById(regionId);
    }

    private void applyType(DictTypeEntity entity, DictTypeRequest request) {
        entity.setTypeCode(request.getTypeCode());
        entity.setTypeName(request.getTypeName());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setRemark(request.getRemark());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
    }

    private void applyItem(DictItemEntity entity, DictItemRequest request) {
        entity.setItemCode(request.getItemCode());
        entity.setItemName(request.getItemName());
        entity.setItemValue(request.getItemValue());
        entity.setParentId(request.getParentId() == null ? 0 : request.getParentId());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setRemark(request.getRemark());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
    }

    private void applyRegion(RegionEntity entity, RegionRequest request) {
        entity.setRegionCode(request.getRegionCode());
        entity.setRegionName(request.getRegionName());
        entity.setParentId(request.getParentId() == null ? 0 : request.getParentId());
        entity.setRegionLevel(request.getRegionLevel());
        entity.setLongitude(request.getLongitude());
        entity.setLatitude(request.getLatitude());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setRemark(request.getRemark());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
    }

    private DictTypeEntity requireType(Long typeId) {
        DictTypeEntity entity = dictTypeMapper.selectById(typeId);
        if (entity == null) {
            throw new ResourceNotFoundException("字典类型不存在");
        }
        return entity;
    }

    private DictTypeEntity requireType(String typeCode) {
        DictTypeEntity entity =
                dictTypeMapper.selectOne(
                        new LambdaQueryWrapper<DictTypeEntity>()
                                .eq(DictTypeEntity::getTypeCode, typeCode));
        if (entity == null) {
            throw new ResourceNotFoundException("字典类型不存在");
        }
        return entity;
    }

    private DictItemEntity requireItem(Long typeId, Long itemId) {
        DictItemEntity entity = dictItemMapper.selectById(itemId);
        if (entity == null || !typeId.equals(entity.getTypeId())) {
            throw new ResourceNotFoundException("字典项不存在");
        }
        return entity;
    }

    private RegionEntity requireRegion(Long regionId) {
        RegionEntity entity = regionMapper.selectById(regionId);
        if (entity == null) {
            throw new ResourceNotFoundException("区域不存在");
        }
        return entity;
    }

    private void ensureTypeCodeAvailable(String code, Long excludeId) {
        DictTypeEntity existing =
                dictTypeMapper.selectOne(
                        new LambdaQueryWrapper<DictTypeEntity>()
                                .eq(DictTypeEntity::getTypeCode, code));
        if (existing != null && (excludeId == null || !excludeId.equals(existing.getId()))) {
            throw new DuplicateResourceException("字典类型编码已存在");
        }
    }

    private void ensureItemCodeAvailable(Long typeId, String code, Long excludeId) {
        DictItemEntity existing =
                dictItemMapper.selectOne(
                        new LambdaQueryWrapper<DictItemEntity>()
                                .eq(DictItemEntity::getTypeId, typeId)
                                .eq(DictItemEntity::getItemCode, code));
        if (existing != null && (excludeId == null || !excludeId.equals(existing.getId()))) {
            throw new DuplicateResourceException("字典项编码已存在");
        }
    }

    private void ensureRegionCodeAvailable(String code, Long excludeId) {
        RegionEntity existing =
                regionMapper.selectOne(
                        new LambdaQueryWrapper<RegionEntity>()
                                .eq(RegionEntity::getRegionCode, code));
        if (existing != null && (excludeId == null || !excludeId.equals(existing.getId()))) {
            throw new DuplicateResourceException("区域编码已存在");
        }
    }

    private void validateItemParent(Long typeId, Long parentId, Long currentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (parentId.equals(currentId)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "字典项不能以自身为父级");
        }
        requireItem(typeId, parentId);
    }

    private void validateRegionParent(Long parentId, Long currentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (parentId.equals(currentId)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "区域不能以自身为父级");
        }
        requireRegion(parentId);
    }

    private List<DictItemVO> buildItemTree(List<DictItemEntity> entities) {
        Map<Long, DictItemVO> byId = new LinkedHashMap<>();
        entities.forEach(item -> byId.put(item.getId(), toItemVO(item)));
        List<DictItemVO> roots = new ArrayList<>();
        for (DictItemVO item : byId.values()) {
            if (item.getParentId() == null
                    || item.getParentId() == 0
                    || !byId.containsKey(item.getParentId())) {
                roots.add(item);
            } else {
                byId.get(item.getParentId()).getChildren().add(item);
            }
        }
        return roots;
    }

    private DictTypeVO toTypeVO(DictTypeEntity entity) {
        DictTypeVO vo = new DictTypeVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private DictItemVO toItemVO(DictItemEntity entity) {
        DictItemVO vo = new DictItemVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private RegionVO toRegionVO(RegionEntity entity) {
        RegionVO vo = new RegionVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
