"use client";

import { Button, Spin, Typography } from "antd";
import { RefreshCw } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { findRouteMeta, resolveAccess } from "@/config/routes";
import { buildLoginUrl, getCurrentRelativeUrl } from "@/lib/auth-navigation";
import { getStoredToken } from "@/lib/auth-token";
import { apiGet, getApiErrorMessage, isAuthRedirectError } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import type { CurrentUser } from "@/types/api";
import styles from "./RouteAccessBoundary.module.css";

export function RouteAccessBoundary({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const status = useAuthStore((state) => state.status);
  const user = useAuthStore((state) => state.user);
  const beginInitialization = useAuthStore((state) => state.beginInitialization);
  const setUser = useAuthStore((state) => state.setUser);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const [initializationError, setInitializationError] = useState<string>();
  const [retryVersion, setRetryVersion] = useState(0);
  const route = useMemo(() => findRouteMeta(pathname), [pathname]);
  const access = resolveAccess(route, status, user);

  const initializeSession = useCallback(async () => {
    const token = getStoredToken();
    if (!token) {
      clearAuth();
      return;
    }

    beginInitialization(token);
    setInitializationError(undefined);
    try {
      setUser(await apiGet<CurrentUser>("/auth/me"));
    } catch (error) {
      if (isAuthRedirectError(error)) {
        clearAuth();
        return;
      }
      setInitializationError(getApiErrorMessage(error, "登录状态加载失败"));
    }
  }, [beginInitialization, clearAuth, setUser]);

  useEffect(() => {
    if (status === "unknown") {
      void initializeSession();
    }
  }, [initializeSession, retryVersion, status]);

  useEffect(() => {
    if (
      status === "authenticated" &&
      user?.mustChangePassword &&
      pathname !== "/settings"
    ) {
      router.replace("/settings?tab=security");
      return;
    }
    if (access === "anonymous") {
      router.replace(buildLoginUrl(getCurrentRelativeUrl()));
    } else if (access === "forbidden" && pathname !== "/forbidden") {
      router.replace(`/forbidden?from=${encodeURIComponent(pathname)}`);
    }
  }, [access, pathname, router, status, user?.mustChangePassword]);

  if (route?.public) {
    return children;
  }

  if (initializationError && status === "unknown") {
    return (
      <AccessState title="暂时无法确认登录状态" description={initializationError}>
        <Button
          icon={<RefreshCw size={16} />}
          onClick={() => setRetryVersion((value) => value + 1)}
        >
          重新检查
        </Button>
      </AccessState>
    );
  }

  if (access !== "allowed") {
    return <AccessState title="正在核验访问权限" loading />;
  }

  return children;
}

function AccessState({
  title,
  description,
  loading = false,
  children,
}: {
  title: string;
  description?: string;
  loading?: boolean;
  children?: React.ReactNode;
}) {
  return (
    <main className={styles.statePage}>
      <section className={styles.stateContent}>
        <span className={styles.seal}>本草</span>
        {loading ? <Spin size="large" /> : null}
        <Typography.Title level={2}>{title}</Typography.Title>
        {description ? <Typography.Paragraph>{description}</Typography.Paragraph> : null}
        {children}
      </section>
    </main>
  );
}
