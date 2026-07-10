"use client";

import { App, ConfigProvider, unstableSetRender } from "antd";
import zhCN from "antd/locale/zh_CN";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";
import { createRoot, type Root } from "react-dom/client";

interface AntdReact19Container extends Element {
  _reactRoot?: Root;
}

unstableSetRender((node, container) => {
  const rootContainer = container as AntdReact19Container;
  rootContainer._reactRoot ??= createRoot(rootContainer);
  const root = rootContainer._reactRoot;

  root.render(node);

  return () =>
    new Promise<void>((resolve) => {
      setTimeout(() => {
        root.unmount();
        resolve();
      });
    });
});

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
          colorPrimary: "#1677ff",
          fontFamily:
            'Inter, "PingFang SC", "Microsoft YaHei", system-ui, -apple-system, sans-serif',
        },
        components: {
          Button: {
            borderRadius: 8,
          },
          Card: {
            borderRadiusLG: 8,
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
