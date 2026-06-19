import { LuSnail } from "react-icons/lu";

export default function MbtiTestLoading() {
  return (
    <div className="flex flex-col items-center justify-center py-20 gap-3 text-muted-foreground">
      <LuSnail className="w-8 h-8 animate-bounce" />
      <p className="text-sm">테스트를 불러오는 중...</p>
    </div>
  );
}
