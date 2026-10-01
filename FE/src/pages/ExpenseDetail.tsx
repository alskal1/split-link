import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import {
  deleteExpense,
  getExpenseUpdateForm,
  updateExpense,
} from "../api/expense";
import { BANK_OPTIONS } from "../constants/bank";
import type { ExpenseDetailResponse } from "../types/expenseType";
import Modal from "../components/Modal";
import ConfirmModal from "../components/ConfirmModal";
import Button from "../components/Button";

interface ExpenseDetailProps {
  slug: string;
  expense: ExpenseDetailResponse;
  isLocked: boolean;
  onClose: () => void;
  onUpdated: () => void;
  onDeleted: () => void;
}

const INFO_BG_COLOR = "#fdf3eb";

/**
 * 결제내역 항목 클릭 시 뜨는 상세 모달 (조회 · 수정 · 삭제)
 * @param slug 방 슬러그
 * @param expense 조회 대상 지출 상세 정보
 * @param isLocked 방 잠금 여부 (잠기면 수정 폼 조회 없이 조회 전용으로 표시하고 수정/삭제 불가)
 * @param onClose 닫기 이벤트
 * @param onUpdated 수정 완료 이벤트
 * @param onDeleted 삭제 완료 이벤트
 */
export default function ExpenseDetail({
  slug,
  expense,
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
  const [bankName, setBankName] = useState(expense.bankName);
  const [accountNumber, setAccountNumber] = useState(expense.accountNumber);
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
    targetMemberIds.length > 0 &&
    bankName.length > 0 &&
    accountNumber.trim().length > 0;

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

  return (
    <>
      <Modal title={expense.title} onClose={onClose}>
        <div className="flex flex-col space-y-2">
          <div className="font-bold">금액</div>
          <input
            className="w-full rounded-[10px] border border-[#e6dfd9] p-3"
            style={{ backgroundColor: INFO_BG_COLOR }}
            value={amount}
            disabled={isLocked}
            onChange={(e) => setAmount(e.target.value.replace(/[^0-9]/g, ""))}
          />
        </div>

        <div className="flex flex-col space-y-2">
          <div className="font-bold">결제일자</div>
          <input
            type="date"
            className="w-full rounded-[10px] border border-[#e6dfd9] p-3"
            style={{ backgroundColor: INFO_BG_COLOR }}
            value={spentAt}
            disabled={isLocked}
            onChange={(e) => setSpentAt(e.target.value)}
          />
        </div>

        <div className="flex flex-col space-y-2">
          <div className="font-bold">결제자</div>
          <select
            className="w-full appearance-none rounded-[10px] border border-[#e6dfd9] p-3"
            style={{ backgroundColor: INFO_BG_COLOR }}
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
        </div>

        <div className="flex flex-col space-y-2">
          <div className="font-bold">은행명 · 계좌번호</div>
          <div className="flex items-center space-x-2">
            <select
              className="flex-1 appearance-none rounded-[10px] border border-[#e6dfd9] p-3"
              style={{ backgroundColor: INFO_BG_COLOR }}
              value={bankName}
              disabled={isLocked}
              onChange={(e) => setBankName(e.target.value)}
            >
              <option value="">은행명</option>
              {BANK_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <input
              className="flex-1 rounded-[10px] border border-[#e6dfd9] p-3"
              style={{ backgroundColor: INFO_BG_COLOR }}
              placeholder="계좌번호"
              value={accountNumber}
              disabled={isLocked}
              onChange={(e) => setAccountNumber(e.target.value)}
            />
          </div>
        </div>

        <div className="flex flex-col space-y-2">
          <div className="font-bold">참여자</div>
          <div className="flex flex-wrap gap-2">
            {roomMembers.map((member) => {
              const selected = targetMemberIds.includes(member.memberId);

              return (
                <label
                  key={member.memberId}
                  className="flex items-center gap-1.5 rounded-full px-3 py-1.5 font-semibold cursor-pointer badge-brand"
                >
                  <input
                    type="checkbox"
                    className="w-4 h-4 accent-[#e85a48]"
                    checked={selected}
                    disabled={isLocked}
                    onChange={() => toggleParticipant(member.memberId)}
                  />
                  <span>{member.name}</span>
                </label>
              );
            })}
          </div>
        </div>

        {isLocked && (
          <div className="explain-text">
            정산이 시작되어 결제내역을 수정하거나 삭제할 수 없어요.
          </div>
        )}

        {isLoadFailed && (
          <div className="explain-text">
            수정 정보를 불러오지 못해 저장할 수 없어요. 닫았다가 다시 열어주세요.
          </div>
        )}

        <div className="flex items-center space-x-2">
          <Button
            title={isDeleting ? "삭제 중..." : "삭제"}
            textColor="#c53829"
            bgColor="#fff"
            className="flex-1 rounded-[10px] border border-[#c53829]"
            disabled={isLocked || isDeleting || isSaving}
            onClick={() => setIsDeleteConfirmOpen(true)}
          />

          <Button
            title={isSaving ? "저장 중..." : "저장"}
            bgColor="#000"
            textColor="#fff"
            className="flex-1 rounded-[10px]"
            disabled={
              isLocked ||
              !isValid ||
              isSaving ||
              isDeleting ||
              isLoading ||
              isLoadFailed
            }
            onClick={handleSave}
          />
        </div>
      </Modal>
      {isDeleteConfirmOpen && (
        <ConfirmModal
          title="결제내역 삭제"
          message="이 결제내역을 삭제할까요? 되돌릴 수 없어요."
          confirmText="삭제"
          onConfirm={handleDelete}
          onCancel={() => setIsDeleteConfirmOpen(false)}
        />
      )}
    </>
  );
}
