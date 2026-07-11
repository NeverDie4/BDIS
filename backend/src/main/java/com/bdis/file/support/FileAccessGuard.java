package com.bdis.file.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.file.entity.FileBusinessEntity;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileBusinessMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FileAccessGuard {

    private final FileBusinessMapper fileBusinessMapper;
    private final BusinessReferenceValidator businessReferenceValidator;

    public FileAccessGuard(
            FileBusinessMapper fileBusinessMapper,
            BusinessReferenceValidator businessReferenceValidator) {
        this.fileBusinessMapper = fileBusinessMapper;
        this.businessReferenceValidator = businessReferenceValidator;
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
                                        businessReferenceValidator.canAccess(
                                                relation.getBizType(), relation.getBizId()));
        if (!allowed) {
            throw new ForbiddenException("无权访问该文件");
        }
    }

    private boolean isAdmin() {
        return CurrentUserUtils.currentRoleCodes().stream().anyMatch("ADMIN"::equalsIgnoreCase);
    }
}
