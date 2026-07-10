# 生物医药数字信息系统 Java 类设计说明书

> 版本：V1.1
> 日期：2026-07-10
> 负责人：全组  
> 适用阶段：详细设计、后端开发、代码生成、测试设计  
> 技术栈：Java、Spring Boot、MyBatis Plus、MySQL 8.0、Redis、Nginx  
> 关联文档：需求分析说明书、模块设计说明书、接口设计规范与接口清单、数据库详细设计说明书、Java 类命名规范
> 本次更新：同步启动类、配置包、模块包迁移状态及识别采集新增表的 Java 映射。

---

## 1. 文档目的

本文档用于说明生物医药数字信息系统后端 Java 类的整体设计，包括包结构、分层结构、公共基础类、模块类清单、实体类映射、DTO/VO/Query/Enum 设计和代码落地优先级。

本文档不替代接口设计文档和数据库设计文档。接口路径、HTTP 方法和请求响应格式以接口设计规范为准；数据库表结构、字段类型和索引以数据库详细设计说明书为准；类命名规则以 Java 类命名规范为准。

本文档同时包含“当前实现基线”和“后续目标设计”。第 4 至 8 节描述当前必须遵守的结构和映射；后续模块类清单用于指导逐步实现，不表示其中每个类都已经存在。当前代码清单以源码和编译结果为准。

---

## 2. 设计依据

| 文档 | 对本设计的影响 |
| --- | --- |
| 需求分析说明书 | 明确系统功能范围和用户角色。 |
| 模块设计说明书 | 确定 20 个模块的边界、依赖关系和优先级。 |
| 接口设计规范与接口清单 | 确定 Controller 的资源化命名、DTO 和 VO 的场景边界。 |
| 数据库详细设计说明书 | 确定 Entity、Mapper 和主要业务对象来源。 |
| Java 类命名规范 | 确定类名后缀、业务英文名称和命名禁忌。 |

---

## 3. 总体设计原则

| 原则 | 设计要求 |
| --- | --- |
| 分层清晰 | Controller、Service、ServiceImpl、Mapper、Entity、DTO、VO、Query、Enum 分层明确。 |
| 业务优先 | 类名优先体现业务对象，不直接照搬所有数据库前缀。 |
| 表类对应 | 每张业务表至少对应一个 Entity 和一个 Mapper。 |
| 模块内聚 | 类按模块归属组织，避免所有类集中在一个 common 或 system 包中。 |
| 低耦合 | 模块之间通过 ID、DTO、VO 或领域服务传递数据，不直接操作其他模块内部实现。 |
| 统一返回 | Controller 统一返回 `Result<T>` 或 `PageResult<T>`。 |
| 统一异常 | 业务异常统一使用 `BusinessException` 及其子类表达。 |
| 可阶段落地 | P0 模块优先实现完整 Controller、Service、Mapper、Entity；P1 模块可先完成 Entity、Mapper 和基础 Service。 |

---

## 4. 基础包结构设计

建议后端根包名为：

```text
com.bdis
```

如果学校或团队已有统一域名规范，可整体替换根包，例如 `edu.cqut.bdis`，但模块包名和类名保持不变。

```text
com.bdis
├── BdisApplication
├── common
│   ├── core
│   ├── exception
│   ├── security
│   ├── constants
│   ├── enums
│   └── utils
├── config
├── modules
│   ├── auth
│   ├── user
│   ├── permission
│   ├── dictionary
│   ├── file
│   ├── audit
│   ├── herb
│   ├── map
│   ├── growth
│   ├── spectrum
│   ├── knowledge
│   ├── course
│   ├── research
│   ├── experiment
│   ├── training
│   ├── evaluation
│   ├── declaration
│   ├── performance
│   ├── soap
│   └── dashboard
```

目标结构统一使用 `com.bdis.modules.<module>`。当前 `audit`、`dashboard`、`file`、`soap` 的部分 Controller、Service、DTO、Query 和 VO 仍位于 `com.bdis.<module>`，而 Entity、Mapper 位于 `com.bdis.modules.<module>`；这是合并后的兼容状态。后续按模块整体迁移，迁移完成前不再新增顶层业务包。

具体迁移顺序、非目标和验收标准见 [03_代码一致性重构需求说明_V1.0_全组_20260710.md](03_代码一致性重构需求说明_V1.0_全组_20260710.md)。

---

## 5. 单模块内部包结构

每个业务模块内部建议采用以下结构。

```text
modules.xxx
├── controller
├── service
│   └── impl
├── mapper
├── entity
├── dto
├── query
├── vo
├── enums
└── converter
```

| 子包 | 职责 |
| --- | --- |
| controller | 接收请求、参数校验、调用 Service、返回统一结果。 |
| service | 定义业务逻辑接口。 |
| service.impl | 实现业务逻辑、事务控制、状态流转和跨模块协调。 |
| mapper | MyBatis Plus 数据访问接口。 |
| entity | 数据库表映射对象。 |
| dto | 新增、修改、提交、审核、导入等请求对象。 |
| query | 列表分页、关键词、状态、时间范围等查询条件。 |
| vo | 列表页、详情页、树形结构、统计看板等响应对象。 |
| enums | 模块内部状态、类型、动作枚举。 |
| converter | Entity、DTO、VO 之间的转换类。 |

---

## 6. 公共基础类设计

### 6.1 启动类

| 类名 | 包路径 | 职责 |
| --- | --- | --- |
| `BdisApplication` | `com.bdis` | Spring Boot 启动入口。 |

### 6.2 通用响应类

| 类名 | 包路径 | 职责 |
| --- | --- | --- |
| `Result<T>` | `common.core` | 普通接口统一响应。 |
| `PageResult<T>` | `common.core` | 分页接口统一响应。 |
| `ResultCodeEnum` | `common.enums` | 成功、参数错误、未授权、无权限、资源不存在等响应编码。 |

### 6.3 通用基类

| 类名 | 包路径 | 职责 |
| --- | --- | --- |
| `BaseEntity` | `common.core` | 通用主键、状态、逻辑删除、创建时间、更新时间等字段。 |
| `BaseQuery` | `common.core` | 分页、关键词、排序、时间范围等通用查询字段。 |
| `CurrentUser` | `common.security` | 当前登录用户上下文对象。 |

### 6.4 异常类

