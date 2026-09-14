import type { ExpenseItemFormValue } from "../types/expenseType";
import Input from "./Input";
import xIcon from "../assets/x-gray.svg";

interface ExpenseItemFormProps {
  index: number;
  item: ExpenseItemFormValue;
  members: string[];
  onChange: (item: ExpenseItemFormValue) => void;
  onRemove?: () => void;
}

/**
 * 지출 항목(항목명 · 금액 · 참여자) 입력 폼
 * @param index 표시용 항목 번호
 * @param item 항목 값
 * @param members 참여자로 선택 가능한 멤버 목록
 * @param onChange 항목 값 변경 이벤트
 * @param onRemove 항목 삭제 이벤트 (그룹 내 항목이 1개뿐이면 전달하지 않아 삭제 버튼을 숨김)
 * @returns
 */
export default function ExpenseItemForm({
  index,
  item,
  members,
  onChange,
  onRemove,
}: ExpenseItemFormProps) {
  /**
   * 참여자 선택 토글
   * @param name 참여자 이름
   */
  const toggleParticipant = (name: string) => {
    const participants = item.participants.includes(name)
      ? item.participants.filter((p) => p !== name)
      : [...item.participants, name];

    onChange({ ...item, participants });
  };

  return (
    <div className="flex flex-col space-y-3 rounded-[10px] bg-[#fdf3eb] p-3">
      <div className="flex items-center justify-between">
        <span className="font-bold">항목 {index}</span>
        {onRemove && (
          <button
            type="button"
            className="w-5 h-5 flex items-center justify-center rounded-full bg-[#e6dfd9] cursor-pointer shrink-0"
            onClick={onRemove}
            aria-label="항목 삭제"
          >
            <img src={xIcon} alt="삭제" className="w-2.5 h-2.5" />
          </button>
        )}
      </div>

      <div className="flex items-center space-x-2">
        <Input
          className="flex-1"
          placeholder="항목명 (예: 저녁 식사)"
          value={item.name}
          onChange={(e) => onChange({ ...item, name: e.target.value })}
        />
        <Input
          className="w-28"
          placeholder="금액"
          value={item.amount}
          onChange={(e) =>
            onChange({
              ...item,
              amount: e.target.value.replace(/[^0-9]/g, ""),
            })
          }
        />
      </div>

      <div className="flex flex-wrap gap-2">
        {members.map((member) => {
          const selected = item.participants.includes(member);

          return (
            <label
              key={member}
              className="flex items-center gap-1.5 rounded-full px-3 py-1.5 font-semibold cursor-pointer badge-brand"
            >
              <input
                type="checkbox"
                className="w-4 h-4 accent-[#e85a48]"
                checked={selected}
                onChange={() => toggleParticipant(member)}
              />
              <span>{member}</span>
            </label>
          );
        })}
      </div>
    </div>
  );
}
