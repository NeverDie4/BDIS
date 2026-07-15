import type { RouteMeta } from "./types";

export const businessRoutes: RouteMeta[] = [
  {
    path: "/",
    title: "首页",
    owner: "platform",
    permission: "dashboard:view",
    navLabel: "首页",
    navOrder: 10,
  },
  {
    path: "/herbs",
    title: "中药材资源",
    owner: "herb",
    permission: "herb:species:view",
    navLabel: "中药材资源",
    navOrder: 20,
  },
  {
    path: "/map",
    title: "分布地图",
    owner: "herb",
    permission: "map:point:view",
    navLabel: "分布地图",
    navOrder: 30,
  },
  {
    path: "/growth",
    title: "生长数据",
    owner: "herb",
    permission: "growth:record:view",
    navLabel: "生长数据",
    navOrder: 40,
  },
  {
    path: "/teaching",
    title: "教学科研",
    owner: "teaching",
    navLabel: "教学科研",
    navOrder: 50,
  },
  {
    path: "/evaluation",
    title: "评价申报",
    owner: "evaluation",
    navLabel: "评价申报",
    navOrder: 60,
  },
  {
    path: "/performance",
    title: "业绩认定",
    owner: "performance",
    permission: "performance:record:view",
    navLabel: "业绩认定",
    navOrder: 65,
  },
];
