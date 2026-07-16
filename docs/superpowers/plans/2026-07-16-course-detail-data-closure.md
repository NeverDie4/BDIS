# 课程详情数据闭环实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让课程元数据、先修课程、文件、视频、药材和课题关联从编辑保存到课程详情形成真实数据闭环，并让教师和管理员可按既有权限编辑。

**Architecture:** 课程基本元数据继续保存到 `edu_course` 现有文本列，服务层用 JSON 编解码列表字段。课程与药材、课题复用已存在的关系表，课程详情接口聚合资源、视频与关联项；前端按接口返回的类型分栏展示并在有 `edu:course:update` 权限时开启编辑入口。

**Tech Stack:** Spring Boot、MyBatis-Plus、Flyway 既有表结构、Next.js、TypeScript、Ant Design、JUnit、Node test。

---

### Task 1: 课程元数据 JSON 持久化与先修课程校验

**Files:**
- Modify: `backend/src/main/java/com/bdis/modules/course/service/impl/CourseServiceImpl.java`
- Modify: `backend/src/main/java/com/bdis/modules/course/vo/CourseDetailVO.java`
- Modify: `backend/src/main/java/com/bdis/modules/course/request/CourseCreateRequest.java`
- Modify: `backend/src/main/java/com/bdis/modules/course/request/CourseUpdateRequest.java`
- Test: `backend/src/test/java/com/bdis/modules/course/service/CourseServiceTest.java`

- [ ] **Step 1: 写出失败测试**

```java
request.setApplicableMajors(List.of("中药学"));
request.setPrerequisiteCourseIds(List.of(12L));
request.setTags(List.of("野外采集"));
courseService.update(11L, request);
assertThat(captured.getApplicableMajors()).isEqualTo("[\"中药学\"]");
assertThat(courseService.getDetail(11L).getTags()).containsExactly("野外采集");
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn.cmd -Dtest=CourseServiceTest test`

Expected: 元数据未写入实体或详情仍为空，测试失败。

- [ ] **Step 3: 最小实现**

```java
private String writeStringList(List<String> values) { return objectMapper.writeValueAsString(values == null ? List.of() : values); }
private List<String> readStringList(String value) { return StringUtils.hasText(value) ? objectMapper.readValue(value, STRING_LIST_TYPE) : List.of(); }
```

在 `create`、`update` 中写入适用专业、教学目标、教学方式、标签和先修课程 ID JSON；在 `toDetailVO` 中回填数组。先修 ID 必须对应启用、已发布且非当前课程的课程，否则抛出业务异常。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn.cmd -Dtest=CourseServiceTest test`

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/bdis/modules/course backend/src/test/java/com/bdis/modules/course/service/CourseServiceTest.java
git commit -m "fix: 持久化课程详情元数据"
```

### Task 2: 课程药材与课题关系接口

**Files:**
- Create: `backend/src/main/java/com/bdis/modules/course/request/CourseRelationUpdateRequest.java`
- Create: `backend/src/main/java/com/bdis/modules/course/vo/CourseRelationOptionVO.java`
- Modify: `backend/src/main/java/com/bdis/modules/course/mapper/CourseMapper.java`
- Modify: `backend/src/main/java/com/bdis/modules/course/service/CourseService.java`
- Modify: `backend/src/main/java/com/bdis/modules/course/service/impl/CourseServiceImpl.java`
- Modify: `backend/src/main/java/com/bdis/modules/course/controller/CourseController.java`
- Modify: `backend/src/main/java/com/bdis/modules/course/vo/CourseDetailVO.java`
- Test: `backend/src/test/java/com/bdis/modules/course/controller/CourseControllerTest.java`
- Test: `backend/src/test/java/com/bdis/modules/course/service/CourseServiceTest.java`

- [ ] **Step 1: 写出失败测试**

```java
mockMvc.perform(put("/courses/11/relations")
        .contentType("application/json")
        .content("{\"version\":0,\"speciesIds\":[21],\"projectIds\":[31]}"))
    .andExpect(status().isOk());
verify(authorizationService).requirePermission("edu:course:update");
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn.cmd -Dtest=CourseControllerTest,CourseServiceTest test`

Expected: 路由不存在或关系未回填，测试失败。

- [ ] **Step 3: 最小实现**

```java
@PutMapping("/{id}/relations")
public Result<CourseDetailVO> updateRelations(@PathVariable Long id, @Valid @RequestBody CourseRelationUpdateRequest request) {
    authorizationService.requirePermission("edu:course:update");
    return Result.success(courseService.updateRelations(id, request));
}
```

