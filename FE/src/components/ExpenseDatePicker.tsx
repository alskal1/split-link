import { useState } from "react";
import { toLocalIso } from "../utils/expenseForm";

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

interface ExpenseDatePickerProps {
  value: string;
  onSelect: (value: string) => void;
}

/**
 * 결제일자 선택 달력
 * @param value 선택된 날짜 (yyyy-mm-dd)
 * @param onSelect 날짜 선택 이벤트
 * @returns
 */
export default function ExpenseDatePicker({
  value,
  onSelect,
}: ExpenseDatePickerProps) {
  // 달력에 표시 중인 연월 (선택값 기준으로 시작)
  const [month, setMonth] = useState(value.slice(0, 7));
  const todayIso = toLocalIso(new Date());

  const [year, monthNum] = month.split("-").map(Number);
  const firstDow = new Date(year, monthNum - 1, 1).getDay();
  const daysInMonth = new Date(year, monthNum, 0).getDate();

  /**
   * 표시 중인 달을 n개월 이동
   * @param diff 이동할 개월 수
   */
  const shiftMonth = (diff: number) => {
    setMonth(toLocalIso(new Date(year, monthNum - 1 + diff, 1)).slice(0, 7));
  };

  return (
    <div className="mb-2.5 animate-sl-up rounded-[18px] bg-white px-3 pt-3.5 pb-2.5">
      <div className="flex items-center justify-between px-1 pb-2.5">
        <button
          type="button"
          aria-label="이전 달"
          onClick={() => shiftMonth(-1)}
          className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full active:bg-paper"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="m15 18-6-6 6-6" />
          </svg>
        </button>
        <div className="text-[15px] font-extrabold">
          {year}년 {monthNum}월
        </div>
        <button
          type="button"
          aria-label="다음 달"
          onClick={() => shiftMonth(1)}
          className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full active:bg-paper"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="m9 18 6-6-6-6" />
          </svg>
        </button>
      </div>
      <div className="grid grid-cols-7 gap-0.5 text-center">
        {WEEKDAYS.map((label, i) => (
          <div
            key={label}
            className={`pt-1 pb-1.5 text-[11px] font-bold ${
              i === 0 ? "text-danger" : "text-ink-faint"
            }`}
          >
            {label}
          </div>
        ))}
        {Array.from({ length: firstDow }, (_, i) => (
          <div key={`empty-${i}`} />
        ))}
        {Array.from({ length: daysInMonth }, (_, i) => {
          const day = i + 1;
          const iso = `${month}-${String(day).padStart(2, "0")}`;
          const isSelected = iso === value;
          const isToday = iso === todayIso;
          const isSunday = (firstDow + i) % 7 === 0;

          return (
            <button
              key={iso}
              type="button"
              onClick={() => onSelect(iso)}
              className={`mx-auto flex h-[38px] w-[38px] cursor-pointer items-center justify-center rounded-full text-sm tabular-nums transition-colors ${
                isSelected
                  ? "bg-ink font-extrabold text-white"
                  : `${isSunday ? "text-danger" : "text-ink"} ${
                      isToday
                        ? "border-[1.5px] border-brand font-extrabold"
                        : "font-semibold"
                    }`
              }`}
            >
              {day}
            </button>
          );
        })}
      </div>
    </div>
  );
}