| 类名 | 包路径 | 职责 |
| --- | --- | --- |
| `BusinessException` | `common.exception` | 通用业务异常。 |
| `UnauthorizedException` | `common.exception` | 未登录或 Token 失效。 |
| `ForbiddenException` | `common.exception` | 无权限访问。 |
| `ResourceNotFoundException` | `common.exception` | 数据不存在。 |
| `DuplicateResourceException` | `common.exception` | 业务编号、账号、编码重复。 |
| `FileStorageException` | `common.exception` | 文件上传、保存、读取失败。 |
| `SoapExchangeException` | `common.exception` | SOAP 请求、解析、转换失败。 |
| `GlobalExceptionHandler` | `common.exception` | 全局异常处理器。 |

### 6.5 配置类

| 类名 | 包路径 | 职责 |
| --- | --- | --- |
| `SecurityConfig` | `config` | Token 认证、访问控制和 CORS 配置。 |
| `MyBatisPlusConfig` | `config` | MyBatis Plus 分页插件配置。 |
| `StaticResourceConfig` | `config` | 本地上传文件静态资源映射。 |
| `OpenApiConfig` | `config` | OpenAPI 接口文档配置。 |

### 6.6 工具类和常量类

| 类名 | 包路径 | 职责 |
| --- | --- | --- |
| `JwtUtils` | `common.security` | Token 生成、解析和校验。 |
| `SecurityUtils` | `common.security` | 当前认证信息读取。 |
| `CurrentUserUtils` | `common.utils` | 当前用户辅助访问。 |
| `SecurityConstants` | `common.constants` | Token、Header、权限相关常量。 |

---

## 7. Entity 与数据库表映射设计

实体类统一使用 `Entity` 后缀，并通过 `@TableName` 映射数据库表。Java 类名优先采用业务对象名称，数据库前缀通过注解保留。

| 数据库表 | Entity 类 | 所属模块 | 说明 |
| --- | --- | --- | --- |
| `sys_organization` | `OrganizationEntity` | user | 机构表。 |
| `sys_department` | `DepartmentEntity` | user | 部门表。 |
| `sys_user` | `UserEntity` | user | 用户表。 |
| `auth_role` | `RoleEntity` | user | 角色表。 |
| `auth_menu` | `MenuEntity` | permission | 菜单表。 |
| `auth_permission` | `PermissionEntity` | permission | 权限表。 |
| `rel_user_role` | `UserRoleEntity` | user | 用户角色关系表。 |
| `rel_role_permission` | `RolePermissionEntity` | permission | 角色权限关系表。 |
| `auth_data_scope` | `DataScopeEntity` | permission | 数据范围权限表。 |
| `dict_type` | `DictTypeEntity` | dictionary | 字典类型表。 |
| `dict_item` | `DictItemEntity` | dictionary | 字典项表。 |
| `dict_region` | `RegionEntity` | dictionary | 区域字典表。 |
| `sys_file_resource` | `FileResourceEntity` | file | 文件资源表。 |
| `sys_file_business` | `FileBusinessEntity` | file | 文件业务关联表。 |
| `log_login` | `LoginLogEntity` | audit | 登录日志表。 |
| `log_operation` | `OperationLogEntity` | audit | 操作日志表。 |
| `log_data_change` | `DataChangeLogEntity` | audit | 数据变更日志表。 |
| `log_file_access` | `FileAccessLogEntity` | audit | 文件访问日志表。 |
| `log_data_sync` | `DataSyncLogEntity` | audit | 数据同步日志表。 |
| `herb_species` | `HerbEntity` | herb | 中药材主数据表。 |
| `herb_base` | `HerbBaseEntity` | map | 中药材基地表。 |
| `herb_distribution` | `MapPointEntity` | map | 地图分布点表。 |
| `herb_growth_record` | `GrowthRecordEntity` | growth | 生长采集记录表。 |
| `herb_growth_review_record` | `GrowthAuditRecordEntity` | growth | 采集审核记录表。 |
| `herb_ai_model_version` | `AiModelVersionEntity` | spectrum | AI 模型版本表。 |
| `herb_atlas` | `SpectrumEntity` | spectrum | 标准图谱表。 |
| `herb_atlas_tag` | `SpectrumTagEntity` | spectrum | 标准图谱标签表。 |
| `herb_image` | `HerbImageEntity` | herb | 用户上传或采集中药材图片表。 |
| `herb_image_recognition` | `ImageRecognitionEntity` | spectrum | 图片 AI 识别结果表。 |
| `herb_image_match` | `SpectrumComparisonEntity` | spectrum | 图片与标准图谱比对结果表。 |
| `herb_atlas_feature` | `HerbAtlasFeatureEntity` | spectrum | 标准图谱特征向量表。 |
| `herb_image_feature` | `HerbImageFeatureEntity` | spectrum | 用户图片特征向量表。 |
| `herb_identification_result` | `HerbIdentificationResultEntity` | spectrum | 图片最终识别结论表。 |
| `herb_collection_task` | `HerbCollectionTaskEntity` | collection | 中药材采集任务表。 |
| `herb_batch` | `HerbBatchEntity` | collection | 采集批次档案表。 |
| `herb_batch_image` | `HerbBatchImageEntity` | collection | 批次与图片关联表。 |
| `herb_knowledge_entity` | `KnowledgeNodeEntity` | knowledge | 知识图谱实体表。 |
| `herb_knowledge_relation` | `KnowledgeRelationEntity` | knowledge | 知识图谱关系类型表。 |
| `herb_knowledge_triple` | `KnowledgeTripleEntity` | knowledge | 知识图谱三元组表。 |
| `edu_course` | `CourseEntity` | course | 实验课程表。 |
| `edu_experiment_step` | `ExperimentStepEntity` | course | 实验步骤表。 |
| `edu_course_resource` | `CourseResourceEntity` | course | 教学课程资源表。 |
| `edu_training_plan` | `TrainingPlanEntity` | training | 培训计划表。 |
| `edu_training_record` | `TrainingRecordEntity` | training | 培训参与记录表。 |
| `edu_training_feedback` | `TrainingFeedbackEntity` | training | 培训反馈表。 |
| `edu_experiment_record` | `ExperimentRecordEntity` | experiment | 实验记录表。 |
| `research_project` | `ResearchProjectEntity` | research | 科研课题表。 |
| `rel_project_member` | `ProjectMemberEntity` | research | 课题成员关系表。 |
| `research_achievement` | `ResearchAchievementEntity` | research | 科研成果表。 |
| `eval_indicator` | `EvaluationIndicatorEntity` | evaluation | 评价指标表。 |
| `eval_task` | `EvaluationTaskEntity` | evaluation | 评价任务表。 |
| `eval_score_record` | `EvaluationScoreRecordEntity` | evaluation | 评价评分记录表。 |
| `eval_result` | `EvaluationResultEntity` | evaluation | 评价结果表。 |
| `eval_application` | `DeclarationEntity` | declaration | 评价申报表。 |
| `eval_review_record` | `DeclarationReviewRecordEntity` | declaration | 申报审核记录表。 |
| `eval_attachment` | `DeclarationMaterialEntity` | declaration | 申报附件表。 |
| `eval_archive` | `DeclarationArchiveEntity` | declaration | 申报档案袋表。 |
| `eval_archive_item` | `DeclarationArchiveItemEntity` | declaration | 申报档案材料项表。 |
| `perf_standard` | `PerformanceStandardEntity` | performance | 业绩认定标准表。 |
| `perf_record` | `PerformanceEntity` | performance | 业绩记录表。 |
| `perf_identification` | `PerformanceAuditEntity` | performance | 业绩认定记录表。 |
| `soap_sync_task` | `SoapSyncTaskEntity` | soap | SOAP 同步任务表。 |
| `soap_exchange_record` | `SoapExchangeRecordEntity` | soap | SOAP 交换记录表。 |
| `stat_dashboard_snapshot` | `DashboardSnapshotEntity` | dashboard | 首页看板统计快照表。 |

