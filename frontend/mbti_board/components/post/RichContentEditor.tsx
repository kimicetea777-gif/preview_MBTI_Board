"use client";

import dynamic from "next/dynamic";
import { useEditor, EditorContent, useEditorState, NodeViewWrapper, ReactNodeViewRenderer } from "@tiptap/react";
import type { NodeViewProps } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Placeholder from "@tiptap/extension-placeholder";
import Image from "@tiptap/extension-image";
import Youtube from "@tiptap/extension-youtube";
import { LuX, LuMoveDiagonal2 } from "react-icons/lu";
import { useEffect, useRef } from "react";
import { MAX_POST_IMAGES } from "@/types/post";
import { ALLOWED_IMAGE_TYPES, ALLOWED_IMAGE_TYPES_LABEL, MAX_IMAGE_BYTES, MAX_IMAGE_BYTES_LABEL } from "@/constants/imageUpload";
import { parseTiptapContent, countTiptapImages } from "@/lib/editor";
import { uploadImage } from "@/lib/api/image";
import { toast } from "@/components/common/Toast";

interface Props {
  initialContent?: string;
  onChange: (json: string) => void;
  placeholder?: string;
  onUpload?: (url: string) => void;
  onUploadingChange?: (count: number) => void;
}

const MIN_IMAGE_WIDTH = 80;

/* Custom Image Componet */
function ImageNodeView({ node, deleteNode, updateAttributes }: NodeViewProps) {
  const isUploading = node.attrs.uploading as boolean;
  const width = node.attrs.width as number | null;
  const wrapperRef = useRef<HTMLDivElement>(null);

  const handleResizeStart = (e: React.PointerEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    const wrapper = wrapperRef.current;
    if (!wrapper) return;

    const startX = e.clientX;
    const startWidth = wrapper.offsetWidth;
    const maxWidth = wrapper.closest(".ProseMirror")?.clientWidth ?? Infinity;
    const handle = e.currentTarget;
    handle.setPointerCapture(e.pointerId);

    const handlePointerMove = (moveEvent: PointerEvent) => {
      const nextWidth = startWidth + (moveEvent.clientX - startX);
      const clamped = Math.round(Math.min(Math.max(nextWidth, MIN_IMAGE_WIDTH), maxWidth));
      updateAttributes({ width: clamped });
    };
    const handlePointerUp = () => {
      handle.releasePointerCapture(e.pointerId);
      handle.removeEventListener("pointermove", handlePointerMove);
      handle.removeEventListener("pointerup", handlePointerUp);
    };
    handle.addEventListener("pointermove", handlePointerMove);
    handle.addEventListener("pointerup", handlePointerUp);
  };

  return (
    <NodeViewWrapper
      ref={wrapperRef}
      className="relative inline-block group my-2"
      contentEditable={false}
      style={width ? { width } : undefined}
    >
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={node.attrs.src}
        alt={node.attrs.alt ?? ""}
        className={`max-w-full rounded${isUploading ? " opacity-60" : ""}`}
        style={width ? { width: "100%" } : undefined}
      />
      {isUploading && (
        <div className="absolute inset-0 flex items-center justify-center rounded">
          <div className="size-8 rounded-full border-2 border-white border-t-transparent animate-spin" />
        </div>
      )}
      {!isUploading && (
        <>
          <button
            type="button"
            onClick={deleteNode}
            aria-label="이미지 삭제"
            className="absolute top-1.5 right-1.5 bg-black/60 hover:bg-black/80 text-white rounded-full p-1 opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity"
          >
            <LuX className="size-3.5" />
          </button>
          <div
            onPointerDown={handleResizeStart}
            role="slider"
            aria-label="이미지 크기 조절"
            aria-valuenow={width ?? undefined}
            className="absolute bottom-0 right-0 p-2 touch-none cursor-nwse-resize opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity"
          >
            <div className="flex items-center justify-center size-5 rounded-sm border border-dashed border-amber-400 bg-black/60">
              <LuMoveDiagonal2 className="size-3 text-amber-400" />
            </div>
          </div>
        </>
      )}
    </NodeViewWrapper>
  );
}

