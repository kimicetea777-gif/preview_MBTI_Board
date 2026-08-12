"use client";

import { useState, useRef, useEffect, useCallback } from "react";
import { API } from "@/constants/apiEndpoints";

export function useImageUpload() {
  const uploadedIdsRef = useRef<Set<string>>(new Set());
  const savedRef = useRef(false);
  const [hasImage, setHasImage] = useState(false);

  const handleUpload = useCallback((id: string) => {
    uploadedIdsRef.current.add(id);
    setHasImage(true);
  }, []);

  const markSaved = useCallback(() => {
    savedRef.current = true;
  }, []);

  // 컴포넌트 언마운트(취소·뒤로가기·탭 이탈) 시 PENDING 이미지 취소 요청
  useEffect(() => {
    const idsRef = uploadedIdsRef;
    const isSavedRef = savedRef;
    return () => {
      if (isSavedRef.current || idsRef.current.size === 0) return;
      const ids = [...idsRef.current];
      // keepalive: 페이지 언로드 중에도 요청이 완료될 수 있도록 보장
      fetch(API.images.cancel, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ids }),
        keepalive: true,
        // credentials: "include", // same-origin이라 불필요 + cross-origin 전환 시 쿠키 유출 방지 위해 비활성화
      });
    };
  }, []);

  return { hasImage, handleUpload, markSaved };
}