在同一事务中软删除该课程旧的 `rel_course_species` 与 `rel_project_course` 有效记录，再批量插入去重后的 ID；详情接口返回 `relatedHerbs`、`relatedProjects` 和供选择的启用药材、有效课题候选项。先调用已有课程归属校验，确保教师不能修改他人课程。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn.cmd -Dtest=CourseControllerTest,CourseServiceTest test`

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/bdis/modules/course backend/src/test/java/com/bdis/modules/course
git commit -m "feat: 支持课程药材课题关联"
```

### Task 3: 前端课程接口与详情映射

**Files:**
- Modify: `frontend/src/lib/courses.ts`
- Modify: `frontend/src/components/teaching/types.ts`
- Test: `frontend/tests/course-detail-data-closure.test.mjs`

- [ ] **Step 1: 写出失败测试**

```js
assert.match(source, /prerequisiteCourseIds/);
assert.match(source, /relatedHerbs/);
assert.match(source, /relatedProjects/);
assert.match(source, /videoResources/);
```

- [ ] **Step 2: 运行测试确认失败**

Run: `node --test frontend/tests/course-detail-data-closure.test.mjs`

Expected: 缺少关系类型或视频分栏映射，测试失败。

- [ ] **Step 3: 最小实现**

```ts
const videoResources = course.resources.filter((item) => item.resourceType === "video");
const fileResources = course.resources.filter((item) => item.resourceType !== "video");
```

扩展课程 API 类型与 `mapCourseDetail`：映射先修 ID、关联药材/课题、文件资源和视频资源，新增 `getCourseRelationOptions` 与 `updateCourseRelations` 请求函数。

- [ ] **Step 4: 运行测试确认通过**

Run: `node --test frontend/tests/course-detail-data-closure.test.mjs`

Expected: 所有断言通过。

- [ ] **Step 5: 提交**

```bash
git add frontend/src/lib/courses.ts frontend/src/components/teaching/types.ts frontend/tests/course-detail-data-closure.test.mjs
git commit -m "feat: 映射课程真实详情数据"
```

### Task 4: 课程编辑和详情交互

**Files:**
- Modify: `frontend/src/components/teaching/CourseEditorModal.tsx`
- Modify: `frontend/src/components/teaching/CourseDetailPanel.tsx`
- Modify: `frontend/src/components/teaching/TeachingPageClient.tsx`
- Modify: `frontend/src/components/teaching/TeachingWorkspace.tsx`
- Modify: `frontend/src/components/teaching/teaching.module.css`
- Test: `frontend/tests/course-detail-data-closure.test.mjs`

- [ ] **Step 1: 写出失败测试**

```js
assert.match(editor, /mode="multiple"/);
assert.match(editor, /mode="tags"/);
assert.match(detail, /编辑关联/);
assert.match(detail, /videoResources/);
assert.doesNotMatch(detail, /relatedCollections/);
```

- [ ] **Step 2: 运行测试确认失败**

Run: `node --test frontend/tests/course-detail-data-closure.test.mjs`

Expected: 编辑控件和真实分栏尚未出现，测试失败。

- [ ] **Step 3: 最小实现**

```tsx
<Select mode="multiple" options={prerequisiteOptions} placeholder="选择已发布先修课程" />
<Select mode="tags" tokenSeparators={[",", "，"]} placeholder="输入标签后按回车添加" />
```

课程资源标签只显示文件行，视频资源标签使用 `video` 控件和视频文件行。关联信息标签在 `canEdit` 为真时提供“编辑关联”按钮，弹窗加载候选药材和课题并保存 ID 集合；保存成功后通过页面回调刷新详情。无权限用户只读且不出现任何写操作。

- [ ] **Step 4: 运行测试确认通过**

Run: `node --test frontend/tests/course-detail-data-closure.test.mjs`

Expected: 所有断言通过。

- [ ] **Step 5: 提交**

```bash
git add frontend/src/components/teaching frontend/tests/course-detail-data-closure.test.mjs
git commit -m "feat: 完善课程详情编辑与资源展示"
```

### Task 5: 全量验证

**Files:**
- Verify only: `backend/src/test/java/com/bdis/modules/course/**`
- Verify only: `frontend/tests/course-detail-data-closure.test.mjs`

- [ ] **Step 1: 运行后端课程回归**

Run: `mvn.cmd -Dtest=CourseServiceTest,CourseControllerTest test`

Expected: `BUILD SUCCESS`。

- [ ] **Step 2: 运行前端类型、静态检查和构建**

Run: `cd frontend; .\node_modules\.bin\tsc.cmd --noEmit; pnpm.cmd lint; pnpm.cmd build`

Expected: 类型检查和构建成功；仅允许报告既有未修改文件的警告。

- [ ] **Step 3: 检查提交内容**

Run: `git diff --check; git status --short --branch`

Expected: 无空白错误，工作树只包含本任务预期内容。
