import { HiInformationCircle } from "react-icons/hi";

export default function SenderEmailNotice() {
  const senderEmail = process.env.NEXT_PUBLIC_SENDER_EMAIL;
  if (!senderEmail) return null;

  return (
    <p className="text-sm text-on-surface-variant flex items-start gap-2">
      <HiInformationCircle className="text-secondary text-lg mt-0.5 shrink-0" />
      <span>
        발신자가{" "}
        <strong className="text-on-surface">{senderEmail}</strong>
        {" "}인지 꼭 확인하세요!
      </span>
    </p>
  );
}
