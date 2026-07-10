# Frontend Skills for BDIS

## Skill 1：本草视觉 Token 统一
用于统一颜色、字体、边框、圆角、阴影和间距。
必须优先检查 tokens.css、globals.css、Ant Design ConfigProvider theme。
目标风格：米白宣纸、草本绿、茶褐细线、温润学术感。
不得使用默认科技蓝和普通后台灰白风。

## Skill 2：AntD 本草化改造
所有业务模块优先使用 Ant Design 组件，如 Card、Button、Tag、Typography、Row、Col、Statistic、List、Table、Drawer、Modal、Tabs。
CSS Modules 只负责视觉定制。
取消重阴影，使用浅褐细线、内描边、宣纸背景和低饱和色。

## Skill 3：Hero 背景显影
用于调试背景图过淡、被遮罩覆盖、裁切不对。
必须检查 background-image 层级、linear-gradient、overlay、opacity、background-size、background-position。
左侧保证文字清晰，右侧保证药材图像可见。

## Skill 4：药材标本卡片
用于药材精选、药材展示栏、药材卡片。
卡片不走重阴影，使用浅褐边框、内描边、克制圆角、宣纸底色。
图片使用 object-fit: contain，缺图时显示优雅占位，不显示破图。

## Skill 5：抽象地图热力
用于首页分布概览。
不接真实地图 SDK，不用 Leaflet。
用 SVG 或抽象区域块模拟重庆地图。
根据区县药材分布数量 count 映射颜色深浅。
完整真实地图功能放在 /map 页面。

## Skill 6：Next Build 诊断
用于 build 卡住或 Codex 假性超时。
优先运行 lint 和 tsc。
完整 build 使用 pnpm exec next build --debug。
不要使用复杂 PowerShell ProcessStartInfo 包装脚本。
不要使用 $psi.ArgumentList.Add。
以 Route 表输出、返回命令提示符、退出码 0 判断 build 成功。