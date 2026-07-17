# BDIS 答辩演示补充素材

请将以下 6 个文件放在本目录。文件名必须与清单完全一致；不要仅修改扩展名伪造文件类型。

| 文件名                                  | 系统中的中文名称               | 用途                   |
| --------------------------------------- | ------------------------------ | ---------------------- |
| `huanglian-observation-form.pdf`        | 黄连生长观测记录表.pdf         | 课程普通资源           |
| `huanglian-identification-demo.mp4`     | 黄连形态识别演示.mp4           | 课程本地视频           |
| `huanglian-experiment-report.pdf`       | 黄连三阶段观察实验报告.pdf     | 学生实验报告、申报材料 |
| `huanglian-stage-comparison.png`        | 黄连阶段影像对比.png           | 课题过程材料、申报材料 |
| `huanglian-research-summary.pdf`        | 黄连数字化研究过程摘要.pdf     | 课题、申报和业绩佐证   |
| `huanglian-digital-archive-summary.pdf` | 黄连全生命周期数字档案摘要.pdf | 业绩佐证               |

## 视频建议

- 时长 15–90 秒即可，现场只做播放展示。
- 建议使用 H.264 视频和 AAC 音频的 MP4，并在答辩用 Chrome 中预先试播。
- 不依赖外部视频网站，不放入真实个人信息、密钥或未授权内容。

## 补齐后执行

```bash
pnpm demo:defense:seed -- --assets-only
```

该命令只允许连接本机且库名以 `_dev` 结尾的 MySQL，会将文件复制到本地存储目录；如 Docker 后端正在运行，也会同步到容器存储卷。

## 当前答辩占位素材

- 四个 PDF 文件均复制自项目的竞品深度分析报告，仅用于现场附件打开演示。
- 阶段对比 PNG 使用项目黄连图集中的现有图片。
- MP4 由用户提供的屏幕录像 WebM 转码而来，编码为 H.264 视频和 AAC 静音音轨。
