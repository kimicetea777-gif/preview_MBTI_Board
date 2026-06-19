"use client";

import { useState, useRef, useEffect, useCallback } from "react";
import { API } from "@/constants/apiEndpoints";

export function useImageUpload() {
  const uploadedUrlsRef = useRef<Set<string>>(new Set());
  const savedRef = useRef(false);
  const [hasImage, setHasImage] = useState(false);

  const handleUpload = useCallback((url: string) => {
    uploadedUrlsRef.current.add(url);
    setHasImage(true);
  }, []);

  const markSaved = useCallback(() => {
    savedRef.current = true;
  }, []);

  // 컴포넌트 언마운트(취소·뒤로가기·탭 이탈) 시 PENDING 이미지 취소 요청
  useEffect(() => {
    const urlsRef = uploadedUrlsRef;
    const isSavedRef = savedRef;
    return () => {
      if (isSavedRef.current || urlsRef.current.size === 0) return;
      const urls = [...urlsRef.current];
      // keepalive: 페이지 언로드 중에도 요청이 완료될 수 있도록 보장
      fetch(API.images.cancel, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ urls }),
        keepalive: true,
        credentials: "include",
      });
    };
  }, []);

  return { hasImage, handleUpload, markSaved };
}
