/**
 * 금액 뒤에 붙일 통화 단위 반환
 * @param currency 통화 코드 (예: KRW, USD)
 * @returns KRW는 "원", 그 외는 통화 코드 그대로
 */
export const getCurrencyUnit = (currency: string): string => {
  const code = (currency ?? "").trim().toUpperCase();

  return code === "KRW" || code === "" ? "원" : code;
};

/**
 * 금액에 통화 단위를 붙여 표시용 문자열로 변환
 * @param amount 금액
 * @param currency 통화 코드 (예: KRW, USD)
 */
export const formatAmount = (amount: number, currency: string): string => {
  const unit = getCurrencyUnit(currency);
  const value = amount.toLocaleString("ko-KR");

  return unit === "원" ? `${value}원` : `${value} ${unit}`;
};

/**
 * 금액 입력값 정제 (숫자와 소수점 하나만 허용, 소수 2자리까지)
 * @param value 입력된 원본 문자열
 */
export const sanitizeAmountInput = (value: string): string => {
  const cleaned = value.replace(/[^0-9.]/g, "");
  const [integer, ...rest] = cleaned.split(".");
  const intPart = integer.slice(0, 10);

  if (rest.length === 0) return intPart;

  return `${intPart}.${rest.join("").slice(0, 2)}`;
};

/**
 * 금액 입력값을 천 단위 구분 표시용으로 변환 (입력 중인 소수점 · 끝자리 0 유지)
 * @param raw sanitizeAmountInput을 거친 문자열
 */
export const formatAmountInput = (raw: string): string => {
  if (!raw) return "";

  const [integer, decimal] = raw.split(".");
  const intText = Number(integer || "0").toLocaleString("ko-KR");

  return decimal === undefined ? intText : `${intText}.${decimal}`;
};
