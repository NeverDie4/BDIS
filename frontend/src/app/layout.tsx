import "antd/dist/reset.css";
import "leaflet/dist/leaflet.css";
import "./globals.css";
import type { Metadata } from "next";
import { Providers } from "./providers";
import { AssistantFloat } from "@/components/assistant-float";

export const metadata: Metadata = {
  title: "BDIS",
  description: "Biomedicine Digital Information System",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body>
        <Providers>
          {children}
          <AssistantFloat />
        </Providers>
      </body>
    </html>
  );
}
