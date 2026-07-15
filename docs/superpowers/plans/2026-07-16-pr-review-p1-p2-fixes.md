# PR P1/P2 Review Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close all eight P1/P2 review findings with regression tests and forward-only database changes.

**Architecture:** Keep authorization in services, reuse existing role and ownership semantics, and use two new forward migrations for permission/index changes. Keep changes local to the affected modules and preserve current public API shapes.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, Flyway, MySQL 8, Node test runner.

---

### Task 1: Restore Flyway history and harden the baseline

**Files:**
- Rename: `backend/src/main/resources/db/migration/V20260715_026__complete_performance_recognition.sql`
- Rename: `backend/src/main/resources/db/migration/V20260715_027__forward_fix_performance_history_backfill.sql`
- Rename: `backend/src/main/resources/db/migration/V20260715_028__harden_performance_participant_uniqueness.sql`
- Modify: `backend/src/test/resources/flyway-dev-migration-baseline.txt`
- Modify: `backend/src/test/java/com/bdis/common/FlywayMigrationVersionTest.java`

- [ ] Add baseline assertions for `V20260714_018/019/020` and verify the test fails while renamed files remain.
- [ ] Restore the three exact names and bytes from `origin/dev`.
- [ ] Run `mvn.cmd -Dtest=FlywayMigrationVersionTest test` and the Node Flyway regression.

### Task 2: Enforce research and training read scopes

**Files:**
- Modify: `backend/src/main/java/com/bdis/modules/research/service/ResearchProjectSubmissionService.java`
- Modify: `backend/src/main/java/com/bdis/modules/research/service/impl/ResearchProjectSubmissionServiceImpl.java`
- Modify: `backend/src/main/java/com/bdis/modules/research/controller/ResearchProjectSubmissionController.java`
- Modify: `backend/src/main/java/com/bdis/modules/research/service/impl/ResearchProjectTaskServiceImpl.java`
- Modify: `backend/src/main/java/com/bdis/modules/research/service/impl/ResearchProjectServiceImpl.java`
- Modify: `backend/src/main/java/com/bdis/modules/training/service/impl/TrainingWorkflowServiceImpl.java`
- Test: research service tests and `TrainingWorkflowServiceTest.java`

- [ ] Add tests showing non-members/non-participants receive `ForbiddenException`, members/participants can read, and deleted submissions are excluded.
- [ ] Move submission list querying behind the service and add `isDeleted=0`.
- [ ] Apply project access checks to task lists, task member lists and review history.
- [ ] Apply training record access checks to evaluations, report versions and completion proof.
- [ ] Run the affected service tests.

### Task 3: Prevent learning-progress inflation

**Files:**
- Modify: `backend/src/main/java/com/bdis/modules/course/mapper/CourseLearningProgressMapper.java`
- Modify: `backend/src/main/java/com/bdis/modules/course/service/impl/CourseLearningProgressServiceImpl.java`
- Create: `backend/src/test/java/com/bdis/modules/course/service/CourseLearningProgressServiceTest.java`

- [ ] Add tests rejecting nonexistent/non-video video IDs and proving only valid step/resource progress contributes to completion.
- [ ] Add a mapper check for active course video resources and constrain completed-count SQL to valid course items.
- [ ] Run `CourseLearningProgressServiceTest`.

### Task 4: Restore returned report submission

**Files:**
- Modify: `backend/src/main/java/com/bdis/modules/experiment/constant/ExperimentArchiveStatus.java`
- Modify: `backend/src/main/java/com/bdis/modules/experiment/service/impl/ExperimentRecordVersionServiceImpl.java`
- Modify/Test: experiment service tests.

- [ ] Add tests for `returned -> submitted` through direct submit and version creation.
- [ ] Allow returned status and call `submitByIdAndVersion` after version insert in the same transaction.
- [ ] Run affected experiment tests.

### Task 5: Split review permission and allow repeat training

**Files:**
- Modify: `backend/src/main/java/com/bdis/modules/research/controller/ResearchProjectController.java`
- Create: `backend/src/main/resources/db/migration/V20260716_005__split_project_submit_review_permission.sql`
- Create: `backend/src/main/resources/db/migration/V20260716_006__allow_repeat_training_attendance.sql`
- Modify/Test: permission migration and Flyway regression tests.

- [ ] Add failing migration/controller assertions for the new permission and dropped unique index.
- [ ] Seed `research:project:submit-review` for ADMIN/RESEARCHER and switch only submit-review endpoint.
- [ ] Drop `uk_edu_training_record_plan_user` with an `information_schema` guarded forward migration.
- [ ] Run migration and controller tests.

### Task 6: Persist task relations

**Files:**
- Modify: `backend/src/main/java/com/bdis/modules/research/mapper/ResearchProjectTaskMapper.java`
- Modify: `backend/src/main/java/com/bdis/modules/research/service/impl/ResearchProjectTaskServiceImpl.java`
- Create: `backend/src/test/java/com/bdis/modules/research/service/ResearchProjectTaskServiceTest.java`

- [ ] Add a test verifying deduplicated course/species relation inserts after task creation.
- [ ] Add mapper insert methods and transactional loops with stable sort order.
- [ ] Run the task service test.

### Task 7: Bind and authorize completion proof files

**Files:**
- Create: `backend/src/main/java/com/bdis/modules/training/file/TrainingRecordFileBusinessAccessPolicy.java`
- Modify: `backend/src/main/java/com/bdis/modules/training/service/impl/TrainingWorkflowServiceImpl.java`
- Create/Test: training workflow and file policy tests.

- [ ] Add tests proving proof generation binds `completion_proof` to `edu_training_record` and participants can view it.
- [ ] Bind the imported private file and implement VIEW/ATTACH policy using record ownership and plan management.
- [ ] Run the affected tests.

### Task 8: Full verification

- [ ] Run all targeted tests from Tasks 1-7.
- [ ] Run `mvn.cmd test` in `backend`.
- [ ] Run Node migration regression tests.
- [ ] Run `git diff --check` and inspect the final scoped diff.
