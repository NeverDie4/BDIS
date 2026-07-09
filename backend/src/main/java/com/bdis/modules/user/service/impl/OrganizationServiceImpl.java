package com.bdis.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.core.PageResult;
import com.bdis.common.exception.DuplicateResourceException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.user.dto.OrganizationDTO;
import com.bdis.modules.user.entity.DepartmentEntity;
import com.bdis.modules.user.entity.OrganizationEntity;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.DepartmentMapper;
import com.bdis.modules.user.mapper.OrganizationMapper;
import com.bdis.modules.user.mapper.UserMapper;
import com.bdis.modules.user.query.OrganizationQuery;
import com.bdis.modules.user.service.OrganizationService;
import com.bdis.modules.user.vo.OrganizationVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationMapper organizationMapper;

    private final DepartmentMapper departmentMapper;

    private final UserMapper userMapper;

    public OrganizationServiceImpl(
            OrganizationMapper organizationMapper,
            DepartmentMapper departmentMapper,
            UserMapper userMapper) {
        this.organizationMapper = organizationMapper;
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<OrganizationVO> page(OrganizationQuery query) {
        LambdaQueryWrapper<OrganizationEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(
                    condition ->
                            condition
                                    .like(OrganizationEntity::getOrganizationNo, query.getKeyword())
                                    .or()
                                    .like(
                                            OrganizationEntity::getOrganizationName,
                                            query.getKeyword()));
        }
        if (query.getStatus() != null) {
            wrapper.eq(OrganizationEntity::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(OrganizationEntity::getId);
        Page<OrganizationEntity> page =
                organizationMapper.selectPage(Page.of(query.getPage(), query.getSize()), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(), page);
    }

    @Override
    public OrganizationVO detail(Long id) {
        return toVO(requireOrganization(id));
    }

    @Override
    public Long create(OrganizationDTO dto) {
        ensureNoAvailable(dto.getOrganizationNo());
        OrganizationEntity entity = new OrganizationEntity();
        apply(entity, dto);
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setCreatedBy(SecurityUtils.currentUser().getUserId());
        organizationMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, OrganizationDTO dto) {
        OrganizationEntity entity = requireOrganization(id);
        apply(entity, dto);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        organizationMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        requireOrganization(id);
        Long departmentCount =
                departmentMapper.selectCount(
                        new LambdaQueryWrapper<DepartmentEntity>()
                                .eq(DepartmentEntity::getOrganizationId, id));
        if (departmentCount > 0) {
            throw new DuplicateResourceException("机构下存在部门，不能删除");
        }
        Long userCount =
                userMapper.selectCount(
                        new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getOrganizationId, id));
        if (userCount > 0) {
            throw new DuplicateResourceException("机构下存在用户，不能删除");
        }
        organizationMapper.deleteById(id);
    }

    private void apply(OrganizationEntity entity, OrganizationDTO dto) {
        entity.setOrganizationNo(dto.getOrganizationNo());
        entity.setOrganizationName(dto.getOrganizationName());
        entity.setOrganizationType(dto.getOrganizationType());
        entity.setContactName(dto.getContactName());
        entity.setContactPhone(dto.getContactPhone());
        entity.setAddress(dto.getAddress());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private OrganizationEntity requireOrganization(Long id) {
        OrganizationEntity entity = organizationMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("机构不存在");
        }
        return entity;
    }

    private void ensureNoAvailable(String organizationNo) {
        OrganizationEntity existed =
                organizationMapper.selectOne(
                        new LambdaQueryWrapper<OrganizationEntity>()
                                .eq(OrganizationEntity::getOrganizationNo, organizationNo));
        if (existed != null) {
            throw new DuplicateResourceException("机构编号已存在");
        }
    }

    private OrganizationVO toVO(OrganizationEntity entity) {
        OrganizationVO vo = new OrganizationVO();
        vo.setId(entity.getId());
        vo.setOrganizationNo(entity.getOrganizationNo());
        vo.setOrganizationName(entity.getOrganizationName());
        vo.setOrganizationType(entity.getOrganizationType());
        vo.setContactName(entity.getContactName());
        vo.setContactPhone(entity.getContactPhone());
        vo.setAddress(entity.getAddress());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
