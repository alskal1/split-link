import toast from "react-hot-toast";
import Modal from "../components/Modal";
import Button from "../components/Button";

/**
 * 결제자에게 내가 보내야 할 정산 금액
 * @param payer 결제자
 * @param amount 보낼 금액
 * @param bankName 결제자 계좌 은행명
 * @param accountNumber 결제자 계좌번호
 */
export interface SettlementDebt {
  payer: string;
  amount: number;
  bankName: string;
  accountNumber: string;
}

/**
 * 나에게 보내야 할 사람과 받을 금액
 * @param debtor 나에게 보낼 사람
 * @param amount 받을 금액
 */
export interface SettlementCredit {
  debtor: string;
  amount: number;
}

interface SettlementSummaryProps {
  debts: SettlementDebt[];
  credits: SettlementCredit[];
  onClose: () => void;
  onComplete: () => void;
}

/**
 * 정산 계산하기 클릭 시 뜨는 정산 요약 모달
 * 결제자이면서 동시에 송금도 해야 하는 경우가 있으므로, 받을 금액과 보낼 금액을 각각 있는 만큼 모두 보여준다.
 * @param debts 결제자별로 보낼 금액 목록
 * @param credits 나에게 보내야 할 사람별 받을 금액 목록
 * @param onClose 닫기 이벤트
 * @param onComplete 정산완료 이벤트
 */
export default function SettlementSummary({
  debts,
  credits,
  onClose,
  onComplete,
}: SettlementSummaryProps) {
  /**
   * 계좌 정보 복사
   * @param debt 복사할 계좌 정보를 가진 정산 대상
   */
  const handleCopy = async (debt: SettlementDebt) => {
    try {
      await navigator.clipboard.writeText(
        `${debt.bankName} ${debt.accountNumber}`,
      );
      toast.success("계좌 정보가 복사됐어요");
    } catch (error) {
      toast.error("복사에 실패했어요");
    }
  };

  const hasCredits = credits.length > 0;
  const hasDebts = debts.length > 0;

  return (
    <Modal title="정산 요약" onClose={onClose}>
      {!hasCredits && !hasDebts && (
        <div className="text-[10pt] text-[#281c18]">정산할 금액이 없어요.</div>
      )}

      {hasCredits && (
        <div className="flex flex-col space-y-3">
          <span className="font-bold">받을 금액</span>
          <div className="text-[10pt] text-[#281c18]">
            아래 인원에게 정산 금액을 받아주세요.
          </div>
          <div className="flex flex-col space-y-4">
            {credits.map((credit) => (
              <div
                key={credit.debtor}
                className="flex items-center justify-between rounded-[10px] bg-[#fdf3eb] p-4"
              >
                <span className="font-bold">{credit.debtor}</span>
                <span className="font-bold text-[#e85a48]">
                  {credit.amount.toLocaleString()}원
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {hasDebts && (
        <div className="flex flex-col space-y-3">
          <span className="font-bold">보낼 금액</span>
          <div className="text-[10pt] text-[#281c18]">
            아래 결제자에게 정산 금액을 보내주세요.
          </div>
          <div className="flex flex-col space-y-4">
            {debts.map((debt) => (
              <div
                key={debt.payer}
                className="flex flex-col space-y-3 rounded-[10px] bg-[#fdf3eb] p-4"
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold">{debt.payer}</span>
                  <span className="font-bold text-[#e85a48]">
                    {debt.amount.toLocaleString()}원
                  </span>
                </div>

                <div className="flex items-center justify-between rounded-[10px] bg-white p-3">
                  <span className="text-[10pt]">
                    {debt.bankName} {debt.accountNumber}
                  </span>
                  <Button
                    title="복사"
                    bgColor="#000"
                    textColor="#fff"
                    className="rounded-[10px] py-1.5 text-[10pt]"
                    onClick={() => handleCopy(debt)}
                  />
                </div>

                <Button
                  title="토스로 송금하기"
                  bgColor="#1b64da"
                  textColor="#fff"
                  className="w-full rounded-[10px]"
                />
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="flex items-center space-x-3">
        <Button
          title="정산완료"
          bgColor="#e85a48"
          textColor="#fff"
          className="flex-1 rounded-[10px]"
          onClick={onComplete}
        />
        <Button
          title="닫기"
          bgColor="#fff"
          textColor="#281c18"
          className="flex-1 rounded-[10px] border border-[#e6dfd9]"
          onClick={onClose}
        />
      </div>
    </Modal>
  );
}
