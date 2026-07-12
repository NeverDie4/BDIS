import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  output: process.platform === "win32" ? undefined : "standalone",
};

export default nextConfig;
