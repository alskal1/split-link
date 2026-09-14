import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import toast from "react-hot-toast";
import { getRoomSummary } from "../api/room";
import { createEmptyExpenseGroup } from "../utils/expenseForm";
import { loadMemberAccess } from "../utils/memberAccessStorage";
import type { ExpenseGroupFormValue } from "../types/expenseType";
import type { RoomSummaryResponse } from "../types/roomType";
import Button from "../components/Button";
import ExpenseForm from "../components/ExpenseForm";
import settingsIcon from "../assets/settings.svg";

export default function SettlementRoom() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  // slug로 조회한 방 정보
  const [room, setRoom] = useState<RoomSummaryResponse | null>(null);

  useEffect(() => {
    // slug가 없을 경우 메인 페이지로 이동
    if (!slug) {
      navigate("/", { replace: true });
      return;
    }

    // 본인 멤버 선택을 거치지 않았을 경우 선택 화면으로 이동
    if (!loadMemberAccess(slug)) {
      navigate(`/rooms/${slug}`, { replace: true });
      return;
    }

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

  if (!room) {
    return null;
  }

  return <SettlementRoomContent room={room} />;
}

function SettlementRoomContent({ room }: { room: RoomSummaryResponse }) {
  const members = room.memberNames;

  // 지출 입력 폼 (결제자 · 날짜 그룹 목록)
  const [groups, setGroups] = useState<ExpenseGroupFormValue[]>([
    createEmptyExpenseGroup(members),
  ]);
  // 등록 완료된 지출 (총 지출 / 내역 집계용)
  const [expenses, setExpenses] = useState<{ amount: number }[]>([]);
  // 등록 진행 여부
  const [isSubmiting, setIsSubmiting] = useState(false);

  const totalAmount = expenses.reduce(
    (sum, expense) => sum + expense.amount,
    0,
  );

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
    setGroups((prev) => [...prev, createEmptyExpenseGroup(members)]);
  };

  /**
   * 입력 폼 초기화
   */
  const handleReset = () => {
    setGroups([createEmptyExpenseGroup(members)]);
  };

  // 유효성 체크
  const isValid = groups.every(
    (group) =>
      group.payer.trim().length > 0 &&
      group.paidAt.length > 0 &&
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
    if (!isValid || isSubmiting) {
      return;
    }

    setIsSubmiting(true);

    try {
      const newExpenses = groups.flatMap((group) =>
        group.items.map((item) => ({ amount: Number(item.amount) })),
      );

      setExpenses((prev) => [...prev, ...newExpenses]);
      handleReset();
    } finally {
      setIsSubmiting(false);
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
          // TODO: 정산방 설정 화면 연결
        >
          <img src={settingsIcon} alt="" className="w-4 h-4" />
        </button>
      </div>

      <div className="flex items-center justify-between rounded-[10px] bg-white p-4">
        <div className="flex flex-col space-y-1">
          <div className="text-[10pt] text-[#281c18]">총 지출</div>
          <div className="text-xl font-bold">
            {totalAmount.toLocaleString()}원
          </div>
        </div>
        <div className="flex flex-col space-y-1 items-end">
          <div className="text-[10pt] text-[#281c18]">내역</div>
          <div className="text-xl font-bold">{expenses.length}건</div>
        </div>
      </div>

      <div className="flex flex-col space-y-4 rounded-[10px] bg-white p-4">
        <div className="flex items-center justify-between">
          <span className="font-bold">지출 추가</span>
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
              members={members}
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
          disabled={!isValid || isSubmiting}
          onClick={handleSubmit}
        />
      </div>

      <Button
        title="정산 계산하기"
        bgColor="#000"
        textColor="#fff"
        className="w-full rounded-2xl h-12.5"
        disabled={expenses.length === 0}
        // TODO: 정산 계산 결과 화면 연결
        onClick={() => {}}
      />
    </div>
  );
}
