package com.bdis.common.security;

import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.DataScopeResultVO;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Validates cross-module business references without coupling the file module to business tables.
 */
@Service
public class BusinessReferenceAccessService {

    private static final Map<String, BusinessReference> REFERENCES =
            Map.ofEntries(
                    Map.entry(
                            "herb_species",
                            new BusinessReference("herb_species", "herb:species:view")),
                    Map.entry(
                            "sys_file_resource",
                            new BusinessReference("sys_file_resource", "file:resource:view")),
                    Map.entry(
                            "herb_image",
                            new BusinessReference("herb_image", "herb:identification:view")),
                    Map.entry(
                            "herb_atlas",
                            new BusinessReference("herb_atlas", "herb:identification:view")),
                    Map.entry(
                            "herb_growth_record",
                            new BusinessReference("herb_growth_record", "growth:record:view")),
                    Map.entry(
                            "map_point",
                            new BusinessReference("herb_distribution", "map:point:view")),
                    Map.entry(
                            "herb_batch",
                            new BusinessReference("herb_batch", "growth:record:view")),
                    Map.entry(
                            "eval_application",
                            new BusinessReference(
                                    "eval_application", "declaration:application:view")),
                    Map.entry(
                            "eval_attachment",
                            new BusinessReference(
                                    "eval_attachment", "declaration:application:view")),
                    Map.entry(
                            "eval_archive",
                            new BusinessReference("eval_archive", "declaration:application:view")),
                    Map.entry(
                            "eval_task",
                            new BusinessReference("eval_task", "evaluation:task:view")),
                    Map.entry(
                            "perf_record",
                            new BusinessReference("perf_record", "performance:record:view")),
                    Map.entry(
                            "research_project",
                            new BusinessReference("research_project", "research:project:detail")),
                    Map.entry(
                            "edu_course", new BusinessReference("edu_course", "edu:course:detail")),
                    Map.entry(
                            "edu_experiment_record",
                            new BusinessReference(
                                    "edu_experiment_record", "edu:experiment-record:detail")),
                    Map.entry(
                            "edu_training_plan",
                            new BusinessReference(
                                    "edu_training_plan", "edu:training-plan:detail")),
                    Map.entry(
                            "soap_sync_task",
                            new BusinessReference("soap_sync_task", "soap:exchange:view")));

    private final JdbcTemplate jdbcTemplate;
    private final AuthorizationService authorizationService;
    private final DataScopeService dataScopeService;

    public BusinessReferenceAccessService(
            JdbcTemplate jdbcTemplate,
            AuthorizationService authorizationService,
            DataScopeService dataScopeService) {
        this.jdbcTemplate = jdbcTemplate;
        this.authorizationService = authorizationService;
        this.dataScopeService = dataScopeService;
    }