---

## 8. Mapper 类设计

Mapper 类统一继承 MyBatis Plus 的 `BaseMapper<Entity>`。关系表可以只提供 Mapper，不一定单独提供 Controller。

```java
public interface HerbMapper extends BaseMapper<HerbEntity> {
}
```

| 模块 | Mapper 类 |
| --- | --- |
| user | `UserMapper`、`RoleMapper`、`OrganizationMapper`、`DepartmentMapper`、`UserRoleMapper` |
| permission | `MenuMapper`、`PermissionMapper`、`RolePermissionMapper`、`DataScopeMapper` |
| dictionary | `DictTypeMapper`、`DictItemMapper`、`RegionMapper` |
| file | `FileResourceMapper`、`FileBusinessMapper` |
| audit | `LoginLogMapper`、`OperationLogMapper`、`DataChangeLogMapper`、`FileAccessLogMapper`、`DataSyncLogMapper` |
| herb | `HerbMapper`、`HerbSpeciesMapper`、`HerbImageMapper` |
| map | `HerbBaseMapper`、`MapPointMapper` |
| growth | `GrowthRecordMapper`、`GrowthAuditRecordMapper` |
| spectrum | `AiModelVersionMapper`、`SpectrumMapper`、`SpectrumTagMapper`、`ImageRecognitionMapper`、`SpectrumComparisonMapper`、`HerbAtlasMapper`、`HerbAtlasTagMapper`、`HerbAtlasFeatureMapper`、`HerbImageFeatureMapper`、`HerbImageMatchMapper`、`HerbIdentificationResultMapper` |
| collection | `HerbCollectionTaskMapper`、`HerbBatchMapper`、`HerbBatchImageMapper` |
| knowledge | `KnowledgeNodeMapper`、`KnowledgeRelationMapper`、`KnowledgeTripleMapper` |
| course | `CourseMapper`、`ExperimentStepMapper`、`CourseResourceMapper` |
| research | `ResearchProjectMapper`、`ProjectMemberMapper`、`ResearchAchievementMapper` |
| experiment | `ExperimentRecordMapper` |
| training | `TrainingPlanMapper`、`TrainingRecordMapper`、`TrainingFeedbackMapper` |
| evaluation | `EvaluationIndicatorMapper`、`EvaluationTaskMapper`、`EvaluationScoreRecordMapper`、`EvaluationResultMapper` |
| declaration | `DeclarationMapper`、`DeclarationReviewRecordMapper`、`DeclarationMaterialMapper`、`DeclarationArchiveMapper`、`DeclarationArchiveItemMapper` |
| performance | `PerformanceStandardMapper`、`PerformanceMapper`、`PerformanceAuditMapper` |
| soap | `SoapSyncTaskMapper`、`SoapExchangeRecordMapper` |
| dashboard | `DashboardSnapshotMapper` |

---

## 9. Controller 与 Service 设计总览

### 9.1 基础支撑模块

| 模块 | Controller | Service | 说明 |
| --- | --- | --- | --- |
| M01 身份认证 | `AuthController` | `AuthService` | 登录、退出、当前用户、Token 处理。 |
| M02 用户角色 | `UserController`、`RoleController`、`OrganizationController`、`DepartmentController` | `UserService`、`RoleService`、`OrganizationService`、`DepartmentService` | 用户、角色、组织、部门维护。 |
| M03 权限控制 | `MenuController`、`PermissionController`、`AuthorizationController`、`DataScopeController` | `MenuService`、`PermissionService`、`AuthorizationService`、`DataScopeService` | 菜单、权限点、权限判定、数据范围。 |
| M04 数据字典 | `DictTypeController`、`DictItemController`、`RegionController` | `DictTypeService`、`DictItemService`、`RegionService` | 字典类型、字典项、区域字典。 |
| M05 文件资源 | `FileResourceController`、`FileBusinessController` | `FileResourceService`、`FileBusinessService` | 上传、预览、下载、业务绑定。 |
| M06 操作审计 | `AuditLogController`、`LoginLogController`、`FileAccessLogController`、`DataSyncLogController` | `AuditLogService`、`LoginLogService`、`FileAccessLogService`、`DataSyncLogService` | 登录、操作、文件访问、数据同步日志查询。 |

### 9.2 中药材核心数据模块

| 模块 | Controller | Service | 说明 |
| --- | --- | --- | --- |
| M07 药材档案 | `HerbController`、`HerbImageController` | `HerbService`、`HerbImageService` | 药材主数据、药材图片。 |
| M08 基地与地图 | `HerbBaseController`、`MapPointController`、`HerbMapController` | `HerbBaseService`、`MapPointService`、`HerbMapService` | 基地、分布点、重庆地图展示数据。 |
| M09 生长采集 | `GrowthRecordController` | `GrowthRecordService` | PC、移动端、SOAP 来源的生长采集数据。 |
| M10 采集审核与溯源 | `GrowthAuditController`、`TraceController` | `GrowthAuditService`、`TraceService` | 提交、审核、退回、归档、溯源链路。 |
| M11 图谱知识 | `SpectrumController`、`ImageRecognitionController`、`SpectrumComparisonController`、`KnowledgeGraphController` | `SpectrumService`、`ImageRecognitionService`、`SpectrumComparisonService`、`KnowledgeGraphService` | 标准图谱、AI 识别、图谱比对、知识关系展示。 |

