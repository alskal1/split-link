import { useCallback, useEffect, useRef, useState } from "react";
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
import { formatAmount } from "../utils/currency";
import {
  clearMemberAccess,
  loadMemberAccess,
  saveMemberAccess,
} from "../utils/memberAccessStorage";
import type {
  ExpenseDetailResponse,
  ExpenseAccountInfo,
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
import ConfirmModal from "../components/ConfirmModal";
import ExpenseAddSheet from "../components/ExpenseAddSheet";
import { PAGE_FONT_FAMILY } from "../constants/avatar";
import ExpenseDetail from "./ExpenseDetail";
import SettlementRoomSetting from "./SettlementRoomSetting";
import SettlementSummary from "./SettlementSummary";
import settingsIcon from "../assets/settings.svg";

// 지출 내역 주기적 갱신 간격 (ms)
const EXPENSE_POLLING_INTERVAL_MS = 10000;

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
    return (
      <div className="flex min-h-screen items-center justify-center bg-paper text-sm text-ink-soft">
        불러오는 중...
      </div>
    );
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
  const loadExpenses = useCallback(async (silent = false) => {
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
    // 주기적 갱신(silent) 중에는 일시적인 실패로 토스트를 반복해서 띄우지 않음
    if (hasFailure && !silent) {
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
        currency: detail.currency,
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
  }, [slug, getExpenseDetailWithRetry]);

  /**
   * 지출 입력 폼 초기 데이터(결제자/참여자 선택용 멤버 목록 등) 새로 조회해 반영
   * (멤버 추가/이름 변경 등 방 설정 저장 직후에도 호출하여 formInit이 최신 멤버 목록을 갖도록 함)
   */
  const loadFormInit = useCallback(async () => {
    const init = await getExpenseFormInit(slug);
    if (init) {
      setFormInit(init);
    }

    return init;
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

  // 다른 멤버의 변경 사항을 반영하기 위한 주기적 갱신 (폴링)
  const isRefreshingRef = useRef(false);
  const isSubmitingRef = useRef(false);
  isSubmitingRef.current = isSubmiting;

  useEffect(() => {
    const refresh = async () => {
      // 탭이 백그라운드이거나, 이전 갱신 또는 지출 등록이 진행 중이면 건너뜀
      if (
        document.hidden ||
        isRefreshingRef.current ||
        isSubmitingRef.current
      ) {
        return;
      }

      isRefreshingRef.current = true;
      try {
        await loadExpenses(true);
      } catch {
        // 일시적인 네트워크 오류는 무시하고 다음 주기에 다시 시도
      } finally {
        isRefreshingRef.current = false;
      }
    };

    const timer = setInterval(refresh, EXPENSE_POLLING_INTERVAL_MS);
    // 다른 탭/앱에 있다가 돌아오면 즉시 최신 상태로 갱신
    document.addEventListener("visibilitychange", refresh);

    return () => {
      clearInterval(timer);
      document.removeEventListener("visibilitychange", refresh);
    };
  }, [loadExpenses]);

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

  // 직전에 폼에 반영한 멤버 PK 목록 (멤버 구성 변경 시 신규 멤버 판별용)
  const syncedMemberIdsRef = useRef<number[] | null>(null);

  // formInit 로드 시 초기 그룹 1개 생성, 이미 그룹이 있으면 변경된 멤버 구성에 맞춰 동기화
  useEffect(() => {
    if (!formInit) {
      return;
    }

    const currentIds = formInit.roomMembers.map((member) => member.memberId);
    const prevIds = syncedMemberIdsRef.current;
    syncedMemberIdsRef.current = currentIds;

    setGroups((prev) => {
      if (prev.length === 0) {
        return [
          createEmptyExpenseGroup(formInit.roomMembers, formInit.defaultAccount),
        ];
      }

      // 제거된 멤버는 결제자/참여자에서 빼고, 새로 추가된 멤버는 참여자로 기본 선택
      const currentSet = new Set(currentIds);
      const prevSet = new Set(prevIds ?? []);
      const addedIds = currentIds.filter((id) => !prevSet.has(id));

      return prev.map((group) => {
        const isPayerRemoved =
          group.payer !== null && !currentSet.has(group.payer);

        return {
          ...group,
          payer: isPayerRemoved ? null : group.payer,
          bankName: isPayerRemoved ? "" : group.bankName,
          accountNumber: isPayerRemoved ? "" : group.accountNumber,
          items: group.items.map((item) => ({
            ...item,
            participants: [
              ...item.participants.filter((id) => currentSet.has(id)),
              ...addedIds,
            ],
          })),
        };
      });
    });
  }, [formInit]);

  const totalAmount = expenseSummary?.totalExpenseAmount ?? 0;

  // 목록 항목별 결제 통화 (목록 응답에는 통화가 없어 지출 상세 기반 기록에서 조회)
  const listCurrencyById = new Map(
    expenses.map((expense) => [Number(expense.id), expense.currency]),
  );

  // 정산 전에는 결제한 통화 그대로 표시 (통화가 섞이면 통화별로 합산)
  const settleKey = (name: string, currency: string) => `${name}|${currency}`;

  // 통화별 총 지출 합계
  const totalByCurrency = new Map<string, number>();
  expenses.forEach((expense) => {
    totalByCurrency.set(
      expense.currency,
      (totalByCurrency.get(expense.currency) ?? 0) + expense.amount,
    );
  });

  // 상대·통화별 정산 합계 (양수: 상대에게 보낼 금액, 음수: 상대에게서 받을 금액)
  // 서버 정산 전 예상 금액이며, 지출의 결제자-참여자 관계를 상대별로 상계해 구함
  const balanceByMember = new Map<string, number>();
  const addBalance = (name: string, currency: string, amount: number) => {
    const key = settleKey(name, currency);
    balanceByMember.set(key, (balanceByMember.get(key) ?? 0) + amount);
  };
  expenses.forEach((expense) => {
    if (expense.isMyPayment) {
      // 내가 결제한 지출: 다른 참여자의 부담금은 내가 받을 금액
      expense.participantShares
        .filter((share) => !share.isSelf)
        .forEach((share) =>
          addBalance(share.name, expense.currency, -share.shareAmount),
        );
      return;
    }

    // 다른 사람이 결제한 지출: 내 부담금은 결제자에게 보낼 금액
    const myShare = expense.participantShares.find((share) => share.isSelf);
    if (myShare) {
      addBalance(expense.payer, expense.currency, myShare.shareAmount);
    }
  });

  const estimatedSendByCurrency = new Map<string, number>();
  const estimatedReceiveByCurrency = new Map<string, number>();
  balanceByMember.forEach((balance, key) => {
    const currency = key.slice(key.lastIndexOf("|") + 1);
    const rounded = Math.round(balance);
    const target =
      rounded > 0 ? estimatedSendByCurrency : estimatedReceiveByCurrency;
    if (rounded !== 0) {
      target.set(currency, (target.get(currency) ?? 0) + Math.abs(rounded));
    }
  });

  /**
   * 통화별 금액을 "100 USD + 50,000원" 형태로 변환 (금액이 없으면 기준통화 0)
   * @param amountByCurrency 통화별 금액
   */
  const formatByCurrency = (amountByCurrency: Map<string, number>): string => {
    const parts = Array.from(amountByCurrency.entries()).map(
      ([currency, amount]) => formatAmount(amount, currency),
    );
    return parts.length > 0 ? parts.join(" + ") : formatAmount(0, baseCurrency);
  };

  // 정산 계산하기 실행 전후로 요약 카드 금액이 바뀌지 않도록 항상 결제 통화 기준 예상 금액을 사용
  const mySendAmount = Array.from(estimatedSendByCurrency.values()).reduce(
    (a, b) => a + b,
    0,
  );
  const myReceiveAmount = Array.from(
    estimatedReceiveByCurrency.values(),
  ).reduce((a, b) => a + b, 0);

  // 표시용 문자열 (결제 통화 기준)
  const totalText =
    totalByCurrency.size === 0
      ? formatAmount(totalAmount, baseCurrency)
      : formatByCurrency(totalByCurrency);
  const receiveText = formatByCurrency(estimatedReceiveByCurrency);
  const sendText = formatByCurrency(estimatedSendByCurrency);

  /**
   * 그룹 값 변경
   * @param groupIndex 그룹 인덱스
   * @param group 변경된 그룹 값
   */
  const handleGroupChange = (
    groupIndex: number,
    group: ExpenseGroupFormValue,
  ) => {
    setGroups((prev) =>
      prev.map((g, i) => {
        if (i !== groupIndex) {
          return g;
        }

        // 결제자가 바뀌면 본인일 때만 저장된 내 계좌를 채우고, 다른 멤버일 때는 비움
        if (formInit && g.payer !== group.payer) {
          const isSelfPayer = group.payer === formInit.currentMemberId;

          return {
            ...group,
            bankName: isSelfPayer
              ? (formInit.defaultAccount?.bankName ?? "")
              : "",
            accountNumber: isSelfPayer
              ? (formInit.defaultAccount?.accountNumber ?? "")
              : "",
          };
        }

        return group;
      }),
    );
  };

  /**
   * 지출 추가 시트 열기 (결제자가 비어 있으면 본인을 기본 결제자로 선택)
   */
  const handleOpenAddExpense = () => {
    if (formInit) {
      setGroups((prev) =>
        prev.map((group, i) =>
          i === 0 && group.payer === null
            ? {
                ...group,
                payer: formInit.currentMemberId,
                // 본인을 기본 결제자로 선택할 때 저장된 내 계좌를 함께 채움 (이미 입력한 값은 유지)
                bankName:
                  group.bankName || (formInit.defaultAccount?.bankName ?? ""),
                accountNumber:
                  group.accountNumber ||
                  (formInit.defaultAccount?.accountNumber ?? ""),
              }
            : group,
        ),
      );
    }
    setIsAddExpenseOpen(true);
  };

  /**
   * 입력 폼 초기화
   */
  const handleReset = (account: ExpenseAccountInfo | null = null) => {
    setGroups([createEmptyExpenseGroup(formMembers, account)]);
  };

  // 유효성 체크
  const isValid = groups.every(
    (group) =>
      group.payer !== null &&
      group.paidAt.length > 0 &&
      (!group.isOverseas || /^[A-Z]{3}$/.test(group.currency)) &&
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
          currency: group.isOverseas ? group.currency : baseCurrency,
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
      // 방금 등록한 계좌가 서버에 저장되므로 최신 기본 계좌를 다시 조회해 다음 지출 폼에 반영
      const [, latestInit] = await Promise.all([
        loadExpenses(),
        loadFormInit().catch(() => undefined),
      ]);

      handleReset((latestInit ?? formInit)?.defaultAccount ?? null);
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

      // 완료 체크 시에만 받음/보냄 안내 (체크 해제 시에는 표시하지 않음)
      if (isDone) {
        const received = mySettlement?.receiveList.find(
          (item) => item.settlementId === settlementId,
        );
        const sent = mySettlement?.sendList.find(
          (item) => item.settlementId === settlementId,
        );

        if (received) {
          toast.success(
            `${received.senderName}님에게 ${formatAmount(received.amount, baseCurrency)}을 받았어요`,
          );
        } else if (sent) {
          toast.success(
            `${sent.receiverName}님에게 ${formatAmount(sent.amount, baseCurrency)}을 보냈어요`,
          );
        }
      }
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

  const hasExpenses = (expenseSummary?.expenses.length ?? 0) > 0;
  const canSettle = hasExpenses && !isSettling && !!expenseSummary;

  return (
    <div
      className={`min-h-screen bg-paper text-ink break-keep ${PAGE_FONT_FAMILY}`}
    >
      <div className="mx-auto max-w-[520px] px-5 pb-[140px]">
        {/* 헤더 */}
        <header className="flex h-16 items-center justify-between">
          <div className="flex items-center gap-2 text-base font-extrabold tracking-[-0.04em]">
            <span className="h-2 w-2 rotate-45 rounded-[2px] bg-brand" />
            splitlink
          </div>
          <button
            type="button"
            aria-label="설정"
            className="-mr-2 flex h-10 w-10 cursor-pointer items-center justify-center rounded-xl text-ink active:bg-[#eae9e4]"
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
            <img src={settingsIcon} alt="" className="h-[21px] w-[21px]" />
          </button>
        </header>

        <div className="pt-3 pb-[22px]">
          <div className="mb-1.5 text-[13px] font-bold text-brand">
            결제내역
          </div>
          <h1 className="text-[length:clamp(26px,7vw,34px)] leading-[1.2] font-extrabold tracking-[-0.045em]">
            {room.title}
          </h1>
        </div>

        {/* 요약 카드 */}
        <div className="mb-7 rounded-[20px] bg-ink p-[22px] text-white">
          <div className="flex items-baseline justify-between gap-3 text-[13px] text-white/60">
            <div>총 지출</div>
            <div>
              <b className="text-white">{expenseSummary?.currentMemberName}</b>{" "}
              기준
            </div>
          </div>
          <div className="mt-1.5 mb-[18px] text-[length:clamp(28px,8vw,36px)] font-extrabold tracking-[-0.04em] whitespace-nowrap tabular-nums">
            {totalText}
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div className="rounded-[14px] bg-white/10 p-3.5">
              <div className="mb-1 text-xs text-white/60">받을 금액</div>
              <div
                className={`text-lg font-extrabold whitespace-nowrap tabular-nums ${
                  myReceiveAmount > 0 ? "text-[#8fd9b0]" : "text-white/45"
                }`}
              >
                {receiveText}
              </div>
            </div>
            <div className="rounded-[14px] bg-white/10 p-3.5">
              <div className="mb-1 text-xs text-white/60">보낼 금액</div>
              <div
                className={`text-lg font-extrabold whitespace-nowrap tabular-nums ${
                  mySendAmount > 0 ? "text-[#f2a99b]" : "text-white/45"
                }`}
              >
                {sendText}
              </div>
            </div>
          </div>
        </div>

        {/* 결제내역 목록 */}
        {!hasExpenses ? (
          <div className="rounded-[20px] border-[1.5px] border-dashed border-[#d3d1cb] px-5 py-12 text-center">
            <div className="mb-1.5 text-base font-extrabold">
              아직 지출이 없어요
            </div>
            <div className="mb-5 text-sm text-ink-soft">
              첫 결제부터 기록해보세요.
            </div>
            {!isLocked && (
              <button
                type="button"
                onClick={handleOpenAddExpense}
                className="cursor-pointer rounded-xl bg-ink px-[22px] py-[13px] text-sm font-extrabold text-white active:scale-[0.96]"
              >
                + 지출 추가
              </button>
            )}
          </div>
        ) : (
          <>
            <div className="mx-1 mb-2.5 flex items-baseline justify-between">
              <div className="text-[15px] font-extrabold">결제 내역</div>
              <div className="text-[13px] text-ink-soft">
                {expenseSummary?.expenses.length}건
              </div>
            </div>
            <div className="mb-8 rounded-[20px] bg-white px-[18px] py-1">
              {expenseSummary?.expenses.map((expense) => (
                <button
                  key={expense.expenseId}
                  type="button"
                  className="flex w-full animate-sl-up cursor-pointer items-center gap-3 border-b border-line py-[15px] text-left last:border-b-0 active:opacity-60"
                  onClick={() => handleExpenseClick(expense.expenseId)}
                >
                  <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-soft text-sm font-extrabold text-brand-dark">
                    {expense.payerName.slice(0, 1)}
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-[15px] font-bold">
                      {expense.title}
                    </div>
                    <div className="mt-0.5 truncate text-xs text-ink-soft">
                      {expense.payerName} 결제 · 참여{" "}
                      {expense.targetMemberCount}명
                    </div>
                  </div>
                  <div className="text-[15px] font-extrabold whitespace-nowrap tabular-nums">
                    {formatAmount(
                      expense.amount,
                      listCurrencyById.get(expense.expenseId) ?? baseCurrency,
                    )}
                  </div>
                </button>
              ))}
            </div>
          </>
        )}

        {isLocked && (
          <p className="text-[13px] leading-[1.6] text-ink-soft">
            정산이 시작되어 결제내역을 추가할 수 없어요.
          </p>
        )}
      </div>

      {!isLocked && isAddExpenseOpen && groups[0] && (
        <ExpenseAddSheet
          group={groups[0]}
          members={formMembers}
          baseCurrency={baseCurrency}
          isValid={isValid && !!formInit}
          isSubmitting={isSubmiting}
          onChange={(next) => handleGroupChange(0, next)}
          onSubmit={handleSubmit}
          onClose={() => setIsAddExpenseOpen(false)}
        />
      )}

      {/* 하단 고정 버튼 */}
      <div className="fixed right-0 bottom-0 left-0 z-20 bg-gradient-to-t from-paper from-70% to-paper/0 px-5 pt-5 pb-[calc(16px+env(safe-area-inset-bottom))]">
        <div className="mx-auto flex max-w-[480px] gap-2">
          {!isLocked && (
            <button
              type="button"
              aria-label="지출 추가"
              onClick={handleOpenAddExpense}
              className="flex shrink-0 cursor-pointer items-center gap-1.5 rounded-[14px] border-[1.5px] border-line bg-white px-5 text-[15px] font-extrabold active:scale-[0.96]"
            >
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
              >
                <path d="M12 5v14" />
                <path d="M5 12h14" />
              </svg>
              지출
            </button>
          )}
          <button
            type="button"
            disabled={!canSettle}
            onClick={handleOpenSummary}
            className={`flex flex-1 items-center justify-center rounded-[14px] p-[18px] text-base font-extrabold text-white transition-[background-color,transform] duration-200 active:scale-[0.98] ${
              canSettle
                ? "cursor-pointer bg-ink"
                : "cursor-not-allowed bg-[#c9c7c1]"
            }`}
          >
            {isSettling
              ? "계산 중..."
              : isLocked
                ? "정산 내역 보기"
                : "정산 계산하기"}
          </button>
        </div>
      </div>

      {isSettingOpen && (
        <SettlementRoomSetting
          slug={slug}
          title={room.title}
          members={formMembers}
          pin={pin}
          baseCurrency={baseCurrency}
          isLocked={isLocked}
          expenseCount={expenseSummary?.expenses.length ?? 0}
          expenseMemberNames={[
            ...new Set(
              expenses.flatMap((expense) => [
                expense.payer,
                ...expense.participantShares.map((share) => share.name),
              ]),
            ),
          ]}
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
          baseCurrency={baseCurrency}
          isLocked={isLocked}
          onClose={() => setSelectedExpense(null)}
          onUpdated={handleExpenseUpdated}
          onDeleted={handleExpenseDeleted}
        />
      )}

      {isSettleConfirmOpen && (
        <ConfirmModal
          title="정산을 계산할까요?"
          message="계산하면 결제내역을 추가·수정·삭제할 수 없어요."
          confirmText="계산하기"
          onConfirm={runSettlement}
          onCancel={() => setIsSettleConfirmOpen(false)}
        />
      )}

      {isSummaryOpen && mySettlement && (
        <SettlementSummary
          roomTitle={room.title}
          memberCount={formMembers.length}
          totalAmount={totalAmount}
          baseCurrency={baseCurrency}
          myName={expenseSummary?.currentMemberName ?? ""}
          sendList={mySettlement.sendList}
          receiveList={mySettlement.receiveList}
          pendingSettlementIds={pendingSettlementIds}
          onClose={() => setIsSummaryOpen(false)}
          onComplete={handleSettlementComplete}
          onToggleDone={handleToggleSettlementDone}
        />
      )}
    </div>
  );
}
