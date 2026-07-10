# 生物医药数字信息系统 Java 类命名规范

> 版本：V1.1
> 日期：2026-07-10
> 负责人：全组
> 本次更新：统一根包名，补充 Request、QueryRequest 与模块分包规则。

## 1. 编写目的

为保证生物医药数字信息系统在多人协作开发过程中的代码风格统一、类名清晰、职责明确，特制定本 Java 类命名规范。本文档只约定 Java 类的命名规则，不涉及具体 Java 类设计和业务实现。

本规范适用于后端项目中的 Controller、Service、Mapper、Entity、DTO、Request、VO、Query、Enum、Config、Exception、Constants、Utils 等 Java 类。

---

## 2. 总体命名原则

Java 类名统一采用 **大驼峰命名法**，即每个英文单词首字母大写。

推荐示例：

```java
HerbController
GrowthRecordService
FileResourceMapper
EvaluationTaskDTO
```

不推荐示例：

```java
herbController
herb_controller
Herbcontroller
Herb_Service
test1
```

类命名应遵循以下原则：

| 原则 | 说明 |
|---|---|
| 见名知意 | 看到类名即可判断类的大致职责 |
| 业务优先 | 类名优先体现业务对象，如 Herb、Course、GrowthRecord |
| 层次清晰 | 通过后缀区分 Controller、Service、Mapper、DTO 等层次 |
| 避免缩写 | 除常见缩写外，不随意缩写业务名称 |
| 避免无意义词 | 不使用 Test、Demo、New、Temp 等无业务含义词语 |

---

## 3. 类命名基础格式

推荐统一格式：

```text
业务对象名 + 层次后缀
```

示例：

```java
HerbController
HerbService
HerbServiceImpl
HerbMapper
HerbEntity
HerbCreateDTO
HerbDetailVO
```

对于复合业务对象，应使用完整业务含义：

```java
GrowthRecordController
EvaluationTaskService
DeclarationMaterialMapper
SoapExchangeLogEntity
```

---

## 4. 各类 Java 类命名规范

### 4.1 Controller 类

Controller 负责接收前端请求、进行参数校验、调用 Service 并返回结果。

命名格式：

```text
业务对象名 + Controller
```

推荐示例：

```java
HerbController
GrowthRecordController
FileResourceController
CourseController
EvaluationTaskController
PerformanceController
SoapExchangeController
```

不推荐示例：

```java
HerbApi
HerbAction
HerbControl
GetHerbController
AddHerbController
```

说明：Controller 应按资源或业务对象命名，不按具体动作命名。

---

### 4.2 Service 接口类

Service 表示业务逻辑接口。

命名格式：

```text
业务对象名 + Service
```

推荐示例：

```java
HerbService
GrowthRecordService
FileResourceService
EvaluationTaskService
PerformanceService
```

不推荐示例：

```java
IHerbService
HerbBusiness
HerbManager
HerbDoService
```

说明：本项目建议 Service 接口不加 `I` 前缀，保持命名简洁。

---

### 4.3 Service 实现类

Service 实现类统一使用 `Impl` 后缀。

命名格式：

```text
业务对象名 + ServiceImpl
```

推荐示例：

```java
HerbServiceImpl
GrowthRecordServiceImpl
FileResourceServiceImpl
EvaluationTaskServiceImpl
```

不推荐示例：

```java
HerbService2
HerbServiceNew
HerbServiceReal
HerbServiceTest
```

---

### 4.4 Mapper 类

Mapper 负责数据库访问，对应 MyBatis Plus 的数据访问层。

命名格式：

```text
业务对象名 + Mapper
```

推荐示例：

```java
HerbMapper
GrowthRecordMapper
FileResourceMapper
CourseMapper
PerformanceMapper
```

不推荐示例：

```java
HerbDao
HerbRepository
HerbSql
HerbDatabase
```

说明：本项目使用 MyBatis Plus，因此统一使用 `Mapper`，不混用 `Dao`、`Repository` 等名称。

---

### 4.5 Entity 实体类

Entity 对应数据库表结构。

命名格式：

```text
业务对象名 + Entity
```

推荐示例：

```java
HerbEntity
GrowthRecordEntity
FileResourceEntity
CourseEntity
EvaluationTaskEntity
PerformanceEntity
```

不推荐示例：

```java
HerbTable
HerbPOJO
HerbBean
HerbInfo
```

说明：为保证实训项目分层清晰，实体类建议统一加 `Entity` 后缀，避免与 DTO、VO 混淆。

---

### 4.6 DTO 类

DTO 用于前后端或服务层之间传递数据，常用于新增、修改、提交、导入等场景。

命名格式：

```text
业务对象名 + 场景 + DTO
```

推荐示例：

```java
HerbCreateDTO
HerbUpdateDTO
GrowthRecordCreateDTO
FileUploadDTO
EvaluationTaskCreateDTO
PerformanceSubmitDTO
SoapImportDTO
```

不推荐示例：

```java
HerbData
HerbParam
HerbInfo
HerbObject
```

