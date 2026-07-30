import { HiInformationCircle } from "react-icons/hi";
import { SENDER_EMAIL } from "@/constants/site";

export default function SenderEmailNotice() {
  if (!SENDER_EMAIL) return null;

  return (
    <p className="text-sm text-on-surface-variant flex items-start gap-2">
      <HiInformationCircle className="text-secondary text-lg mt-0.5 shrink-0" />
      <span>
        발신자가{" "}
        <strong className="text-on-surface">{SENDER_EMAIL}</strong>
        {" "}인지 꼭 확인하세요!
      </span>
    </p>
  );
}
