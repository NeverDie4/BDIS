export type ProfileSettings = {
  userId: number;
  username: string;
  realName?: string;
  phoneNumber?: string;
  email?: string;
  avatarUrl?: string;
  organizationId?: number;
  organizationName?: string;
  departmentId?: number;
  departmentName?: string;
  roleCodes: string[];
  lastLoginAt?: string;
  passwordChangedAt?: string;
  mustChangePassword: boolean;
};

export type SettingNamespace<T extends object = Record<string, unknown>> = {
  version: number;
  schemaVersion: number;
  values: T;
};

export type UserSession = {
  sessionId: string;
  current: boolean;
  clientType: string;
  deviceName?: string;
  ipAddress?: string;
  issuedAt: string;
  lastActiveAt: string;
  expiresAt: string;
  status: "active" | "revoked" | "expired";
};

export type AppearanceSettings = {
  contentDensity: "comfortable" | "compact";
  sidebarMode: "auto" | "expanded" | "collapsed";
  reduceMotion: boolean;
};

export type CommonSettings = {
  defaultLandingPath: "/" | "/profile" | "/settings" | "/dashboard";
  timezone: string;
  locale: "zh-CN";
};

export type NotificationSettings = {
  siteEnabled: boolean;
  emailEnabled: boolean;
  taskReminderEnabled: boolean;
  reviewReminderEnabled: boolean;
  quietHoursEnabled: boolean;
  quietHoursStart: string;
  quietHoursEnd: string;
};
