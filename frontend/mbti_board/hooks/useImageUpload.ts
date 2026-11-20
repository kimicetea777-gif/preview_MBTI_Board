"use client";

import { useState, useCallback } from "react";

// 본문에 이미지를 한 번이라도 올렸는지 — 작성 화면의 안내 표시용.
// 저장하지 않고 떠난 이미지는 따로 지우지 않는다: Image Server에 PENDING으로 남았다가 GC가 24시간 뒤 정리한다.
// (예전에는 언마운트 시 취소 요청을 보냈지만, 저장 요청 중 탭을 닫으면 저장된 글의 이미지까지 지우는 경합이 있어 제거.
//  설계: Document/image_async_confirm_plan.md §1.1)
export function useImageUpload() {
  const [hasImage, setHasImage] = useState(false);

  const handleUpload = useCallback(() => {
    setHasImage(true);
  }, []);

  return { hasImage, handleUpload };
}
