"use client";

import "@ant-design/v5-patch-for-react-19";
import { App, ConfigProvider } from "antd";
import zhCN from "antd/locale/zh_CN";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30_000,
            refetchOnWindowFocus: false,
          },
        },
      }),
  );

  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          borderRadius: 8,
          borderRadiusLG: 12,
          colorBgBase: "#fffaf2",
          colorBgContainer: "#fffaf2",
          colorBgLayout: "#f7f1e8",
          colorBorder: "#e8ddcc",
          colorError: "#b42318",
          colorInfo: "#1f6f78",
          colorPrimary: "#0f5f3b",
          colorSuccess: "#237a42",
          colorText: "#1f2a24",
          colorTextSecondary: "#6f766d",
          colorWarning: "#b7791f",
          fontFamily:
            '"Source Han Serif SC", "Songti SC", "Microsoft YaHei", system-ui, -apple-system, sans-serif',
        },
        components: {
          Button: {
            borderRadius: 8,
            colorPrimary: "#0f5f3b",
            colorPrimaryHover: "#0b4f31",
          },
          Card: {
            borderRadiusLG: 12,
            colorBgContainer: "#fffaf2",
          },
          Drawer: {
            colorBgElevated: "#fffaf2",
          },
          Tag: {
            borderRadiusSM: 8,
          },
        },
      }}
    >
      <App>
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
      </App>
    </ConfigProvider>
  );
}
