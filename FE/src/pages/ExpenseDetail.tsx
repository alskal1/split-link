import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import {
  deleteExpense,
  getExpenseUpdateForm,
  updateExpense,
} from "../api/expense";
import { BANK_OPTIONS } from "../constants/bank";
import {
  formatAmountInput,
  getCurrencyUnit,
  sanitizeAmountInput,
} from "../utils/currency";
import type { ExpenseDetailResponse } from "../types/expenseType";
import BottomSheet from "../components/BottomSheet";
import ConfirmModal from "../components/ConfirmModal";

interface ExpenseDetailProps {
  slug: string;
  expense: ExpenseDetailResponse;
  baseCurrency: string;
  isLocked: boolean;
  onClose: () => void;
  onUpdated: () => void;
  onDeleted: () => void;
}

/**
 * 결제내역 항목 클릭 시 뜨는 상세 바텀시트 (조회 · 수정 · 삭제)
 * @param slug 방 슬러그
 * @param expense 조회 대상 지출 상세 정보
 * @param baseCurrency 정산방 기준통화 (지출 결제 통화가 없을 때 금액 단위로 대체 표시)
 * @param isLocked 방 잠금 여부 (잠기면 수정 폼 조회 없이 조회 전용으로 표시하고 수정/삭제 불가)
 * @param onClose 닫기 이벤트
 * @param onUpdated 수정 완료 이벤트
 * @param onDeleted 삭제 완료 이벤트
 */