    public void validate(String bizType, Long bizId) {
        if (bizType == null || bizType.isBlank()) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "业务类型不能为空");
        }
        if (bizId == null || bizId <= 0) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "业务 ID 不合法");
        }
        String normalizedType = normalizeType(bizType);
        BusinessReference reference = REFERENCES.get(normalizedType);
        if (reference == null) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "不支持的业务类型");
        }
        if (!authorizationService.hasPermission(reference.permissionCode())) {
            throw new ForbiddenException("无权访问该业务对象");
        }
        Long count =
                jdbcTemplate.queryForObject(
                        "select count(*) from "
                                + reference.tableName()
                                + " where id = ? and coalesce(is_deleted, 0) = 0",
                        Long.class,
                        bizId);
        if (count == null || count == 0) {
            String message =
                    switch (normalizedType) {
                        case "edu_course" -> "Course not found";
                        case "edu_experiment_record" -> "Experiment record not found";
                        case "research_project" -> "Research project not found";
                        default -> "关联业务对象不存在";
                    };
            throw new ResourceNotFoundException(message);
        }
        if (!hasRowAccess(normalizedType, bizId)) {
            throw new ForbiddenException("业务对象超出当前数据范围");
        }
    }

    private boolean hasRowAccess(String bizType, Long bizId) {
        if (CurrentUserUtils.currentRoleCodes().isEmpty()
                || CurrentUserUtils.currentRoleCodes().stream()
                        .anyMatch("ADMIN"::equalsIgnoreCase)) {
            return true;
        }
        if ("herb_growth_record".equals(bizType)) {
            Long collectorId =
                    jdbcTemplate.queryForObject(
                            "select collector_id from herb_growth_record where id = ? and"
                                    + " is_deleted = 0",
                            Long.class,
                            bizId);
            return canAccessGrowthCollector(collectorId);
        }
        if ("herb_image".equals(bizType)) {
            List<Map<String, Object>> owners =
                    jdbcTemplate.queryForList(
                            """
                            select image.uploader_id,
                                   growth.collector_id as growth_collector_id
                            from herb_image image
                            left join herb_growth_record growth
                              on growth.id = image.growth_record_id and growth.is_deleted = 0
                            where image.id = ? and image.is_deleted = 0
                            """,
                            bizId);
            if (owners.isEmpty()) {
                return false;
            }
            Map<String, Object> owner = owners.getFirst();
            return canAccessGrowthCollector(number(owner.get("uploader_id")))
                    || canAccessGrowthCollector(number(owner.get("growth_collector_id")));
        }
        if ("herb_batch".equals(bizType)) {
            List<Map<String, Object>> owners =
                    jdbcTemplate.queryForList(
                            """
                            select batch.created_by as batch_created_by,
                                   task.created_by as task_created_by,
                                   task.collector_id as task_collector_id
                            from herb_batch batch
                            left join herb_collection_task task
                              on task.id = batch.task_id and task.is_deleted = 0
                            where batch.id = ? and batch.is_deleted = 0
                            """,
                            bizId);
            if (owners.isEmpty()) {
                return false;
            }
            Map<String, Object> owner = owners.getFirst();
            return canAccessGrowthCollector(number(owner.get("batch_created_by")))
                    || canAccessGrowthCollector(number(owner.get("task_created_by")))
                    || canAccessGrowthCollector(number(owner.get("task_collector_id")));
        }
        if ("eval_application".equals(bizType)) {
            return canAccessOwner(
                    ownerId("select applicant_id from eval_application where id = ?", bizId),
                    "eval_application");
        }
        if ("eval_attachment".equals(bizType)) {
            return canAccessOwner(
                    ownerId(
                            """
                            select application.applicant_id
                            from eval_attachment attachment
                            join eval_application application
                              on application.id = attachment.application_id
                             and application.is_deleted = 0
                            where attachment.id = ? and attachment.is_deleted = 0
                            """,
                            bizId),
                    "eval_application");
        }
        if ("eval_archive".equals(bizType)) {
            return canAccessOwner(
                    ownerId("select owner_id from eval_archive where id = ?", bizId),
                    "eval_application");
        }
        if ("eval_task".equals(bizType)) {
            return canAccessOwner(
                    ownerId("select owner_id from eval_task where id = ?", bizId), "eval_task");
        }
        if ("perf_record".equals(bizType)) {
            return canAccessOwner(
                    ownerId("select user_id from perf_record where id = ?", bizId), "perf_record");
        }
        if ("research_project".equals(bizType)) {
            return canAccessResearchProject(bizId);
        }
        if ("edu_course".equals(bizType)) {
            return canAccessCourse(bizId);
        }
        if ("edu_experiment_record".equals(bizType)) {
            return canAccessExperimentRecord(bizId);
        }
        if ("edu_training_plan".equals(bizType)) {
            return canAccessTrainingPlan(bizId);
        }
        return true;
    }

    private boolean canAccessResearchProject(Long projectId) {
        List<Map<String, Object>> projects =
                jdbcTemplate.queryForList(
                        "select leader_id from research_project where id = ? and is_deleted = 0",
                        projectId);
        if (projects.isEmpty()) {
            return false;
        }
        if (CurrentUserUtils.currentUserId().equals(number(projects.getFirst().get("leader_id")))) {
            return true;
        }
        Long memberCount =
                jdbcTemplate.queryForObject(
                        """
                        select count(*) from rel_project_member
                        where project_id = ? and user_id = ? and member_status = 'active'
                        """,
                        Long.class,
                        projectId,
                        CurrentUserUtils.currentUserId());
        return memberCount != null && memberCount > 0;
    }

    private boolean canAccessCourse(Long courseId) {
        List<Map<String, Object>> courses =
                jdbcTemplate.queryForList(
                        """
                        select teacher_id, created_by, publish_status
                        from edu_course
                        where id = ? and is_deleted = 0
                        """,
                        courseId);
        if (courses.isEmpty()) {
            return false;
        }
        Map<String, Object> course = courses.getFirst();
        Long currentUserId = CurrentUserUtils.currentUserId();
        if (currentUserId.equals(number(course.get("teacher_id")))
                || currentUserId.equals(number(course.get("created_by")))) {
            return true;
        }
        return CurrentUserUtils.currentRoleCodes().stream().anyMatch("STUDENT"::equalsIgnoreCase)
                && "published".equalsIgnoreCase(String.valueOf(course.get("publish_status")));
    }

    private boolean canAccessExperimentRecord(Long recordId) {
        List<Map<String, Object>> records =
                jdbcTemplate.queryForList(
                        """
                        select recorder_id, course_id, project_id
                        from edu_experiment_record
                        where id = ? and is_deleted = 0
                        """,
                        recordId);
        if (records.isEmpty()) {
            return false;
        }
        Map<String, Object> record = records.getFirst();
        Long currentUserId = CurrentUserUtils.currentUserId();
        if (currentUserId.equals(number(record.get("recorder_id")))) {
            return true;
        }
        Long courseId = number(record.get("course_id"));
        if (courseId != null && isCourseTeacher(courseId, currentUserId)) {
            return true;
        }
        Long projectId = number(record.get("project_id"));
        return projectId != null && canAccessResearchProject(projectId);
    }

    private boolean canAccessTrainingPlan(Long planId) {
        List<Map<String, Object>> plans =
                jdbcTemplate.queryForList(
                        """
                        select owner_id, trainer_id
                        from edu_training_plan
                        where id = ? and is_deleted = 0
                        """,
                        planId);
        if (plans.isEmpty()) {
            return false;
        }
        Map<String, Object> plan = plans.getFirst();
        Long currentUserId = CurrentUserUtils.currentUserId();
        if (currentUserId.equals(number(plan.get("owner_id")))
                || currentUserId.equals(number(plan.get("trainer_id")))) {
            return true;
        }
        Long participantCount =
                jdbcTemplate.queryForObject(
                        "select count(*) from edu_training_record where plan_id = ? and user_id = ?",
                        Long.class,
                        planId,
                        currentUserId);
        return participantCount != null && participantCount > 0;
    }

    private boolean isCourseTeacher(Long courseId, Long userId) {
        List<Long> teacherIds =
                jdbcTemplate.queryForList(
                        "select teacher_id from edu_course where id = ? and is_deleted = 0",
                        Long.class,
                        courseId);
        return !teacherIds.isEmpty() && userId.equals(teacherIds.getFirst());
    }

    private Long ownerId(String sql, Long bizId) {
        List<Long> owners = jdbcTemplate.queryForList(sql, Long.class, bizId);
        return owners.isEmpty() ? null : owners.getFirst();
    }

    private boolean canAccessOwner(Long ownerId, String resourceType) {
        if (ownerId != null && ownerId.equals(CurrentUserUtils.currentUserId())) {
            return true;
        }
        if (ownerId == null) {
            return false;
        }
        DataScopeResultVO scope = dataScopeService.resolveForCurrentUser(resourceType);
        if (scope.isAllIncluded()) {
            return true;
        }
        List<Map<String, Object>> users =
                jdbcTemplate.queryForList(
                        "select organization_id, department_id from sys_user where id = ? and"
                                + " is_deleted = 0",
                        ownerId);
        if (users.isEmpty()) {
            return false;
        }
        Map<String, Object> user = users.getFirst();
        Long organizationId = number(user.get("organization_id"));
        Long departmentId = number(user.get("department_id"));
        return (organizationId != null && scope.getOrganizationIds().contains(organizationId))
                || (departmentId != null && scope.getDepartmentIds().contains(departmentId));
    }

    private boolean canAccessGrowthCollector(Long collectorId) {
        if (collectorId != null && collectorId.equals(CurrentUserUtils.currentUserId())) {
            return true;
        }
        if (collectorId == null) {
            return false;
        }
        DataScopeResultVO scope = dataScopeService.resolveForCurrentUser("herb_growth_record");
        if (scope.isAllIncluded()) {
            return true;
        }
        List<Map<String, Object>> users =
                jdbcTemplate.queryForList(
                        "select organization_id, department_id from sys_user where id = ? and"
                                + " is_deleted = 0",
                        collectorId);
        if (users.isEmpty()) {
            return false;
        }
        Map<String, Object> user = users.getFirst();
        Long organizationId = number(user.get("organization_id"));
        Long departmentId = number(user.get("department_id"));
        return (organizationId != null && scope.getOrganizationIds().contains(organizationId))
                || (departmentId != null && scope.getDepartmentIds().contains(departmentId));
    }

    private Long number(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private String normalizeType(String bizType) {
        return switch (bizType) {
            case "herb" -> "herb_species";
            case "declaration", "application" -> "eval_application";
            case "attachment" -> "eval_attachment";
            case "archive" -> "eval_archive";
            case "evaluation_task" -> "eval_task";
            case "performance" -> "perf_record";
            case "file" -> "sys_file_resource";
            default -> bizType;
        };
    }

    private record BusinessReference(String tableName, String permissionCode) {}
}
