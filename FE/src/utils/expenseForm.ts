import type {
  ExpenseGroupFormValue,
  ExpenseItemFormValue,
} from "../types/expenseType";

/**
 * 빈 지출 항목 생성
 * @param members 기본으로 선택할 참여자 목록 (기본 전체 선택)
 */
export const createEmptyExpenseItem = (
  members: string[],
): ExpenseItemFormValue => ({
  id: crypto.randomUUID(),
  name: "",
  amount: "",
  participants: [...members],
});

/**
 * 빈 지출 그룹(결제자 · 날짜 단위) 생성
 * @param members 기본으로 선택할 참여자 목록
 */
export const createEmptyExpenseGroup = (
  members: string[],
): ExpenseGroupFormValue => ({
  id: crypto.randomUUID(),
  payer: "",
  paidAt: new Date().toISOString().slice(0, 10),
  isOverseas: false,
  currency: "USD",
  bankName: "",
  accountNumber: "",
  items: [createEmptyExpenseItem(members)],
});
