import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Sharp는 네이티브 바이너리 — 번들에 포함하지 않고 런타임에 require
  serverExternalPackages: ["sharp"],

  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'pub-c18e48534e75482194072aa82c9bca99.r2.dev',
      },
    ],
  },
};

export default nextConfig;