### 9.3 教学科研与管理业务模块

| 模块 | Controller | Service | 说明 |
| --- | --- | --- | --- |
| M12 实验课程 | `CourseController`、`ExperimentStepController`、`CourseResourceController` | `CourseService`、`ExperimentStepService`、`CourseResourceService` | 课程、步骤、课件、视频资源。 |
| M13 课题研究 | `ResearchProjectController`、`ResearchAchievementController` | `ResearchProjectService`、`ProjectMemberService`、`ResearchAchievementService` | 课题、成员、成果。 |
| M14 实验记录 | `ExperimentRecordController` | `ExperimentRecordService` | 实验过程、结果、附件、归档。 |
| M15 培训过程 | `TrainingPlanController`、`TrainingRecordController`、`TrainingFeedbackController` | `TrainingPlanService`、`TrainingRecordService`、`TrainingFeedbackService` | 培训计划、素材关联、参与记录、结果反馈。 |
| M16 评价管理 | `EvaluationIndicatorController`、`EvaluationTaskController`、`EvaluationScoreController`、`EvaluationResultController` | `EvaluationIndicatorService`、`EvaluationTaskService`、`EvaluationScoreService`、`EvaluationResultService` | 指标、任务、评分、结果确认。 |
| M17 申报档案 | `DeclarationController`、`DeclarationMaterialController`、`DeclarationArchiveController` | `DeclarationService`、`DeclarationReviewService`、`DeclarationMaterialService`、`DeclarationArchiveService` | 申报、审核、附件、档案袋。 |
| M18 业绩认定 | `PerformanceController`、`PerformanceStandardController`、`PerformanceAuditController` | `PerformanceService`、`PerformanceStandardService`、`PerformanceAuditService` | 业绩填报、标准、审核认定、统计。 |

### 9.4 集成与展示模块

| 模块 | Controller | Service | 说明 |
| --- | --- | --- | --- |
| M19 SOAP 数据交换 | `SoapExchangeController`、`SoapSyncTaskController` | `SoapExchangeService`、`SoapSyncTaskService`、`SoapImportService` | SOAP 任务、请求响应记录、XML 转换、业务导入。 |
| M20 首页看板 | `DashboardController` | `DashboardService` | 首页统计、待办、地图概览、快照缓存。 |

---

## 10. DTO、Query、VO 设计

### 10.1 通用 DTO

| 类名 | 包路径 | 职责 |
| --- | --- | --- |
| `IdDTO` | `common.core` | 单 ID 请求对象。 |
| `BatchIdDTO` | `common.core` | 批量 ID 请求对象。 |
| `StatusUpdateDTO` | `common.core` | 通用状态修改请求对象。 |
| `SortOrderDTO` | `common.core` | 排序调整请求对象。 |
| `ReviewDTO` | `common.core` | 通用审核请求对象，包含动作、意见、目标状态。 |
| `BusinessFileBindDTO` | `modules.file.dto` | 文件与业务对象绑定请求。 |

### 10.2 基础支撑 DTO/VO

| 模块 | DTO | Query | VO |
| --- | --- | --- | --- |
| auth | `LoginDTO`、`PasswordChangeDTO` | 无 | `LoginVO`、`CurrentUserVO`、`TokenVO` |
| user | `UserCreateDTO`、`UserUpdateDTO`、`RoleAssignDTO`、`RoleCreateDTO`、`RoleUpdateDTO`、`OrganizationCreateDTO`、`DepartmentCreateDTO` | `UserQuery`、`RoleQuery`、`OrganizationQuery`、`DepartmentQuery` | `UserListVO`、`UserDetailVO`、`RoleDetailVO`、`OrganizationTreeVO`、`DepartmentTreeVO` |
| permission | `MenuCreateDTO`、`PermissionCreateDTO`、`AuthorizationDecisionDTO`、`DataScopeSaveDTO` | `MenuQuery`、`PermissionQuery` | `MenuTreeVO`、`PermissionVO`、`AuthorizationDecisionVO`、`DataScopeVO` |
| dictionary | `DictTypeCreateDTO`、`DictItemCreateDTO`、`RegionCreateDTO` | `DictQuery`、`RegionQuery` | `DictTypeVO`、`DictItemVO`、`RegionTreeVO` |
| file | `FileUploadDTO`、`FileBusinessBindDTO` | `FileResourceQuery` | `FileResourceVO`、`FilePreviewVO`、`FileDownloadVO` |
| audit | 无 | `AuditLogQuery`、`LoginLogQuery`、`FileAccessLogQuery`、`DataSyncLogQuery` | `AuditLogVO`、`LoginLogVO`、`FileAccessLogVO`、`DataSyncLogVO` |

### 10.3 中药材核心 DTO/VO

| 模块 | DTO | Query | VO |
| --- | --- | --- | --- |
| herb | `HerbCreateDTO`、`HerbUpdateDTO`、`HerbImageUploadDTO` | `HerbQuery`、`HerbImageQuery` | `HerbListVO`、`HerbDetailVO`、`HerbImageVO` |
| map | `HerbBaseCreateDTO`、`MapPointCreateDTO`、`MapPointUpdateDTO` | `HerbBaseQuery`、`MapPointQuery` | `HerbBaseVO`、`MapPointVO`、`HerbMapVO` |
| growth | `GrowthRecordCreateDTO`、`GrowthRecordUpdateDTO`、`GrowthRecordSubmitDTO`、`GrowthAuditDTO`、`GrowthImportDTO` | `GrowthRecordQuery`、`GrowthAuditQuery` | `GrowthRecordListVO`、`GrowthRecordDetailVO`、`GrowthAuditRecordVO`、`TraceChainVO` |
| spectrum | `SpectrumCreateDTO`、`SpectrumTagDTO`、`ImageRecognitionDTO`、`SpectrumComparisonDTO` | `SpectrumQuery`、`ImageRecognitionQuery` | `SpectrumVO`、`ImageRecognitionVO`、`SpectrumComparisonVO` |
| knowledge | `KnowledgeNodeDTO`、`KnowledgeRelationDTO`、`KnowledgeTripleDTO` | `KnowledgeGraphQuery` | `KnowledgeNodeVO`、`KnowledgeRelationVO`、`KnowledgeGraphVO` |

### 10.4 教学科研 DTO/VO

