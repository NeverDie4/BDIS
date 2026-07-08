# BDIS

BDIS（Biomedicine Digital Information System，生物医药数字信息系统）是面向中药材科研、教学、培训与管理场景的信息系统。项目当前仅完成工程初始化，不包含业务类设计、数据库表结构和具体业务实现，这些内容由小组在后续详细设计确定后补充。

## 功能概括

系统目标是支撑中药材生长数据实时采集、存储、图谱比对、数据对比分析、溯源管理、线上教学、文件上传下载、自动备份与层级管理等能力。系统还需要支持手机 APP、电脑终端共享数据，并可与校内 SOAP 系统进行数据交换。

主要功能方向包括：

- 建立重庆市中药材分布网络地图，便于科研人员、学生和科研机构在线查询品种分布地点。
- 对接校内 SOAP 系统，并支持手机端采集中药材生长数据。
- 存储中药材试验课程、试验课题和培训素材，支撑教学、课题研究、成果转化和培训过程跟踪。
- 建立中药材评价体系，跟踪评价过程和结果，为非遗申请等申报场景沉淀素材。
- 支持工作业绩录入、审核、编辑、分级分类统计与认定，并支持标准动态更新。

## 技术栈

- 前端：Next.js App Router、TypeScript、Ant Design、Zustand、TanStack Query、Axios、Leaflet。
- 后端：Java 21、Spring Boot、Spring MVC、Spring Security、MyBatis-Plus、Flyway、Redis、Apache CXF、SpringDoc OpenAPI。
- 数据：MySQL 8.0.x。
- 部署：Docker Compose + nginx。

## 项目结构

```text
frontend/          Next.js 前端工程
backend/           Spring Boot 后端工程
flyway/            独立数据库迁移模块
deploy/nginx/      nginx 反向代理配置
docs/              项目需求与设计文档
```

## 本地开发

首次初始化建议：

```bash
pnpm install
pnpm setup
pnpm check
```

前端：

```bash
pnpm dev
```

后端需要 Java 21 和 Maven：

```bash
pnpm dev:api
```

Docker Compose：

```bash
pnpm compose:up
```

数据库迁移：

```bash
pnpm dev:infra
pnpm db:migrate
pnpm db:validate
```

提交前可运行：

```bash
pnpm validate
```

更多命令说明见 [docs/10_脚本命令使用指南_V1.0_全组_20260707.md](docs/10_脚本命令使用指南_V1.0_全组_20260707.md)。数据库迁移规则见 [docs/10_Flyway数据库迁移规则_V1.0_全组_20260708.md](docs/10_Flyway数据库迁移规则_V1.0_全组_20260708.md)。

## 配置隔离

不同成员的数据库用户名、密码、端口等本地配置不应写死进代码，也不要提交 `.env`。

- Docker Compose 会自动读取项目根目录的 `.env`。
- 直接运行后端时，Spring Boot 会尝试读取当前目录或上一级目录的 `.env`，也可以读取系统环境变量，例如 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_DATABASE`。
- Windows 可在 IntelliJ IDEA / VS Code 运行配置中填写环境变量，或使用 PowerShell 设置临时变量。
- Linux 可在 shell 中使用 `export MYSQL_USER=...`，或通过 IDE 运行配置注入。
- `.env.example` 只作为字段模板，真实 `.env` 已被 `.gitignore` 忽略。

后端配置示例：

```yaml
spring:
  datasource:
    username: ${MYSQL_USER:bdis}
    password: ${MYSQL_PASSWORD:change-me}
```

这种写法表示：优先读取环境变量，未设置时使用默认值。团队协作时建议每个人维护自己的 `.env` 或 IDE 环境变量。

## 跨平台约定

项目使用 `.editorconfig` 统一编码、缩进和换行。当前约定为 UTF-8、LF、空格缩进。Windows 开发者无需手动改成 CRLF，现代 IDE 会按 `.editorconfig` 保存文件，能减少跨平台换行差异。
