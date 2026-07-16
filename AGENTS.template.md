# BDIS AI Agent Rule Template

本文件是团队 AI 开发规则模板。请根据所使用的工具复制到对应位置后再使用，不要直接把个人规则文件提交到 Git。

## 1. 项目身份

- 项目名称：BDIS。
- 中文名称：生物医药数字信息系统。
- 技术栈：Next.js + TypeScript 前端，Spring Boot + Java 21 后端，MySQL 8.0.x，Redis，Docker Compose + nginx。

## 2. 工作原则

1. 先读后改：修改前先阅读相关文件、`README.md` 和 `docs` 中对应规范。
2. 小步修改：一次任务只处理明确目标，不做无关重构。
3. 保护成员改动：修改前检查 `git status`，不覆盖不属于当前任务的改动。
4. 配置隔离：不得提交 `.env`、`frontend/.env.local`、密钥、Token、个人路径或本地 IDE 配置。
5. 数据安全：用户私有数据必须使用 JWT/security context 中的当前用户身份校验，不信任客户端传入的 `userId`。
6. 权限优先：读取、修改、删除资源前必须校验资源所有权、数据范围或管理权限。
7. 跨平台优先：脚本优先使用 Node.js / pnpm，避免只适用于单一操作系统的写法。

## 3. 必读文档

执行任务前，按任务类型优先阅读：

- 项目概览：[README.md](README.md)
- 技术栈：[docs/03_技术栈详细设计规范_V1.0_全组_20260707.md](docs/03_技术栈详细设计规范_V1.0_全组_20260707.md)
- 文档命名：[docs/10_文档命名规范简化版_V1.0_全组_20260707.md](docs/10_文档命名规范简化版_V1.0_全组_20260707.md)
- 版本控制：[docs/10_版本控制规则说明_V1.0_全组_20260707.md](docs/10_版本控制规则说明_V1.0_全组_20260707.md)
- 脚本命令：[docs/10_脚本命令使用指南_V1.1_全组_20260710.md](docs/10_脚本命令使用指南_V1.1_全组_20260710.md)

## 4. 常用命令

所有命令默认在项目根目录执行。

```powershell
pnpm setup
pnpm check
pnpm validate
pnpm clean
pnpm dev
pnpm dev:api
pnpm dev:infra
pnpm compose:up
pnpm compose:down
```

如果只修改文档，可不强制运行完整构建，但应说明未运行的原因。

## 5. 允许做的事

- 修改 README、docs、配置模板、脚本说明等资料。
- 调整工程化配置、跨平台脚本、lint/build/test 命令。
- 修复项目启动、构建、依赖、环境变量读取等问题。
- 根据已确认的设计补充页面、接口、配置说明和测试说明。

## 6. 禁止擅自做的事

- 不得写死数据库账号、密码、Token、API Key 或个人绝对路径。
- 不得删除或重写他人成果。
- 不得把 `.env`、构建产物、日志、缓存提交到 Git。
- 不得为了通过检查而降低 lint、类型检查、测试或安全配置。
- 未经确认不得扩大需求范围或引入无关重构。

## 7. 前端约定

- 前端目录：`frontend/`。
- 使用 Next.js App Router、TypeScript、Ant Design。
- 环境变量模板：`frontend/.env.example`。
- 本地环境变量：`frontend/.env.local`，不得提交。
- API 地址优先读取 `NEXT_PUBLIC_API_BASE_URL`。

## 8. 后端约定

- 后端目录：`backend/`。
- 使用 Java 21、Spring Boot、Maven。
- 后端配置通过环境变量和 `.env` 注入，不在 `application.yml` 写个人账号密码。
- Flyway 迁移脚本必须和数据库设计文档保持一致。
- 涉及登录用户数据的接口必须进行身份、权限或数据范围校验。

## 9. 文档约定

- 正式文档放入 `docs/`。
- 过时或重复文档放入 `docs/deprecated/`。
- 文档命名格式：`编号_文档名称_V版本号_负责人_日期`。
- 不使用“最终版”“最新版”“新建文档”等模糊命名。

## 10. 输出要求

完成任务后说明：

1. 修改了哪些文件。
2. 为什么这样修改。
3. 运行了哪些检查命令及结果。
4. 是否有未完成事项、风险或需要小组确认的点。

如果任务失败，说明失败命令、失败原因和建议处理方式。