| 模块 | DTO | Query | VO |
| --- | --- | --- | --- |
| course | `CourseCreateDTO`、`CourseUpdateDTO`、`ExperimentStepDTO`、`CourseResourceDTO` | `CourseQuery`、`CourseResourceQuery` | `CourseListVO`、`CourseDetailVO`、`ExperimentStepVO`、`CourseResourceVO` |
| research | `ResearchProjectCreateDTO`、`ResearchProjectUpdateDTO`、`ProjectMemberDTO`、`ResearchAchievementDTO` | `ResearchProjectQuery`、`ResearchAchievementQuery` | `ResearchProjectVO`、`ProjectMemberVO`、`ResearchAchievementVO` |
| experiment | `ExperimentRecordCreateDTO`、`ExperimentRecordUpdateDTO`、`ExperimentRecordArchiveDTO` | `ExperimentRecordQuery` | `ExperimentRecordListVO`、`ExperimentRecordDetailVO` |
| training | `TrainingPlanCreateDTO`、`TrainingPlanUpdateDTO`、`TrainingRecordDTO`、`TrainingFeedbackDTO` | `TrainingPlanQuery`、`TrainingRecordQuery` | `TrainingPlanVO`、`TrainingRecordVO`、`TrainingFeedbackVO` |
| evaluation | `EvaluationIndicatorDTO`、`EvaluationTaskCreateDTO`、`EvaluationScoreDTO`、`EvaluationResultConfirmDTO` | `EvaluationTaskQuery`、`EvaluationIndicatorQuery` | `EvaluationIndicatorVO`、`EvaluationTaskVO`、`EvaluationScoreRecordVO`、`EvaluationResultVO` |
| declaration | `DeclarationCreateDTO`、`DeclarationSubmitDTO`、`DeclarationReviewDTO`、`DeclarationMaterialDTO`、`DeclarationArchiveGenerateDTO` | `DeclarationQuery`、`DeclarationArchiveQuery` | `DeclarationVO`、`DeclarationDetailVO`、`DeclarationMaterialVO`、`DeclarationArchiveVO` |
| performance | `PerformanceCreateDTO`、`PerformanceSubmitDTO`、`PerformanceAuditDTO`、`PerformanceStandardDTO` | `PerformanceQuery`、`PerformanceStandardQuery` | `PerformanceVO`、`PerformanceDetailVO`、`PerformanceAuditVO`、`PerformanceStatisticsVO` |

### 10.5 集成展示 DTO/VO

| 模块 | DTO | Query | VO |
| --- | --- | --- | --- |
| soap | `SoapSyncTaskDTO`、`SoapExchangeDTO`、`SoapImportDTO`、`SoapRetryDTO` | `SoapSyncTaskQuery`、`SoapExchangeQuery` | `SoapSyncTaskVO`、`SoapExchangeRecordVO`、`SoapImportResultVO` |
| dashboard | 无 | `DashboardQuery` | `DashboardSummaryVO`、`DashboardTodoVO`、`DashboardMapVO`、`DashboardSnapshotVO` |

---

## 11. 核心 Service 方法设计

### 11.1 通用 CRUD 方法

普通资源型 Service 建议统一提供以下方法。

```java
PageResult<XxxListVO> page(XxxQuery query);

XxxDetailVO detail(Long id);

Long create(XxxCreateDTO dto);

void update(Long id, XxxUpdateDTO dto);

void delete(Long id);
```

### 11.2 状态流转方法

有审核、归档、提交、确认语义的模块，应单独提供业务方法，不使用万能 `handle` 方法。

| 场景 | 推荐方法 |
| --- | --- |
| 提交 | `submit(Long id, XxxSubmitDTO dto)` |
| 审核通过 | `approve(Long id, ReviewDTO dto)` |
| 审核退回 | `reject(Long id, ReviewDTO dto)` |
| 归档 | `archive(Long id, XxxArchiveDTO dto)` |
| 确认结果 | `confirm(Long id, XxxConfirmDTO dto)` |
| 重试同步 | `retry(Long id, SoapRetryDTO dto)` |

### 11.3 重点业务 Service 方法

| Service | 核心方法 | 说明 |
| --- | --- | --- |
| `AuthService` | `login`、`logout`、`currentUser` | 登录、退出、当前用户。 |
| `AuthorizationService` | `decide`、`checkPermission`、`resolveDataScope` | 权限判定和数据范围解析。 |
| `FileResourceService` | `upload`、`preview`、`download`、`remove` | 文件上传、预览、下载、删除。 |
| `FileBusinessService` | `bind`、`unbind`、`listByBusiness` | 文件和业务对象关联。 |
| `GrowthRecordService` | `createFromApp`、`createFromPc`、`createFromSoap`、`submit` | 生长数据来源统一入口。 |
| `GrowthAuditService` | `approve`、`reject`、`archive`、`history` | 采集记录状态流转。 |
| `TraceService` | `buildTraceChain` | 溯源链路查询。 |
| `SpectrumComparisonService` | `compare`、`listMatchResult` | 图谱比对结果管理。 |
| `KnowledgeGraphService` | `graphByHerb`、`saveTriple` | 知识图谱展示和三元组维护。 |
| `EvaluationResultService` | `calculate`、`confirm`、`snapshot` | 评价结果计算和冻结。 |
| `DeclarationArchiveService` | `generate`、`addItem`、`removeItem` | 申报档案袋生成和材料维护。 |
| `PerformanceAuditService` | `approve`、`reject`、`identify` | 业绩认定状态流转。 |
| `SoapImportService` | `parseXml`、`convertToImportDTO`、`importGrowthData` | SOAP 数据解析和业务导入。 |
| `DashboardService` | `summary`、`todo`、`mapOverview`、`snapshot` | 首页统计和快照。 |

---

## 12. 枚举类设计

枚举类统一使用 `Enum` 后缀。数据库中保存编码，前端展示中文名称通过字典或枚举描述获得。

