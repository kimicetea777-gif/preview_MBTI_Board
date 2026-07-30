export const STEP = {
  EMAIL: "email",   //이메일 입력
  EMAIL_SENT: "email-sent",   //이메일 전송 완료
  FORM: "form",   //정보 입력
  DONE: "done",   // 가입 완료
} as const;

export type Step = (typeof STEP)[keyof typeof STEP];
