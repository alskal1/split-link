import { useMemo, useState } from "react";
import toast from "react-hot-toast";
import Modal from "../components/Modal";
import Button from "../components/Button";
import QrCode from "../components/QrCode";
import { isMobileDevice } from "../utils/device";
import type {
  SettlementReceiveItem,
  SettlementSendItem,
} from "../types/settlementType";

interface SettlementSummaryProps {
  sendList: SettlementSendItem[];
  receiveList: SettlementReceiveItem[];
  pendingSettlementIds: number[];
  onClose: () => void;
  onComplete: () => void;
  onToggleDone: (settlementId: number, isDone: boolean) => void;
}

/**
 * 완료 여부를 토글하는 체크박스 (금액 옆에 붙여 표시)
 * @param isDone 완료 여부
 * @param disabled 비활성화 여부 (상태 변경 요청 진행 중)
 * @param onChange 토글 변경 이벤트
 */
function DoneCheckbox({
  isDone,
  disabled,
  onChange,
}: {
  isDone: boolean;
  disabled: boolean;
  onChange: () => void;
}) {
  return (
    <input
      type="checkbox"
      checked={isDone}
      disabled={disabled}
      onChange={onChange}
      aria-label="완료 여부"
      className="h-5 w-5 shrink-0 cursor-pointer accent-[#e85a48] disabled:cursor-default disabled:opacity-50"
    />
  );
}

/**
 * 정산 계산하기 클릭 시 뜨는 정산 요약 모달 (서버가 계산·저장한 정산 내역 표시)
 * 결제자이면서 동시에 송금도 해야 하는 경우가 있으므로, 받을 금액과 보낼 금액을 각각 있는 만큼 모두 보여준다.
 * @param sendList 결제자별로 보낼 금액 목록
 * @param receiveList 나에게 보내야 할 사람별 받을 금액 목록
 * @param pendingSettlementIds 완료 상태 변경 요청이 진행 중인 정산 내역 PK 목록
 * @param onClose 닫기 이벤트
 * @param onComplete 정산완료 이벤트
 * @param onToggleDone 개별 항목 완료 상태 토글 이벤트
 */
