"use client";

import dynamic from "next/dynamic";
import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Image from "@tiptap/extension-image";
import { useEffect } from "react";

interface Props {
  content: string | undefined;
}

function RichContentViewerInner({ content }: Props) {
  const editor = useEditor({
    extensions: [StarterKit, Image.configure({ inline: false })],
    editable: false,
    editorProps: {
      attributes: {
        class: "prose prose-sm max-w-none px-4 py-3",
      },
    },
  });

  useEffect(() => {
    if (!editor || !content) return;
    try {
      editor.commands.setContent(JSON.parse(content));
    } catch { /* 유효하지 않은 JSON은 무시 */ }
  }, [editor, content]);

  // content가 아직 없으면 스켈레톤 표시 (initialData 로딩 중 상태)
  if (!content) {
    return (
      <div className="space-y-2 px-4 py-3 animate-pulse">
        <div className="h-4 bg-gray-200 rounded w-3/4" />
        <div className="h-4 bg-gray-200 rounded w-full" />
        <div className="h-4 bg-gray-200 rounded w-5/6" />
        <div className="h-4 bg-gray-200 rounded w-2/3" />
      </div>
    );
  }

  return <EditorContent editor={editor} />;
}

// Tiptap은 SSR 불가 — 클라이언트 전용으로 dynamic import
export default dynamic(() => Promise.resolve(RichContentViewerInner), { ssr: false });
