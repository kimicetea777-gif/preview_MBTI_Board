export const FIND_ACCOUNT_TAB = {
  EMAIL: "email",
  PASSWORD: "password",
  PASSWORD_SENT: "passwordSent",
} as const;
export type FindAccountTab = typeof FIND_ACCOUNT_TAB[keyof typeof FIND_ACCOUNT_TAB];

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe: boolean;
}

export interface SignupRequest {
  email: string;
  password: string;
  nickname: string;
  mbtiId: string;
  termsAgreed: boolean;
  privacyAgreed: boolean;
  age14Agreed: boolean;
}

export interface LoginResponse {
  accessToken: string;
  email: string;
  nickname: string;
  role: string;
}

export interface OAuth2SignupConfirmRequest {
  signupToken: string;
  termsAgreed: boolean;
  privacyAgreed: boolean;
  age14Agreed: boolean;
}

export interface EmailVerifySendRequest {
  email: string;
}

export interface EmailVerifyConfirmRequest {
  token: string;
}

export interface FindEmailRequest {
  recoveryEmail: string;
}

export interface FindEmailResponse {
  maskedEmail: string;
  isSocialOnly: boolean;
}

export interface PasswordResetRequest {
  email: string;
}

export interface PasswordResetConfirmRequest {
  token: string;
  newPassword: string;
}