说明：DTO 名称应尽量体现使用场景，例如 `CreateDTO`、`UpdateDTO`、`SubmitDTO`、`ImportDTO`。

#### 4.6.1 Request 类

Request 专用于 Controller 接收的 HTTP 请求，命名格式为：

```text
业务对象名 + 场景 + Request
```

推荐示例：

```java
HerbSpeciesCreateRequest
HerbAtlasQueryRequest
HerbIdentificationReviewRequest
```

DTO 与 Request 的边界如下：

1. 只服务于 HTTP 入参的对象使用 `Request`。
2. Service 之间传递、集成交换或可脱离 HTTP 使用的对象使用 `DTO`。
3. 同一个对象不得同时提供仅后缀不同、字段完全相同的 DTO 和 Request。
4. 现有 DTO 可兼容保留，修改相关接口时再按上述边界逐步统一，不做无业务收益的批量改名。

---

### 4.7 VO 类

VO 用于返回给前端展示的数据。

命名格式：

```text
业务对象名 + 场景 + VO
```

推荐示例：

```java
HerbListVO
HerbDetailVO
GrowthRecordDetailVO
DashboardSummaryVO
KnowledgeGraphVO
EvaluationTaskDetailVO
```

不推荐示例：

```java
HerbResult
HerbReturn
HerbShow
HerbPageData
```

说明：列表页和详情页展示字段不同，可以分别使用 `ListVO` 和 `DetailVO`。

---

### 4.8 Query 类

Query 用于封装查询条件，适合列表筛选、分页、搜索等场景。

命名格式：

```text
业务对象名 + Query
```

推荐示例：

```java
HerbQuery
GrowthRecordQuery
FileResourceQuery
CourseQuery
PerformanceQuery
AuditLogQuery
```

不推荐示例：

```java
HerbSearch
HerbCondition
HerbFilterParam
```

说明：可复用的业务查询对象使用 `Query`；只作为 HTTP 查询参数的对象可使用 `QueryRequest`。不混用 `Search`、`Condition`、`Filter`。

---

### 4.9 Enum 枚举类

枚举类用于表示固定状态、类型、级别、阶段等。

命名格式：

```text
业务含义 + Enum
```

推荐示例：

```java
UserStatusEnum
AuditStatusEnum
FileTypeEnum
GrowthStageEnum
EvaluationResultEnum
PerformanceLevelEnum
```

不推荐示例：

```java
UserStatus
StatusType
AuditType
FileKind
```

---

### 4.10 Config 配置类

配置类统一使用 `Config` 后缀。

命名格式：

```text
配置对象 + Config
```

推荐示例：

```java
WebMvcConfig
RedisConfig
SecurityConfig
MyBatisPlusConfig
FileStorageConfig
SwaggerConfig
```

不推荐示例：

```java
WebSetting
RedisManager
SecurityUtil
```

---

### 4.11 Exception 异常类

异常类统一使用 `Exception` 后缀。

命名格式：

```text
异常含义 + Exception
```

推荐示例：

```java
BusinessException
UnauthorizedException
ForbiddenException
ResourceNotFoundException
FileStorageException
SoapExchangeException
```

不推荐示例：

```java
ErrorInfo
BusinessError
FailException
WrongException
```

---

### 4.12 Constants 常量类

常量类统一使用 `Constants` 后缀。

命名格式：

```text
业务范围 + Constants
```

推荐示例：

```java
SystemConstants
FileConstants
SecurityConstants
RedisKeyConstants
AuditConstants
```

不推荐示例：

```java
Const
Common
StaticData
AllConstants
```

说明：不建议将所有常量都集中在一个 `CommonConstants` 中，应按业务或用途适当拆分。

---

### 4.13 Utils 工具类

工具类统一使用 `Utils` 后缀。

命名格式：

```text
工具对象 + Utils
```

推荐示例：

```java
DateUtils
FileUtils
JwtUtils
ExcelUtils
XmlUtils
```

不推荐示例：

```java
DateTool
FileHelper
CommonUtil
MyUtils
```

说明：工具类应只放无状态的通用方法，不应包含具体业务逻辑。

---

## 5. 推荐业务对象英文命名

为避免同一业务对象出现多种英文写法，本项目统一采用以下英文名称。

| 中文业务 | 推荐英文 |
|---|---|
| 药材 / 中药材 | Herb |
| 药材基地 | HerbBase |
| 地图点位 | MapPoint |
| 生长记录 | GrowthRecord |
| 生长指标 | GrowthIndicator |
| 采集审核 | GrowthAudit |
| 溯源记录 | TraceEvent |
| 文件资源 | FileResource |
| 图谱 / 样本图谱 | Spectrum |
| 图谱比对 | SpectrumComparison |
| 知识图谱 | KnowledgeGraph |
| 实验课程 | Course |
| 课题研究 | ResearchProject |
| 实验记录 | ExperimentRecord |
| 培训活动 | Training |
| 评价标准 | EvaluationStandard |
| 评价任务 | EvaluationTask |
| 评价记录 | EvaluationRecord |
| 申报档案 | Declaration |
| 申报材料 | DeclarationMaterial |
| 工作业绩 | Performance |
| 业绩审核 | PerformanceAudit |
| SOAP 交换 | SoapExchange |
| 操作日志 | AuditLog |
| 数据字典 | Dictionary |
| 首页看板 | Dashboard |

