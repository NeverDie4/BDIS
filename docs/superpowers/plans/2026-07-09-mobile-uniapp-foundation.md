# Mobile Uniapp Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an isolated uni-app Vue 3 mobile project for herb collection staff with base routing, request wrappers, API modules, utilities, and static page skeletons.

**Architecture:** Create a new `mobile/` workspace package so it does not interfere with the existing Next.js PC frontend in `frontend/`. Keep backend URL and mobile API prefix centralized in `mobile/src/config/index.js`; pages use focused API modules rather than hardcoded full URLs.

**Tech Stack:** uni-app, Vue 3, Vite, JavaScript, pnpm workspace.

---

### Task 1: Workspace And Uni-App Package

**Files:**
- Create: `mobile/package.json`
- Modify: `pnpm-workspace.yaml`
- Modify: `package.json`
- Modify: `.gitignore`

- [ ] Add `mobile` as a workspace package.
- [ ] Add root scripts `dev:mobile` and `build:mobile:h5`.
- [ ] Add uni-app dependencies in `mobile/package.json`.
- [ ] Ignore mobile build artifacts `mobile/dist/` and `mobile/unpackage/`.

### Task 2: Core Uni-App Entrypoints

**Files:**
- Create: `mobile/src/main.js`
- Create: `mobile/src/App.vue`
- Create: `mobile/src/pages.json`
- Create: `mobile/src/manifest.json`

- [ ] Initialize Vue 3 with `createSSRApp`.
- [ ] Add common mobile styles for `.page`, `.card`, `.title`, `.row`, buttons, and empty states.
- [ ] Register all required routes and a three-item tabBar.
- [ ] Add minimal manifest metadata for H5 development.

### Task 3: Config, Request, APIs, And Utilities

**Files:**
- Create: `mobile/src/config/index.js`
- Create: `mobile/src/api/request.js`
- Create: `mobile/src/api/mobileTaskApi.js`
- Create: `mobile/src/api/mobileBatchApi.js`
- Create: `mobile/src/api/mobileImageApi.js`
- Create: `mobile/src/utils/storage.js`
- Create: `mobile/src/utils/user.js`
- Create: `mobile/src/utils/format.js`
- Create: `mobile/src/utils/constants.js`

- [ ] Centralize `baseUrl`, `mobilePrefix`, and `timeout`.
- [ ] Implement `request(options)` with loading, JSON headers, query/body support, and unified response handling.
- [ ] Implement `uploadFile(options)` with `uni.uploadFile`, fixed `file` field name, JSON parsing, and unified response handling.
- [ ] Encapsulate task, batch, and image result endpoints.
- [ ] Add local storage helpers, current collector defaults, status maps, and formatting helpers.

### Task 4: Static Page Skeletons

**Files:**
- Create: `mobile/src/pages/index/index.vue`
- Create: `mobile/src/pages/task/list.vue`
- Create: `mobile/src/pages/task/detail.vue`
- Create: `mobile/src/pages/batch/list.vue`
- Create: `mobile/src/pages/batch/create.vue`
- Create: `mobile/src/pages/batch/detail.vue`
- Create: `mobile/src/pages/image/upload.vue`
- Create: `mobile/src/pages/image/result.vue`
- Create: `mobile/src/pages/mine/index.vue`

- [ ] Add static mobile-friendly skeletons only.
- [ ] Add basic navigation buttons so routes can be opened.
- [ ] Show current collector information from the unified user helper on the mine page.
- [ ] Do not implement real list rendering, upload behavior, login, or backend changes.

### Task 5: Verification

**Files:**
- Read: `mobile/package.json`
- Read: `mobile/src/pages.json`

- [ ] Run dependency/build verification if uni-app dependencies are available.
- [ ] If dependencies are unavailable, report that `pnpm install` is required before H5 startup.
- [ ] Confirm API modules import successfully by checking static import paths.
