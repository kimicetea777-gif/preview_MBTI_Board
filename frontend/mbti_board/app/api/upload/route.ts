import { NextRequest, NextResponse } from "next/server";
import { ALLOWED_IMAGE_TYPES, ALLOWED_IMAGE_TYPES_LABEL, MAX_IMAGE_BYTES, MAX_IMAGE_BYTES_LABEL } from "@/constants/imageUpload";
import { IMAGE_SERVER_TIMEOUT_MS } from "@/constants/timeouts";

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
  if (!ALLOWED_IMAGE_TYPES.has(file.type)) return fail(`지원하지 않는 형식입니다. (${ALLOWED_IMAGE_TYPES_LABEL}만 허용)`);
  if (file.size > MAX_IMAGE_BYTES) return fail(`이미지 크기는 ${MAX_IMAGE_BYTES_LABEL}를 초과할 수 없습니다.`);

  try {
    const upstream = new FormData();
    upstream.append("file", file, file.name);

    // signal은 응답 본문(json()) 읽기까지 적용된다
    const upstreamRes = await fetch(`${process.env.IMAGE_SERVER_URL}/images`, {
      method: "POST",
      headers: { "X-Api-Key": process.env.IMAGE_SERVER_API_KEY ?? "" },
      body: upstream,
      signal: AbortSignal.timeout(IMAGE_SERVER_TIMEOUT_MS),
    });

    if (!upstreamRes.ok) return fail("이미지 업로드에 실패했습니다.", 500);

    const json = await upstreamRes.json();
    const { id, url } = json ?? {};
    if (!id || !url) return fail("이미지 업로드에 실패했습니다.", 500);

    return NextResponse.json({ success: true, data: { id, url } });
  } catch (error) {
    if (error instanceof DOMException && error.name === "TimeoutError") {
      return fail("이미지 서버 응답이 늦습니다. 잠시 후 다시 시도해 주세요.", 504);
    }
    return fail("이미지 처리 중 오류가 발생했습니다.", 500);
  }
}
