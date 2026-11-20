import { API } from "@/constants/apiEndpoints";
import { IMAGE_UPLOAD_TIMEOUT_MS } from "@/constants/timeouts";

// File Next.js로 전달
// 제한시간이 지나면 fetch가 TimeoutError로 reject된다 — 호출부(RichContentEditor)의 catch가 자리표시 이미지를 지우고
// 업로드 중 카운트를 내려 주므로, 응답이 없어도 "업로드 중"으로 저장 버튼이 막힌 채 남지 않는다.
export async function uploadImage(file: File): Promise<{ id: string; url: string }> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await fetch(API.images.upload, {
    method: "POST",
    body: formData,
    credentials: "include",
    signal: AbortSignal.timeout(IMAGE_UPLOAD_TIMEOUT_MS),
  });
  if (!res.ok) throw new Error("이미지 업로드에 실패했습니다.");
  const json = await res.json();
  return json.data as { id: string; url: string };
}
