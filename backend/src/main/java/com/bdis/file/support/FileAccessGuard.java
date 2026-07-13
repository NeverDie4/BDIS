package com.bdis.file.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.policy.FileBusinessAction;
import com.bdis.file.policy.FileBusinessPolicyRegistry;
import com.bdis.modules.file.entity.FileBusinessEntity;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileBusinessMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FileAccessGuard {

    private final FileBusinessMapper fileBusinessMapper;
    private final FileBusinessPolicyRegistry policyRegistry;

    public FileAccessGuard(
            FileBusinessMapper fileBusinessMapper, FileBusinessPolicyRegistry policyRegistry) {
        this.fileBusinessMapper = fileBusinessMapper;
        this.policyRegistry = policyRegistry;
    }

    public void requireAuthenticatedAccess(FileResourceEntity entity) {
        Long currentUserId = CurrentUserUtils.currentUserId();
        if (isAdmin()
                || "public".equalsIgnoreCase(entity.getAccessLevel())
                || (currentUserId != null && currentUserId.equals(entity.getUploaderId()))) {
            return;
        }
        List<FileBusinessEntity> relations =
                fileBusinessMapper.selectList(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getFileId, entity.getId()));
        boolean allowed =
                relations.stream()
                        .anyMatch(
                                relation ->
                                        policyRegistry.can(
                                                relation.getBizType(),
                                                relation.getBizId(),
                                                FileBusinessAction.VIEW));
        if (!allowed) {
            throw new ForbiddenException("无权访问该文件");
        }
    }

    private boolean isAdmin() {
        return CurrentUserUtils.currentRoleCodes().stream().anyMatch("ADMIN"::equalsIgnoreCase);
    }
}
