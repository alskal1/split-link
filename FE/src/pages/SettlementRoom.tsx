import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import toast from "react-hot-toast";
import { getRoomSummary } from "../api/room";
import {
  createExpenses,
  getExpenseDetail,
  getExpenseFormInit,
  getExpenseList,
} from "../api/expense";
import { createEmptyExpenseGroup } from "../utils/expenseForm";
import {
  clearMemberAccess,
  loadMemberAccess,
  saveMemberAccess,
} from "../utils/memberAccessStorage";
import type {
  ExpenseDetailResponse,
  ExpenseFormInitResponse,
  ExpenseGroupFormValue,
  ExpenseListResponse,
  ExpenseRecord,
} from "../types/expenseType";
import type { MemberAccessStorage, RoomSummaryResponse } from "../types/roomType";
import Button from "../components/Button";
import ExpenseForm from "../components/ExpenseForm";
import ExpenseDetail from "./ExpenseDetail";
import SettlementRoomSetting from "./SettlementRoomSetting";
import SettlementSummary from "./SettlementSummary";
import type { SettlementCredit, SettlementDebt } from "./SettlementSummary";
import settingsIcon from "../assets/settings.svg";

export default function SettlementRoom() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  // slug로 조회한 방 정보
  const [room, setRoom] = useState<RoomSummaryResponse | null>(null);
  // 본인 멤버 선택(입장) 시 저장된 로컬 접근 정보 (pin, baseCurrency 포함)
  const [memberAccess, setMemberAccess] = useState<MemberAccessStorage | null>(
    null,
  );

  useEffect(() => {
    // slug가 없을 경우 메인 페이지로 이동
    if (!slug) {
      navigate("/", { replace: true });
      return;
    }

    // 본인 멤버 선택을 거치지 않았을 경우 선택 화면으로 이동
    const access = loadMemberAccess(slug);
    if (!access) {
      navigate(`/rooms/${slug}`, { replace: true });
      return;
    }
    setMemberAccess(access);

    (async () => {
      try {
        const summary = await getRoomSummary(slug);

        // 방 정보 없을 경우 메인 페이지로 이동
        if (!summary) {
          navigate("/", { replace: true });
          return;
        }

        setRoom(summary);
      } catch (error) {
        toast.error("방 정보를 불러오지 못했어요");
        navigate("/", { replace: true });
      }
    })();
  }, [slug, navigate]);

  if (!room || !slug || !memberAccess) {
    return null;
  }

  /**
   * 정산방 설정 저장 완료 반영
   * @param updated 수정된 방 상세 정보
   * @param newPin 저장 시 사용된(변경됐다면 새) 입장코드
   */
  const handleSettingSaved = (
    updated: { title: string; baseCurrency: string; memberNames: string[] },
    newPin: string,
  ) => {
    setRoom((prev) =>
      prev
        ? { ...prev, title: updated.title, memberNames: updated.memberNames }
        : prev,
    );

    const nextMemberAccess: MemberAccessStorage = {
      ...memberAccess,
      pin: newPin,
      baseCurrency: updated.baseCurrency,
    };
    saveMemberAccess(nextMemberAccess);
    setMemberAccess(nextMemberAccess);
  };

  /**
   * 정산방 삭제 완료 반영
   */
  const handleSettingDeleted = () => {
    clearMemberAccess(slug);
    navigate("/", { replace: true });
  };

  return (
    <SettlementRoomContent
      room={room}
      slug={slug}
      pin={memberAccess.pin}
      baseCurrency={memberAccess.baseCurrency}
      memberName={memberAccess.memberName}
      onSettingSaved={handleSettingSaved}
      onSettingDeleted={handleSettingDeleted}
    />
  );
}

