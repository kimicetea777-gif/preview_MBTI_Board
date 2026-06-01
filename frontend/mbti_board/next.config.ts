import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async redirects() {
    return [
      { source: "/board/free",   destination: "/board?type=FREE",   permanent: true },
      { source: "/board/vote",   destination: "/board?type=VOTE",   permanent: true },
      { source: "/board/debate", destination: "/board?type=DEBATE", permanent: true },
    ];
  },
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
