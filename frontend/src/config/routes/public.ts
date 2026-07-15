import type { RouteMeta } from "./types";

export const publicRoutes: RouteMeta[] = [
  { path: "/login", title: "登录", owner: "auth", public: true },
  { path: "/forbidden", title: "访问受限", owner: "platform" },
  { path: "/profile", title: "个人主页", owner: "auth" },
  { path: "/settings", title: "个人设置", owner: "auth" },
  {
    path: "/about",
    title: "关于我们",
    owner: "platform",
    public: true,
    navLabel: "关于我们",
    navOrder: 70,
  },
  {
    path: "/trace/growth/[traceCode]",
    title: "生长溯源",
    owner: "platform",
    public: true,
  },
  {
    path: "/trace/digital-life/[traceCode]",
    title: "中药材数字生命档案",
    owner: "platform",
    public: true,
  },
  {
    path: "/mobile/collect",
    title: "移动采集",
    owner: "mobile",
    public: true,
  },
];
