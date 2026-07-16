# BDIS AI Agent 使用说明

本文档说明团队如何统一使用 AI 开发规则。根目录中的 `AGENTS.template.md` 是可提交的团队模板；各工具真正读取的项目级或个人规则文件已被 `.gitignore` 忽略。模板以当前冻结基线为准，个人规则不得继续保留与模板冲突的“项目初始化中”等过时约束。

## 一、推荐做法

1. 团队共同维护 `AGENTS.template.md`。
2. 每个成员根据自己使用的 AI 工具复制模板到对应位置。
3. 个人可在本地规则文件中补充工具偏好，但不得提交。
4. 如果规则对全组都有价值，先更新 `AGENTS.template.md`，再通知成员合并同步，不直接覆盖个人工具偏好。

## 二、不同工具的使用方式

| 工具 | 建议做法 | 是否提交 |
| --- | --- | --- |
| OpenAI Codex | 优先将模板合并到项目根目录 `AGENTS.md`，供支持项目规则的 Codex 使用；全局规则可另放在 `~/.codex/AGENTS.md`，项目临时覆盖可使用 `AGENTS.override.md`。 | 不提交个人规则 |
| TRAE CN | 在 IDE 设置中创建规则，或把模板内容复制到项目 `.trae/rules/` 下的规则文件。 | 不提交 `.trae/rules/` |
| GitHub Copilot | 将模板内容复制到 `.github/copilot-instructions.md`。 | 当前不提交 |
| Claude 类工具 | 可复制为根目录 `CLAUDE.md`。 | 当前不提交 |
| 其他 Agent | 优先读取 `AGENTS.template.md`，再按工具要求复制到对应规则文件。 | 只提交模板 |

## 三、为什么不直接提交 AGENTS.md

不同 AI 工具读取规则文件的方式不同，且每个成员可能有自己的全局规则。如果直接提交真实规则文件，容易出现：

- 某个工具的规则影响其他成员。
- 临时实验规则被误提交。
- 团队规则和个人规则混在一起。
- 后续切换工具时难以维护。

因此本项目采用“模板提交、实际规则本地化”的方式。

## 四、快速开始

### OpenAI Codex

```bash
cp AGENTS.template.md AGENTS.md
```

已有本地 `AGENTS.md` 包含个人工具规则时，应手工合并模板，不要直接覆盖。全局通用偏好可单独维护在 `~/.codex/AGENTS.md`。如需项目临时覆盖，可复制一份：

```bash
cp AGENTS.template.md AGENTS.override.md
```

`AGENTS.override.md` 已被 `.gitignore` 忽略。

### GitHub Copilot

```bash
mkdir -p .github
cp AGENTS.template.md .github/copilot-instructions.md
```

`.github/copilot-instructions.md` 已被 `.gitignore` 忽略。若后续小组决定统一提交 Copilot 指令，需要先从 `.gitignore` 中移除该规则。

### TRAE CN

通过 IDE 设置界面创建规则，或手动创建：

```bash
mkdir -p .trae/rules
cp AGENTS.template.md .trae/rules/bdis-agent-rules.md
```

`.trae/rules/` 已被 `.gitignore` 忽略。

### Claude 类工具

```bash
cp AGENTS.template.md CLAUDE.md
```

`CLAUDE.md` 已被 `.gitignore` 忽略。

## 五、维护规则

当团队需要调整 AI 行为时：

1. 先修改 `AGENTS.template.md`。
2. 在提交说明中写明规则变更原因。
3. 通知成员手动同步到自己的项目级或工具规则文件；同步后检查当前阶段、文档索引和 Flyway 约束没有保留旧值。
4. 如果只是个人偏好，写在本地规则文件中，不修改模板。

## 六、当前忽略的 AI 规则文件

以下文件或目录属于个人/工具本地配置，已加入 `.gitignore`：

```text
AGENTS.md
AGENTS.override.md
CLAUDE.md
.github/copilot-instructions.md
.trae/rules/
```

如后续某个工具规则需要成为团队统一规范，应先讨论，再调整 `.gitignore`。
