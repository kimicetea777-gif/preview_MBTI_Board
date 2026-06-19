import { FiMessageSquare } from "react-icons/fi";

export default function MyCommentsPreview() {
  return (
    <div className="h-full flex flex-col bg-surface-container-lowest rounded-2xl border border-outline-variant/30 p-6 shadow-sm">
      <div className="flex items-center justify-between mb-5">
        <div className="flex items-center gap-2">
          <FiMessageSquare className="w-4 h-4 text-muted-foreground" />
          <h2 className="font-heading text-base font-bold text-foreground">내가 작성한 댓글</h2>
        </div>
        <button className="text-xs text-muted-foreground hover:text-foreground transition-colors">
          전체 보기
        </button>
      </div>
      <div className="flex flex-col items-center justify-center flex-1 min-h-32 rounded-xl border border-dashed border-outline-variant/50 bg-surface-container-low">
        <FiMessageSquare className="w-6 h-6 text-outline-variant mb-2" />
        <p className="text-sm text-muted-foreground">아직 작성한 댓글이 없습니다.</p>
      </div>
    </div>
  );
}
