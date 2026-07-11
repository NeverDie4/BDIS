import type { ID, StatusType } from "./common";

export type DataScope = "ALL" | "ORG" | "DEPARTMENT" | "SELF" | "CUSTOM";

export type Permission = {
  id: ID;
  permissionCode: string;
  permissionName: string;
  permissionType: "menu" | "button" | "api";
  parentId?: ID;
  children?: Permission[];
  status: StatusType;
};

export type Role = {
  id: ID;
  roleCode: string;
  roleName: string;
  dataScope: DataScope;
  permissions: string[];
  description?: string;
  status: StatusType;
};

export type SystemUser = {
  id: ID;
  username: string;
  realName: string;
  department: string;
  phone?: string;
  email?: string;
  roles: string[];
  dataScope: DataScope;
  avatarUrl?: string;
  status: StatusType;
  lastLoginAt?: string;
};

export type OperationLog = {
  id: ID;
  operator: string;
  moduleName: string;
  action: string;
  targetName: string;
  operationTime: string;
  ipAddress: string;
  result: "成功" | "失败";
  traceId: string;
};

export type LoginLog = {
  id: ID;
  username: string;
  realName: string;
  loginTime: string;
  ipAddress: string;
  device: string;
  result: "成功" | "失败";
};

export type FileAccessLog = {
  id: ID;
  username: string;
  fileName: string;
  businessModule: string;
  accessType: "预览" | "下载" | "上传" | "删除";
  accessTime: string;
  ipAddress: string;
};