function SettlementRoomContent({
  room,
  slug,
  pin,
  baseCurrency,
  memberName,
  onSettingSaved,
  onSettingDeleted,
}: {
  room: RoomSummaryResponse;
  slug: string;
  pin: string;
  baseCurrency: string;
  memberName: string;
  onSettingSaved: (
    updated: { title: string; baseCurrency: string; memberNames: string[] },
    newPin: string,
  ) => void;
  onSettingDeleted: () => void;
}) {
  const members = room.memberNames;
  // 정산방 설정 모달 오픈 여부
  const [isSettingOpen, setIsSettingOpen] = useState(false);
  // 지출 추가 드롭다운 오픈 여부
  const [isAddExpenseOpen, setIsAddExpenseOpen] = useState(false);
  // 상세 모달을 띄운 결제내역 항목 (서버에서 조회한 상세 정보)
  const [selectedExpense, setSelectedExpense] =
    useState<ExpenseDetailResponse | null>(null);
  // 정산 요약(보낼 금액) 모달 오픈 여부
  const [isSummaryOpen, setIsSummaryOpen] = useState(false);

  // 지출 입력 폼 (결제자 · 날짜 그룹 목록) - formInit 로드 후 초기화
  const [groups, setGroups] = useState<ExpenseGroupFormValue[]>([]);
  // 등록 완료된 지출 (결제내역 리스트 / 정산 요약 계산용)
  const [expenses, setExpenses] = useState<ExpenseRecord[]>([]);
  // 등록 진행 여부
  const [isSubmiting, setIsSubmiting] = useState(false);
  // 서버에서 조회한 지출 목록 및 정산 요약 (총 지출 금액 표시용)
  const [expenseSummary, setExpenseSummary] =
    useState<ExpenseListResponse | null>(null);
  // 지출 입력 폼 초기 데이터 (결제자/참여자 이름 -> 멤버 PK 매핑용)
  const [formInit, setFormInit] = useState<ExpenseFormInitResponse | null>(
    null,
  );

  /**
   * 지출 목록/정산 요약 및 정산 집계용 지출 상세를 서버에서 새로 조회해 반영
   * (등록 · 수정 · 삭제 직후 항상 이 함수로 새로고침하여 로컬 상태가 서버와 어긋나지 않도록 함)
   */
  const loadExpenses = useCallback(async () => {
    const summary = await getExpenseList(slug);
    if (!summary) {
      return;
    }
    setExpenseSummary(summary);

    // 조회 도중 다른 사용자가 삭제한 항목이 있어도(reject) 나머지 항목으로 계속 진행
    const results = await Promise.allSettled(
      summary.expenses.map((item) => getExpenseDetail(slug, item.expenseId)),
    );
    const details = results
      .filter(
        (
          result,
        ): result is PromiseFulfilledResult<ExpenseDetailResponse | undefined> =>
          result.status === "fulfilled",
      )
      .map((result) => result.value);

    const records: ExpenseRecord[] = details
      .filter((detail): detail is ExpenseDetailResponse => !!detail)
      .map((detail) => ({
        id: String(detail.expenseId),
        name: detail.title,
        amount: detail.amount,
        paidAt: detail.spentAt.slice(0, 10),
        payer: detail.payerName,
        bankName: detail.bankName,
        accountNumber: detail.accountNumber,
        isMyPayment: detail.isMyPayment,
        participantShares: detail.targetMembers.map((member) => ({
          name: member.name,
          shareAmount: member.shareAmount,
          isSelf: member.isSelf,
        })),
      }));

    setExpenses(records);
  }, [slug]);

  /**
   * 지출 입력 폼 초기 데이터(결제자/참여자 선택용 멤버 목록 등) 새로 조회해 반영
   * (멤버 추가/이름 변경 등 방 설정 저장 직후에도 호출하여 formInit이 최신 멤버 목록을 갖도록 함)
   */
  const loadFormInit = useCallback(async () => {
    const init = await getExpenseFormInit(slug);
    if (init) {
      setFormInit(init);
    }
  }, [slug]);

  useEffect(() => {
    (async () => {
      try {
        await Promise.all([loadFormInit(), loadExpenses()]);
      } catch (error) {
        toast.error(
          error instanceof Error
            ? error.message
            : "지출 정보를 불러오지 못했어요",
        );
      }
    })();
  }, [slug, loadFormInit, loadExpenses]);

  // 지출 입력 폼 결제자/참여자 선택용 멤버 목록 (formInit 로드 전에는 빈 배열)
  const formMembers = formInit?.roomMembers ?? [];

  // formInit 로드 완료 시 초기 그룹 1개 생성 (이미 그룹이 있으면 건너뜀)
  useEffect(() => {
    if (!formInit) {
      return;
    }
    setGroups((prev) =>
      prev.length === 0
        ? [createEmptyExpenseGroup(formInit.roomMembers)]
        : prev,
    );
  }, [formInit]);

  const totalAmount = expenseSummary?.totalExpenseAmount ?? 0;

  // 내가 결제한 금액
  const myPaidAmount = expenses
    .filter((expense) => expense.isMyPayment)
    .reduce((sum, expense) => sum + expense.amount, 0);

  // 내가 부담해야 할 금액 (서버가 1원 오차 보정까지 계산한 정확한 부담금 합산)
  const myShareAmount = expenses.reduce((sum, expense) => {
    const myShare = expense.participantShares.find((share) => share.isSelf);
    return sum + (myShare?.shareAmount ?? 0);
  }, 0);

  // 내가 보낼 금액 (부담해야 할 금액이 결제한 금액보다 많을 때만 발생)
  const myOwedAmount = Math.max(0, Math.round(myShareAmount - myPaidAmount));

  // 결제자별로 내가 보내야 할 정산 금액 (서버가 계산한 내 부담금을 결제자별로 합산)
  const debts: SettlementDebt[] = members
    .filter((member) => member !== memberName)
    .map((payer) => {
      const payerExpenses = expenses.filter((expense) => expense.payer === payer);
      const amount = payerExpenses.reduce((sum, expense) => {
        const myShare = expense.participantShares.find((share) => share.isSelf);
        return sum + (myShare?.shareAmount ?? 0);
      }, 0);
      const accountExpense = payerExpenses.find(
        (expense) => expense.bankName && expense.accountNumber,
      );

      return {
        payer,
        amount: Math.round(amount),
        bankName: accountExpense?.bankName ?? "",
        accountNumber: accountExpense?.accountNumber ?? "",
      };
    })
    .filter((debt) => debt.amount > 0);

  // 내가 결제한 항목별로 다른 멤버가 나에게 보내야 할 정산 금액 (서버가 계산한 참여자별 부담금 사용)
  const credits: SettlementCredit[] = members
    .filter((member) => member !== memberName)
    .map((debtor) => {
      const myExpenses = expenses.filter((expense) => expense.isMyPayment);
      const amount = myExpenses.reduce((sum, expense) => {
        const share = expense.participantShares.find(
          (participant) => participant.name === debtor,
        );
        return sum + (share?.shareAmount ?? 0);
      }, 0);

      return { debtor, amount: Math.round(amount) };
    })
    .filter((credit) => credit.amount > 0);

  /**
   * 그룹 값 변경
   * @param groupIndex 그룹 인덱스
   * @param group 변경된 그룹 값
   */
  const handleGroupChange = (
    groupIndex: number,
    group: ExpenseGroupFormValue,
  ) => {
    setGroups((prev) => prev.map((g, i) => (i === groupIndex ? group : g)));
  };

  /**
   * 그룹 삭제
   * @param groupIndex 그룹 인덱스
   */
  const handleGroupRemove = (groupIndex: number) => {
    setGroups((prev) => prev.filter((_, i) => i !== groupIndex));
  };

  /**
   * 그룹 추가
   */
  const handleAddGroup = () => {
    setGroups((prev) => [...prev, createEmptyExpenseGroup(formMembers)]);
  };

  /**
   * 입력 폼 초기화
   */
  const handleReset = () => {
    setGroups([createEmptyExpenseGroup(formMembers)]);
  };

  // 유효성 체크
  const isValid = groups.every(
    (group) =>
      group.payer !== null &&
      group.paidAt.length > 0 &&
      group.bankName.length > 0 &&
      group.accountNumber.trim().length > 0 &&
      group.items.every(
        (item) =>
          item.name.trim().length > 0 &&
          Number(item.amount) > 0 &&
          item.participants.length > 0,
      ),
  );

  /**
   * 지출 추가하기 버튼 클릭 이벤트
   */
  const handleSubmit = async () => {
    if (!isValid || isSubmiting || !formInit) {
      return;
    }

    setIsSubmiting(true);

    try {
      const expenseGroups = groups.map((group) => {
        if (group.payer === null) {
          throw new Error("결제자 정보를 찾을 수 없어요");
        }

        return {
          payerId: group.payer,
          spentAt: `${group.paidAt}T00:00:00`,
          currency: group.isOverseas ? group.currency : undefined,
          bankName: group.bankName,
          accountNumber: group.accountNumber,
          items: group.items.map((item) => ({
            title: item.name,
            amount: Number(item.amount),
            targetMemberIds: item.participants,
          })),
        };
      });

      await createExpenses(slug, { expenseGroups });
      await loadExpenses();

      handleReset();
      setIsAddExpenseOpen(false);
      toast.success("지출을 등록했어요");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "지출 등록에 실패했어요",
      );
    } finally {
      setIsSubmiting(false);
    }
  };

  /**
   * 결제내역 항목 클릭 시 상세 정보 조회 후 모달 오픈
   * @param expenseId 조회할 지출 내역 PK
   */
  const handleExpenseClick = async (expenseId: number) => {
    try {
      const detail = await getExpenseDetail(slug, expenseId);
      if (detail) {
        setSelectedExpense(detail);
      }
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : "지출 상세 정보를 불러오지 못했어요",
      );
    }
  };

  /**
   * 결제내역 상세 모달에서 수정 완료 반영
   */
  const handleExpenseUpdated = async () => {
    setSelectedExpense(null);

    try {
      await loadExpenses();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "지출 목록을 불러오지 못했어요",
      );
    }
  };

  /**
   * 결제내역 상세 모달에서 삭제 완료 반영
   */
  const handleExpenseDeleted = async () => {
    setSelectedExpense(null);

    try {
      await loadExpenses();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "지출 목록을 불러오지 못했어요",
      );
    }
  };

  /**
   * 정산완료 처리 (등록된 결제내역 전체 초기화)
   */
  const handleSettlementComplete = () => {
    setExpenses([]);
    setIsSummaryOpen(false);
  };

  return (
    <div className="flex flex-col flex-1 space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex flex-col">
          <div className="text-xl font-bold">결제내역 입력</div>
          <div className="explain-text">{room.title}</div>
        </div>
        <button
          type="button"
          className="w-9 h-9 flex items-center justify-center rounded-full bg-white cursor-pointer"
          aria-label="설정"
          onClick={() => setIsSettingOpen(true)}
        >
          <img src={settingsIcon} alt="" className="w-4 h-4" />
        </button>
      </div>

      {isSettingOpen && (
        <SettlementRoomSetting
          slug={slug}
          title={room.title}
          members={members}
          pin={pin}
          baseCurrency={baseCurrency}
          onClose={() => setIsSettingOpen(false)}
          onSaved={(updated, newPin) => {
            onSettingSaved(updated, newPin);
            setIsSettingOpen(false);

            loadFormInit().catch(() => {
              toast.error("멤버 정보를 새로고침하지 못했어요");
            });
          }}
          onDeleted={onSettingDeleted}
        />
      )}

      {selectedExpense && (
        <ExpenseDetail
          slug={slug}
          expense={selectedExpense}
          onClose={() => setSelectedExpense(null)}
          onUpdated={handleExpenseUpdated}
          onDeleted={handleExpenseDeleted}
        />
      )}

      {isSummaryOpen && (
        <SettlementSummary
          debts={debts}
          credits={credits}
          onClose={() => setIsSummaryOpen(false)}
          onComplete={handleSettlementComplete}
        />
      )}

      <div className="flex items-center justify-between rounded-[10px] bg-white p-4">
        <div className="flex flex-col space-y-1">
          <div className="text-[10pt] text-[#281c18]">총 지출</div>
          <div className="text-xl font-bold">
            {totalAmount.toLocaleString()}원
          </div>
        </div>
        <div className="flex flex-col space-y-1 items-end">
          <div className="text-[10pt] text-[#281c18]">내가 보낼 금액</div>
          <div className="text-xl font-bold text-[#e85a48]">
            {myOwedAmount.toLocaleString()}원
          </div>
        </div>
      </div>

      {expenseSummary && expenseSummary.expenses.length > 0 && (
        <div className="flex flex-col space-y-3">
          <span className="font-bold">결제내역</span>
          <div className="flex flex-col space-y-3">
            {expenseSummary.expenses.map((expense) => (
              <button
                key={expense.expenseId}
                type="button"
                className="flex w-full items-center justify-between rounded-[10px] bg-white p-4 text-left cursor-pointer"
                onClick={() => handleExpenseClick(expense.expenseId)}
              >
                <div className="flex flex-col space-y-1">
                  <div className="font-bold">{expense.title}</div>
                  <div className="text-[10pt] text-[#281c18]">
                    결제자: {expense.payerName} · 참여{" "}
                    {expense.targetMemberCount}명
                  </div>
                </div>
                <div className="font-bold">
                  {expense.amount.toLocaleString()}원
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      <div className="flex flex-col space-y-4 rounded-[10px] bg-white p-4">
        <button
          type="button"
          className="flex items-center justify-between cursor-pointer"
          aria-expanded={isAddExpenseOpen}
          onClick={() => setIsAddExpenseOpen((prev) => !prev)}
        >
          <span className="font-bold">지출 추가</span>
          <svg
            className={`w-4 h-4 text-[#281c18] transition-transform ${
              isAddExpenseOpen ? "rotate-180" : ""
            }`}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </button>

        {isAddExpenseOpen && (
          <>
            <div className="flex justify-end">
              <button
                type="button"
                className="text-[10pt] text-[#281c18] cursor-pointer"
                onClick={handleReset}
              >
                초기화
              </button>
            </div>

            <div className="flex flex-col space-y-4">
              {groups.map((group, groupIndex) => (
                <ExpenseForm
                  key={group.id}
                  index={groupIndex + 1}
                  group={group}
                  members={formMembers}
                  onChange={(next) => handleGroupChange(groupIndex, next)}
                  onRemove={
                    groups.length > 1
                      ? () => handleGroupRemove(groupIndex)
                      : undefined
                  }
                />
              ))}
            </div>

            <button
              type="button"
              className="w-full rounded-[10px] border border-dashed border-[#e6dfd9] py-2.5 text-[10pt] font-semibold text-[#281c18] cursor-pointer"
              onClick={handleAddGroup}
            >
              + 새 결제자 · 날짜로 그룹 추가
            </button>

            <Button
              title={isSubmiting ? "추가 중..." : "추가하기"}
              bgColor="#000"
              textColor="#fff"
              className="w-full rounded-[10px]"
              disabled={!isValid || isSubmiting || !formInit}
              onClick={handleSubmit}
            />
          </>
        )}
      </div>

      <Button
        title="정산 계산하기"
        bgColor="#e85a48"
        textColor="#fff"
        className="w-full rounded-2xl h-12.5"
        disabled={expenses.length === 0}
        onClick={() => setIsSummaryOpen(true)}
      />
    </div>
  );
}
