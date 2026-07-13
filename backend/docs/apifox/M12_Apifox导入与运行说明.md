# M12_Apifox 导入与运行说明

本目录中的文件基于当前 BDIS 后端真实代码生成，仅用于本地联调。生成过程未启动后端、未发送接口请求、未执行数据库写操作，也未修改 Java、Flyway、SecurityConfig 或前端代码。

## 文件

- `M12_实验课程真实联调.postman_collection.json`：Postman Collection v2.1，可直接导入 Apifox。
- `BDIS_本地环境.postman_environment.json`：本地环境变量。
- `M12_测试数据.json`：不包含真实账号、密码、Token、teacherId 或 fileId。

## 真实接口依据

- Spring context-path 是 `/api`，Controller 的类级路径是 `/courses`，因此外部路径是 `/api/courses`。
- 登录接口：`POST /api/auth/sessions`，请求字段只有 `username`、`password`。
- 登录成功响应中的 Token 路径是 `data.accessToken`。
- M12 权限由 Controller 中的 `AuthorizationService.requirePermission(...)` 校验，不是 `@PreAuthorize`。
- M12 业务成功响应均由 `Result.success(...)` 返回，当前代码实际 HTTP 状态为 200。
- Request 时间字段使用 `LocalDateTime`，示例格式为 `2026-07-11T09:00:00`，不带时区。

## 导入顺序

1. 打开 Apifox，选择项目。
2. 导入 `M12_实验课程真实联调.postman_collection.json`，格式选择 Postman Collection。
3. 导入 `BDIS_本地环境.postman_environment.json`，选择环境导入。
4. 在 Apifox 右上角环境下拉框选择 `BDIS 本地开发`。
5. 先填写环境变量，再按 Collection 的目录顺序执行。

## 必须手工填写的变量

- `username`：已有有效用户账号。
- `password`：对应账号密码。不要从项目 `.env` 读取或写入共享环境文件。
- `teacherId`：真实存在、未逻辑删除且 `status = 1` 的教师用户 ID。
- `fileId`：真实存在、未逻辑删除且 `status = 1` 的文件资源 ID。
- `limitedToken`：可选。手工准备一个有效但没有 M12 权限的普通账号 Token；Collection 不会自动生成或伪造它。

`token` 不需要手工填写。执行“用户登录”后置脚本会从 `data.accessToken` 自动保存。`courseNo`、`courseId`、`stepId`、`resourceId` 也会由流程脚本自动保存。

## 后端启动

在 `backend` 目录执行：

```powershell
mvn.cmd -DskipTests spring-boot:run
```

启动前确认 `FLYWAY_ENABLED=false`，避免本地启动意外执行迁移。也可以直接使用已经运行的本地后端。

## 推荐运行顺序

1. `01_环境检查`：确认服务可访问；未登录返回 401 也属于预期。
2. `02_身份认证`：填写账号密码，执行登录，再执行当前用户信息。
3. `03_课程基础`：创建课程、更新课程并验证重复课程编号。
4. `04_实验步骤`：创建并更新步骤；脚本会自动维护 `stepId` 和内部 `stepVersion`。
5. `05_课程资源`：先填写真实 `fileId`，再绑定资源并验证重复绑定。
6. `06_发布状态机`：按 draft → published → offline → published 顺序验证状态机。
7. `07_权限异常`：先执行无 Token 测试；填写 `limitedToken` 后再执行普通账号 403 测试。
8. `08_数据清理`：仅清理本次生成的课程、步骤和课程资源关系。该目录会删除课程业务数据，确认不再需要数据后再执行。

## 自动提取与断言

- 登录：提取 `data.accessToken` 到 `token`。
- 创建课程：提取 `data.id` 到 `courseId`，并断言初始 `publishStatus = draft`、`steps` 和 `resources` 为数组。
- 创建步骤：提取 `data.id` 到 `stepId`，从真实响应提取 `data.version` 到内部环境变量 `stepVersion`。
- 绑定资源：提取 `data.id` 到 `resourceId`，断言返回的 `fileId` 与环境变量一致，并只检查真实 VO 中存在的文件元数据字段。
- 统一成功断言：HTTP 200、`code = SUCCESS`、JSON 可解析。
- 统一失败断言：HTTP 状态、业务 `code` 和非空 `message` 同时检查。

## 当前后端错误映射

| HTTP | code | 当前含义 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 参数校验或普通业务校验失败 |
| 401 | `UNAUTHORIZED` | 未登录、Token 无效或账号无效 |
| 403 | `FORBIDDEN` | 缺少权限 |
| 404 | `NOT_FOUND` | 课程、步骤或资源不存在 |
| 409 | `CONFLICT` | 状态冲突、重复步骤/资源、乐观锁或已发布禁止修改 |
| 500 | `SYSTEM_ERROR` | 未处理系统异常 |

## 常见排查

- 401：检查登录请求是否误带旧的 `Authorization`；登录请求必须是 No Auth，成功后再由脚本保存 Token。
- 403：确认 Token 对应账号是否拥有 Controller 使用的权限编码，例如 `edu:course:add`、`edu:course-step:save`、`edu:course-resource:add`、`edu:course:publish`。
- 400：检查字段名称、必填项、数字字段和 LocalDateTime 格式；课程重复编号当前预期是 400 / `VALIDATION_ERROR`。
- 404：检查 `courseId`、`stepId`、`resourceId` 和 `teacherId` 是否来自当前数据库。
- 409：检查课程状态、重复步骤编号、重复资源绑定、步骤 `version` 是否过期。
- 500：查看后端日志；不要把 500 当成业务冲突的正常结果。

## 审计日志只读检查

完成联调后，可在具备只读权限的数据库客户端执行以下查询检查 M12 写操作审计。不要在 Apifox 脚本中执行 SQL：

```sql
SELECT id, operator_id, operation_module, operation_type, biz_type, biz_id,
       result_status, error_message, operation_time
FROM log_operation
WHERE biz_type IN ('edu_course', 'edu_experiment_step')
ORDER BY operation_time DESC, id DESC;
```

资源绑定还会调用 M05 文件业务绑定；解绑课程资源只删除业务关联，不删除 `sys_file_resource` 文件本体。

## 安全边界

- 环境文件中的 `username`、`password`、`token`、`limitedToken` 默认均为空。
- 不要把真实密码、数据库密码、JWT 密钥、初始化令牌或真实 Token 写入共享文件。
- 不要在 Collection 中自动读取 `.env`。
- 不要把 `limitedToken` 伪造成管理员 Token。
- 清理目录只操作本次课程及其业务关联，不会自动删除文件本体；运行前请确认 `courseId`、`stepId`、`resourceId` 指向本次联调数据。
- 该 Collection 不包含 M13、M14、M15 接口。

