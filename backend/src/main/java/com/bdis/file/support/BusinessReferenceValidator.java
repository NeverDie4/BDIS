package com.bdis.file.support;

import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.DataScopeResultVO;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BusinessReferenceValidator {

    private static final Map<String, BusinessReference> REFERENCES =
            Map.ofEntries(
                    Map.entry(
                            "herb_species",
                            new BusinessReference("herb_species", "herb:species:view")),
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
                            "herb_batch",
                            new BusinessReference("herb_batch", "growth:record:view")),
                    Map.entry(
                            "eval_application",
                            new BusinessReference("eval_application", "file:resource:view")),
                    Map.entry(
                            "perf_record",
                            new BusinessReference("perf_record", "file:resource:view")),
                    Map.entry(
                            "research_project",
                            new BusinessReference("research_project", "file:resource:view")),
                    Map.entry(
                            "edu_training_plan",
                            new BusinessReference("edu_training_plan", "file:resource:view")),
                    Map.entry(
                            "soap_sync_task",
                            new BusinessReference("soap_sync_task", "soap:exchange:view")));

    private final JdbcTemplate jdbcTemplate;
    private final AuthorizationService authorizationService;
    private final DataScopeService dataScopeService;

    public BusinessReferenceValidator(
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
        BusinessReference reference = REFERENCES.get(bizType);
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
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "关联业务对象不存在");
        }
        if (!hasRowAccess(bizType, bizId)) {
            throw new ForbiddenException("业务对象超出当前数据范围");
        }
    }

    public boolean canAccess(String bizType, Long bizId) {
        try {
            validate(bizType, bizId);
            return true;
        } catch (BusinessException exception) {
            return false;
        }
    }

    private boolean hasRowAccess(String bizType, Long bizId) {
        if (CurrentUserUtils.currentRoleCodes().stream().anyMatch("ADMIN"::equalsIgnoreCase)) {
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
        return true;
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

    private record BusinessReference(String tableName, String permissionCode) {}
}
