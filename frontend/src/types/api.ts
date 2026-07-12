export type ApiResult<T> = {
  code: string;
  message: string;
  data: T;
  timestamp: string;
};

export type PageResult<T> = {
  records: T[];
  page: number;
  size: number;
  total: number;
};

export type CurrentUser = {
  userId: number;
  username: string;
  realName?: string;
  organizationId?: number;
  departmentId?: number;
  roleCodes: string[];
  roleIds: number[];
  permissions: string[];
  mustChangePassword?: boolean;
};

export type LoginResult = {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: CurrentUser;
  preferredLandingPath?: string;
  mustChangePassword?: boolean;
};

export type MenuItem = {
  id: number;
  parentId?: number;
  menuCode: string;
  menuName: string;
  routePath?: string;
  componentPath?: string;
  icon?: string;
  visible?: number;
  sortOrder?: number;
  status?: number;
  children?: MenuItem[];
};

export type Permission = {
  id: number;
  permissionCode: string;
  permissionName: string;
  permissionType: string;
  menuId?: number;
  apiPath?: string;
  requestMethod?: string;
  description?: string;
  status?: number;
};

export type Role = {
  id: number;
  roleCode: string;
  roleName: string;
  roleType?: string;
  dataScope?: string;
  description?: string;
  sortOrder?: number;
  status?: number;
};

export type User = {
  id: number;
  userNo: string;
  username: string;
  realName?: string;
  phoneNumber?: string;
  email?: string;
  organizationId?: number;
  departmentId?: number;
  status?: number;
  lastLoginAt?: string;
  mustChangePassword?: boolean;
  roles?: Role[];
};

export type Organization = {
  id: number;
  organizationNo: string;
  organizationName: string;
  organizationType?: string;
  contactName?: string;
  contactPhone?: string;
  address?: string;
  status?: number;
};

export type Department = {
  id: number;
  departmentNo: string;
  departmentName: string;
  organizationId: number;
  parentId?: number;
  sortOrder?: number;
  status?: number;
  children?: Department[];
};
