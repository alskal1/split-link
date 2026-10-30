import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import BottomSheet from "../components/BottomSheet";
import Modal from "../components/Modal";
import QrCode from "../components/QrCode";
import { isMobileDevice } from "../utils/device";
import { formatAmount } from "../utils/currency";
import type {
  SettlementReceiveItem,
  SettlementSendItem,
} from "../types/settlementType";

interface SettlementSummaryProps {
  roomTitle: string;
  memberCount: number;
  totalAmount: number;
  baseCurrency: string;
  myName: string;
  sendList: SettlementSendItem[];
  receiveList: SettlementReceiveItem[];
  pendingSettlementIds: number[];
  onClose: () => void;
  onComplete: () => void;
  onToggleDone: (settlementId: number, isDone: boolean) => void;
}

/**
 * 정산 계산하기 클릭 시 뜨는 정산 요약 모달 (서버가 계산·저장한 정산 내역 표시)
 * 결제자이면서 동시에 송금도 해야 하는 경우가 있으므로, 받을 금액과 보낼 금액을 각각 있는 만큼 모두 보여준다.
 * @param roomTitle 방 제목
 * @param memberCount 참여 멤버 수
 * @param totalAmount 방 전체 지출 합계
 * @param baseCurrency 정산방 기준통화
 * @param myName 내 이름
 * @param sendList 결제자별로 보낼 금액 목록
 * @param receiveList 나에게 보내야 할 사람별 받을 금액 목록
 * @param pendingSettlementIds 완료 상태 변경 요청이 진행 중인 정산 내역 PK 목록
 * @param onClose 닫기 이벤트
 * @param onComplete 정산완료 이벤트
 * @param onToggleDone 개별 항목 완료 상태 토글 이벤트
 */
