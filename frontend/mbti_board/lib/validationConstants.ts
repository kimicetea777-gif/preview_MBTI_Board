export const PASSWORD_REGEX = /^(?=.*[a-zA-Z])(?=.*\d).{10,}$/;
export const PASSWORD_MESSAGE = "비밀번호는 영문+숫자 포함 10자 이상이어야 합니다.";

export const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const EMAIL_MESSAGE = "이메일 형식이 올바르지 않습니다.";

export const NICKNAME_REGEX = /^[가-힣a-zA-Z0-9_]{2,12}$/;
export const NICKNAME_MESSAGE = "별명은 한글, 영문, 숫자, 언더바(_)만 사용할 수 있습니다.";

/** 비밀번호 유효성 검사. 오류가 없으면 null 반환 */
export function validatePassword(password: string, confirm: string): string | null {
  if (!PASSWORD_REGEX.test(password)) return PASSWORD_MESSAGE;
  if (password !== confirm) return "비밀번호가 일치하지 않습니다.";
  return null;
}
