/**
 * 내가 보내야 할 정산 대상 단건 (Debtor -> Creditor)
 * @param settlementId 정산 내역 PK
 * @param receiverId 받는 사람(결제자) 멤버 PK
 * @param receiverName 받는 사람 이름
 * @param amount 보낼 금액
 * @param bankName 받는 사람 계좌 은행명
 * @param accountNumber 받는 사람 계좌번호
 * @param remittanceLink 토스 송금 딥링크 (계좌 정보가 없으면 null)
 * @param isDone 송금 완료 여부
 */
export interface SettlementSendItem {
  settlementId: number;
  receiverId: number;
  receiverName: string;
  amount: number;
  bankName: string;
  accountNumber: string;
  remittanceLink: string | null;
  isDone: boolean;
}

/**
 * 내가 받아야 할 정산 대상 단건 (Creditor <- Debtor)
 * @param settlementId 정산 내역 PK
 * @param senderId 보내는 사람 멤버 PK
 * @param senderName 보내는 사람 이름
 * @param amount 받을 금액
 * @param isDone 송금 완료 여부
 */
export interface SettlementReceiveItem {
  settlementId: number;
  senderId: number;
  senderName: string;
  amount: number;
  isDone: boolean;
}

/**
 * 내 정산 내역 조회 응답 (정산 계산하기 실행 후 조회)
 * @param totalSendAmount 내가 보낼 총 금액
 * @param totalReceiveAmount 내가 받을 총 금액
 * @param sendList 내가 보낼 대상 목록
 * @param receiveList 내가 받을 대상 목록
 */
export interface RoomMySettlementResponse {
  totalSendAmount: number;
  totalReceiveAmount: number;
  sendList: SettlementSendItem[];
  receiveList: SettlementReceiveItem[];
}