说明：本项目中的“中药材”统一命名为 `Herb`，不使用 `Medicine`，因为系统管理对象是中药材资源，而不是药品本身。

---

## 6. 命名示例

### 6.1 药材档案模块示例

```java
HerbController
HerbService
HerbServiceImpl
HerbMapper
HerbEntity
HerbCreateDTO
HerbUpdateDTO
HerbQuery
HerbListVO
HerbDetailVO
```

### 6.2 生长采集模块示例

```java
GrowthRecordController
GrowthRecordService
GrowthRecordServiceImpl
GrowthRecordMapper
GrowthRecordEntity
GrowthRecordCreateDTO
GrowthRecordUpdateDTO
GrowthRecordQuery
GrowthRecordListVO
GrowthRecordDetailVO
```

### 6.3 文件资源模块示例

```java
FileResourceController
FileResourceService
FileResourceServiceImpl
FileResourceMapper
FileResourceEntity
FileUploadDTO
FileResourceQuery
FileResourceVO
```

---

## 7. 命名禁忌

### 7.1 禁止使用无意义类名

不推荐：

```java
TestController
DemoService
DataManager
CommonHandler
MyUtils
NewService
TempController
```

### 7.2 禁止使用数字区分版本

不推荐：

```java
HerbService1
HerbService2
GrowthRecordControllerNew
FileUploadControllerOld
```

如果确实有版本差异，应通过包名、接口版本或分支管理，而不是在类名中乱加数字。

### 7.3 禁止中英文混合

不推荐：

```java
Herb药材Controller
药材Service
Growth采集Record
用户RoleMapper
```

推荐统一英文：

```java
HerbController
UserRoleMapper
GrowthRecordService
```

### 7.4 禁止层次后缀混乱

不推荐：

```java
HerbDao
HerbRepository
HerbMapper
```

本项目统一使用：

```java
HerbMapper
```

### 7.5 禁止一个类名承担多个职责

不推荐：

```java
HerbAndCourseAndFileController
SystemManageController
AllBusinessService
CommonBusinessService
```

如果类名中出现过多业务词，通常说明模块职责划分不清，应重新拆分。

---

## 8. 包命名建议

包名统一使用小写字母，不使用下划线，不使用大写。

推荐基础包名：

```java
com.bdis
```

推荐基础结构：

```text
com.bdis
├── common
├── config
└── modules
    └── 业务模块
        ├── controller
        ├── service
        │   └── impl
        ├── mapper
        ├── entity
        ├── dto
        ├── vo
        └── query
```

如果项目后期模块较多，也可以按业务模块分包：

```text
com.bdis.modules.herb
├── controller
├── service
├── mapper
├── entity
├── dto
└── vo
```

项目已采用按业务模块分包方式。新业务类统一放入 `com.bdis.modules.<module>`；`common` 仅保存真正跨模块的基础能力，顶层 `config` 保存全局配置。现存顶层业务包作为兼容代码逐步迁移，不再新增同类结构。

---

## 9. 最终简化规则

小组开发时重点遵守以下规则：

```text
Controller：业务名 + Controller
Service：业务名 + Service
Service 实现：业务名 + ServiceImpl
Mapper：业务名 + Mapper
Entity：业务名 + Entity
DTO：业务名 + 场景 + DTO
Request：业务名 + 场景 + Request
VO：业务名 + 场景 + VO
Query：业务名 + Query；HTTP 专用查询可使用 QueryRequest
Enum：业务名 + Enum
Config：配置名 + Config
Exception：异常名 + Exception
Constants：范围名 + Constants
Utils：工具名 + Utils
```

示例：

```java
HerbController
HerbService
HerbServiceImpl
HerbMapper
HerbEntity
HerbCreateDTO
HerbDetailVO
HerbQuery
AuditStatusEnum
FileStorageConfig
BusinessException
FileConstants
DateUtils
```

---

## 10. 文档说明

本项目 Java 类命名统一采用大驼峰命名法，类名应体现清晰的业务含义和所在层次。Controller、Service、Mapper、Entity、DTO、Request、VO、Query、Enum、Config、Exception、Constants、Utils 等不同类型的类应使用统一后缀进行区分。业务对象命名应保持一致，例如中药材统一命名为 Herb，生长记录统一命名为 GrowthRecord，文件资源统一命名为 FileResource，评价任务统一命名为 EvaluationTask。项目中禁止使用无意义类名、数字后缀、中英文混合命名和层次后缀混用。通过统一 Java 类命名规范，可以提高代码可读性，降低多人协作成本，并方便后续开发、测试和维护。
