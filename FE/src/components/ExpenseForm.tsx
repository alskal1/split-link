import { BANK_OPTIONS } from "../constants/bank";
import { CURRENCY_OPTIONS } from "../constants/currency";
import { createEmptyExpenseItem } from "../utils/expenseForm";
import type {
  ExpenseFormMemberInfo,
  ExpenseGroupFormValue,
  ExpenseItemFormValue,
} from "../types/expenseType";
import Input from "./Input";
import Select from "./Select";
import ExpenseItemForm from "./ExpenseItemForm";
import xIcon from "../assets/x-gray.svg";

interface ExpenseFormProps {
  index: number;
  group: ExpenseGroupFormValue;
  members: ExpenseFormMemberInfo[];
  onChange: (group: ExpenseGroupFormValue) => void;
  onRemove?: () => void;
}

/**
 * 지출 입력 폼 (결제자 · 결제일자 · 계좌 + 지출 항목 목록)
 * @param index 표시용 그룹 번호
 * @param group 그룹 값
 * @param members 참여자로 선택 가능한 멤버 목록
 * @param onChange 그룹 값 변경 이벤트
 * @param onRemove 그룹 삭제 이벤트 (그룹이 1개뿐이면 전달하지 않아 삭제 버튼을 숨김)
 * @returns
 */
export default function ExpenseForm({
  index,
  group,
  members,
  onChange,
  onRemove,
}: ExpenseFormProps) {
  /**
   * 항목 추가
   */
  const handleAddItem = () => {
    onChange({
      ...group,
      items: [...group.items, createEmptyExpenseItem(members)],
    });
  };

  /**
   * 항목 값 변경
   * @param itemIndex 항목 인덱스
   * @param item 변경된 항목 값
   */
  const handleItemChange = (itemIndex: number, item: ExpenseItemFormValue) => {
    const items = group.items.map((prev, i) => (i === itemIndex ? item : prev));
    onChange({ ...group, items });
  };

  /**
   * 항목 삭제
   * @param itemIndex 항목 인덱스
   */
  const handleItemRemove = (itemIndex: number) => {
    onChange({
      ...group,
      items: group.items.filter((_, i) => i !== itemIndex),
    });
  };

  return (
    <div className="flex flex-col space-y-4 rounded-[10px] border border-[#e6dfd9] bg-[#fdf3eb] p-4">
      <div className="flex items-center justify-between">
        <span className="font-bold">항목 {index}</span>
        {onRemove && (
          <button
            type="button"
            className="w-5 h-5 flex items-center justify-center rounded-full bg-[#f3ede7] cursor-pointer shrink-0"
            onClick={onRemove}
            aria-label="그룹 삭제"
          >
            <img src={xIcon} alt="삭제" className="w-2.5 h-2.5" />
          </button>
        )}
      </div>

      <div className="flex flex-col space-y-2">
        <div className="flex items-center space-x-1.5">
          <span className="w-5 h-5 flex items-center justify-center rounded-full bg-[#e85a48] text-white text-[10pt] font-bold shrink-0">
            1
          </span>
          <span className="font-bold">결제자 · 결제일자 · 화폐단위</span>
        </div>

        <div className="flex flex-col space-y-1">
          <label className="text-[10pt] text-[#281c18]">결제자</label>
          <Select
            height="44px"
            options={[
              { label: "결제자 선택", value: "" },
              ...members.map((member) => ({
                label: member.name,
                value: String(member.memberId),
              })),
            ]}
            option={group.payer !== null ? String(group.payer) : ""}
            onChange={(value) =>
              onChange({ ...group, payer: value ? Number(value) : null })
            }
          />
        </div>

        <div className="flex flex-col space-y-1">
          <label className="text-[10pt] text-[#281c18]">결제일자</label>
          <input
            type="date"
            className="border border-[#e6dfd9] rounded-[10px] p-2 h-11 bg-white"
            value={group.paidAt}
            onChange={(e) => onChange({ ...group, paidAt: e.target.value })}
          />
        </div>

        <label className="flex items-center space-x-2 cursor-pointer">
          <input
            type="checkbox"
            className="w-4 h-4 accent-[#e85a48]"
            checked={group.isOverseas}
            onChange={(e) =>
              onChange({ ...group, isOverseas: e.target.checked })
            }
          />
          <span>해외결제</span>
        </label>

        {group.isOverseas && (
          <Select
            options={CURRENCY_OPTIONS.filter((opt) => opt.value !== "ETC")}
            option={group.currency}
            onChange={(value) => onChange({ ...group, currency: value })}
          />
        )}

        <div className="flex flex-col space-y-1">
          <label className="text-[10pt] text-[#281c18]">송금받을 계좌</label>
          <div className="flex items-center space-x-2">
            <Select
              className="flex-1"
              height="44px"
              options={[
                { label: "은행 선택", value: "" },
                ...BANK_OPTIONS,
              ]}
              option={group.bankName}
              onChange={(value) => onChange({ ...group, bankName: value })}
            />
            <Input
              className="flex-1"
              placeholder="계좌번호"
              value={group.accountNumber}
              onChange={(e) =>
                onChange({ ...group, accountNumber: e.target.value })
              }
            />
          </div>
        </div>
      </div>

      <div className="flex flex-col space-y-2">
        <div className="flex items-center space-x-1.5">
          <span className="w-5 h-5 flex items-center justify-center rounded-full bg-[#e85a48] text-white text-[10pt] font-bold shrink-0">
            2
          </span>
          <span className="font-bold">항목명 · 금액 · 참여자</span>
        </div>

        <div className="flex flex-col space-y-3">
          {group.items.map((item, itemIndex) => (
            <ExpenseItemForm
              key={item.id}
              index={itemIndex + 1}
              item={item}
              members={members}
              onChange={(next) => handleItemChange(itemIndex, next)}
              onRemove={
                group.items.length > 1
                  ? () => handleItemRemove(itemIndex)
                  : undefined
              }
            />
          ))}
        </div>

        <button
          type="button"
          className="w-full rounded-[10px] border border-dashed border-[#e6dfd9] py-2.5 text-[10pt] font-semibold text-[#281c18] cursor-pointer"
          onClick={handleAddItem}
        >
          + 이 결제자 · 날짜로 항목 추가
        </button>
      </div>
    </div>
  );
}
