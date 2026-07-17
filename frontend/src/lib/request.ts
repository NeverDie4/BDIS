"use client";

import axios from "axios";
import type { AxiosError, InternalAxiosRequestConfig } from "axios";
import type { ApiResult, LoginResult } from "@/types/api";
import { buildLoginUrl, getCurrentRelativeUrl } from "./auth-navigation";
import {
  clearStoredToken,
  getStoredRefreshToken,
  getStoredToken,
  setStoredAuthTokens,
} from "./auth-token";
import { getOrCreateDeviceId } from "./device-id";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";
const AUTH_REDIRECT_FLAG = "__bdisAuthRedirect";
const REQUEST_TIMEOUT_MESSAGE = "请求仍在处理中，请稍后查看结果";
const REQUEST_TIMEOUT_PATTERN = /timeout|timed out|time out|超时|请求时间过长|响应时间过长/i;

type AuthRedirectError = {
  [AUTH_REDIRECT_FLAG]?: true;
};

type RetryableRequestConfig = InternalAxiosRequestConfig & {
  _authRetry?: boolean;
};

let refreshPromise: Promise<string> | null = null;

function isPasswordChangeRequired(error: unknown) {
  return (
    axios.isAxiosError<ApiResult<unknown>>(error) &&
    error.response?.status === 403 &&
    error.response.data?.code === "PASSWORD_CHANGE_REQUIRED"
  );
}

function sanitizeTimeoutError(error: unknown) {
  if (!isRequestTimeoutError(error)) {
    return;
  }
  if (error instanceof Error) {
    error.message = REQUEST_TIMEOUT_MESSAGE;
  }
  if (axios.isAxiosError<ApiResult<unknown>>(error) && error.response?.data) {
    error.response.data.message = REQUEST_TIMEOUT_MESSAGE;
  }
}

export const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    Accept: "application/json",
  },
});

async function refreshAccessToken() {
  const refreshToken = getStoredRefreshToken();
  if (!refreshToken) {
    throw new Error("missing refresh token");
  }
  const deviceId = getOrCreateDeviceId();
  const response = await axios.post<ApiResult<LoginResult>>(
    `${API_BASE_URL}/auth/sessions/refresh`,
    { refreshToken },
    {
      timeout: 15000,
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
        ...(deviceId ? { "X-Device-Id": deviceId } : {}),
      },
    },
  );
  const session = response.data.data;
  setStoredAuthTokens(session.accessToken, session.refreshToken);
  return session.accessToken;
}

function redirectToLogin(error: unknown) {
  clearStoredToken();
  if (typeof window !== "undefined" && window.location.pathname !== "/login") {
    (error as AuthRedirectError)[AUTH_REDIRECT_FLAG] = true;
    window.location.href = buildLoginUrl(getCurrentRelativeUrl());
  }
}

function shouldRefreshAccessToken(config: RetryableRequestConfig | undefined) {
  if (!config || config._authRetry || !getStoredRefreshToken()) {
    return false;
  }
  const url = String(config.url ?? "");
  const method = String(config.method ?? "get").toLowerCase();
  return !(
    (method === "post" && url.includes("/auth/sessions")) ||
    url.includes("/auth/sessions/refresh") ||
    url.includes("/auth/bootstrap-admin")
  );
}

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
  async (error: unknown) => {
    sanitizeTimeoutError(error);
    if (isPasswordChangeRequired(error) && typeof window !== "undefined") {
      const securitySettingsUrl = "/settings?tab=security";
      if (`${window.location.pathname}${window.location.search}` !== securitySettingsUrl) {
        window.location.href = securitySettingsUrl;
      }
      return Promise.reject(error);
    }
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const config = error.config as RetryableRequestConfig | undefined;
      if (config && shouldRefreshAccessToken(config)) {
        config._authRetry = true;
        try {
          refreshPromise = refreshPromise ?? refreshAccessToken();
          const accessToken = await refreshPromise;
          config.headers.Authorization = `Bearer ${accessToken}`;
          return request(config);
        } catch (refreshError) {
          if ((refreshError as AxiosError).response?.status === 401) {
            redirectToLogin(error);
          }
          return Promise.reject(refreshError);
        } finally {
          refreshPromise = null;
        }
      }
      redirectToLogin(error);
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

export function isRequestTimeoutError(error: unknown) {
  if (axios.isAxiosError<ApiResult<unknown>>(error)) {
    const responseMessage = error.response?.data?.message;
    const message = [error.message, typeof responseMessage === "string" ? responseMessage : ""]
      .filter(Boolean)
      .join(" ");
    return (
      error.code === "ECONNABORTED"
      || error.code === "ETIMEDOUT"
      || error.response?.status === 408
      || error.response?.status === 504
      || REQUEST_TIMEOUT_PATTERN.test(message)
    );
  }
  if (error instanceof Error) {
    return error.name === "AbortError" || REQUEST_TIMEOUT_PATTERN.test(error.message);
  }
  return false;
}

export function getApiErrorMessage(error: unknown, fallback = "操作失败") {
  if (isRequestTimeoutError(error)) {
    return REQUEST_TIMEOUT_MESSAGE;
  }
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
