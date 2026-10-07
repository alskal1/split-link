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
import {
  executeSettlement,
  getMySettlement,
  updateRemittanceStatus,
} from "../api/settlement";
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
import type { RoomMySettlementResponse } from "../types/settlementType";
import type {
  MemberAccessStorage,
  RoomMemberResponse,
  RoomSummaryResponse,
} from "../types/roomType";
import Button from "../components/Button";
import ConfirmModal from "../components/ConfirmModal";
import ExpenseForm from "../components/ExpenseForm";
import ExpenseDetail from "./ExpenseDetail";
import SettlementRoomSetting from "./SettlementRoomSetting";
import SettlementSummary from "./SettlementSummary";
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

    // slug 변경/언마운트 이후 도착한 응답이 상태·이동에 반영되지 않도록 취소 플래그 사용
    let cancelled = false;

    (async () => {
      try {
        const summary = await getRoomSummary(slug);
        if (cancelled) {
          return;
        }

        // 방 정보 없을 경우 메인 페이지로 이동
        if (!summary) {
          navigate("/", { replace: true });
          return;
        }

        setRoom(summary);
      } catch (error) {
        if (cancelled) {
          return;
        }

        toast.error("방 정보를 불러오지 못했어요");
        navigate("/", { replace: true });
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [slug, navigate]);

  if (!room || !slug || !memberAccess) {
    return <div className="explain-text">불러오는 중...</div>;
  }

  /**
   * 정산방 설정 저장 완료 반영
   * @param updated 수정된 방 상세 정보
   * @param newPin 저장 시 사용된(변경됐다면 새) 입장코드
   * @param savedMembers 저장 후 새로 등록된 멤버 목록
   * @returns 접근 정보 갱신 성공 여부
   */
  const handleSettingSaved = async (
    updated: { title: string; baseCurrency: string; memberNames: string[] },
    newPin: string,
    savedMembers: RoomMemberResponse[],
  ): Promise<boolean> => {
    setRoom((prev) =>
      prev
        ? { ...prev, title: updated.title, memberNames: updated.memberNames }
        : prev,
    );

    // 토큰에는 roomId/memberId만 담기므로 설정 저장 후에도 기존 토큰을 그대로 사용
    // (이름이 바뀌었을 수 있으므로 memberId로 우선 찾음)
    const me =
      savedMembers.find(
        (member) => member.memberId === memberAccess.memberId,
      ) ??
      savedMembers.find((member) => member.name === memberAccess.memberName);

    if (!me) {
      clearMemberAccess(slug);
      toast.error("내 멤버가 목록에서 제외되어 다시 선택해야 해요");
      navigate(`/rooms/${slug}`, { replace: true });
      return false;
    }

    const nextMemberAccess: MemberAccessStorage = {
      ...memberAccess,
      title: updated.title,
      memberId: me.memberId,
      memberName: me.name,
      pin: newPin,
      baseCurrency: updated.baseCurrency,
    };
    saveMemberAccess(nextMemberAccess);
    setMemberAccess(nextMemberAccess);
    return true;
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
  onSettingSaved,
  onSettingDeleted,
}: {
  room: RoomSummaryResponse;
  slug: string;
  pin: string;
  baseCurrency: string;
  onSettingSaved: (
    updated: { title: string; baseCurrency: string; memberNames: string[] },
    newPin: string,
    savedMembers: RoomMemberResponse[],
  ) => Promise<boolean>;
  onSettingDeleted: () => void;
}) {
  // 정산방 설정 모달 오픈 여부
  const [isSettingOpen, setIsSettingOpen] = useState(false);
  // 지출 추가 드롭다운 오픈 여부
  const [isAddExpenseOpen, setIsAddExpenseOpen] = useState(false);
  // 상세 모달을 띄운 결제내역 항목 (서버에서 조회한 상세 정보)
  const [selectedExpense, setSelectedExpense] =
    useState<ExpenseDetailResponse | null>(null);
  // 정산 요약(보낼 금액) 모달 오픈 여부
  const [isSummaryOpen, setIsSummaryOpen] = useState(false);
  // 정산 계산하기(서버 API 호출)로 조회한 내 정산 내역
  const [mySettlement, setMySettlement] =
    useState<RoomMySettlementResponse | null>(null);
  // 정산 실행 진행 여부
  const [isSettling, setIsSettling] = useState(false);
  // 송금 완료 상태 변경 요청이 진행 중인 정산 내역 PK 목록
  const [pendingSettlementIds, setPendingSettlementIds] = useState<number[]>(
    [],
  );

  const [isSettleConfirmOpen, setIsSettleConfirmOpen] = useState(false);

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
   * 지출 상세 조회 (일시적인 실패에 대비해 1회 자동 재시도)
   * @param expenseId 조회할 지출 내역 PK
   */
  const getExpenseDetailWithRetry = useCallback(
    async (expenseId: number): Promise<ExpenseDetailResponse | undefined> => {
      try {
        return await getExpenseDetail(slug, expenseId);
      } catch {
        return await getExpenseDetail(slug, expenseId);
      }
    },
    [slug],
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

    // 다른 사용자가 삭제한 항목 등 재시도 후에도 계속 실패하는 항목이 있으면(reject)
    // 나머지 항목으로는 계속 진행하되, 정산 금액이 실제와 다를 수 있음을 사용자에게 알림
    const results = await Promise.allSettled(
      summary.expenses.map((item) => getExpenseDetailWithRetry(item.expenseId)),
    );

    const hasFailure = results.some((result) => result.status === "rejected");
    if (hasFailure) {
      toast.error(
        "일부 지출 내역을 불러오지 못했어요. 화면을 새로고침해 다시 확인해주세요",
      );
    }

    const details = results
      .filter(
        (
          result,
        ): result is PromiseFulfilledResult<
          ExpenseDetailResponse | undefined
        > => result.status === "fulfilled",
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

  // 정산 실행으로 방이 잠긴 여부 (잠기면 결제내역 추가·수정·삭제, 멤버 변경 불가)
  const isLocked = expenseSummary?.isLocked ?? false;

  useEffect(() => {
    if (!isLocked) {
      return;
    }

    let cancelled = false;
    (async () => {
      try {
        const settlement = await getMySettlement(slug);
        if (!cancelled && settlement) {
          // 이미 값이 있으면 덮어쓰지 않음
          setMySettlement((prev) => prev ?? settlement);
        }
      } catch {
        // 조회 실패 시 지출 기반 예상 금액을 계속 표시
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [slug, isLocked]);

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

  // 상대별 정산 합계 (양수: 상대에게 보낼 금액, 음수: 상대에게서 받을 금액)
  // 서버 정산 전 예상 금액이며, 지출의 결제자-참여자 관계를 상대별로 상계해 구함
  const balanceByMember = new Map<string, number>();
  const addBalance = (name: string, amount: number) => {
    balanceByMember.set(name, (balanceByMember.get(name) ?? 0) + amount);
  };
  expenses.forEach((expense) => {
    if (expense.isMyPayment) {
      // 내가 결제한 지출: 다른 참여자의 부담금은 내가 받을 금액
      expense.participantShares
        .filter((share) => !share.isSelf)
        .forEach((share) => addBalance(share.name, -share.shareAmount));
      return;
    }

    // 다른 사람이 결제한 지출: 내 부담금은 결제자에게 보낼 금액
    const myShare = expense.participantShares.find((share) => share.isSelf);
    if (myShare) {
      addBalance(expense.payer, myShare.shareAmount);
    }
  });

  const balances = Array.from(balanceByMember.values()).map(Math.round);
  const estimatedSendAmount = balances
    .filter((balance) => balance > 0)
    .reduce((sum, balance) => sum + balance, 0);
  const estimatedReceiveAmount = balances
    .filter((balance) => balance < 0)
    .reduce((sum, balance) => sum - balance, 0);

  // 정산 실행 후에는 실제 송금 대상 기준인 서버 정산 값을 사용
  const mySendAmount = mySettlement
    ? Math.round(mySettlement.totalSendAmount)
    : estimatedSendAmount;
  const myReceiveAmount = mySettlement
    ? Math.round(mySettlement.totalReceiveAmount)
    : estimatedReceiveAmount;

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
    if (!isValid || isSubmiting || !formInit || isLocked) {
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
        error instanceof Error
          ? error.message
          : "지출 목록을 불러오지 못했어요",
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
        error instanceof Error
          ? error.message
          : "지출 목록을 불러오지 못했어요",
      );
    }
  };

  /**
   * 정산 계산하기 버튼 클릭 이벤트
   * (아직 정산 실행 전이면 수정 불가 안내 확인 후 서버에 정산 실행을 요청해 방을 잠그고 최소 송금 내역을 계산·저장한 뒤,
   * 이미 실행된 방이면 저장된 정산 내역을 그대로 조회해 요약 모달에 표시)
   */
  const handleOpenSummary = async () => {
    if (isSettling) {
      return;
    }

    // 정산 실행 시 방이 잠겨 지출을 수정할 수 없으므로, 아직 실행 전인 방은 사용자에게 먼저 확인
    if (!expenseSummary?.isLocked) {
      setIsSettleConfirmOpen(true);
      return;
    }

    await runSettlement();
  };

  /**
   * 정산 실행(필요 시) 후 내 정산 내역 조회
   */
  const runSettlement = async () => {
    setIsSettleConfirmOpen(false);
    setIsSettling(true);

    try {
      if (!expenseSummary?.isLocked) {
        await executeSettlement(slug);

        // 서버에서 방이 이미 잠겼으므로, 이후 단계가 실패해도 재실행되지 않도록 로컬 상태를 먼저 반영
        setExpenseSummary((prev) =>
          prev ? { ...prev, isLocked: true } : prev,
        );

        // 목록 새로고침 실패가 정산 내역 조회를 막지 않도록 분리
        try {
          await loadExpenses();
        } catch {
          toast.error("지출 목록을 새로고침하지 못했어요");
        }
      }

      const settlement = await getMySettlement(slug);
      if (!settlement) {
        toast.error("정산 내역을 불러오지 못했어요");
        return;
      }

      setMySettlement(settlement);
      setIsSummaryOpen(true);
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "정산 계산에 실패했어요",
      );
    } finally {
      setIsSettling(false);
    }
  };

  /**
   * 정산완료 처리 (요약 모달만 닫고, 정산 내역은 이후에도 다시 조회 가능)
   */
  const handleSettlementComplete = () => {
    setIsSummaryOpen(false);
    toast.success("정산을 완료했어요");
  };

  /**
   * 개별 송금 완료 상태 토글 (낙관적 업데이트 후 실패 시 롤백)
   * @param settlementId 정산 내역 PK
   * @param isDone 변경할 완료 여부
   */
  const handleToggleSettlementDone = async (
    settlementId: number,
    isDone: boolean,
  ) => {
    // 같은 항목의 요청이 진행 중이면 중복 요청 방지
    if (pendingSettlementIds.includes(settlementId)) {
      return;
    }

    /**
     * 해당 정산 내역 항목의 완료 여부만 변경 (다른 항목 상태는 유지)
     */
    const applyDone = (value: boolean) => {
      setMySettlement((prev) =>
        prev
          ? {
              ...prev,
              sendList: prev.sendList.map((item) =>
                item.settlementId === settlementId
                  ? { ...item, isDone: value }
                  : item,
              ),
              receiveList: prev.receiveList.map((item) =>
                item.settlementId === settlementId
                  ? { ...item, isDone: value }
                  : item,
              ),
            }
          : prev,
      );
    };

    setPendingSettlementIds((prev) => [...prev, settlementId]);
    applyDone(isDone);

    try {
      await updateRemittanceStatus(slug, settlementId, isDone);
    } catch (error) {
      applyDone(!isDone);
      toast.error(
        error instanceof Error ? error.message : "상태 변경에 실패했어요",
      );
    } finally {
      setPendingSettlementIds((prev) =>
        prev.filter((id) => id !== settlementId),
      );
    }
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
          onClick={() => {
            if (!formInit) {
              toast.error(
                "멤버 정보를 불러오는 중이에요. 잠시 후 다시 시도해주세요",
              );
              return;
            }
            setIsSettingOpen(true);
          }}
        >
          <img src={settingsIcon} alt="" className="w-4 h-4" />
        </button>
      </div>

      {isSettingOpen && (
        <SettlementRoomSetting
          slug={slug}
          title={room.title}
          members={formMembers}
          pin={pin}
          baseCurrency={baseCurrency}
          isLocked={isLocked}
          onClose={() => setIsSettingOpen(false)}
          onSaved={async (updated, newPin, savedMembers) => {
            const isRefreshed = await onSettingSaved(
              updated,
              newPin,
              savedMembers,
            );
            if (!isRefreshed) {
              return;
            }

            setIsSettingOpen(false);

            loadFormInit().catch((error) => {
              console.error("멤버 정보 새로고침 실패", error);
              toast.error(
                error instanceof Error
                  ? error.message
                  : "멤버 정보를 새로고침하지 못했어요",
              );
            });
          }}
          onDeleted={onSettingDeleted}
        />
      )}

      {selectedExpense && (
        <ExpenseDetail
          slug={slug}
          expense={selectedExpense}
          isLocked={isLocked}
          onClose={() => setSelectedExpense(null)}
          onUpdated={handleExpenseUpdated}
          onDeleted={handleExpenseDeleted}
        />
      )}

      {isSettleConfirmOpen && (
        <ConfirmModal
          title="정산 계산"
          message="정산을 계산하면 이후에는 결제내역을 추가·수정·삭제할 수 없어요. 계속할까요?"
          confirmText="계산하기"
          onConfirm={runSettlement}
          onCancel={() => setIsSettleConfirmOpen(false)}
        />
      )}

      {isSummaryOpen && mySettlement && (
        <SettlementSummary
          sendList={mySettlement.sendList}
          receiveList={mySettlement.receiveList}
          pendingSettlementIds={pendingSettlementIds}
          onClose={() => setIsSummaryOpen(false)}
          onComplete={handleSettlementComplete}
          onToggleDone={handleToggleSettlementDone}
        />
      )}

      <div className="flex items-center justify-between rounded-[10px] bg-white p-4">
        <div className="flex flex-col space-y-1">
          <div className="text-[10pt] text-[#281c18]">총 지출</div>
          <div className="text-xl font-bold">
            {totalAmount.toLocaleString()}원
          </div>
        </div>
        <div className="flex items-start space-x-6">
          <div className="flex flex-col space-y-1 items-end">
            <div className="text-[10pt] text-[#281c18]">내가 보낼 금액</div>
            <div className="text-xl font-bold text-[#e85a48]">
              {mySendAmount.toLocaleString()}원
            </div>
          </div>
          <div className="flex flex-col space-y-1 items-end">
            <div className="text-[10pt] text-[#281c18]">내가 받을 금액</div>
            <div className="text-xl font-bold text-[#2f9e6e]">
              {myReceiveAmount.toLocaleString()}원
            </div>
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

      {isLocked ? (
        <div className="explain-text">
          정산이 시작되어 결제내역을 추가할 수 없어요.
        </div>
      ) : (
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
      )}

      <Button
        title={
          isSettling
            ? "계산 중..."
            : isLocked
              ? "정산 내역 보기"
              : "정산 계산하기"
        }
        bgColor="#e85a48"
        textColor="#fff"
        className="w-full rounded-2xl h-12.5"
        disabled={
          !expenseSummary || expenseSummary.expenses.length === 0 || isSettling
        }
        onClick={handleOpenSummary}
      />
    </div>
  );
}
