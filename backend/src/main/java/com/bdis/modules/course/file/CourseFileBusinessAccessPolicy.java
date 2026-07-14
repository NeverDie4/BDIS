package com.bdis.modules.course.file;

import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.policy.FileBusinessAccessPolicy;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.permission.service.AuthorizationService;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class CourseFileBusinessAccessPolicy implements FileBusinessAccessPolicy {

    private final CourseMapper courseMapper;
    private final AuthorizationService authorizationService;

    public CourseFileBusinessAccessPolicy(
            CourseMapper courseMapper, AuthorizationService authorizationService) {
        this.courseMapper = courseMapper;
        this.authorizationService = authorizationService;
    }

    @Override
    public String bizType() {
        return "edu_course";
    }

    @Override
    public boolean exists(Long bizId) {
        return courseMapper.selectById(bizId) != null;
    }

    @Override
    public boolean canView(Long bizId) {
        CourseEntity course = courseMapper.selectById(bizId);
        return course != null
                && authorizationService.hasPermission("edu:course:detail")
                && (isAdmin() || isOwner(course) || isStudentPublished(course));
    }

    @Override
    public boolean canAttach(Long bizId) {
        return canManage(bizId, "edu:course:update");
    }

    @Override
    public boolean canDetach(Long bizId) {
        return canAttach(bizId);
    }

    @Override
    public boolean canPublish(Long bizId) {
        return canManage(bizId, "edu:course:publish");
    }

    private boolean canManage(Long bizId, String permission) {
        CourseEntity course = courseMapper.selectById(bizId);
        return course != null
                && authorizationService.hasPermission(permission)
                && (isAdmin() || isOwner(course));
    }

    private boolean isOwner(CourseEntity course) {
        Long userId = CurrentUserUtils.currentUserId();
        return Objects.equals(userId, course.getCreatedBy())
                || Objects.equals(userId, course.getTeacherId());
    }

    private boolean isStudentPublished(CourseEntity course) {
        return CurrentUserUtils.currentRoleCodes().stream()
                        .anyMatch(role -> "STUDENT".equalsIgnoreCase(role))
                && "published".equalsIgnoreCase(course.getPublishStatus());
    }

    private boolean isAdmin() {
        return CurrentUserUtils.currentRoleCodes().stream()
                .anyMatch(SecurityConstants.ADMIN_ROLE_CODE::equalsIgnoreCase);
    }
}
