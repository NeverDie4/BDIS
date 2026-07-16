package com.bdis.modules.research.file;

import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.policy.FileBusinessAccessPolicy;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ResearchProjectFileBusinessAccessPolicy implements FileBusinessAccessPolicy {

    private final ResearchProjectMapper projectMapper;
    private final AuthorizationService authorizationService;

    public ResearchProjectFileBusinessAccessPolicy(
            ResearchProjectMapper projectMapper, AuthorizationService authorizationService) {
        this.projectMapper = projectMapper;
        this.authorizationService = authorizationService;
    }

    @Override
    public String bizType() {
        return "research_project";
    }

    @Override
    public boolean exists(Long bizId) {
        return projectMapper.selectById(bizId) != null;
    }

    @Override
    public boolean canView(Long bizId) {
        return isAllowed(bizId, "research:project:detail", false);
    }

    @Override
    public boolean canAttach(Long bizId) {
        return isAllowed(bizId, "research:project-material:add", true);
    }

    @Override
    public boolean canDetach(Long bizId) {
        return isAllowed(bizId, "research:project-material:delete", true);
    }

    @Override
    public boolean canPublish(Long bizId) {
        return canAttach(bizId);
    }

    private boolean isAllowed(Long bizId, String permission, boolean manage) {
        ResearchProjectEntity project = projectMapper.selectById(bizId);
        if (project == null || !authorizationService.hasPermission(permission)) {
            return false;
        }
        if (isAdmin() || Objects.equals(CurrentUserUtils.currentUserId(), project.getLeaderId())) {
            return true;
        }
        return !manage && projectMapper.existsActiveMember(bizId, CurrentUserUtils.currentUserId());
    }

    private boolean isAdmin() {
        return CurrentUserUtils.currentRoleCodes().stream()
                .anyMatch(SecurityConstants.ADMIN_ROLE_CODE::equalsIgnoreCase);
    }
}
