"use client";

import { LoginBrandPanel } from "@/components/login/LoginBrandPanel";
import {
  LoginFormPanel,
  type BootstrapFormValues,
  type LoginFormValues,
} from "@/components/login/LoginFormPanel";
import { resolvePostLoginPath } from "@/config/routes";
import { apiPost, getApiErrorMessage } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import type { CurrentUser, LoginResult } from "@/types/api";
import { useQueryClient } from "@tanstack/react-query";
import { App } from "antd";
import { useRouter } from "next/navigation";
import { useState } from "react";
import styles from "./LoginPage.module.css";

export default function LoginPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { message } = App.useApp();
  const setAuth = useAuthStore((state) => state.setAuth);
  const [loginLoading, setLoginLoading] = useState(false);
  const [bootstrapLoading, setBootstrapLoading] = useState(false);

  async function login(values: LoginFormValues) {
    if (loginLoading) return;

    setLoginLoading(true);
    try {
      const result = await apiPost<LoginResult>("/auth/sessions", values);
      queryClient.clear();
      setAuth(result.accessToken, result.refreshToken, result.user);
      message.success("登录成功");

      if (result.mustChangePassword || result.user.mustChangePassword) {
        router.replace("/settings?tab=security");
        return;
      }

      const returnUrl = resolvePostLoginPath(
        result.user,
        typeof window === "undefined"
          ? undefined
          : new URLSearchParams(window.location.search).get("returnUrl"),
        result.preferredLandingPath,
      );
      router.replace(returnUrl);
    } catch (error) {
      message.error(getApiErrorMessage(error, "登录失败"));
    } finally {
      setLoginLoading(false);
    }
  }

  async function bootstrap(values: BootstrapFormValues) {
    if (bootstrapLoading) return;

    setBootstrapLoading(true);
    try {
      await apiPost<CurrentUser>("/auth/bootstrap-admin", values);
      message.success("管理员已初始化，请使用该账号登录");
    } catch (error) {
      message.error(getApiErrorMessage(error, "管理员初始化失败"));
    } finally {
      setBootstrapLoading(false);
    }
  }

  return (
    <main className={styles.loginPage}>
      <LoginBrandPanel />
      <LoginFormPanel
        bootstrapLoading={bootstrapLoading}
        loginLoading={loginLoading}
        onBootstrapSubmit={bootstrap}
        onSubmit={login}
      />
    </main>
  );
}
