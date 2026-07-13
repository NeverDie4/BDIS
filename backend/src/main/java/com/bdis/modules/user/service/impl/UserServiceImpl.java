package com.bdis.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.core.PageResult;
import com.bdis.common.exception.DuplicateResourceException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.settings.service.UserSessionService;
import com.bdis.modules.user.dto.AdminPasswordResetDTO;
import com.bdis.modules.user.dto.RoleAssignDTO;
import com.bdis.modules.user.dto.UserCreateDTO;
import com.bdis.modules.user.dto.UserUpdateDTO;
import com.bdis.modules.user.entity.RoleEntity;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.entity.UserRoleEntity;
import com.bdis.modules.user.mapper.RoleMapper;
import com.bdis.modules.user.mapper.UserMapper;
import com.bdis.modules.user.mapper.UserRoleMapper;
import com.bdis.modules.user.query.UserQuery;
import com.bdis.modules.user.service.UserService;
import com.bdis.modules.user.vo.RoleVO;
import com.bdis.modules.user.vo.UserVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final UserRoleMapper userRoleMapper;

    private final PasswordEncoder passwordEncoder;

    private final UserSessionService userSessionService;

    public UserServiceImpl(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder,
            UserSessionService userSessionService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.userSessionService = userSessionService;
    }

    @Override
    public PageResult<UserVO> page(UserQuery query) {
        Page<UserEntity> page =
                userMapper.selectUserPage(Page.of(query.getPage(), query.getSize()), query);
        List<UserVO> records = attachRoles(page.getRecords());
        return PageResult.of(records, page);
    }

    @Override
    public UserVO detail(Long id) {
        UserEntity user = requireUser(id);
        return attachRoles(List.of(user)).getFirst();
    }

    @Override
    @Transactional
    public Long create(UserCreateDTO dto) {
        ensureUsernameAvailable(dto.getUsername(), null);
        UserEntity user = new UserEntity();
        user.setUserNo(generateNo("U"));
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setEmail(dto.getEmail());
        user.setOrganizationId(dto.getOrganizationId());
        user.setDepartmentId(dto.getDepartmentId());
        user.setMustChangePassword(!Boolean.FALSE.equals(dto.getMustChangePassword()));
        user.setStatus(1);
        user.setCreatedBy(SecurityUtils.currentUser().getUserId());
        userMapper.insert(user);
        replaceRoles(user.getId(), dto.getRoleIds());
        return user.getId();
    }

    @Override
    @Transactional
    public void update(Long id, UserUpdateDTO dto) {
        UserEntity user = requireUser(id);
        applyUpdate(user, dto);
        userMapper.updateById(user);
        if (dto.getRoleIds() != null) {
            replaceRoles(id, dto.getRoleIds());
        }
    }

    @Override
    @Transactional
    public void patch(Long id, UserUpdateDTO dto) {
        update(id, dto);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireUser(id);
        if (SecurityUtils.currentUser().getUserId().equals(id)) {
            throw new ForbiddenException("不能删除当前登录用户");
        }
        userRoleMapper.delete(
                new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, id));
        userMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void assignRoles(Long id, RoleAssignDTO dto) {
        requireUser(id);
        replaceRoles(id, dto.getRoleIds());
    }

    @Override
    @Transactional
    public void resetPassword(Long id, AdminPasswordResetDTO dto) {
        UserEntity user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(!Boolean.FALSE.equals(dto.getMustChangePassword()));
        user.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        userMapper.updateById(user);
        userSessionService.revokeAllForUser(id, "admin_password_reset");
    }

    private void applyUpdate(UserEntity user, UserUpdateDTO dto) {
        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
        }
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getOrganizationId() != null) {
            user.setOrganizationId(dto.getOrganizationId());
        }
        if (dto.getDepartmentId() != null) {
            user.setDepartmentId(dto.getDepartmentId());
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        user.setUpdatedBy(SecurityUtils.currentUser().getUserId());
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(
                new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId));
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        Long operatorId = SecurityUtils.currentUser().getUserId();
        for (Long roleId : roleIds) {
            RoleEntity role = roleMapper.selectById(roleId);
            if (role == null || role.getStatus() == null || role.getStatus() != 1) {
                throw new ResourceNotFoundException("角色不存在或已停用：" + roleId);
            }
            UserRoleEntity relation = new UserRoleEntity();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            relation.setCreatedBy(operatorId);
            userRoleMapper.insert(relation);
        }
    }

    private UserEntity requireUser(Long id) {
        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        return user;
    }

    private void ensureUsernameAvailable(String username, Long excludeId) {
        UserEntity existed =
                userMapper.selectOne(
                        new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
        if (existed != null && (excludeId == null || !existed.getId().equals(excludeId))) {
            throw new DuplicateResourceException("账号已存在");
        }
    }

    private List<UserVO> attachRoles(List<UserEntity> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = users.stream().map(UserEntity::getId).toList();
        List<UserRoleEntity> relations =
                userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRoleEntity>()
                                .in(UserRoleEntity::getUserId, userIds));
        Map<Long, List<RoleVO>> roleMap = new LinkedHashMap<>();
        if (!relations.isEmpty()) {
            List<Long> roleIds =
                    relations.stream().map(UserRoleEntity::getRoleId).distinct().toList();
            Map<Long, RoleVO> rolesById = new LinkedHashMap<>();
            for (RoleEntity role : roleMapper.selectBatchIds(roleIds)) {
                rolesById.put(role.getId(), toRoleVO(role));
            }
            for (UserRoleEntity relation : relations) {
                RoleVO role = rolesById.get(relation.getRoleId());
                if (role != null) {
                    roleMap.computeIfAbsent(relation.getUserId(), ignored -> new ArrayList<>())
                            .add(role);
                }
            }
        }
        List<UserVO> result = new ArrayList<>();
        for (UserEntity user : users) {
            UserVO vo = toVO(user);
            vo.setRoles(roleMap.getOrDefault(user.getId(), List.of()));
            result.add(vo);
        }
        return result;
    }

    private UserVO toVO(UserEntity user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUserNo(user.getUserNo());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhoneNumber(user.getPhoneNumber());
        vo.setEmail(user.getEmail());
        vo.setOrganizationId(user.getOrganizationId());
        vo.setDepartmentId(user.getDepartmentId());
        vo.setStatus(user.getStatus());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setMustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()));
        return vo;
    }

    private RoleVO toRoleVO(RoleEntity role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setRoleType(role.getRoleType());
        vo.setDataScope(role.getDataScope());
        vo.setDescription(role.getDescription());
        vo.setSortOrder(role.getSortOrder());
        vo.setStatus(role.getStatus());
        return vo;
    }

    private String generateNo(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    }
}
