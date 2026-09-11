/**
 * 지출 항목 (항목명 · 금액 · 참여자)
 * @param id 항목 식별자 (프론트 전용)
 * @param name 항목명
 * @param amount 금액 (입력 중엔 문자열로 관리, 제출 시 숫자로 변환)
 * @param participants 참여자 이름 목록
 */
export interface ExpenseItemFormValue {
  id: string;
  name: string;
  amount: string;
  participants: string[];
}

/**
 * 결제자 · 날짜 단위의 지출 그룹
 * @param id 그룹 식별자 (프론트 전용)
 * @param payer 결제자
 * @param paidAt 결제일자 (yyyy-mm-dd)
 * @param isOverseas 해외결제 여부
 * @param currency 화폐단위 (해외결제 시에만 사용)
 * @param bankName 송금받을 계좌 은행명
 * @param accountNumber 송금받을 계좌번호
 * @param items 그룹에 속한 지출 항목 목록
 */
export interface ExpenseGroupFormValue {
  id: string;
  payer: string;
  paidAt: string;
  isOverseas: boolean;
  currency: string;
  bankName: string;
  accountNumber: string;
  items: ExpenseItemFormValue[];
}
