import type { Metadata } from "next";
import { APP_NAME, CONTACT_EMAIL, CONTACT_MAILTO } from "@/constants/site";

export const metadata: Metadata = {
  title: `개인정보처리방침 | ${APP_NAME}`,
};

export const dynamic = "force-static";

export default function PrivacyPage() {
  return (
    <article className="max-w-3xl mx-auto px-6 py-12 text-on-surface">
      <h1 className="text-2xl font-bold mb-2">개인정보처리방침</h1>
      <p className="text-sm text-on-surface-variant mb-10">시행일: 2026년 7월 23일</p>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">1. 수집하는 개인정보 항목</h2>
        <div className="text-sm leading-relaxed text-on-surface-variant space-y-3">
          <p><strong className="text-on-surface">회원가입 시 수집 (필수)</strong></p>
          <ul className="list-disc pl-5 space-y-1">
            <li>이메일 주소</li>
            <li>비밀번호 (암호화 저장)</li>
            <li>닉네임</li>
          </ul>
          <p><strong className="text-on-surface">회원가입 시 수집 (선택)</strong></p>
          <ul className="list-disc pl-5 space-y-1">
            <li>MBTI 유형</li>
          </ul>
          <p><strong className="text-on-surface">소셜 로그인(구글) 가입 시 제공받는 정보</strong></p>
          <ul className="list-disc pl-5 space-y-1">
            <li>이메일 주소 — 소셜 서비스 제공자로부터 제공받음</li>
          </ul>
          <p><strong className="text-on-surface">비회원 이용 시 수집</strong></p>
          <ul className="list-disc pl-5 space-y-1">
            <li>비회원 비밀번호 (게시글·댓글 수정·삭제 시 본인 확인용, 암호화 저장)</li>
          </ul>
          <p><strong className="text-on-surface">서비스 이용 중 자동 수집</strong></p>
          <ul className="list-disc pl-5 space-y-1">
            <li>서비스 이용 기록 (작성한 게시글·댓글)</li>
            <li>접속 IP 주소 (보안 목적, 서버 로그)</li>
            <li>게시글·댓글 작성 시 IP 주소 (회원·비회원 모두, 어뷰징 방지 및 수사 협조 목적)</li>
          </ul>
        </div>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">2. 개인정보 수집 및 이용 목적</h2>
        <ul className="text-sm leading-relaxed text-on-surface-variant list-disc pl-5 space-y-2">
          <li>회원 식별 및 서비스 제공</li>
          <li>비밀번호 재설정 등 계정 관련 이메일 발송</li>
          <li>소셜 계정을 통한 간편 가입 및 로그인 제공</li>
          <li>비회원 게시글·댓글의 수정·삭제 시 작성자 본인 확인</li>
          <li>부정 이용 방지 및 서비스 운영</li>
        </ul>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">3. 개인정보 보유 및 이용 기간</h2>
        <div className="text-sm leading-relaxed text-on-surface-variant space-y-3">
          <p>회원 탈퇴 시 비밀번호·닉네임 등 대부분의 회원 정보는 즉시 익명화되어 더 이상 개인을 식별할 수 없는 상태가 되며, 익명화된 계정 레코드는 매일 새벽 배치를 통해 최대 24시간 이내 데이터베이스에서 완전히 삭제됩니다.</p>
          <p>단, 탈퇴 회원이 작성한 게시글·댓글은 작성자 정보만 제거된 채 서비스에 보존됩니다.</p>
          <p>게시글·댓글에 기록된 IP 주소는 해당 게시물 삭제 시 함께 파기됩니다.</p>
          <p><strong className="text-on-surface">탈퇴 후 보존되는 정보 (수사 협조·불법 재가입 방지 목적)</strong></p>
          <ul className="list-disc pl-5 space-y-1">
            <li>이메일 주소 (암호화 보관)</li>
            <li>탈퇴 시 접속 IP 주소</li>
          </ul>
          <ul className="list-none pl-0 space-y-1">
            <li>· 보존 기간: 탈퇴일로부터 <strong className="text-on-surface">12개월</strong></li>
            <li>· 보존 방법: 별도 테이블에 암호화하여 분리 보관</li>
            <li>· 파기 방법: 보존 기간 만료 시 완전 삭제</li>
          </ul>
        </div>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">4. 개인정보의 제3자 제공</h2>
        <p className="text-sm leading-relaxed text-on-surface-variant">
          수집된 개인정보는 원칙적으로 외부에 제공하지 않습니다.
          다만 법령의 규정에 의거하거나 수사 목적으로 법령에 정해진 절차와 방법에 따라
          수사기관의 요구가 있는 경우에는 예외로 합니다.
        </p>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">5. 개인정보 처리 위탁</h2>
        <div className="text-sm leading-relaxed text-on-surface-variant space-y-2">
          <p>서비스 운영을 위해 아래 업체에 개인정보 처리 업무를 위탁하고 있습니다.</p>
          <ul className="list-disc pl-5 space-y-1">
            <li>이메일 발송: Google (Gmail SMTP) — 이메일 인증 및 알림 발송</li>
            <li>파일 스토리지: Cloudflare R2 — 업로드된 이미지 저장</li>
          </ul>
        </div>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">6. 이용자의 권리</h2>
        <div className="text-sm leading-relaxed text-on-surface-variant space-y-3">
          <ul className="list-disc pl-5 space-y-2">
            <li>회원은 언제든지 자신의 개인정보를 조회·수정할 수 있습니다.</li>
            <li>회원 탈퇴를 통해 개인정보 삭제를 요청할 수 있습니다.</li>
            <li>개인정보 관련 문의(열람·정정·처리정지 요구 등)는 아래 연락처로 요청할 수 있습니다.</li>
          </ul>
          <p><strong className="text-on-surface">구체적인 이용 방법</strong></p>
          <ul className="list-disc pl-5 space-y-1">
            <li>닉네임·MBTI 정보 수정: [마이페이지] 상단 프로필 영역에서 직접 수정</li>
            <li>비밀번호 변경, 이메일 찾기: [마이페이지] &gt; [계정 설정]에서 AS 계정 관리로 이동</li>
            <li>회원 탈퇴: [마이페이지] &gt; [계정 설정] &gt; [회원 탈퇴]</li>
            <li>그 외 열람·정정·처리정지 요구: 9번 항목의 담당자 이메일로 문의</li>
          </ul>
        </div>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">7. 개인정보 보호 조치</h2>
        <ul className="text-sm leading-relaxed text-on-surface-variant list-disc pl-5 space-y-2">
          <li>비밀번호는 암호화하여 저장합니다.</li>
          <li>탈퇴 후 보존 이메일은 암호화하여 별도 저장합니다.</li>
          <li>통신 구간 암호화(HTTPS/TLS)를 적용합니다.</li>
          <li>접근 권한 최소화 및 내부 접근 통제를 시행합니다.</li>
        </ul>
      </section>

      <section className="mb-10">
        <h2 className="text-lg font-semibold mb-3">8. 쿠키의 설치·운영 및 거부</h2>
        <div className="text-sm leading-relaxed text-on-surface-variant space-y-3">
          <p>
            서비스는 로그인 상태 유지를 위해 쿠키를 사용합니다. 로그인 시 &ldquo;자동 로그인&rdquo;을
            선택하면 로그인 유지용 쿠키가 최대 7일간 보관되며, 선택하지 않으면 브라우저를 종료할 때
            함께 삭제됩니다.
          </p>
          <ul className="list-disc pl-5 space-y-1">
            <li>로그인 유지 쿠키: 로그인 상태를 서버에서 확인하기 위한 용도 (개인을 식별할 수 있는 정보 미포함)</li>
            <li>로그인 상태 확인용 보조 쿠키: 화면 표시 상태를 판단하기 위한 용도 (개인정보 미포함)</li>
          </ul>
          <p>
            이용자는 브라우저 설정을 통해 쿠키 저장을 거부할 수 있습니다. 다만 이 경우 로그인 상태
            유지 등 서비스 일부 기능 이용에 제한이 있을 수 있습니다.
          </p>
        </div>
      </section>

      <section>
        <h2 className="text-lg font-semibold mb-3">9. 개인정보 보호 담당자</h2>
        <ul className="text-sm leading-relaxed text-on-surface-variant space-y-1">
          <li>서비스명: {APP_NAME}</li>
          <li>운영 형태: 개인 운영</li>
          <li>담당자: 김아이스티</li>
          <li>이메일: <a href={CONTACT_MAILTO} className="underline hover:text-on-surface">{CONTACT_EMAIL}</a></li>
        </ul>
      </section>
    </article>
  );
}