export default function ExpenseDetail({
  slug,
  expense,
  baseCurrency,
  isLocked,
  onClose,
  onUpdated,
  onDeleted,
}: ExpenseDetailProps) {
  // 수정 폼 초기 데이터(결제자/참여자 선택용 멤버 목록) 로딩 여부
  // (잠긴 방은 수정할 수 없어 수정 폼을 조회하지 않으므로 로딩하지 않음)
  const [isLoading, setIsLoading] = useState(!isLocked);
  // 수정 폼 초기 데이터 조회 실패 여부 (멤버 목록 없이 저장되는 것을 방지)
  const [isLoadFailed, setIsLoadFailed] = useState(false);
  // 수정 폼에서 선택 가능한 방 멤버 목록
  // (잠긴 방은 상세 정보에 있는 결제자와 참여자만으로 조회 전용 화면을 구성)
  const [roomMembers, setRoomMembers] = useState<
    { memberId: number; name: string }[]
  >(() =>
    isLocked
      ? [
          { memberId: expense.payerId, name: expense.payerName },
          ...expense.targetMembers.filter(
            (member) => member.memberId !== expense.payerId,
          ),
        ].map(({ memberId, name }) => ({ memberId, name }))
      : [],
  );

  // 수정 입력값 (항목명은 모달 제목으로만 표시, 별도 수정 UI 없음)
  const [title, setTitle] = useState(expense.title);
  const [amount, setAmount] = useState(String(expense.amount));
  const [spentAt, setSpentAt] = useState(expense.spentAt.slice(0, 10));
  const [payerId, setPayerId] = useState(String(expense.payerId));
  const [bankName, setBankName] = useState(expense.bankName ?? "");
  const [accountNumber, setAccountNumber] = useState(
    expense.accountNumber ?? "",
  );
  const [targetMemberIds, setTargetMemberIds] = useState<number[]>(
    expense.targetMembers.map((member) => member.memberId),
  );

  // 저장/삭제 진행 여부
  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false);

  useEffect(() => {
    if (isLocked) {
      return;
    }

    let cancelled = false;

    (async () => {
      try {
        const form = await getExpenseUpdateForm(slug, expense.expenseId);
        if (cancelled) {
          return;
        }

        if (!form) {
          setIsLoadFailed(true);
          return;
        }

        setRoomMembers(form.roomMembers);
        setTitle(form.title);
        setAmount(String(form.amount));
        setSpentAt(form.spentAt.slice(0, 10));
        setPayerId(String(form.payerId));
        setBankName(form.bankName ?? "");
        setAccountNumber(form.accountNumber ?? "");
        setTargetMemberIds(form.targetMemberIds);
      } catch (error) {
        if (cancelled) {
          return;
        }

        setIsLoadFailed(true);
        toast.error(
          error instanceof Error
            ? error.message
            : "지출 수정 정보를 불러오지 못했어요",
        );
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [slug, expense.expenseId, isLocked]);

  const isValid =
    title.trim().length > 0 &&
    Number(amount) > 0 &&
    payerId.length > 0 &&
    bankName.trim().length > 0 &&
    accountNumber.trim().length > 0 &&
    targetMemberIds.length > 0;

  // 계좌 정보는 지출의 결제자 본인만 수정 가능 (그 외에는 조회만 가능)
  const canEditAccount = expense.isMyPayment && !isLocked;
  // 기존 은행명이 선택지에 없는 값이어도 선택 상태가 유지되도록 옵션에 포함
  const bankOptions =
    bankName && !BANK_OPTIONS.some((option) => option.value === bankName)
      ? [...BANK_OPTIONS, { label: bankName, value: bankName }]
      : BANK_OPTIONS;

  /**
   * 참여자 선택 토글
   * @param memberId 참여자 PK
   */
  const toggleParticipant = (memberId: number) => {
    setTargetMemberIds((prev) =>
      prev.includes(memberId)
        ? prev.filter((id) => id !== memberId)
        : [...prev, memberId],
    );
  };

  /**
   * 수정사항 저장
   */
  const handleSave = async () => {
    if (!isValid || isSaving || isDeleting) {
      return;
    }

    setIsSaving(true);

    try {
      await updateExpense(slug, expense.expenseId, {
        payerId: Number(payerId),
        title: title.trim(),
        amount: Number(amount),
        bankName,
        accountNumber: accountNumber.trim(),
        spentAt: `${spentAt}T00:00:00`,
        targetMemberIds,
      });
      toast.success("결제내역을 수정했어요");
      onUpdated();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "결제내역 수정에 실패했어요",
      );
    } finally {
      setIsSaving(false);
    }
  };

  /**
   * 항목 삭제
   */
  const handleDelete = async () => {
    if (isDeleting || isSaving) {
      return;
    }

    setIsDeleteConfirmOpen(false);

    setIsDeleting(true);

    try {
      await deleteExpense(slug, expense.expenseId);
      toast.success("결제내역을 삭제했어요");
      onDeleted();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "결제내역 삭제에 실패했어요",
      );
    } finally {
      setIsDeleting(false);
    }
  };

  const canSave =
    !isLocked &&
    isValid &&
    !isSaving &&
    !isDeleting &&
    !isLoading &&
    !isLoadFailed;
  const fieldClass =
    "w-full rounded-xl border-[1.5px] border-line bg-white px-4 py-3.5 text-[15px] font-semibold outline-none transition-colors focus:border-brand disabled:cursor-not-allowed disabled:opacity-60";

  return (
    <>
      <BottomSheet
        title={expense.title}
        onClose={onClose}
        footer={
          <div className="grid grid-cols-[1fr_1.4fr] gap-2">
            <button
              type="button"
              disabled={isLocked || isDeleting || isSaving}
              onClick={() => setIsDeleteConfirmOpen(true)}
              className="cursor-pointer rounded-[14px] border-[1.5px] border-line bg-transparent p-4 text-[15px] font-bold text-danger disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isDeleting ? "삭제 중..." : "삭제"}
            </button>
            <button
              type="button"
              disabled={!canSave}
              onClick={handleSave}
              className={`rounded-[14px] p-4 text-[15px] font-extrabold text-white transition-colors ${
                canSave
                  ? "cursor-pointer bg-ink"
                  : "cursor-not-allowed bg-[#d3d1cb]"
              }`}
            >
              {isSaving ? "저장 중..." : "저장"}
            </button>
          </div>
        }
      >
        <div className="mb-4">
          <div className="mb-2 text-xs font-bold text-ink-soft">
            금액 ({getCurrencyUnit(expense.currency || baseCurrency)})
          </div>
          <input
            className={`${fieldClass} text-xl font-extrabold tabular-nums`}
            inputMode="decimal"
            value={formatAmountInput(amount)}
            disabled={isLocked}
            onChange={(e) => setAmount(sanitizeAmountInput(e.target.value))}
          />
        </div>

        <div className="mb-4 grid grid-cols-2 gap-2.5">
          <div className="min-w-0">
            <div className="mb-2 text-xs font-bold text-ink-soft">결제일자</div>
            <input
              type="date"
              className={`${fieldClass} min-w-0 appearance-none px-3`}
              value={spentAt}
              disabled={isLocked}
              onChange={(e) => setSpentAt(e.target.value)}
            />
          </div>
          <div className="min-w-0">
            <div className="mb-2 text-xs font-bold text-ink-soft">결제자</div>
            <div className="relative">
              <select
                className={`${fieldClass} min-w-0 appearance-none truncate pr-10 font-bold`}
                value={payerId}
                disabled={isLocked}
                onChange={(e) => setPayerId(e.target.value)}
              >
                <option value="">결제자 선택</option>
                {roomMembers.map((member) => (
                  <option key={member.memberId} value={String(member.memberId)}>
                    {member.name}
                  </option>
                ))}
              </select>
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#5F5E5A"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                className="pointer-events-none absolute top-1/2 right-3.5 -translate-y-1/2"
              >
                <path d="m6 9 6 6 6-6" />
              </svg>
            </div>
          </div>
        </div>

        <div className="mb-4">
          <div className="mb-2 text-xs font-bold text-ink-soft">
            송금받을 계좌
          </div>
          {canEditAccount ? (
            <div className="flex items-center gap-2">
              <select
                className={`${fieldClass} flex-1 font-bold`}
                value={bankName}
                onChange={(e) => setBankName(e.target.value)}
              >
                <option value="">은행 선택</option>
                {bankOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <input
                className={`${fieldClass} flex-1`}
                inputMode="numeric"
                placeholder="계좌번호"
                value={accountNumber}
                onChange={(e) =>
                  setAccountNumber(e.target.value.replace(/[^0-9-]/g, ""))
                }
              />
            </div>
          ) : (
            <div className="rounded-xl bg-white px-4 py-3.5 text-[15px] font-semibold">
              {bankName || accountNumber
                ? `${bankName} ${accountNumber}`.trim()
                : "등록된 계좌가 없어요"}
            </div>
          )}
        </div>

        <div className="mb-2">
          <div className="mb-2 text-xs font-bold text-ink-soft">참여자</div>
          <div className="flex flex-wrap gap-1.5">
            {roomMembers.map((member) => {
              const selected = targetMemberIds.includes(member.memberId);

              return (
                <button
                  key={member.memberId}
                  type="button"
                  disabled={isLocked}
                  onClick={() => toggleParticipant(member.memberId)}
                  className={`flex cursor-pointer items-center gap-[5px] rounded-full px-3.5 py-2 text-sm font-bold transition-colors active:scale-95 disabled:cursor-not-allowed ${
                    selected
                      ? "bg-ink text-white"
                      : "bg-[#edece7] text-ink-soft"
                  }`}
                >
                  <span
                    className={`flex ${selected ? "opacity-100" : "opacity-35"}`}
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
                    >
                      <path d="M20 6 9 17l-5-5" />
                    </svg>
                  </span>
                  {member.name}
                </button>
              );
            })}
          </div>
        </div>

        {isLocked && (
          <p className="mt-3 text-[13px] leading-[1.6] text-ink-soft">
            정산이 시작되어 결제내역을 수정하거나 삭제할 수 없어요.
          </p>
        )}

        {isLoadFailed && (
          <p className="mt-3 text-[13px] leading-[1.6] text-ink-soft">
            수정 정보를 불러오지 못해 저장할 수 없어요. 닫았다가 다시
            열어주세요.
          </p>
        )}
      </BottomSheet>
      {isDeleteConfirmOpen && (
        <ConfirmModal
          title="결제내역을 삭제할까요?"
          message="되돌릴 수 없어요."
          confirmText="삭제"
          variant="danger"
          onConfirm={handleDelete}
          onCancel={() => setIsDeleteConfirmOpen(false)}
        />
      )}
    </>
  );
}
