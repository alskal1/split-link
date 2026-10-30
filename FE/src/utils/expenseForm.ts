import type {
  ExpenseAccountInfo,
  ExpenseFormMemberInfo,
  ExpenseGroupFormValue,
  ExpenseItemFormValue,
} from "../types/expenseType";

/**
 * 폼 항목 식별용 id 생성
 * crypto.randomUUID는 보안 컨텍스트(HTTPS/localhost)에서만 존재하므로 LAN IP(http) 접속 시를 위해 대체 방식 사용
 */
const createId = (): string => {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }

  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
};

/**
 * 빈 지출 항목 생성
 * @param members 기본으로 선택할 참여자 목록 (기본 전체 선택)
 */
export const createEmptyExpenseItem = (
  members: ExpenseFormMemberInfo[],
): ExpenseItemFormValue => ({
  id: createId(),
  name: "",
  amount: "",
  participants: members.map((member) => member.memberId),
});

/**
 * 빈 지출 그룹(결제자 · 날짜 단위) 생성
 * @param members 기본으로 선택할 참여자 목록
 * @param defaultAccount 본인 결제 시 기본으로 채울 계좌 정보 (미등록 시 null)
 */
export const createEmptyExpenseGroup = (
  members: ExpenseFormMemberInfo[],
  defaultAccount: ExpenseAccountInfo | null = null,
): ExpenseGroupFormValue => ({
  id: createId(),
  payer: null,
  paidAt: new Date().toISOString().slice(0, 10),
  isOverseas: false,
  currency: "USD",
  bankName: defaultAccount?.bankName ?? "",
  accountNumber: defaultAccount?.accountNumber ?? "",
  items: [createEmptyExpenseItem(members)],
});

/**
 * 로컬 시간 기준 yyyy-mm-dd 문자열 변환
 * @param date 변환할 날짜
 */
export const toLocalIso = (date: Date): string => {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};