function toYoutubeEmbedUrl(url: string): string {
  const match = url.match(/(?:youtube\.com\/watch\?(?:\S+&)?v=|youtu\.be\/)([\w-]{11})/);
  return match ? `https://www.youtube-nocookie.com/embed/${match[1]}` : url;
}
/* Custom Youtube By URL Componet */
function YoutubeNodeView({ node, deleteNode }: NodeViewProps) {
  return (
    <NodeViewWrapper className="my-2 group" contentEditable={false}>
      <div className="relative w-full" style={{ paddingTop: "56.25%" }}>
        <iframe
          src={toYoutubeEmbedUrl(node.attrs.src as string)}
          className="absolute inset-0 w-full h-full rounded"
          allowFullScreen
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
        />
        <button
          type="button"
          onClick={deleteNode}
          aria-label="영상 삭제"
          className="absolute top-1.5 right-1.5 bg-black/60 hover:bg-black/80 text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity"
        >
          <LuX className="size-3.5" />
        </button>
      </div>
    </NodeViewWrapper>
  );
}

const CustomImage = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      uploading: {
        default: false,
        parseHTML: () => false,
        renderHTML: () => ({}),
      },
      width: {
        default: null,
        parseHTML: (element) => {
          const value = element.style.width || element.getAttribute("width");
          return value ? parseInt(value, 10) : null;
        },
        renderHTML: (attributes) => {
          if (!attributes.width) return {};
          return { style: `width: ${attributes.width}px` };
        },
      },
    };
  },
  addNodeView() {
    return ReactNodeViewRenderer(ImageNodeView);
  },
}).configure({ inline: false, allowBase64: false });


