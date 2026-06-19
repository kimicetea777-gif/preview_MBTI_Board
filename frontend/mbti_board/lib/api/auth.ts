import { apiClient } from "@/lib/apiClient";
import { API } from "@/constants/apiEndpoints";
import type { ApiResponse } from "@/types/api";
import type {
  LoginRequest,
  LoginResponse,
  SignupRequest,
  EmailVerifySendRequest,
  EmailVerifyConfirmRequest,
  FindEmailRequest,
  FindEmailResponse,
  PasswordResetRequest,
  PasswordResetConfirmRequest,
} from "@/types/auth";

export async function login(data: LoginRequest): Promise<LoginResponse> {
  const res = await apiClient.post<ApiResponse<LoginResponse>>(API.auth.login, data);
  return res.data.data;
}

export async function signup(data: SignupRequest): Promise<void> {
  await apiClient.post<ApiResponse<void>>(API.auth.signup, data);
}

export async function sendEmailVerification(data: EmailVerifySendRequest): Promise<void> {
  await apiClient.post<ApiResponse<void>>(API.auth.emailVerifySend, data);
}

export async function confirmEmailVerification(data: EmailVerifyConfirmRequest): Promise<string> {
  const res = await apiClient.post<ApiResponse<string>>(API.auth.emailVerifyConfirm, data);
  return res.data.data;
}

export async function findEmail(data: FindEmailRequest): Promise<FindEmailResponse> {
  const res = await apiClient.post<ApiResponse<FindEmailResponse>>(API.auth.findEmail, data);
  return res.data.data;
}

export async function requestPasswordReset(data: PasswordResetRequest): Promise<void> {
  await apiClient.post<ApiResponse<void>>(API.auth.passwordResetRequest, data);
}

export async function confirmPasswordReset(data: PasswordResetConfirmRequest): Promise<void> {
  await apiClient.post<ApiResponse<void>>(API.auth.passwordResetConfirm, data);
}
