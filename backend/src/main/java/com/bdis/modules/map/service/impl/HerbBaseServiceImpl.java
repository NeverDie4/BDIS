package com.bdis.modules.map.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.DuplicateResourceException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.dictionary.entity.RegionEntity;
import com.bdis.modules.dictionary.mapper.RegionMapper;
import com.bdis.modules.map.dto.HerbBaseRequest;
import com.bdis.modules.map.entity.HerbBaseEntity;
import com.bdis.modules.map.mapper.HerbBaseMapper;
import com.bdis.modules.map.query.HerbBaseQuery;
import com.bdis.modules.map.service.HerbBaseService;
import com.bdis.modules.map.vo.HerbBaseVO;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HerbBaseServiceImpl implements HerbBaseService {

    private final HerbBaseMapper herbBaseMapper;
    private final RegionMapper regionMapper;
    private final JdbcTemplate jdbcTemplate;

    public HerbBaseServiceImpl(
            HerbBaseMapper herbBaseMapper, RegionMapper regionMapper, JdbcTemplate jdbcTemplate) {
        this.herbBaseMapper = herbBaseMapper;
        this.regionMapper = regionMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PageResult<HerbBaseVO> page(HerbBaseQuery query) {
        LambdaQueryWrapper<HerbBaseEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(
                    item ->
                            item.like(HerbBaseEntity::getBaseNo, query.getKeyword())
                                    .or()
                                    .like(HerbBaseEntity::getBaseName, query.getKeyword())
                                    .or()
                                    .like(HerbBaseEntity::getAddress, query.getKeyword()));
        }
        wrapper.eq(query.getStatus() != null, HerbBaseEntity::getStatus, query.getStatus())
                .eq(query.getRegionId() != null, HerbBaseEntity::getRegionId, query.getRegionId())
                .eq(
                        StringUtils.hasText(query.getBaseType()),
                        HerbBaseEntity::getBaseType,
                        query.getBaseType())
                .orderByDesc(HerbBaseEntity::getId);
        Page<HerbBaseEntity> result =
                herbBaseMapper.selectPage(Page.of(query.getPage(), query.getSize()), wrapper);
        return PageResult.of(result.getRecords().stream().map(this::toVO).toList(), result);
    }

    @Override
    public List<HerbBaseVO> listEnabled() {
        return herbBaseMapper
                .selectList(
                        new LambdaQueryWrapper<HerbBaseEntity>()
                                .eq(HerbBaseEntity::getStatus, 1)
                                .orderByAsc(HerbBaseEntity::getBaseName))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public HerbBaseVO detail(Long id) {
        return toVO(requireBase(id));
    }

    @Override
    @Transactional
    public Long create(HerbBaseRequest request) {
        ensureCodeAvailable(request.getBaseNo(), null);
        requireRegionIfPresent(request.getRegionId());
        HerbBaseEntity entity = new HerbBaseEntity();
        apply(entity, request);
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setCreatedBy(SecurityUtils.currentUser().getUserId());
        herbBaseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(Long id, HerbBaseRequest request) {
        HerbBaseEntity entity = requireBase(id);
        ensureCodeAvailable(request.getBaseNo(), id);
        requireRegionIfPresent(request.getRegionId());
        apply(entity, request);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        herbBaseMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireBase(id);
        Long referenceCount =
                jdbcTemplate.queryForObject(
                        """
                        select
                          (select count(*) from herb_distribution where base_id = ? and is_deleted = 0)
                          + (select count(*) from herb_collection_task where base_id = ? and is_deleted = 0)
                          + (select count(*) from herb_batch where base_id = ? and is_deleted = 0)
                          + (select count(*) from herb_image where base_id = ? and is_deleted = 0)
                        """,
                        Long.class,
                        id,
                        id,
                        id,
                        id);
        if (referenceCount != null && referenceCount > 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "基地已被点位、采集任务或采集数据引用，不能删除");
        }
        herbBaseMapper.deleteById(id);
    }

    private void apply(HerbBaseEntity entity, HerbBaseRequest request) {
        entity.setBaseNo(request.getBaseNo());
        entity.setBaseName(request.getBaseName());
        entity.setBaseType(request.getBaseType());
        entity.setRegionId(request.getRegionId());
        entity.setAddress(request.getAddress());
        entity.setLongitude(request.getLongitude());
        entity.setLatitude(request.getLatitude());
        entity.setContactName(request.getContactName());
        entity.setContactPhone(request.getContactPhone());
        entity.setDescription(request.getDescription());
        entity.setRemark(request.getRemark());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
    }

    private HerbBaseEntity requireBase(Long id) {
        HerbBaseEntity entity = herbBaseMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("药材基地不存在");
        }
        return entity;
    }

    private RegionEntity requireRegionIfPresent(Long regionId) {
        if (regionId == null) {
            return null;
        }
        RegionEntity entity = regionMapper.selectById(regionId);
        if (entity == null) {
            throw new ResourceNotFoundException("区域不存在");
        }
        return entity;
    }

    private void ensureCodeAvailable(String code, Long excludeId) {
        HerbBaseEntity existing =
                herbBaseMapper.selectOne(
                        new LambdaQueryWrapper<HerbBaseEntity>()
                                .eq(HerbBaseEntity::getBaseNo, code));
        if (existing != null && (excludeId == null || !excludeId.equals(existing.getId()))) {
            throw new DuplicateResourceException("基地编号已存在");
        }
    }

    private HerbBaseVO toVO(HerbBaseEntity entity) {
        HerbBaseVO vo = new HerbBaseVO();
        BeanUtils.copyProperties(entity, vo);
        if (entity.getRegionId() != null) {
            RegionEntity region = regionMapper.selectById(entity.getRegionId());
            vo.setRegionName(region == null ? null : region.getRegionName());
        }
        return vo;
    }
}