| 枚举类 | 用途 | 示例值 |
| --- | --- | --- |
| `UserStatusEnum` | 用户状态 | `ENABLED`、`DISABLED` |
| `UserTypeEnum` | 用户类型 | `ADMIN`、`TEACHER`、`STUDENT`、`COLLECTOR`、`REVIEWER` |
| `DataScopeEnum` | 数据范围 | `ALL`、`ORGANIZATION`、`DEPARTMENT`、`SELF`、`CUSTOM` |
| `FileTypeEnum` | 文件类型 | `IMAGE`、`VIDEO`、`DOCUMENT`、`ATLAS`、`MATERIAL` |
| `StorageTypeEnum` | 文件存储方式 | `LOCAL`、`OSS`、`COS` |
| `ReviewStatusEnum` | 审核状态 | `DRAFT`、`SUBMITTED`、`APPROVED`、`REJECTED`、`ARCHIVED` |
| `ReviewActionEnum` | 审核动作 | `SUBMIT`、`APPROVE`、`REJECT`、`ARCHIVE` |
| `SourceTypeEnum` | 数据来源 | `PC`、`APP`、`SOAP`、`IMPORT`、`MANUAL` |
| `GrowthStageEnum` | 生长阶段 | `SEEDLING`、`GROWING`、`FLOWERING`、`FRUITING`、`MATURE` |
| `ImageTypeEnum` | 图片类型 | `WHOLE`、`LEAF`、`FLOWER`、`ROOT`、`PROCESSED` |
| `ImagePurposeEnum` | 图片用途 | `MAP_COVER`、`COLLECTION`、`RECOGNITION`、`ATLAS_CANDIDATE` |
| `ProcessStatusEnum` | 图片处理状态 | `PENDING`、`RECOGNIZING`、`RECOGNIZED`、`MATCHED` |
| `MatchLevelEnum` | 图谱匹配等级 | `HIGH`、`PARTIAL`、`LOW`、`INSUFFICIENT_ATLAS` |
| `PublishStatusEnum` | 发布状态 | `DRAFT`、`PUBLISHED`、`OFFLINE` |
| `TrainingStatusEnum` | 培训状态 | `NOT_STARTED`、`IN_PROGRESS`、`COMPLETED` |
| `EvaluationTaskStatusEnum` | 评价任务状态 | `DRAFT`、`SCORING`、`CONFIRMED`、`CLOSED` |
| `DeclarationStatusEnum` | 申报状态 | `DRAFT`、`SUBMITTED`、`APPROVED`、`REJECTED`、`ARCHIVED` |
| `PerformanceTypeEnum` | 业绩类型 | `RESEARCH`、`TEACHING`、`TRAINING`、`APPLICATION` |
| `PerformanceStatusEnum` | 业绩认定状态 | `DRAFT`、`SUBMITTED`、`APPROVED`、`REJECTED` |
| `SoapSyncStatusEnum` | SOAP 同步状态 | `PENDING`、`PROCESSING`、`SUCCESS`、`FAILED` |
| `SoapSyncDirectionEnum` | SOAP 同步方向 | `INBOUND`、`OUTBOUND` |

---

## 13. Converter 类设计

Converter 用于避免 Controller 或 Service 中堆积对象转换代码。简单场景可使用 MapStruct，也可手写静态方法。

| 模块 | Converter 类 |
| --- | --- |
| user | `UserConverter`、`RoleConverter`、`OrganizationConverter` |
| permission | `MenuConverter`、`PermissionConverter`、`DataScopeConverter` |
| file | `FileResourceConverter` |
| herb | `HerbConverter`、`HerbImageConverter` |
| map | `MapPointConverter`、`HerbBaseConverter` |
| growth | `GrowthRecordConverter`、`GrowthAuditConverter` |
| spectrum | `SpectrumConverter`、`SpectrumComparisonConverter` |
| knowledge | `KnowledgeGraphConverter` |
| course | `CourseConverter`、`CourseResourceConverter` |
| research | `ResearchProjectConverter`、`ResearchAchievementConverter` |
| training | `TrainingConverter` |
| evaluation | `EvaluationConverter` |
| declaration | `DeclarationConverter`、`DeclarationArchiveConverter` |
| performance | `PerformanceConverter` |
| soap | `SoapExchangeConverter` |
| dashboard | `DashboardConverter` |

---

## 14. 模块关键类设计

### 14.1 M01 身份认证模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `AuthController` | Controller | 登录、退出、当前用户查询。 |
| `AuthService` | Service | 定义认证业务。 |
| `AuthServiceImpl` | ServiceImpl | 校验账号密码、生成 Token、写登录日志。 |
| `LoginDTO` | DTO | 登录请求。 |
| `LoginVO` | VO | 登录结果，包含 Token 和用户摘要。 |
| `CurrentUserVO` | VO | 当前用户、角色、菜单、权限摘要。 |

### 14.2 M02 用户角色模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `UserController` | Controller | 用户增删改查、启停、角色分配。 |
| `RoleController` | Controller | 角色增删改查。 |
| `OrganizationController` | Controller | 机构维护。 |
| `DepartmentController` | Controller | 部门树维护。 |
| `UserService` | Service | 用户资料、状态、角色绑定。 |
| `RoleService` | Service | 角色资料和角色授权。 |
| `OrganizationService` | Service | 机构维护。 |
| `DepartmentService` | Service | 部门树维护。 |

### 14.3 M03 权限控制模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `MenuController` | Controller | 菜单维护和树形查询。 |
| `PermissionController` | Controller | 权限点维护。 |
| `AuthorizationController` | Controller | 权限判定接口。 |
| `DataScopeController` | Controller | 数据范围配置。 |
| `AuthorizationService` | Service | 菜单权限、按钮权限、接口权限和数据范围判断。 |
| `DataScopeService` | Service | 角色默认数据范围和资源级数据范围解析。 |

### 14.4 M04 数据字典模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `DictTypeController` | Controller | 字典类型维护。 |
| `DictItemController` | Controller | 字典项维护。 |
| `RegionController` | Controller | 区域树维护。 |
| `DictService` | Service | 字典统一查询能力，可由其他模块调用。 |

### 14.5 M05 文件资源模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `FileResourceController` | Controller | 文件上传、预览、下载、删除。 |
| `FileBusinessController` | Controller | 文件与业务对象绑定、解绑。 |
| `FileResourceService` | Service | 维护文件元数据，统一处理文件主数据。 |
| `FileBusinessService` | Service | 维护 `bizType + bizId + fileId` 关系。 |
| `FileStorageService` | Service | 抽象本地存储或对象存储。 |
| `LocalFileStorageServiceImpl` | ServiceImpl | 本地文件存储实现。 |

### 14.6 M06 操作审计模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `AuditLogController` | Controller | 操作日志和数据变更日志查询。 |
| `LoginLogController` | Controller | 登录日志查询。 |
| `FileAccessLogController` | Controller | 文件访问日志查询。 |
| `DataSyncLogController` | Controller | 同步日志查询。 |
| `AuditLogService` | Service | 写入和查询操作审计。 |
| `AuditAspect` | Component | 通过注解记录关键业务操作。 |
| `AuditLogAnnotation` | Annotation | 标记需要审计的方法。 |

