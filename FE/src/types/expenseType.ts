/**
 * 지출 항목 (항목명 · 금액 · 참여자)
 * @param id 항목 식별자 (프론트 전용)
 * @param name 항목명
 * @param amount 금액 (입력 중엔 문자열로 관리, 제출 시 숫자로 변환)
 * @param participants 참여자 멤버 PK 목록
 */
export interface ExpenseItemFormValue {
  id: string;
  name: string;
  amount: string;
  participants: number[];
}

/**
 * 결제자 · 날짜 단위의 지출 그룹
 * @param id 그룹 식별자 (프론트 전용)
 * @param payer 결제자 멤버 PK (미선택 시 null)
 * @param paidAt 결제일자 (yyyy-mm-dd)
 * @param isOverseas 해외결제 여부
 * @param currency 화폐단위 (해외결제 시에만 사용)
 * @param bankName 송금받을 계좌 은행명
 * @param accountNumber 송금받을 계좌번호
 * @param items 그룹에 속한 지출 항목 목록
 */
export interface ExpenseGroupFormValue {
  id: string;
  payer: number | null;
  paidAt: string;
  isOverseas: boolean;
  currency: string;
  bankName: string;
  accountNumber: string;
  items: ExpenseItemFormValue[];
}

/**
 * 지출 내역 정산 집계용 참여자 부담금 (서버가 계산한 1원 오차 보정 완료 값)
 * @param name 참여자 이름
 * @param shareAmount 해당 참여자의 부담금
 * @param isSelf 현재 접속자 본인 여부
 */
export interface ExpenseRecordParticipantShare {
  name: string;
  shareAmount: number;
  isSelf: boolean;
}

/**
 * 등록 완료된 지출 내역 (결제내역 리스트 표시 / 정산 집계용)
 * 지출 상세 조회(ExpenseDetailResponse) 응답을 정산 집계용으로 가공한 값
 * @param id 항목 식별자
 * @param name 항목명
 * @param amount 금액
 * @param paidAt 결제일자 (yyyy-mm-dd)
 * @param payer 결제자
 * @param bankName 송금받을 계좌 은행명
 * @param accountNumber 송금받을 계좌번호
 * @param isMyPayment 현재 접속자가 결제자인지 여부
 * @param participantShares 참여자별 부담금 목록
 */
export interface ExpenseRecord {
  id: string;
  name: string;
  amount: number;
  paidAt: string;
  payer: string;
  bankName: string;
  accountNumber: string;
  isMyPayment: boolean;
  participantShares: ExpenseRecordParticipantShare[];
}

/**
 * 로그인한 사용자의 정산 상태
 * - PENDING: 방 정산 미마감 (지출 입력 중)
 * - RECEIVE / SEND / ZERO: 정산 마감 후 받을 금액 / 보낼 금액 / 0원
 */
export type SettlementStatus = "RECEIVE" | "SEND" | "ZERO" | "PENDING";

/**
 * 지출 내역 목록 표시용 항목 응답
 * @param expenseId 지출 PK
 * @param title 지출 항목명
 * @param amount 결제 금액
 * @param payerName 결제자 이름
 * @param isMyPayment 현재 접속자가 결제자인지 여부
 * @param targetMemberCount 정산 참여 인원 수
 */
export interface ExpenseItemResponse {
  expenseId: number;
  title: string;
  amount: number;
  payerName: string;
  isMyPayment: boolean;
  targetMemberCount: number;
}

/**
 * 지출 목록 및 상단 정산 요약 정보 응답
 * @param roomTitle 방 제목
 * @param currentMemberName 현재 접속한 사용자 이름
 * @param isLocked 방 정산 마감 여부
 * @param totalExpenseAmount 방 전체 총 지출 금액
 * @param settlementStatus 로그인한 사용자의 정산 상태
 * @param mySettlementAmount 정산 필요 금액 (마감 전에는 null)
 * @param expenses 등록된 지출 목록
 */
export interface ExpenseListResponse {
  roomTitle: string;
  currentMemberName: string;
  isLocked: boolean;
  totalExpenseAmount: number;
  settlementStatus: SettlementStatus;
  mySettlementAmount: number | null;
  expenses: ExpenseItemResponse[];
}

/**
 * 지출 상세 조회 응답의 참여자 단건 (부담금 정보)
 * @param memberId 참여자 PK
 * @param name 참여자 이름
 * @param shareAmount 개별 최종 부담금 (1원 오차 보정)
 * @param isSelf 현재 접속자 본인 여부
 */
export interface ExpenseDetailTargetMember {
  memberId: number;
  name: string;
  shareAmount: number;
  isSelf: boolean;
}

