import type { NextConfig } from "next";

const basePath = process.env.NEXT_PUBLIC_BASE_PATH || "";
const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  output: "standalone",
  basePath: basePath || undefined,
  async rewrites() {
    // 로컬 개발: /api → back. EC2(nginx)에서는 /allohub/api 를 nginx가 직접 back으로 프록시.
    if (basePath) {
      return [];
    }
    return [
      {
        source: "/api/:path*",
        destination: `${backendUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