### 14.7 M07 药材档案模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `HerbController` | Controller | 药材主数据增删改查。 |
| `HerbImageController` | Controller | 药材图片查询和上传关联。 |
| `HerbService` | Service | 药材档案维护、分类、详情查询。 |
| `HerbImageService` | Service | 药材图片元数据维护。 |

### 14.8 M08 基地与地图模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `HerbBaseController` | Controller | 基地信息维护。 |
| `MapPointController` | Controller | 地图点位增删改查。 |
| `HerbMapController` | Controller | 重庆中药材分布地图数据查询。 |
| `HerbBaseService` | Service | 基地业务。 |
| `MapPointService` | Service | 分布点业务。 |
| `HerbMapService` | Service | 地图展示数据聚合。 |

### 14.9 M09 生长采集模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `GrowthRecordController` | Controller | 生长记录新增、修改、查询、提交。 |
| `GrowthRecordService` | Service | 接收 PC、APP、SOAP 来源的生长数据。 |
| `GrowthRecordServiceImpl` | ServiceImpl | 校验药材、地图点位、采集人和文件关联。 |
| `GrowthRecordCreateDTO` | DTO | 生长采集新增请求。 |
| `GrowthImportDTO` | DTO | SOAP 或批量导入转换后的标准采集对象。 |
| `GrowthRecordDetailVO` | VO | 生长记录详情展示。 |

### 14.10 M10 采集审核与溯源模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `GrowthAuditController` | Controller | 提交、审核通过、退回、归档。 |
| `TraceController` | Controller | 溯源链路查询。 |
| `GrowthAuditService` | Service | 采集记录状态流转和审核历史。 |
| `TraceService` | Service | 组装采集记录、图片、审核、来源数据。 |
| `GrowthAuditDTO` | DTO | 审核动作和意见。 |
| `TraceChainVO` | VO | 溯源链路展示。 |

### 14.11 M11 图谱知识模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `SpectrumController` | Controller | 标准图谱维护。 |
| `ImageRecognitionController` | Controller | AI 识别结果查询。 |
| `SpectrumComparisonController` | Controller | 图谱比对结果查询。 |
| `KnowledgeGraphController` | Controller | 知识图谱关系展示。 |
| `SpectrumService` | Service | 标准图谱和标签维护。 |
| `ImageRecognitionService` | Service | AI 识别结果保存和查询。 |
| `SpectrumComparisonService` | Service | 图谱比对结果保存和查询。 |
| `KnowledgeGraphService` | Service | 节点、关系、三元组维护和图谱展示。 |

### 14.12 M12 实验课程模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `CourseController` | Controller | 实验课程维护和发布状态维护。 |
| `ExperimentStepController` | Controller | 实验步骤维护。 |
| `CourseResourceController` | Controller | 课件、视频、指导书等资源维护。 |
| `CourseService` | Service | 课程主数据业务。 |
| `ExperimentStepService` | Service | 实验步骤业务。 |
| `CourseResourceService` | Service | 课程资源业务。 |

### 14.13 M13 课题研究模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `ResearchProjectController` | Controller | 课题维护、成员维护。 |
| `ResearchAchievementController` | Controller | 科研成果维护。 |
| `ResearchProjectService` | Service | 课题、成员、周期、关联药材维护。 |
| `ProjectMemberService` | Service | 课题成员维护。 |
| `ResearchAchievementService` | Service | 成果维护和材料关联。 |

### 14.14 M14 实验记录模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `ExperimentRecordController` | Controller | 实验记录新增、修改、归档、查询。 |
| `ExperimentRecordService` | Service | 课程或课题下的实验过程和结果记录。 |

### 14.15 M15 培训过程模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `TrainingPlanController` | Controller | 培训计划维护。 |
| `TrainingRecordController` | Controller | 培训参与记录和结果维护。 |
| `TrainingFeedbackController` | Controller | 培训反馈维护。 |
| `TrainingPlanService` | Service | 培训计划和培训素材关联。 |
| `TrainingRecordService` | Service | 参与人员、进度、结果记录。 |
| `TrainingFeedbackService` | Service | 反馈评价记录。 |

培训素材不单独新建 `TrainingResourceEntity`。初版建议通过 `FileBusinessEntity` 绑定到 `TrainingPlanEntity`，即 `bizType = training_plan`，`bizId = planId`，`fileUsage = material`。

### 14.16 M16 评价管理模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `EvaluationIndicatorController` | Controller | 评价指标维护。 |
| `EvaluationTaskController` | Controller | 评价任务创建和状态维护。 |
| `EvaluationScoreController` | Controller | 评分记录维护。 |
| `EvaluationResultController` | Controller | 评价结果计算和确认。 |
| `EvaluationIndicatorService` | Service | 指标树和权重维护。 |
| `EvaluationTaskService` | Service | 评价任务业务。 |
| `EvaluationScoreService` | Service | 评分记录业务。 |
| `EvaluationResultService` | Service | 评价结果快照和确认。 |

`EvaluationResultEntity` 表示确认后的结果快照，不作为每次列表查询时实时重算的临时对象。

### 14.17 M17 申报档案模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `DeclarationController` | Controller | 申报创建、提交、详情查询。 |
| `DeclarationMaterialController` | Controller | 申报附件维护。 |
| `DeclarationArchiveController` | Controller | 申报档案袋生成和材料项维护。 |
| `DeclarationService` | Service | 申报主流程。 |
| `DeclarationReviewService` | Service | 申报审核历史。 |
| `DeclarationMaterialService` | Service | 申报附件和文件关联。 |
| `DeclarationArchiveService` | Service | 档案袋聚合和材料项维护。 |

### 14.18 M18 业绩认定模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `PerformanceController` | Controller | 业绩填报、提交、查询。 |
| `PerformanceStandardController` | Controller | 认定标准维护。 |
| `PerformanceAuditController` | Controller | 业绩审核认定。 |
| `PerformanceService` | Service | 业绩主数据和来源关联。 |
| `PerformanceStandardService` | Service | 分类分级标准维护。 |
| `PerformanceAuditService` | Service | 审核、退回、认定记录。 |

### 14.19 M19 SOAP 数据交换模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `SoapSyncTaskController` | Controller | SOAP 同步任务配置和重试。 |
| `SoapExchangeController` | Controller | SOAP 交换记录查询。 |
| `SoapSyncTaskService` | Service | 同步任务维护。 |
| `SoapExchangeService` | Service | 请求响应记录、错误信息记录。 |
| `SoapImportService` | Service | XML 解析、标准 DTO 转换、业务导入。 |
| `SoapClient` | Component | SOAP 请求客户端封装。 |
| `SoapXmlParser` | Component | SOAP XML 解析。 |