/**
 * 지출 단건 상세 조회 응답
 * @param expenseId 지출 PK
 * @param title 지출 항목명
 * @param amount 결제 금액
 * @param currency 결제 통화 (예: KRW, USD)
 * @param fxRate 환율
 * @param spentAt 결제 일시 (ISO-8601)
 * @param payerId 결제자 PK
 * @param payerName 결제자 이름
 * @param bankName 송금받을 계좌 은행명
 * @param accountNumber 송금받을 계좌번호
 * @param isMyPayment 현재 접속자가 결제자인지 여부
 * @param targetMembers 지출 참여자 목록 (부담금 정보)
 */
export interface ExpenseDetailResponse {
  expenseId: number;
  title: string;
  amount: number;
  currency: string;
  fxRate: number;
  spentAt: string;
  payerId: number;
  payerName: string;
  bankName: string;
  accountNumber: string;
  isMyPayment: boolean;
  targetMembers: ExpenseDetailTargetMember[];
}

/**
 * 지출 입력 폼용 정산 계좌 정보
 * @param bankName 은행명
 * @param accountNumber 계좌번호
 */
export interface ExpenseAccountInfo {
  bankName: string;
  accountNumber: string;
}

/**
 * 지출 입력 폼용 멤버 단건
 * @param memberId 멤버 PK
 * @param name 멤버 이름
 * @param isActive 방 진입 완료 여부
 * @param isSelf 현재 로그인한 본인 여부
 */
export interface ExpenseFormMemberInfo {
  memberId: number;
  name: string;
  isActive: boolean;
  isSelf: boolean;
}

/**
 * 지출 입력 폼 초기화 정보 응답
 * @param currentMemberId 현재 접속한 멤버 PK
 * @param defaultAccount 현재 멤버의 정산용 계좌 정보 (미등록 시 null)
 * @param roomMembers 방 내 전체 참여 멤버 목록
 */
export interface ExpenseFormInitResponse {
  currentMemberId: number;
  defaultAccount: ExpenseAccountInfo | null;
  roomMembers: ExpenseFormMemberInfo[];
}

/**
 * 지출 수정 폼용 멤버 단건
 * @param memberId 멤버 PK
 * @param name 멤버 이름
 * @param isActive 방 진입 완료 여부
 */
export interface ExpenseUpdateFormMemberInfo {
  memberId: number;
  name: string;
  isActive: boolean;
}

/**
 * 지출 내역 수정 폼 조회 응답
 * @param expenseId 지출 PK
 * @param title 지출 항목명
 * @param amount 결제 금액
 * @param currency 결제 통화 (수정 불가, 표시용)
 * @param spentAt 결제 일시 (ISO-8601)
 * @param payerId 결제자 PK
 * @param targetMemberIds 지출에 지정된 참여자 PK 목록
 * @param roomMembers 결제자/참여자 선택용 방 전체 멤버 목록
 */
export interface ExpenseUpdateFormResponse {
  expenseId: number;
  title: string;
  amount: number;
  currency: string;
  spentAt: string;
  payerId: number;
  targetMemberIds: number[];
  roomMembers: ExpenseUpdateFormMemberInfo[];
}

/**
 * 지출 내역 수정 요청
 * @param payerId 결제자 PK
 * @param title 지출 항목명
 * @param amount 결제 금액
 * @param spentAt 결제 일시 (ISO-8601, 예: 2026-09-17T00:00:00)
 * @param targetMemberIds 함께 정산할 참여 멤버 ID 목록
 */
export interface ExpenseUpdateRequest {
  payerId: number;
  title: string;
  amount: number;
  spentAt: string;
  targetMemberIds: number[];
}

/**
 * 지출 항목 등록 요청
 * @param title 지출 항목명
 * @param amount 결제 금액
 * @param targetMemberIds 함께 정산할 참여 멤버 ID 목록
 */
export interface ExpenseItemRequest {
  title: string;
  amount: number;
  targetMemberIds: number[];
}

/**
 * 결제자 · 결제일시 · 통화 · 계좌 그룹 등록 요청
 * @param payerId 결제자 PK
 * @param spentAt 결제 일시 (ISO-8601, 예: 2026-09-17T00:00:00)
 * @param currency 결제 통화 (해외결제 아닐 경우 생략)
 * @param bankName 정산 계좌 은행명
 * @param accountNumber 정산 계좌번호
 * @param items 그룹에 속한 지출 항목 목록
 */
export interface ExpenseGroupRequest {
  payerId: number;
  spentAt: string;
  currency?: string;
  bankName: string;
  accountNumber: string;
  items: ExpenseItemRequest[];
}

/**
 * 지출 일괄 등록 요청 바디
 * @param expenseGroups 등록할 지출 그룹 목록
 */
export interface ExpenseBatchCreateRequest {
  expenseGroups: ExpenseGroupRequest[];
}
