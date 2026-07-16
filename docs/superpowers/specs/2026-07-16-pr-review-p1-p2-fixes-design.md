# PR P1/P2 Review Fixes Design

## Goal

修复当前教学科研 PR 中的 Flyway 历史迁移破坏、数据范围越权、课程进度伪造、实验报告退回后无法重提、课题审核权限、培训重复参加、课题任务关联丢失及培训证明不可下载问题。

## Design

- 恢复 `origin/dev` 已发布的 `V20260714_018/019/020` 文件名和原始字节，并把它们纳入 Flyway SHA-256 基线；所有新数据库变化使用 `V20260716_005+`。
- 读取数据必须在 Service 层验证业务归属。课题数据允许管理员、负责人和有效成员读取；培训记录允许管理员、参与者、计划负责人和讲师读取。所有新增列表查询过滤逻辑删除行。
- `video` 是 `edu_course_resource.resource_type='video'` 的资源视图，不是独立进度实体。保存时校验课程和资源类型，汇总时只统计有效步骤和有效资源，避免任意或重复记录抬高完成度。
- `returned` 报告可以再次提交；创建报告版本视为一次重新提交，并原子更新实验主记录状态和提交人/时间。
- 新增 `research:project:submit-review`，授予 ADMIN 和 RESEARCHER；`research:project:review` 继续仅供 ADMIN/REVIEWER。
- 培训允许多次参加。删除遗留的 `(plan_id,user_id)` 唯一索引，继续使用每次参加唯一的 `attendance_no`。
- 课题任务创建事务同时写入 `rel_research_task_course` 和 `rel_research_task_species`，请求 ID 去重并保留顺序。
- 完成证明绑定到 `edu_training_record`，通过培训记录文件策略让参与者、计划负责人、讲师和管理员访问。

## Verification

每项先运行失败测试，再实施最小修复。完成后运行相关 JUnit、Node Flyway 回归、后端全量测试和 `git diff --check`。
