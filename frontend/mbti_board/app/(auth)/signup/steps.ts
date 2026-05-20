export const STEP = {
  EMAIL: "email",
  EMAIL_SENT: "email-sent",
  FORM: "form",
  DONE: "done",
} as const;

export type Step = (typeof STEP)[keyof typeof STEP];
