import type { DataScope, FileAccessLog, LoginLog, OperationLog, Permission, Role, SystemUser } from "@/types/user";

export const dataScopes: Array<{ label: string; value: DataScope; description: string }> = [
  { label: "全部数据", value: "ALL", description: "可查看系统内全部业务数据。" },
  { label: "机构数据", value: "ORG", description: "可查看当前机构范围内数据。" },
  { label: "部门数据", value: "DEPARTMENT", description: "可查看所属部门及下级数据。" },
  { label: "本人数据", value: "SELF", description: "仅可查看本人创建或负责的数据。" },
  { label: "自定义数据", value: "CUSTOM", description: "按授权规则配置可见范围。" },
];

export const permissionTree: Permission[] = [
  {
    id: "perm-menu",
    permissionCode: "menu",
    permissionName: "菜单权限",
    permissionType: "menu",
    status: "normal",
    children: [
      {
        id: "perm-menu-herbs",
        permissionCode: "herb:species:list",
        permissionName: "中药材资源",
        permissionType: "menu",
        parentId: "perm-menu",
        status: "normal",
      },
      {
        id: "perm-menu-map",
        permissionCode: "map:distribution:view",
        permissionName: "分布地图",
        permissionType: "menu",
        parentId: "perm-menu",
        status: "normal",
      },
      {
        id: "perm-menu-growth",
        permissionCode: "growth:record:list",
        permissionName: "生长数据",
        permissionType: "menu",
        parentId: "perm-menu",
        status: "normal",
      },
    ],
  },
  {
    id: "perm-button",
    permissionCode: "button",
    permissionName: "按钮权限",
    permissionType: "button",
    status: "normal",
    children: [
      {
        id: "perm-button-herb-create",
        permissionCode: "herb:species:create",
        permissionName: "新增药材",
        permissionType: "button",
        parentId: "perm-button",
        status: "normal",
      },
      {
        id: "perm-button-growth-submit",
        permissionCode: "growth:record:submit",
        permissionName: "提交采集记录",
        permissionType: "button",
        parentId: "perm-button",
        status: "normal",
      },
    ],
  },
  {
    id: "perm-api",
    permissionCode: "api",
    permissionName: "接口权限",
    permissionType: "api",
    status: "normal",
    children: [
      {
        id: "perm-api-file-upload",
        permissionCode: "file:resource:upload",
        permissionName: "文件上传接口",
        permissionType: "api",
        parentId: "perm-api",
        status: "normal",
      },
      {
        id: "perm-api-course-list",
        permissionCode: "course:resource:list",
        permissionName: "课程资源查询接口",
        permissionType: "api",
        parentId: "perm-api",
        status: "normal",
      },
    ],
  },
];

export const roles: Role[] = [
  {
    id: "role-admin",
    roleCode: "SYSTEM_ADMIN",
    roleName: "系统管理员",
    dataScope: "ALL",
    permissions: ["herb:species:list", "growth:record:list", "file:resource:upload", "course:resource:list"],
    description: "负责用户、角色、权限、字典和系统配置。",
    status: "normal",
  },
  {
    id: "role-teacher",
    roleCode: "RESEARCH_TEACHER",
    roleName: "科研教师",
    dataScope: "DEPARTMENT",
    permissions: ["herb:species:list", "map:distribution:view", "course:resource:list"],
    description: "负责课程资料、科研项目和药材资源维护。",
    status: "normal",
  },
  {
    id: "role-collector",
    roleCode: "FIELD_COLLECTOR",
    roleName: "采集协作员",
    dataScope: "SELF",
    permissions: ["growth:record:list", "growth:record:submit"],
    description: "负责移动采集、生长记录暂存和提交。",
    status: "normal",
  },
];

export const users: SystemUser[] = [
  {
    id: "user-001",
    username: "admin",
    realName: "系统管理员",
    department: "信息化管理组",
    phone: "13800000001",
    email: "admin@bdis.local",
    roles: ["SYSTEM_ADMIN"],
    dataScope: "ALL",
    avatarUrl: "/mock/avatar/admin.png",
    status: "normal",
    lastLoginAt: "2026-07-09 08:45:00",
  },
  {
    id: "user-002",
    username: "teacher-chen",
    realName: "陈老师",
    department: "生物医药教研室",
    phone: "13800000002",
    email: "chen@bdis.local",
    roles: ["RESEARCH_TEACHER"],
    dataScope: "DEPARTMENT",
    avatarUrl: "/mock/avatar/teacher.png",
    status: "normal",
    lastLoginAt: "2026-07-09 09:10:00",
  },
  {
    id: "user-003",
    username: "collector-a",
    realName: "采集员A",
    department: "数字采集实训组",
    phone: "13800000003",
    roles: ["FIELD_COLLECTOR"],
    dataScope: "SELF",
    status: "normal",
    lastLoginAt: "2026-07-09 09:35:00",
  },
];

export const operationLogs: OperationLog[] = [
  {
    id: "op-001",
    operator: "陈老师",
    moduleName: "中药材资源",
    action: "编辑药材档案",
    targetName: "黄连",
    operationTime: "2026-07-09 09:20:00",
    ipAddress: "192.168.10.21",
    result: "成功",
    traceId: "TRACE-OP-001",
  },
  {
    id: "op-002",
    operator: "采集员A",
    moduleName: "生长数据",
    action: "提交采集记录",
    targetName: "GR-20260708-001",
    operationTime: "2026-07-09 09:38:00",
    ipAddress: "192.168.10.32",
    result: "成功",
    traceId: "TRACE-OP-002",
  },
];

export const loginLogs: LoginLog[] = [
  {
    id: "login-001",
    username: "admin",
    realName: "系统管理员",
    loginTime: "2026-07-09 08:45:00",
    ipAddress: "192.168.10.11",
    device: "Chrome / Windows",
    result: "成功",
  },
  {
    id: "login-002",
    username: "collector-a",
    realName: "采集员A",
    loginTime: "2026-07-09 09:35:00",
    ipAddress: "192.168.10.32",
    device: "Mobile H5 / Android",
    result: "成功",
  },
];

export const fileAccessLogs: FileAccessLog[] = [
  {
    id: "file-log-001",
    username: "teacher-chen",
    fileName: "药用植物标本识别课件.pptx",
    businessModule: "教学科研",
    accessType: "上传",
    accessTime: "2026-07-09 10:00:00",
    ipAddress: "192.168.10.21",
  },
  {
    id: "file-log-002",
    username: "collector-a",
    fileName: "黄连现场图片-001.jpg",
    businessModule: "生长数据",
    accessType: "上传",
    accessTime: "2026-07-09 10:15:00",
    ipAddress: "192.168.10.32",
  },
];
