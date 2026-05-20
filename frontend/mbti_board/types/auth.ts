export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  nickname: string;
  mbtiId: string;
}

export interface LoginResponse {
  accessToken: string;
  email: string;
  nickname: string;
  role: string;
}

export interface EmailVerifySendRequest {
  email: string;
}

export interface EmailVerifyConfirmRequest {
  token: string;
}
