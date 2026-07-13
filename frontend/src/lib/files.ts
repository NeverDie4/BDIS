import axios from "axios";
import { request } from "@/lib/request";

export interface ApiResult<T> {
  code: string;
  message: string;
  data: T;
  timestamp: string;
}

export interface FileResource {
  id: number;
  fileNo: string;
  fileName: string;
  originalFilename?: string;
  fileType?: string;
  fileFormat?: string;
  fileSize?: number;
  fileUrl: string;
  thumbnailUrl?: string;
  storageType?: string;
  accessLevel?: "private" | "public";
  uploadedAt?: string;
}

export interface UploadFileOptions {
  bizType?: string;
  bizId?: number;
  fileUsage?: string;
  accessLevel?: "private" | "public";
}

export async function uploadFile(file: File, options?: UploadFileOptions) {
  const formData = new FormData();
  formData.append("file", file);
  if (options?.bizType) {
    formData.append("bizType", options.bizType);
  }
  if (options?.bizId) {
    formData.append("bizId", String(options.bizId));
  }
  if (options?.fileUsage) {
    formData.append("fileUsage", options.fileUsage);
  }
  if (options?.accessLevel) {
    formData.append("accessLevel", options.accessLevel);
  }

  const response = await request.post<ApiResult<FileResource>>("/files/upload", formData);
  return withBrowserFileUrl(response.data.data);
}

export async function deleteFileResource(fileId: number) {
  await request.delete(`/files/${fileId}`);
}

function withBrowserFileUrl(file: FileResource): FileResource {
  return {
    ...file,
    fileUrl: toBrowserUrl(file.fileUrl),
    thumbnailUrl: file.thumbnailUrl ? toBrowserUrl(file.thumbnailUrl) : file.thumbnailUrl,
  };
}

function toBrowserUrl(value: string) {
  if (/^https?:\/\//i.test(value)) {
    return value;
  }

  const baseURL = request.defaults.baseURL ?? "";
  if (!baseURL) {
    return value;
  }

  const apiUrl = new URL(baseURL, window.location.origin);
  return `${apiUrl.origin}${value.startsWith("/") ? value : `/${value}`}`;
}

export function getFileRequestErrorMessage(error: unknown) {
  if (!axios.isAxiosError(error)) {
    return "文件上传失败，请稍后重试";
  }
  if (!error.response) {
    return "文件上传失败，无法连接后端服务";
  }
  if (error.response.status === 413) {
    return "文件上传失败，文件大小超过后端限制";
  }
  return `文件上传失败，后端返回 ${error.response.status}`;
}
