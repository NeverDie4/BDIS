# BDIS

BDIS（Biomedicine Digital Information System，生物医药数字信息系统）是面向中药材科研、教学、培训与管理场景的信息系统。当前仓库以 2026-07-16 的集成冻结版本为基线：已完成用户会话与权限、药材资源与地图、生长采集审核与公开溯源、教学科研与培训、评价申报、业绩认定、文件审计、AI 服务接入及本地 SOAP 演示链路。功能范围、演示边界和文档优先级见 [09_冻结版功能范围与文档索引_V1.0_全组_20260716.md](docs/09_冻结版功能范围与文档索引_V1.0_全组_20260716.md)。

## 功能概括

系统目标是支撑中药材生长数据实时采集、存储、图谱比对、数据对比分析、溯源管理、线上教学、文件上传下载、自动备份与层级管理等能力。系统还需要支持手机 APP、电脑终端共享数据，并可与校内 SOAP 系统进行数据交换。

主要功能方向包括：

- 建立重庆市中药材分布网络地图，便于科研人员、学生和科研机构在线查询品种分布地点。
- 支持手机端与 Web 端协同采集、审核中药材生长数据，并通过二维码提供公开溯源与数字生命档案展示。
- 支持实验课程、科研课题、实验记录和培训计划的教学科研协同；具体业务权限与数据范围由后端统一校验。
- 支持评价任务、指标体系、申报档案与过程材料管理，为评价和申报场景沉淀可追溯资料。
- 支持工作业绩录入、参与人维护、佐证材料、审核认定、标准版本快照及分类统计。
- 提供仅用于本地演示的 SOAP 生长记录交换：默认关闭，不连接真实校内系统；演示时须显式开启 `BDIS_SOAP_MODE=mock`。

## 技术栈

- 前端：Next.js App Router、TypeScript、Ant Design、Zustand、TanStack Query、Axios、Leaflet。
- 后端：Java 21、Spring Boot、Spring MVC、Spring Security、MyBatis-Plus、Flyway、Redis、Apache CXF、SpringDoc OpenAPI。
- 数据：MySQL 8.0.x。
- 部署：Docker Compose + nginx。

## 项目结构

```text
frontend/          Next.js 前端工程
mobile/            uni-app 移动端工程（由移动端小组独立维护）
backend/           Spring Boot 后端工程
ai_service/        FastAPI 图像识别与特征提取服务
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
pnpm setup
# 修改 .env 中的数据库、Redis、Bootstrap Token 与 JWT 密钥
pnpm compose:up
```

Compose 会等待 MySQL、Redis 健康后启动后端，并由后端自动执行 Flyway 迁移。`ARK_API_KEY` 未配置时识别服务仍可启动并通过健康检查，但调用大模型识别接口会返回明确的配置错误。特征提取服务首次启动时会下载经过哈希校验的 ResNet50 权重，并保存到 `ai-model-cache` 卷供后续启动复用；因此首次启动需要能够访问 `download.pytorch.org`。

Compose 中 MySQL 的容器端口仍为 `3306`，宿主机默认通过 `127.0.0.1:3307` 访问，避免与本机 MySQL 冲突。需要其他端口时在 `.env` 中设置 `MYSQL_DOCKER_PORT`。

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

更多命令说明见 [docs/10_脚本命令使用指南_V1.2_全组_20260716.md](docs/10_脚本命令使用指南_V1.2_全组_20260716.md)。数据库设计见 [docs/05_数据库详细设计说明书_V1.0_全组_20260710.md](docs/05_数据库详细设计说明书_V1.0_全组_20260710.md)，迁移规则见 [docs/10_Flyway数据库迁移规则_V1.0_全组_20260708.md](docs/10_Flyway数据库迁移规则_V1.0_全组_20260708.md)。

鉴权接口的最新续期约定见 [docs/04_鉴权会话续期接口说明_V1.0_全组_20260716.md](docs/04_鉴权会话续期接口说明_V1.0_全组_20260716.md)；业绩认定、SOAP 演示的专项说明分别见 [业绩认定模块实施与接口说明](docs/04_业绩认定模块实施与接口说明_V1.0_全组_20260714.md) 和 [SOAP 模拟校内系统对接预备方案](docs/03_SOAP模拟校内系统对接预备方案_V1.0_全组_20260715.md)。

## 配置隔离

不同成员的数据库用户名、密码、端口等本地配置不应写死进代码，也不要提交 `.env`。

- Docker Compose 会自动读取项目根目录的 `.env`。
- 文件上传接口只接受 `private`，通过 `/api/files/{id}/content` 携带 Bearer Token 获取；文件必须先绑定业务对象，再由对应业务模块通过 Policy 校验后发布，才能使用 `/api/public-files/{id}/content` 匿名访问。`/api/files/uploads/**` 旧物理路径由安全配置显式拒绝，底层存储目录不直接暴露。
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
