import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",

  async headers() {
    return [
      {
        source: "/(.*)",
        headers: [
          { key: "X-Content-Type-Options",  value: "nosniff" }, //MIME 타입을 임의로 추측하는 것을 막음
          { key: "X-Frame-Options",          value: "SAMEORIGIN" }, //구형 브라우저용 Clickjacking 방지 (같은 도메인 iframe 허용) DENY : 모두 거부
          { key: "Content-Security-Policy",  value: "frame-ancestors 'self'" }, //최신 브라우저용 — 외부 도메인의 iframe 삽입 차단
          { key: "Referrer-Policy",          value: "strict-origin-when-cross-origin" },  //URL 정보 유출 → 토큰/경로 노출 방지
          { key: "Permissions-Policy",       value: "camera=(), microphone=(), geolocation=()" }, //XSS 이후 카메라·위치 권한 악용 방지
        ],
      },
    ];
  },

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