export default function SettlementSummary({
  sendList,
  receiveList,
  pendingSettlementIds,
  onClose,
  onComplete,
  onToggleDone,
}: SettlementSummaryProps) {
  // QR 모달에 띄울 대상 (모바일이 아닌 환경에서 "토스로 송금하기" 클릭 시 설정)
  const [qrModalItem, setQrModalItem] = useState<SettlementSendItem | null>(
    null,
  );

  const isMobile = useMemo(() => isMobileDevice(), []);

  /**
   * "토스로 송금하기" 클릭 처리
   * 모바일 웹에서는 토스 앱으로 바로 이동하고, 딥링크를 실행할 수 없는 환경(데스크톱 등)에서는
   * 휴대폰으로 스캔할 수 있는 QR 모달을 띄운다.
   * @param item 송금 대상
   */
  const handleRemitClick = (item: SettlementSendItem) => {
    if (!item.remittanceLink) {
      toast.error("계좌 정보가 없어 송금 링크를 열 수 없어요");
      return;
    }

    if (isMobile) {
      window.location.href = item.remittanceLink;
      return;
    }

    setQrModalItem(item);
  };

  /**
   * 계좌 정보 복사
   * @param item 복사할 계좌 정보를 가진 정산 대상
   */
  const handleCopy = async (item: SettlementSendItem) => {
    try {
      await navigator.clipboard.writeText(
        `${item.bankName} ${item.accountNumber}`,
      );
      toast.success("계좌 정보가 복사됐어요");
    } catch (error) {
      toast.error("복사에 실패했어요");
    }
  };

  const hasCredits = receiveList.length > 0;
  const hasDebts = sendList.length > 0;

  const doneCreditsCount = receiveList.filter((item) => item.isDone).length;
  const doneDebtsCount = sendList.filter((item) => item.isDone).length;

  // 내 보낼/받을 항목이 모두 완료 체크된 경우에만 정산완료 가능
  const isAllDone =
    doneCreditsCount === receiveList.length &&
    doneDebtsCount === sendList.length;

  return (
    <>
      <Modal title="정산 요약" onClose={onClose}>
        {!hasCredits && !hasDebts && (
          <div className="text-[10pt] text-[#281c18]">
            정산할 금액이 없어요.
          </div>
        )}

        {hasCredits && (
          <div className="flex flex-col space-y-3">
            <div className="flex items-center justify-between">
              <span className="font-bold">받을 금액</span>
              <span className="text-[9pt] text-[#281c18]">
                {doneCreditsCount}/{receiveList.length}명 완료
              </span>
            </div>
            <div className="text-[10pt] text-[#281c18]">
              아래 인원에게 정산 금액을 받아주세요.
            </div>
            <div className="flex flex-col space-y-4">
              {receiveList.map((item) => (
                <div
                  key={item.settlementId}
                  className={`flex items-center justify-between rounded-[10px] p-4 ${
                    item.isDone ? "bg-gray-100" : "bg-[#fdf3eb]"
                  }`}
                >
                  <span
                    className={`font-bold ${
                      item.isDone ? "text-gray-400" : ""
                    }`}
                  >
                    {item.senderName}
                  </span>
                  <div className="flex items-center space-x-3">
                    <span
                      className={`font-bold ${
                        item.isDone
                          ? "text-gray-400 line-through"
                          : "text-[#e85a48]"
                      }`}
                    >
                      {item.amount.toLocaleString()}원
                    </span>
                    <DoneCheckbox
                      isDone={item.isDone}
                      disabled={pendingSettlementIds.includes(item.settlementId)}
                      onChange={() =>
                        onToggleDone(item.settlementId, !item.isDone)
                      }
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {hasDebts && (
          <div className="flex flex-col space-y-3">
            <div className="flex items-center justify-between">
              <span className="font-bold">보낼 금액</span>
              <span className="text-[9pt] text-[#281c18]">
                {doneDebtsCount}/{sendList.length}명 완료
              </span>
            </div>
            <div className="text-[10pt] text-[#281c18]">
              아래 결제자에게 정산 금액을 보내주세요.
            </div>
            <div className="flex flex-col space-y-4">
              {sendList.map((item) => (
                <div
                  key={item.settlementId}
                  className={`flex flex-col space-y-3 rounded-[10px] p-4 ${
                    item.isDone ? "bg-gray-100" : "bg-[#fdf3eb]"
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span
                      className={`font-bold ${
                        item.isDone ? "text-gray-400" : ""
                      }`}
                    >
                      {item.receiverName}
                    </span>
                    <div className="flex items-center space-x-3">
                      <span
                        className={`font-bold ${
                          item.isDone
                            ? "text-gray-400 line-through"
                            : "text-[#e85a48]"
                        }`}
                      >
                        {item.amount.toLocaleString()}원
                      </span>
                      <DoneCheckbox
                        isDone={item.isDone}
                        disabled={pendingSettlementIds.includes(
                          item.settlementId,
                        )}
                        onChange={() =>
                          onToggleDone(item.settlementId, !item.isDone)
                        }
                      />
                    </div>
                  </div>

                  <div
                    className={`flex items-center justify-between rounded-[10px] p-3 ${
                      item.isDone ? "bg-gray-50" : "bg-white"
                    }`}
                  >
                    <span
                      className={`text-[10pt] ${
                        item.isDone ? "text-gray-400" : ""
                      }`}
                    >
                      {item.bankName} {item.accountNumber}
                    </span>
                    <Button
                      title="복사"
                      bgColor="#000"
                      textColor="#fff"
                      className="rounded-[10px] py-1.5 text-[10pt]"
                      onClick={() => handleCopy(item)}
                    />
                  </div>

                  <Button
                    title="토스로 송금하기"
                    bgColor="#1b64da"
                    textColor="#fff"
                    className="w-full rounded-[10px]"
                    onClick={() => handleRemitClick(item)}
                  />
                </div>
              ))}
            </div>
          </div>
        )}

        {!isAllDone && (
          <div className="text-center text-[9pt] text-[#281c18]">
            모든 송금을 체크하면 정산을 완료할 수 있어요.
          </div>
        )}

        <div className="flex items-center space-x-3">
          <Button
            title="정산완료"
            bgColor="#e85a48"
            textColor="#fff"
            className="flex-1 rounded-[10px]"
            disabled={!isAllDone || pendingSettlementIds.length > 0}
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

      {qrModalItem && qrModalItem.remittanceLink && (
        <Modal title="QR로 송금하기" onClose={() => setQrModalItem(null)}>
          <div className="flex flex-col items-center space-y-3">
            <QrCode value={qrModalItem.remittanceLink} size={200} />
            <div className="text-center text-[10pt] text-[#281c18]">
              휴대폰 토스 앱 카메라로 QR코드를 스캔해서
              <br />
              {qrModalItem.receiverName}님에게{" "}
              {qrModalItem.amount.toLocaleString()}원을 송금해주세요
            </div>
          </div>
        </Modal>
      )}
    </>
  );
}
