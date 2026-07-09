package com.bdis.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.exception.DuplicateResourceException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.user.dto.DepartmentDTO;
import com.bdis.modules.user.entity.DepartmentEntity;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.DepartmentMapper;
import com.bdis.modules.user.mapper.UserMapper;
import com.bdis.modules.user.query.DepartmentQuery;
import com.bdis.modules.user.service.DepartmentService;
import com.bdis.modules.user.vo.DepartmentVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    private final UserMapper userMapper;

    public DepartmentServiceImpl(DepartmentMapper departmentMapper, UserMapper userMapper) {
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<DepartmentVO> tree(DepartmentQuery query) {
        LambdaQueryWrapper<DepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(
                    condition ->
                            condition
                                    .like(DepartmentEntity::getDepartmentNo, query.getKeyword())
                                    .or()
                                    .like(DepartmentEntity::getDepartmentName, query.getKeyword()));
        }
        if (query.getOrganizationId() != null) {
            wrapper.eq(DepartmentEntity::getOrganizationId, query.getOrganizationId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(DepartmentEntity::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(DepartmentEntity::getSortOrder).orderByDesc(DepartmentEntity::getId);
        return buildTree(departmentMapper.selectList(wrapper));
    }

    @Override
    public DepartmentVO detail(Long id) {
        return toVO(requireDepartment(id));
    }

    @Override
    public Long create(DepartmentDTO dto) {
        ensureNoAvailable(dto.getDepartmentNo());
        DepartmentEntity entity = new DepartmentEntity();
        apply(entity, dto);
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setCreatedBy(SecurityUtils.currentUser().getUserId());
        departmentMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, DepartmentDTO dto) {
        DepartmentEntity entity = requireDepartment(id);
        apply(entity, dto);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        departmentMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        requireDepartment(id);
        Long childCount =
                departmentMapper.selectCount(
                        new LambdaQueryWrapper<DepartmentEntity>()
                                .eq(DepartmentEntity::getParentId, id));
        if (childCount > 0) {
            throw new DuplicateResourceException("部门存在子部门，不能删除");
        }
        Long userCount =
                userMapper.selectCount(
                        new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getDepartmentId, id));
        if (userCount > 0) {
            throw new DuplicateResourceException("部门下存在用户，不能删除");
        }
        departmentMapper.deleteById(id);
    }

    private List<DepartmentVO> buildTree(List<DepartmentEntity> entities) {
        Map<Long, DepartmentVO> byId = new LinkedHashMap<>();
        for (DepartmentEntity entity : entities) {
            byId.put(entity.getId(), toVO(entity));
        }
        List<DepartmentVO> roots = new ArrayList<>();
        for (DepartmentVO item : byId.values()) {
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

    private void apply(DepartmentEntity entity, DepartmentDTO dto) {
        entity.setDepartmentNo(dto.getDepartmentNo());
        entity.setDepartmentName(dto.getDepartmentName());
        entity.setOrganizationId(dto.getOrganizationId());
        entity.setParentId(dto.getParentId() == null ? 0 : dto.getParentId());
        entity.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private DepartmentEntity requireDepartment(Long id) {
        DepartmentEntity entity = departmentMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("部门不存在");
        }
        return entity;
    }

    private void ensureNoAvailable(String departmentNo) {
        DepartmentEntity existed =
                departmentMapper.selectOne(
                        new LambdaQueryWrapper<DepartmentEntity>()
                                .eq(DepartmentEntity::getDepartmentNo, departmentNo));
        if (existed != null) {
            throw new DuplicateResourceException("部门编号已存在");
        }
    }

    private DepartmentVO toVO(DepartmentEntity entity) {
        DepartmentVO vo = new DepartmentVO();
        vo.setId(entity.getId());
        vo.setDepartmentNo(entity.getDepartmentNo());
        vo.setDepartmentName(entity.getDepartmentName());
        vo.setOrganizationId(entity.getOrganizationId());
        vo.setParentId(entity.getParentId());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
