"use client";

import dynamic from "next/dynamic";
import { useEditor, EditorContent, useEditorState } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Placeholder from "@tiptap/extension-placeholder";
import { useEffect, useRef } from "react";

interface Props {
  initialContent?: string;
  onChange: (json: string) => void;
  placeholder?: string;
}

function parseContent(json?: string) {
  if (!json) return undefined;
  try { return JSON.parse(json); } catch { return undefined; }
}

function RichContentEditorInner({ initialContent, onChange, placeholder }: Props) {
  const onChangeRef = useRef(onChange);
  useEffect(() => { onChangeRef.current = onChange; });

  const editor = useEditor({
    extensions: [
      StarterKit,
      Placeholder.configure({ placeholder: placeholder ?? "내용을 입력하세요." }),
    ],
    content: parseContent(initialContent),
    onUpdate: ({ editor }) => {
      onChangeRef.current(JSON.stringify(editor.getJSON()));
    },
    editorProps: {
      attributes: {
        class: "prose prose-sm max-w-none min-h-[200px] px-4 py-3 focus:outline-none",
      },
    },
  });

  // 수정 페이지에서 initialContent 또는 editor가 뒤늦게 준비될 때 반영
  useEffect(() => {
    if (!editor || !initialContent) return;
    const parsed = parseContent(initialContent);
    if (parsed) editor.commands.setContent(parsed);
  }, [editor, initialContent]);

  // 선택 영역·마크 변경을 구독해 툴바 활성 상태 반영 (Tiptap v3)
  const active = useEditorState({
    editor,
    selector: (ctx) => ({
      bold:       ctx.editor?.isActive("bold")               ?? false,
      italic:     ctx.editor?.isActive("italic")             ?? false,
      h2:         ctx.editor?.isActive("heading", { level: 2 }) ?? false,
      h3:         ctx.editor?.isActive("heading", { level: 3 }) ?? false,
      bulletList: ctx.editor?.isActive("bulletList")         ?? false,
      code:       ctx.editor?.isActive("code")               ?? false,
    }),
  });

  return (
    <div className="rounded-lg border border-gray-300 overflow-hidden">
      <nav className="flex flex-wrap gap-1 border-b border-gray-200 bg-gray-50 px-2 py-1.5">
        <button
          type="button"
          onClick={() => editor?.chain().focus().toggleBold().run()}
          className={`rounded px-2 py-1 text-xs font-bold transition-colors ${
            active.bold ? "bg-gray-900 text-white" : "text-gray-600 hover:bg-gray-200"
          }`}
        >
          B
        </button>
        <button
          type="button"
          onClick={() => editor?.chain().focus().toggleItalic().run()}
          className={`rounded px-2 py-1 text-xs italic transition-colors ${
            active.italic ? "bg-gray-900 text-white" : "text-gray-600 hover:bg-gray-200"
          }`}
        >
          I
        </button>
        <button
          type="button"
          onClick={() => editor?.chain().focus().toggleHeading({ level: 2 }).run()}
          className={`rounded px-2 py-1 text-xs transition-colors ${
            active.h2 ? "bg-gray-900 text-white" : "text-gray-600 hover:bg-gray-200"
          }`}
        >
          H2
        </button>
        <button
          type="button"
          onClick={() => editor?.chain().focus().toggleHeading({ level: 3 }).run()}
          className={`rounded px-2 py-1 text-xs transition-colors ${
            active.h3 ? "bg-gray-900 text-white" : "text-gray-600 hover:bg-gray-200"
          }`}
        >
          H3
        </button>
        <button
          type="button"
          onClick={() => editor?.chain().focus().toggleBulletList().run()}
          className={`rounded px-2 py-1 text-xs transition-colors ${
            active.bulletList ? "bg-gray-900 text-white" : "text-gray-600 hover:bg-gray-200"
          }`}
        >
          ・목록
        </button>
        <button
          type="button"
          onClick={() => editor?.chain().focus().toggleCode().run()}
          className={`rounded px-2 py-1 text-xs font-mono transition-colors ${
            active.code ? "bg-gray-900 text-white" : "text-gray-600 hover:bg-gray-200"
          }`}
        >
          {"</>"}
        </button>
      </nav>
      <EditorContent editor={editor} />
    </div>
  );
}

// Tiptap은 SSR 불가 — 클라이언트 전용으로 dynamic import
export default dynamic(() => Promise.resolve(RichContentEditorInner), { ssr: false });
