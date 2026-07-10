export type ID = string | number;

export type StatusType =
  | "normal"
  | "disabled"
  | "draft"
  | "published"
  | "pending"
  | "approved"
  | "rejected"
  | "archived";

export type PageResult<T> = {
  records: T[];
  total: number;
  pageNo: number;
  pageSize: number;
};

export type SelectOption = {
  label: string;
  value: string | number;
  disabled?: boolean;
};