### 14.20 M20 首页看板模块

| 类名 | 类型 | 职责 |
| --- | --- | --- |
| `DashboardController` | Controller | 首页统计、待办、地图概览。 |
| `DashboardService` | Service | 聚合业务模块统计数据。 |
| `DashboardSnapshotService` | Service | 看板快照缓存维护。 |

---

## 15. 关键设计约束

### 15.1 文件资源主数据约束

文件主数据以 `FileResourceEntity` 为准，业务表中的 `fileUrl`、`imageUrl`、`thumbnailUrl` 只作为兼容字段或快速展示字段。新增和修改文件时，优先写入 `sys_file_resource` 和 `sys_file_business`，业务表只保存必要展示冗余。

### 15.2 数据权限约束

`RoleEntity.dataScope` 表示角色默认数据范围，`DataScopeEntity` 表示按资源类型配置的细粒度数据范围。权限判断时优先读取 `DataScopeEntity`，不存在资源级配置时再使用 `RoleEntity.dataScope`。

### 15.3 多态关联约束

`bizType + bizId`、`sourceType + sourceId` 不创建物理外键，由 Service 层统一校验。建议提供 `BusinessReferenceValidator` 组件，集中校验业务类型是否合法、业务 ID 是否存在、当前用户是否有访问权限。

### 15.4 评价结果快照约束

`EvaluationScoreRecordEntity` 是评分明细，`EvaluationResultEntity` 是确认后的评价结果快照。结果确认后应尽量保持稳定，申报档案读取确认结果，避免历史申报材料因评分规则变化而发生变化。

### 15.5 SOAP 导入约束

SOAP 模块不直接绕过业务 Service 写核心表。XML 解析后先转换为 `GrowthImportDTO`、`HerbCreateDTO` 等标准对象，再调用对应业务 Service 完成校验、入库和日志记录。

### 15.6 培训素材约束

培训素材通过文件资源模块统一管理。初版使用 `FileBusinessEntity` 将文件绑定到 `TrainingPlanEntity`，避免为培训单独新增重复文件表。

---

## 16. 实现优先级建议

### 16.1 第一阶段：P0 必做完整实现

| 模块 | 建议实现范围 |
| --- | --- |
| auth | Controller、Service、DTO、VO、Token 工具、登录日志。 |
| user | 用户、角色、组织、部门的完整 CRUD 和角色绑定。 |
| permission | 菜单、权限点、基础数据范围判断。 |
| dictionary | 字典类型、字典项、区域树。 |
| file | 文件上传、下载、预览、业务绑定。 |
| audit | 登录日志、操作日志、文件访问日志基础查询。 |
| herb | 药材主数据、图片关联。 |
| map | 基地、地图点位、重庆分布地图数据。 |
| growth | 生长采集新增、查询、提交。 |
| growth audit | 审核、退回、归档、溯源链。 |
| course | 课程、实验步骤、课程资源。 |
| dashboard | 首页统计和待办。 |

### 16.2 第二阶段：P1 推荐实现

| 模块 | 建议实现范围 |
| --- | --- |
| spectrum | 标准图谱、图谱标签、人工比对结果。 |
| knowledge | 知识节点、关系、三元组和图谱展示。 |
| research | 课题、成员、科研成果。 |
| experiment | 实验记录、实验结果、归档。 |
| training | 培训计划、素材关联、培训记录、反馈。 |
| evaluation | 评价指标、任务、评分、结果确认。 |
| declaration | 申报、审核、附件、档案袋。 |
| performance | 业绩填报、标准、审核认定。 |
| soap | SOAP 任务、交换记录、XML 导入。 |

### 16.3 第三阶段：扩展实现

| 能力 | 建议类 |
| --- | --- |
| 原生 APP 采集端 | `AppGrowthRecordController`、`AppUploadController` |
| IoT 实时采集 | `IotDeviceController`、`IotGrowthDataService` |
| 向量检索 | `VectorSearchService`、`SpectrumVectorService` |
| 自动备份 | `BackupTaskService`、`BackupRecordEntity` |
| 文件版本控制 | `FileVersionEntity`、`FileVersionService` |

---

## 17. 示例类结构

以下以生长采集模块为例，展示类之间的基本协作关系。

```text
GrowthRecordController
    ↓
GrowthRecordService
    ↓
GrowthRecordServiceImpl
    ├── HerbService
    ├── MapPointService
    ├── FileBusinessService
    ├── GrowthRecordMapper
    └── AuditLogService
```

示例方法设计：

```java
public interface GrowthRecordService {

    PageResult<GrowthRecordListVO> page(GrowthRecordQuery query);

    GrowthRecordDetailVO detail(Long id);

    Long create(GrowthRecordCreateDTO dto);

    Long createFromApp(GrowthRecordCreateDTO dto);

    Long createFromSoap(GrowthImportDTO dto);

    void update(Long id, GrowthRecordUpdateDTO dto);

    void submit(Long id, GrowthRecordSubmitDTO dto);
}
```

---

## 18. 类设计与数据库关系说明

1. 每张数据库表至少对应一个 `Entity` 和一个 `Mapper`。
2. 并非每张关系表都需要单独 Controller，例如 `UserRoleEntity`、`RolePermissionEntity`、`FileBusinessEntity` 可由所属业务 Service 内部维护。
3. P0 表建议完整生成 Controller、Service、Mapper、Entity、DTO、Query、VO。
4. P1 预留表可先生成 Entity、Mapper 和基础 Service，后续按功能进度补充 Controller 和 DTO/VO。
5. 文件、审核、申报、业绩、SOAP 等敏感业务必须调用审计服务写入操作日志。
6. 状态字段统一使用枚举或字典编码，避免在业务代码中散落魔法字符串。

---

## 19. 结论

本 Java 类设计以 20 个模块为边界，以数据库 58 张表为实体来源，以 RESTful 接口规范为 Controller 设计依据，以 Java 类命名规范为命名标准。整体采用 Spring Boot + MyBatis Plus 的典型分层结构，能够支持初版 P0 功能快速落地，同时为图谱知识、评价申报、培训过程、业绩认定和 SOAP 数据交换等 P1 模块保留稳定扩展空间。

初期开发时建议优先完成 P0 模块的完整闭环，不必一次性实现所有 P1 模块的业务代码。P1 模块可以先完成表结构、Entity、Mapper 和基础 Service，保证文档完整、代码结构稳定，并为后续迭代保留清晰入口。
