import type {
  ExpenseFormMemberInfo,
  ExpenseGroupFormValue,
  ExpenseItemFormValue,
} from "../types/expenseType";

/**
 * 빈 지출 항목 생성
 * @param members 기본으로 선택할 참여자 목록 (기본 전체 선택)
 */
export const createEmptyExpenseItem = (
  members: ExpenseFormMemberInfo[],
): ExpenseItemFormValue => ({
  id: crypto.randomUUID(),
  name: "",
  amount: "",
  participants: members.map((member) => member.memberId),
});

/**
 * 빈 지출 그룹(결제자 · 날짜 단위) 생성
 * @param members 기본으로 선택할 참여자 목록
 */
export const createEmptyExpenseGroup = (
  members: ExpenseFormMemberInfo[],
): ExpenseGroupFormValue => ({
  id: crypto.randomUUID(),
  payer: null,
  paidAt: new Date().toISOString().slice(0, 10),
  isOverseas: false,
  currency: "USD",
  bankName: "",
  accountNumber: "",
  items: [createEmptyExpenseItem(members)],
});
