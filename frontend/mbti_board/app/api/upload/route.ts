import { NextRequest, NextResponse } from "next/server";
import sharp from "sharp";
import { S3Client, PutObjectCommand } from "@aws-sdk/client-s3";
import { randomUUID } from "crypto";

const ALLOWED_TYPES = new Set(["image/jpeg", "image/png", "image/gif", "image/webp"]);
const MAX_BYTES = 5 * 1024 * 1024;
const MAX_WIDTH = 1280;

// 모듈 레벨 싱글턴 — 커넥션 재사용
const r2 = new S3Client({
  region: "auto",
  endpoint: `https://${process.env.R2_ACCOUNT_ID}.r2.cloudflarestorage.com`,
  credentials: {
    accessKeyId: process.env.R2_ACCESS_KEY!,
    secretAccessKey: process.env.R2_SECRET_KEY!,
  },
});

function fail(message: string, status = 400) {
  return NextResponse.json({ success: false, message }, { status });
}

export async function POST(req: NextRequest) {
  let formData: FormData;
  try {
    formData = await req.formData();
  } catch {
    return fail("요청 형식이 올바르지 않습니다.");
  }

  const file = formData.get("file");
  if (!(file instanceof File)) return fail("파일이 없습니다.");
  if (!ALLOWED_TYPES.has(file.type)) return fail("지원하지 않는 형식입니다. (jpg·png·gif·webp만 허용)");
  if (file.size > MAX_BYTES) return fail("이미지 크기는 5MB를 초과할 수 없습니다.");

  try {
    const input = Buffer.from(await file.arrayBuffer());

    const processed = await sharp(input, { 
      animated: true, // GIF/WebP 애니메이션 읽기 활성화
      limitInputPixels: false // 매우 큰 GIF 처리 시 필요할 수 있음
    })
      .resize({ 
        width: MAX_WIDTH, 
        withoutEnlargement: true 
      })
      .webp({ 
        quality: 85,
        // effort: 4 // 압축 효율 설정 (1~6, 높을수록 파일은 작아지지만 느림)
      })
      .toBuffer();

    const key = `images/${randomUUID()}.webp`;
    await r2.send(new PutObjectCommand({
      Bucket: process.env.R2_BUCKET!,
      Key: key,
      Body: processed,
      ContentType: "image/webp",
      ContentLength: processed.byteLength,
      CacheControl: "public, max-age=31536000, immutable", // 1년 캐싱
    }));

    const imageUrl = `${process.env.R2_PUBLIC_URL}/${key}`;

    // R2 업로드 성공 후 Java 백엔드에 PENDING 등록
    // 실패 시 즉시 삭제(cancelImages)도 동작하지 않음 — 단, env 미설정이 아니면 일시적 네트워크 오류가 원인이므로
    // 스케줄러가 24h 후 정리하는 대신 이미지가 영구적으로 남을 수 있음 (허용 가능한 수준의 trade-off)
    const backendUrl = process.env.BACKEND_INTERNAL_URL;
    const internalKey = process.env.INTERNAL_API_KEY;
    if (backendUrl && internalKey) {
      fetch(`${backendUrl}/api/images/pending`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-Internal-Key": internalKey },
        body: JSON.stringify({ url: imageUrl }),
      }).catch((e) => console.error("[upload] PENDING 등록 실패:", e));
    }

    return NextResponse.json({
      success: true,
      data: { url: imageUrl },
    });
  } catch {
    return fail("이미지 처리 중 오류가 발생했습니다.", 500);
  }
}
