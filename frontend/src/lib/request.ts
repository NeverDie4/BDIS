"use client";

import axios from "axios";
import type { ApiResult } from "@/types/api";
import { buildLoginUrl, getCurrentRelativeUrl } from "./auth-navigation";
import { clearStoredToken, getStoredToken } from "./auth-token";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";
const AUTH_REDIRECT_FLAG = "__bdisAuthRedirect";

type AuthRedirectError = {
  [AUTH_REDIRECT_FLAG]?: true;
};

export const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    Accept: "application/json",
  },
});

request.interceptors.request.use((config) => {
  const token = getStoredToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
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