export default function SettlementSummary({
  roomTitle,
  memberCount,
  totalAmount,
  baseCurrency,
  myName,
  sendList,
  receiveList,
  pendingSettlementIds,
  onClose,
  onComplete,
  onToggleDone,
}: SettlementSummaryProps) {
  // 계좌 복사 완료 표시 중인 정산 내역 PK
  const [copiedId, setCopiedId] = useState<number | null>(null);

  // 복사 완료 표시를 2초 뒤 원복
  useEffect(() => {
    if (copiedId === null) {
      return;
    }

    const timer = setTimeout(() => setCopiedId(null), 2000);

    return () => clearTimeout(timer);
  }, [copiedId]);

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
      setCopiedId(item.settlementId);
    } catch (error) {
      toast.error("복사에 실패했어요");
    }
  };

  const hasCredits = receiveList.length > 0;
  const hasDebts = sendList.length > 0;
  const isEven = !hasCredits && !hasDebts;

  const doneCreditsCount = receiveList.filter((item) => item.isDone).length;
  const doneDebtsCount = sendList.filter((item) => item.isDone).length;

  // 내 보낼/받을 항목이 모두 완료 체크된 경우에만 정산완료 가능
  const isAllDone =
    doneCreditsCount === receiveList.length &&
    doneDebtsCount === sendList.length;

  const receiveTotal = receiveList.reduce((sum, item) => sum + item.amount, 0);
  const sendTotal = sendList.reduce((sum, item) => sum + item.amount, 0);
  const receivedTotal = receiveList
    .filter((item) => item.isDone)
    .reduce((sum, item) => sum + item.amount, 0);
  const progressPct =
    receiveTotal > 0 ? (receivedTotal / receiveTotal) * 100 : 0;

  // 받을 금액을 먼저, 보낼 금액을 이어서 한 목록으로 표시
  const rows = [
    ...receiveList.map((item) => ({
      kind: "receive" as const,
      settlementId: item.settlementId,
      isDone: item.isDone,
      from: item.senderName,
      to: myName,
      amount: item.amount,
      send: null,
    })),
    ...sendList.map((item) => ({
      kind: "send" as const,
      settlementId: item.settlementId,
      isDone: item.isDone,
      from: myName,
      to: item.receiverName,
      amount: item.amount,
      send: item,
    })),
  ];

  const pillClass =
    "flex h-[30px] shrink-0 cursor-pointer items-center gap-1 whitespace-nowrap rounded-full border border-line bg-white px-2.5 text-xs font-bold active:scale-95";

  return (
    <>
      <BottomSheet
        onClose={onClose}
        footer={
          <div className="grid grid-cols-[1.4fr_1fr] gap-2">
            <button
              type="button"
              className="cursor-pointer rounded-[14px] bg-ink p-4 text-[15px] font-extrabold text-white active:scale-[.97] disabled:cursor-default disabled:opacity-40"
              disabled={!isAllDone || pendingSettlementIds.length > 0}
              onClick={onComplete}
            >
              정산완료
            </button>
            <button
              type="button"
              className="cursor-pointer rounded-[14px] border-[1.5px] border-line bg-transparent p-4 text-[15px] font-bold active:scale-[.97]"
              onClick={onClose}
            >
              닫기
            </button>
          </div>
        }
      >
        <div className="rounded-[20px] border border-line bg-white p-5 shadow-[0_1px_0_rgba(18,18,18,0.05),0_20px_40px_-24px_rgba(18,18,18,0.22)]">
          <div className="flex items-end justify-between gap-3 border-b border-dashed border-line pb-4">
            <div className="min-w-0">
              <div className="mb-1 truncate text-xs text-ink-soft">
                {roomTitle} · {memberCount}명
              </div>
              <div className="text-[26px] font-extrabold tracking-[-0.03em] tabular-nums">
                {formatAmount(totalAmount, baseCurrency)}
              </div>
            </div>
          </div>

          {hasCredits && (
            <div className="pt-3.5 pb-1.5">
              <div className="mb-2 flex justify-between text-xs text-ink-soft tabular-nums">
                <span>받은 금액</span>
                <span className="whitespace-nowrap">
                  <b className="text-ink">
                    {formatAmount(receivedTotal, baseCurrency)}
                  </b>{" "}
                  / {formatAmount(receiveTotal, baseCurrency)}
                </span>
              </div>
              <div className="h-1.5 overflow-hidden rounded-mdbg-[#edece7]">
                <div
                  className="h-full rounded-md bg-brand transition-[width] duration-500"
                  style={{ width: `${progressPct}%` }}
                />
              </div>
            </div>
          )}

          <div className="flex flex-col">
            {rows.map((row) => {
              const isPending = pendingSettlementIds.includes(row.settlementId);
              const item = row.send;
              const hasAccount = !!item && !!item.accountNumber;

              return (
                <div
                  key={`${row.kind}-${row.settlementId}`}
                  role="button"
                  className={`-mx-2.5 rounded-[10px] border-b border-line px-2.5 py-3.5 transition-colors ${
                    row.isDone ? "bg-brand-soft/50" : ""
                  } ${isPending ? "cursor-default opacity-60" : "cursor-pointer"}`}
                  onClick={() => {
                    if (!isPending) {
                      onToggleDone(row.settlementId, !row.isDone);
                    }
                  }}
                >
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex min-w-0 items-center gap-2 text-[15px] font-semibold">
                      <span
                        className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full border-[1.5px] text-white transition-colors ${
                          row.isDone
                            ? "border-brand bg-brand"
                            : "border-[#d3d1cb] bg-transparent"
                        }`}
                      >
                        <svg
                          width="11"
                          height="11"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="3.5"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          style={{ opacity: row.isDone ? 1 : 0 }}
                        >
                          <path d="M20 6 9 17l-5-5" />
                        </svg>
                      </span>
                      <span className="truncate">{row.from}</span>
                      <span className="text-ink-faint">→</span>
                      <span className="truncate">{row.to}</span>
                    </div>
                    <div className="flex shrink-0 items-center gap-2.5">
                      <span className="text-[15px] font-bold tabular-nums">
                        {formatAmount(row.amount, baseCurrency)}
                      </span>
                      <span
                        className={`rounded-md px-2 py-1 text-[11px] font-bold transition-colors ${
                          row.isDone
                            ? "bg-brand-soft text-brand"
                            : row.kind === "send"
                              ? "bg-[#fbeeea] text-danger"
                              : "bg-paper text-ink-soft"
                        }`}
                      >
                        {row.isDone
                          ? "완료"
                          : row.kind === "send"
                            ? "보낼 돈"
                            : "받을 돈"}
                      </span>
                    </div>
                  </div>

                  {item && (
                    <div className="mt-2 flex items-center gap-1.5">
                      <span
                        className={`min-w-0 flex-1 truncate text-xs tabular-nums ${
                          hasAccount ? "text-ink-soft" : "text-ink-faint"
                        }`}
                      >
                        {hasAccount
                          ? `${item.bankName} ${item.accountNumber}`
                          : "등록된 계좌가 없어요"}
                      </span>
                      {hasAccount && (
                        <button
                          type="button"
                          aria-label="계좌번호 복사"
                          className={pillClass}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleCopy(item);
                          }}
                        >
                          <svg
                            width="12"
                            height="12"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2.4"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          >
                            <rect width="14" height="14" x="8" y="8" rx="2" />
                            <path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2" />
                          </svg>
                          {copiedId === item.settlementId ? "복사됨" : "복사"}
                        </button>
                      )}
                      <button
                        type="button"
                        aria-label="토스로 송금"
                        className={pillClass}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleRemitClick(item);
                        }}
                      >
                        토스 송금
                        <svg
                          width="12"
                          height="12"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2.4"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        >
                          <path d="M7 17 17 7" />
                          <path d="M7 7h10v10" />
                        </svg>
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {isEven && (
            <div className="pt-[18px] pb-1 text-center text-sm text-ink-soft">
              주고받을 금액이 없어요.
            </div>
          )}

          {!isEven && (
            <div className="mt-3 text-xs text-ink-soft">
              항목을 누르면 완료로 체크돼요. 모두 체크하면 정산을 완료할 수
              있어요.
            </div>
          )}
        </div>
      </BottomSheet>

      {qrModalItem && qrModalItem.remittanceLink && (
        <Modal title="QR로 송금하기" onClose={() => setQrModalItem(null)}>
          <div className="flex flex-col items-center space-y-3">
            <QrCode value={qrModalItem.remittanceLink} size={200} />
            <div className="text-center text-[10pt] text-[#281c18]">
              휴대폰 토스 앱 카메라로 QR코드를 스캔해서
              <br />
              {qrModalItem.receiverName}님에게{" "}
              {formatAmount(qrModalItem.amount, baseCurrency)}을 송금해주세요
            </div>
          </div>
        </Modal>
      )}
    </>
  );
}
