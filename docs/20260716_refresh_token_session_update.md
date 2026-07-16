# Refresh Token 与会话生命周期更新

日期：2026-07-16

## 鉴权接口

- `POST /api/auth/sessions` 登录成功后返回 `accessToken`、`refreshToken`、`expiresIn`、`refreshExpiresIn`、`tokenType` 和用户信息。
- `accessToken` 默认有效期为 1440 分钟。
- `refreshToken` 默认有效期为 7 天。
- `POST /api/auth/sessions/refresh` 使用 refresh token 换取新的 access token 和 refresh token。
- Refresh token 每次刷新成功后都会轮换，旧 refresh token 立即失效。
- 登录接口和 refresh 接口本身不得触发自动刷新。
- 普通业务接口返回 401 时，客户端应先刷新 token 并重放原请求；refresh 明确返回 401 或没有 refresh token 时，才清理本地会话并跳转登录。
- `DELETE /api/auth/sessions/current` 在 access token 过期后可以先刷新再调用，以确保服务端 refresh 会话被撤销。

## 会话表字段

`auth_user_session` 新增字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `refresh_token_hash` | `CHAR(64)` | Refresh Token 的 SHA-256 哈希，不保存原文 |
| `refresh_expires_at` | `DATETIME` | Refresh Token 到期时间 |

新增索引：

```text
UNIQUE KEY uk_auth_user_session_refresh_token_hash (refresh_token_hash)
KEY idx_auth_user_session_refresh_expires_at (refresh_expires_at)
```

## 生命周期规则

- `expires_at` 表示 access token 到期时间。
- `refresh_expires_at` 表示完整登录会话可刷新的到期时间。
- 新数据应按 `refresh_expires_at` 判断会话是否过期；历史无 refresh 字段的数据才回退到 `expires_at`。
- Refresh token 轮换必须使用旧哈希作为更新条件，避免同一个旧 token 被并发重复消费。

## 安全债务

当前 Web 与移动端按项目既有方案把 refresh token 存放在本地存储。该方案会扩大 XSS 后的凭据有效期，后续应评估迁移到 HttpOnly Cookie 或 BFF 模式。
