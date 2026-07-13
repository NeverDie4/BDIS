import type { RouteMeta } from "./types";

export const authAdminRoutes: RouteMeta[] = [
  {
    path: "/dashboard",
    title: "后台首页",
    owner: "auth",
    permission: "auth:dashboard:view",
  },
  {
    path: "/dashboard/auth",
    title: "权限后台",
    owner: "auth",
    permission: "auth:center:view",
  },
  {
    path: "/dashboard/users",
    title: "用户管理",
    owner: "auth",
    permission: "auth:user:view",
  },
  {
    path: "/dashboard/roles",
    title: "角色管理",
    owner: "auth",
    permission: "auth:role:view",
  },
  {
    path: "/dashboard/permissions",
    title: "菜单权限",
    owner: "auth",
    permission: "auth:permission:view",
  },
  {
    path: "/dashboard/organizations",
    title: "组织机构",
    owner: "auth",
    permission: "auth:organization:view",
  },
  {
    path: "/dashboard/departments",
    title: "部门管理",
    owner: "auth",
    permission: "auth:department:view",
  },
  {
    path: "/dashboard/audit",
    title: "操作审计",
    owner: "auth",
    permission: "audit:center:view",
  },
];
