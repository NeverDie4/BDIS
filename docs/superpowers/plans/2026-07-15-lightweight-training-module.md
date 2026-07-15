# Lightweight Training Module Implementation Plan

> **For agentic workers:** Implement task-by-task with verification after each task.

**Goal:** Replace the existing training surface with three lightweight modal workflows for plan viewing/administration, participation statistics, and participant feedback, while keeping the backend and database aligned to existing courses, research projects, bases, teachers, and students.

**Architecture:** Keep the existing teaching overview page and open three modal panels from its cards. Reuse existing training APIs where compatible, add only the missing resource-association, participation-statistics, and questionnaire behavior, and preserve role checks in the backend. Use existing Flyway/MyBatis conventions and do not touch unrelated dirty files.

**Tech Stack:** Next.js/React/Ant Design, TypeScript, Spring Boot, MyBatis-Plus, MySQL, Flyway.

---

### Task 1: Reconcile current training contracts

**Files:**
- Inspect: `frontend/src/components/teaching/TrainingOverviewPanel.tsx`
- Inspect: `frontend/src/components/teaching/TrainingManagementPanel.tsx`
- Inspect: `frontend/src/lib/training.ts`
- Inspect: `backend/src/main/java/com/bdis/modules/training/**`
- Inspect: `backend/src/main/resources/db/migration/V20260715_023__complete_training_workflow.sql`

- [ ] Confirm the existing response fields, permission names, and table columns before editing.
- [ ] Keep unrelated uncommitted changes outside the training files untouched.

### Task 2: Implement the three modal frontend workflows

**Files:**
- Modify: `frontend/src/components/teaching/TrainingOverviewPanel.tsx`
- Modify: `frontend/src/components/teaching/TrainingManagementPanel.tsx`
- Modify: `frontend/src/lib/training.ts`
- Modify: `frontend/src/components/teaching/teaching.module.css`
- Modify: `frontend/src/types/teaching.ts` only if shared types need alignment

- [ ] Open plan, record, and feedback modal panels from the existing cards.
- [ ] Keep plan creation admin-only; allow every role to view published plans.
- [ ] Limit plan fields to name, time, and associations with existing resources and participants.
- [ ] Show record counts/person-times and the current user's participation records.
- [ ] Allow only participants to submit one questionnaire per plan; show feedback summaries to all roles and detailed feedback only to authorized users.
- [ ] Remove or hide old UI actions for training materials, task progress, grades, reports, and completion proofs.

### Task 3: Align backend behavior

**Files:**
- Modify: `backend/src/main/java/com/bdis/modules/training/controller/**`
- Modify: `backend/src/main/java/com/bdis/modules/training/service/**`
- Modify: `backend/src/main/java/com/bdis/modules/training/request/**`
- Modify: `backend/src/main/java/com/bdis/modules/training/query/**`
- Modify: `backend/src/main/java/com/bdis/modules/training/vo/**`
- Test: `backend/src/test/java/com/bdis/modules/training/**`

- [ ] Enforce read access for all roles and create/update/delete access for administrators only.
- [ ] Build plan details from existing course, research-project, base, teacher, and student IDs.
- [ ] Generate participation records from selected participants without duplicate `(plan_id, user_id)` rows.
- [ ] Return aggregate participation counts and current-user records.
- [ ] Enforce feedback submission only for participants and one submission per participant per plan.

### Task 4: Add the minimal database migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V20260715_030__lightweight_training_module.sql`

- [ ] Add or adjust only association tables and questionnaire/statistics constraints required by the confirmed model.
- [ ] Preserve existing historical training data where possible.
- [ ] Add indexes for plan, user, role, and submitted time.
- [ ] Do not add training task, material, grade, report, or completion-proof fields beyond existing compatibility requirements.

### Task 5: Verify and review

- [ ] Run the frontend typecheck/build command used by the repository.
- [ ] Run focused backend training tests and the backend test/build command.
- [ ] Run migration validation if the project provides it.
- [ ] Inspect `git diff --stat` and `git status --short` to confirm only intended training files plus this plan changed.
- [ ] Report any pre-existing failures separately from failures introduced by this work.
