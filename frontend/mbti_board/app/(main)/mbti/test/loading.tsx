import LoadingIndicator from "@/components/common/LoadingIndicator";
import { LuSnail } from "react-icons/lu";

export default function MbtiTestLoading() {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-muted-foreground">
      <LuSnail className="w-8 h-8 animate-bounce" />
      <LoadingIndicator message="테스트 준비 중"/>
    </div>
  );
}
