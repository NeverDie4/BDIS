"use client";

import axios from "axios";
import type { ApiResult } from "@/types/api";
import { buildLoginUrl, getCurrentRelativeUrl } from "./auth-navigation";
import { clearStoredToken, getStoredToken } from "./auth-token";
import { getOrCreateDeviceId } from "./device-id";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";
const AUTH_REDIRECT_FLAG = "__bdisAuthRedirect";

type AuthRedirectError = {
  [AUTH_REDIRECT_FLAG]?: true;
};

function isPasswordChangeRequired(error: unknown) {
  return (
    axios.isAxiosError<ApiResult<unknown>>(error) &&
    error.response?.status === 403 &&
    error.response.data?.code === "PASSWORD_CHANGE_REQUIRED"
  );
}

export const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    Accept: "application/json",
  },
});

request.interceptors.request.use((config) => {
  const deviceId = getOrCreateDeviceId();
  if (deviceId) {
    config.headers["X-Device-Id"] = deviceId;
  }
  const token = getStoredToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (isPasswordChangeRequired(error) && typeof window !== "undefined") {
      const securitySettingsUrl = "/settings?tab=security";
      if (`${window.location.pathname}${window.location.search}` !== securitySettingsUrl) {
        window.location.href = securitySettingsUrl;
      }
      return Promise.reject(error);
    }
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      clearStoredToken();
      if (typeof window !== "undefined" && window.location.pathname !== "/login") {
        (error as AuthRedirectError)[AUTH_REDIRECT_FLAG] = true;
        window.location.href = buildLoginUrl(getCurrentRelativeUrl());
      }
    }
    return Promise.reject(error);
  },
);

export function isAuthRedirectError(error: unknown) {
  return Boolean(
    error && typeof error === "object" && (error as AuthRedirectError)[AUTH_REDIRECT_FLAG],
  );
}

export async function apiGet<T>(url: string, params?: Record<string, unknown>) {
  const response = await request.get<ApiResult<T>>(url, { params });
  return response.data.data;
}

export async function apiPost<T>(url: string, data?: unknown) {
  const response = await request.post<ApiResult<T>>(url, data);
  return response.data.data;
}

export function getApiErrorMessage(error: unknown, fallback = "操作失败") {
  if (axios.isAxiosError<ApiResult<unknown>>(error)) {
    return error.response?.data?.message || error.message || fallback;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

export async function apiPut<T>(url: string, data?: unknown) {
  const response = await request.put<ApiResult<T>>(url, data);
  return response.data.data;
}

export async function apiPatch<T>(url: string, data?: unknown) {
  const response = await request.patch<ApiResult<T>>(url, data);
  return response.data.data;
}

export async function apiDelete<T>(url: string) {
  const response = await request.delete<ApiResult<T>>(url);
  return response.data.data;
}
