import { API } from "@/constants/apiEndpoints";

// File Next.js로 전달
export async function uploadImage(file: File): Promise<string> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await fetch(API.images.upload, { method: "POST", body: formData, credentials: "include" });
  if (!res.ok) throw new Error("이미지 업로드에 실패했습니다.");
  const json = await res.json();
  return json.data.url as string;
}
