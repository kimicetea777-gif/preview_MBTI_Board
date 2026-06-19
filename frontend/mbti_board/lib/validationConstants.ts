export const PASSWORD_REGEX = /^(?=.*[a-zA-Z])(?=.*\d).{10,}$/;
export const PASSWORD_MESSAGE = "비밀번호는 영문+숫자 포함 10자 이상이어야 합니다.";

export const PHONE_REGEX = /^01[016789](-\d{3,4}-\d{4}|\d{7,8})$/;
export const PHONE_MESSAGE = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)";

/** 비밀번호 유효성 검사. 오류가 없으면 null 반환 */
export function validatePassword(password: string, confirm: string): string | null {
  if (!PASSWORD_REGEX.test(password)) return PASSWORD_MESSAGE;
  if (password !== confirm) return "비밀번호가 일치하지 않습니다.";
  return null;
}
