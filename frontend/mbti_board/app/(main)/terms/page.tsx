import type { Metadata } from "next";
import { APP_NAME, CONTACT_EMAIL, CONTACT_MAILTO } from "@/constants/site";

export const metadata: Metadata = {
  title: "이용약관", // 루트 layout의 title.template이 " | APP_NAME"을 붙임
};

export const dynamic = "force-static";

export default function TermsPage() {
  return (
    <article className="max-w-3xl mx-auto px-6 py-12 text-on-surface">
      <h1 className="text-2xl font-bold mb-2">이용약관</h1>
      <p className="text-sm text-on-surface-variant mb-10">시행일: 2026년 7월 23일</p>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">제1조 (목적)</h2>
        <p className="text-sm leading-relaxed text-on-surface-variant">
          이 약관은 {APP_NAME}(이하 &ldquo;서비스&rdquo;)의 이용 조건 및 절차, 회원과 서비스 운영자의 권리·의무 및 책임 사항을 규정함을 목적으로 합니다.
        </p>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">제2조 (서비스 소개)</h2>
        <p className="text-sm leading-relaxed text-on-surface-variant">
          {APP_NAME}는 MBTI 유형 기반 커뮤니티 플랫폼으로, 자유글·투표·토론 게시판을 제공합니다.
          회원 및 비회원 모두 게시글·댓글 작성이 가능하며, 투표 및 댓글 반응 기능은 회원 전용입니다.
          회원가입 및 로그인은 이메일 또는 구글 소셜 계정으로 가능합니다.
        </p>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">제3조 (이용 자격)</h2>
        <p className="text-sm leading-relaxed text-on-surface-variant">
          회원 및 비회원을 포함한 모든 이용자는 만 14세 이상이어야 합니다. 만 14세 미만인 자는 서비스를
          이용할 수 없으며, 회원가입 또는 비회원 게시글·댓글 작성 시 만 14세 이상임을 확인합니다.
          만 14세 미만으로 확인될 경우 서비스 운영자는 사전 통지 없이 가입·작성을 거부하거나 이용을
          제한할 수 있습니다.
        </p>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">제4조 (이용 규칙)</h2>
        <ul className="text-sm leading-relaxed text-on-surface-variant list-disc pl-5 space-y-2">
          <li>타인을 비방·모욕하거나 명예를 훼손하는 게시물을 작성해서는 안 됩니다.</li>
          <li>음란물, 폭력적 내용, 불법 정보를 게시해서는 안 됩니다.</li>
          <li>광고·홍보성 도배 게시물 작성을 금지합니다.</li>
          <li>타인의 개인정보를 무단으로 수집·게시해서는 안 됩니다.</li>
          <li>서비스의 정상적인 운영을 방해하는 행위를 해서는 안 됩니다.</li>
        </ul>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">제5조 (게시물 정책)</h2>
        <div className="text-sm leading-relaxed text-on-surface-variant space-y-3">
          <p>게시물의 저작권은 작성자 본인에게 있습니다.</p>
          <p>
            이용자는 게시물을 서비스에 게시함으로써 서비스 운영자에게 해당 게시물을 서비스 내에
            노출·저장·전송하고 서비스를 운영·홍보하는 데 사용할 수 있는 비독점적 이용 허락을 부여합니다.
            이 이용 허락은 게시물이 서비스에서 삭제되면 종료됩니다.
          </p>
          <p>
            서비스 운영자는 서비스 품질 유지를 위해 이용 규칙에 위반되는 게시물을 사전 고지 없이
            삭제하거나 접근을 제한할 수 있습니다.
          </p>
          <p>
            탈퇴 회원의 게시글·댓글은 삭제되지 않고 익명으로 보존됩니다.
            또한 탈퇴 시 일부 개인정보(이메일, 탈퇴 시 접속 IP)는 수사 협조 및
            불법 재가입 방지를 위해 탈퇴일로부터 12개월간 암호화하여 보존된 후 파기됩니다.
            자세한 내용은 개인정보처리방침을 참고하세요.
          </p>
        </div>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">제6조 (제재 및 이용 제한)</h2>
        <p className="text-sm leading-relaxed text-on-surface-variant">
          이용 규칙을 위반한 경우 서비스 운영자는 경고, 일시 정지, 영구 이용 정지 등의 제재를
          가할 수 있습니다. 제재 사유는 가능한 범위에서 이용자에게 안내합니다.
        </p>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">제7조 (면책 조항)</h2>
        <p className="text-sm leading-relaxed text-on-surface-variant">
          서비스 운영자는 천재지변, 시스템 장애 등 불가항력적 사유로 발생한 서비스 중단에 대해
          책임을 지지 않습니다. 또한 이용자가 게시한 정보·게시물로 인해 발생한 이용자 간의 분쟁,
          명예훼손 등 법적 책임은 해당 게시물 작성자에게 있습니다. 서비스는 운영상 필요에 따라
          사전 공지 후 내용이 변경되거나 일시 중단될 수 있습니다.
        </p>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">제8조 (약관 변경)</h2>
        <p className="text-sm leading-relaxed text-on-surface-variant">
          약관이 변경될 경우 서비스 내 공지를 통해 사전 안내합니다. 변경된 약관에 동의하지 않을 경우
          회원 탈퇴를 통해 서비스 이용을 종료할 수 있습니다.
        </p>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">제9조 (준거법 및 관할법원)</h2>
        <p className="text-sm leading-relaxed text-on-surface-variant">
          이 약관과 관련하여 서비스 운영자와 이용자 간에 발생한 분쟁에는 대한민국 법률을 적용합니다.
          소송이 제기될 경우 관할 법원은 민사소송법상의 관할 법원에 따릅니다.
        </p>
      </section>

      <section>
        <h2 className="text-lg font-semibold mb-3">제10조 (운영자 정보)</h2>
        <ul className="text-sm leading-relaxed text-on-surface-variant space-y-1">
          <li>서비스명: {APP_NAME}</li>
          <li>운영 형태: 개인 운영</li>
          <li>문의: <a href={CONTACT_MAILTO} className="underline hover:text-on-surface">{CONTACT_EMAIL}</a></li>
        </ul>
      </section>
    </article>
  );
}