function RichContentEditorInner({ initialContent, onChange, placeholder, onUpload, onUploadingChange }: Props) {
  const onChangeRef = useRef(onChange);
  useEffect(() => { onChangeRef.current = onChange; });

  const fileInputRef = useRef<HTMLInputElement>(null);
  const uploadingCountRef = useRef(0);

  const editor = useEditor({
    extensions: [
      StarterKit,
      Placeholder.configure({ placeholder: placeholder ?? "내용을 입력하세요." }),
      CustomImage,
      Youtube.extend({
        addNodeView() { return ReactNodeViewRenderer(YoutubeNodeView); },
      }).configure({ nocookie: true }),
    ],
    content: parseTiptapContent(initialContent),
    onUpdate: ({ editor }) => {
      onChangeRef.current(JSON.stringify(editor.getJSON()));
    },
    editorProps: {
      attributes: {
        class: "prose prose-sm max-w-none min-h-[200px] px-4 py-3 focus:outline-none",
      },
      handleKeyDown: (_view, e) => {
        // 한글 IME 조합 중 ProseMirror의 중복 키 처리 방지
        if (e.isComposing) return true;
        return false;
      },
    },
  });

  // 수정 페이지에서 initialContent 또는 editor가 뒤늦게 준비될 때 반영
  // 주의: 최초 1회만 동기화 — onChange가 매 입력마다 같은 state를 initialContent로 되돌려주는
  //       상위 컴포넌트 패턴이라, 가드 없이 매번 setContent를 부르면 타이핑/이미지 삽입마다 커서가 초기화됨
  const hasSyncedInitialContent = useRef(false);
  useEffect(() => {
    if (!editor || !initialContent || hasSyncedInitialContent.current) return;
    const parsed = parseTiptapContent(initialContent);
    if (parsed) {
      editor.commands.setContent(parsed);
      hasSyncedInitialContent.current = true;
    }
  }, [editor, initialContent]);

  const active = useEditorState({
    editor,
    selector: (ctx) => ({
      bold:         ctx.editor?.isActive("bold")                  ?? false,
      italic:       ctx.editor?.isActive("italic")                ?? false,
      underline:    ctx.editor?.isActive("underline")             ?? false,
      strike:       ctx.editor?.isActive("strike")                ?? false,
      h2:           ctx.editor?.isActive("heading", { level: 2 }) ?? false,
      h3:           ctx.editor?.isActive("heading", { level: 3 }) ?? false,
      bulletList:   ctx.editor?.isActive("bulletList")            ?? false,
      orderedList:  ctx.editor?.isActive("orderedList")           ?? false,
      blockquote:   ctx.editor?.isActive("blockquote")            ?? false,
      code:         ctx.editor?.isActive("code")                  ?? false,
    }),
  });

  const handleImageSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !editor) return;
    e.target.value = "";

    if (!ALLOWED_IMAGE_TYPES.has(file.type)) {
      toast.error(`지원하지 않는 형식입니다. (${ALLOWED_IMAGE_TYPES_LABEL}만 허용)`);
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      toast.error(`이미지 크기는 ${MAX_IMAGE_BYTES_LABEL}를 초과할 수 없습니다.`);
      return;
    }
    const currentImageCount = countTiptapImages(editor.getJSON() as Record<string, unknown>);
    if (currentImageCount >= MAX_POST_IMAGES) {
      toast.error(`이미지는 최대 ${MAX_POST_IMAGES}개까지 첨부할 수 있습니다.`);
      return;
    }

    const blobUrl = URL.createObjectURL(file);
    editor
      .chain()
      .focus()
      .insertContent([
        { type: "image", attrs: { src: blobUrl, uploading: true } },
        { type: "paragraph" },
      ])
      .run();

    uploadingCountRef.current += 1;
    onUploadingChange?.(uploadingCountRef.current);

    try {
      // Next.js -> spring -> R2에 저장된 URL
      const r2Url = await uploadImage(file);

      if (!editor.isDestroyed) {
        const tr = editor.state.tr;
        editor.state.doc.descendants((node, pos) => {
          if (node.type.name === "image" && node.attrs.src === blobUrl) {
            tr.setNodeMarkup(pos, undefined, { ...node.attrs, src: r2Url, uploading: false });
            return false;
          }
        });
        editor.view.dispatch(tr);
      }

      onUpload?.(r2Url);
    } catch {
      if (!editor.isDestroyed) {
        const tr = editor.state.tr;
        editor.state.doc.descendants((node, pos) => {
          if (node.type.name === "image" && node.attrs.src === blobUrl) {
            tr.delete(pos, pos + node.nodeSize);
            return false;
          }
        });
        editor.view.dispatch(tr);
      }
      toast.error("이미지 업로드에 실패했습니다.");
    } finally {
      URL.revokeObjectURL(blobUrl);
      uploadingCountRef.current -= 1;
      onUploadingChange?.(uploadingCountRef.current);
    }
  };

  return (
    <div className="rounded-lg border border-gray-300 overflow-hidden bg-background">
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
          onClick={() => editor?.chain().focus().toggleUnderline().run()}
          className={`rounded px-2 py-1 text-xs underline transition-colors ${
            active.underline ? "bg-gray-900 text-white" : "text-gray-600 hover:bg-gray-200"
          }`}
        >
          U
        </button>
        <button
          type="button"
          onClick={() => editor?.chain().focus().toggleStrike().run()}
          className={`rounded px-2 py-1 text-xs line-through transition-colors ${
            active.strike ? "bg-gray-900 text-white" : "text-gray-600 hover:bg-gray-200"
          }`}
        >
          S
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
          onClick={() => editor?.chain().focus().toggleOrderedList().run()}
          className={`rounded px-2 py-1 text-xs transition-colors ${
            active.orderedList ? "bg-gray-900 text-white" : "text-gray-600 hover:bg-gray-200"
          }`}
        >
          1.목록
        </button>
        <button
          type="button"
          onClick={() => editor?.chain().focus().toggleBlockquote().run()}
          className={`rounded px-2 py-1 text-xs transition-colors ${
            active.blockquote ? "bg-gray-900 text-white" : "text-gray-600 hover:bg-gray-200"
          }`}
        >
          ❝인용
        </button>
        {/* <button
          type="button"
          onClick={() => editor?.chain().focus().toggleCode().run()}
          className={`rounded px-2 py-1 text-xs font-mono transition-colors ${
            active.code ? "bg-gray-900 text-white" : "text-gray-600 hover:bg-gray-200"
          }`}
        >
          {"</>"}
        </button> */}
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className="rounded px-2 py-1 text-xs text-gray-600 hover:bg-gray-200 transition-colors"
        >
          사진
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept={[...ALLOWED_IMAGE_TYPES].join(",")}
          className="hidden"
          onChange={handleImageSelect}
        />
      </nav>
      <EditorContent editor={editor} />
    </div>
  );
}

// Tiptap은 SSR 불가 — 클라이언트 전용으로 dynamic import
export default dynamic(() => Promise.resolve(RichContentEditorInner), { ssr: false });
