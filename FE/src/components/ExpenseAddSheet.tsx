import { useState } from "react";
import { BANK_OPTIONS } from "../constants/bank";
import { BASE_CURRENCY_CARDS } from "../constants/currency";
import { AVATAR_PALETTE } from "../constants/avatar";
import { createEmptyExpenseItem, toLocalIso } from "../utils/expenseForm";
import {
  formatAmountInput,
  getCurrencyUnit,
  sanitizeAmountInput,
} from "../utils/currency";
import type {
  ExpenseFormMemberInfo,
  ExpenseGroupFormValue,
  ExpenseItemFormValue,
} from "../types/expenseType";
import BottomSheet from "./BottomSheet";
import ExpenseDatePicker from "./ExpenseDatePicker";

// 기준통화 결제(방 생성 시 선택한 통화)를 나타내는 통화 select 값
const DOMESTIC = "KR";

// 통화 select에서 "기타(직접입력)"를 나타내는 값

interface ExpenseAddSheetProps {
  group: ExpenseGroupFormValue;
  baseCurrency: string;
  members: ExpenseFormMemberInfo[];
  isValid: boolean;
  isSubmitting: boolean;
  onChange: (group: ExpenseGroupFormValue) => void;
  onSubmit: () => void;
  onClose: () => void;
}

/**
 * 지출 추가 바텀시트 (결제자 · 결제일자 · 통화 · 계좌 + 지출 항목 목록)
 * @param group 입력 중인 지출 그룹
 * @param baseCurrency 정산방 기준통화 (기본 결제 통화로 표시)
 * @param members 결제자/참여자로 선택 가능한 멤버 목록
 * @param isValid 제출 가능 여부
 * @param isSubmitting 등록 진행 여부
 * @param onChange 그룹 값 변경 이벤트
 * @param onSubmit 추가하기 버튼 클릭 이벤트
 * @param onClose 닫기 이벤트 (오버레이 클릭, X 버튼 클릭)
 * @returns
 */
export default function ExpenseAddSheet({
  group,
  baseCurrency,
  members,
  isValid,
  isSubmitting,
  onChange,
  onSubmit,
  onClose,
}: ExpenseAddSheetProps) {
  const [isCalendarOpen, setIsCalendarOpen] = useState(false);
  const [isBankOpen, setIsBankOpen] = useState(true);

  const today = new Date();
  const todayIso = toLocalIso(today);
  const yesterdayIso = toLocalIso(new Date(today.getTime() - 864e5));

  /**
   * yyyy-mm-dd를 "n월 n일"로 변환
   */
  const formatMonthDay = (value: string) => {
    const [, m, d] = value.split("-");
    return `${Number(m)}월 ${Number(d)}일`;
  };

  // 기준통화 외에 선택 가능한 통화
  const overseasCurrencies = BASE_CURRENCY_CARDS.filter(
    (opt) => opt.value !== baseCurrency,
  );
  const baseUnit = getCurrencyUnit(baseCurrency);

  const currencyValue = !group.isOverseas ? DOMESTIC : group.currency;
  const currencyLabel = !group.isOverseas
    ? `${baseUnit} 결제`
    : (overseasCurrencies.find((opt) => opt.value === group.currency)?.label ??
      "해외");
  const unit = group.isOverseas ? group.currency || "통화" : baseUnit;
  const hasBank = group.bankName.length > 0 || group.accountNumber.length > 0;
  const bankChip = hasBank
    ? `${group.bankName || "계좌"}${
        group.accountNumber
          ? ` ${group.accountNumber.replace(/-/g, "").slice(-4)}`
          : ""
      }`
    : "송금받을 계좌";

  // 항목명, 금액, 참여자가 모두 채워진 항목
  const validItems = group.items.filter(
    (item) =>
      item.name.trim().length > 0 &&
      Number(item.amount) > 0 &&
      item.participants.length > 0,
  );
  const total = validItems.reduce((sum, item) => sum + Number(item.amount), 0);
  const submitLabel = isSubmitting
    ? "추가 중..."
    : validItems.length > 1
      ? `${validItems.length}건 추가하기`
      : "추가하기";
  const canSubmit = isValid && !isSubmitting;

  /**
   * 통화 select 변경 이벤트
   * @param value DOMESTIC 또는 통화 코드
   */
  const handleCurrencyChange = (value: string) => {
    if (value === DOMESTIC) {
      onChange({ ...group, isOverseas: false });
      return;
    }

    onChange({ ...group, isOverseas: true, currency: value });
  };

  /**
   * 항목 값 변경
   */
  const handleItemChange = (
    itemId: string,
    patch: Partial<ExpenseItemFormValue>,
  ) => {
    onChange({
      ...group,
      items: group.items.map((item) =>
        item.id === itemId ? { ...item, ...patch } : item,
      ),
    });
  };

  /**
   * 항목 추가 (직전 항목의 참여자 선택을 그대로 이어받음)
   */
  const handleAddItem = () => {
    const last = group.items[group.items.length - 1];
    const next = createEmptyExpenseItem(members);

    onChange({
      ...group,
      items: [
        ...group.items,
        last ? { ...next, participants: [...last.participants] } : next,
      ],
    });
  };

  /**
   * 참여자 선택 토글
   */
  const toggleParticipant = (item: ExpenseItemFormValue, memberId: number) => {
    handleItemChange(item.id, {
      participants: item.participants.includes(memberId)
        ? item.participants.filter((id) => id !== memberId)
        : [...item.participants, memberId],
    });
  };

  const chipBase =
    "flex items-center gap-1.5 rounded-xl border-[1.5px] px-3.5 py-2.5 text-sm font-bold whitespace-nowrap cursor-pointer";

  return (
    <BottomSheet
      title="지출 추가"
      onClose={onClose}
      footer={
        <div className="flex items-center gap-3.5">
          <div className="shrink-0">
            <div className="text-[11px] font-bold text-ink-soft">합계</div>
            <div className="text-[17px] font-extrabold whitespace-nowrap tabular-nums">
              {total.toLocaleString("ko-KR")}
              {unit}
            </div>
          </div>
          <button
            type="button"
            disabled={!canSubmit}
            onClick={onSubmit}
            className={`flex-1 rounded-[14px] p-4 text-base font-extrabold text-white transition-[background-color,transform] duration-200 active:scale-[0.98] ${
              canSubmit
                ? "cursor-pointer bg-ink"
                : "cursor-not-allowed bg-[#c9c7c1]"
            }`}
          >
            {submitLabel}
          </button>
        </div>
      }
    >
      {/* 결제자 */}
      <div className="mb-2 text-xs font-bold text-ink-soft">결제자</div>
      <div className="-mx-5 mb-[18px] flex gap-1.5 overflow-x-auto px-5 pb-0.5 [scrollbar-width:none]">
        {members.map((member, i) => {
          const selected = group.payer === member.memberId;
          const [bg, color] = AVATAR_PALETTE[i % AVATAR_PALETTE.length];

          return (
            <button
              key={member.memberId}
              type="button"
              onClick={() => onChange({ ...group, payer: member.memberId })}
              className={`flex shrink-0 cursor-pointer items-center gap-[7px] rounded-full py-1.5 pr-3.5 pl-1.5 text-sm font-bold transition-colors active:scale-95 ${
                selected ? "bg-ink text-white" : "bg-white text-ink"
              }`}
            >
              <span
                className="flex h-7 w-7 items-center justify-center rounded-full text-xs font-extrabold"
                style={{
                  backgroundColor: selected ? "#fff" : bg,
                  color,
                }}
              >
                {member.name.slice(0, 1)}
              </span>
              {member.name}
            </button>
          );
        })}
      </div>

      {/* 날짜 · 통화 · 계좌 칩 */}
      <div className="mb-2.5 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => setIsCalendarOpen((prev) => !prev)}
          className={`${chipBase} ${
            isCalendarOpen
              ? "border-brand bg-brand-soft"
              : "border-line bg-white"
          }`}
        >
          <svg
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <rect width="18" height="18" x="3" y="4" rx="2" />
            <path d="M16 2v4" />
            <path d="M8 2v4" />
            <path d="M3 10h18" />
          </svg>
          {group.paidAt === todayIso
            ? "오늘"
            : group.paidAt === yesterdayIso
              ? "어제"
              : formatMonthDay(group.paidAt)}
        </button>

        <label
          className={`relative ${chipBase} ${
            group.isOverseas
              ? "border-brand bg-brand-soft"
              : "border-line bg-white"
          }`}
        >
          <svg
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <circle cx="12" cy="12" r="10" />
            <path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20" />
            <path d="M2 12h20" />
          </svg>
          {currencyLabel}
          <svg
            width="13"
            height="13"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="shrink-0"
          >
            <path d="m6 9 6 6 6-6" />
          </svg>
          <select
            value={currencyValue}
            onChange={(e) => handleCurrencyChange(e.target.value)}
            aria-label="결제 통화"
            className="absolute inset-0 h-full w-full cursor-pointer opacity-0"
          >
            <option value={DOMESTIC}>기준통화 결제 ({baseUnit})</option>
            {overseasCurrencies.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </label>

        <button
          type="button"
          onClick={() => setIsBankOpen((prev) => !prev)}
          className={`${chipBase} ${
            hasBank ? "border-brand bg-brand-soft" : "border-line bg-white"
          }`}
        >
          <svg
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M3 22h18" />
            <path d="M6 18v-7" />
            <path d="M10 18v-7" />
            <path d="M14 18v-7" />
            <path d="M18 18v-7" />
            <path d="m12 2 8 5H4z" />
          </svg>
          {bankChip}
        </button>
      </div>

      {isCalendarOpen && (
        <ExpenseDatePicker
          value={group.paidAt}
          onSelect={(paidAt) => {
            onChange({ ...group, paidAt });
            setIsCalendarOpen(false);
          }}
        />
      )}

      {group.isOverseas && (
        <div className="mx-0.5 mb-2.5 text-xs text-ink-soft">
          {formatMonthDay(group.paidAt)} 환율로 환산돼요
        </div>
      )}

      {isBankOpen && (
        <div className="mb-2.5 grid animate-sl-up grid-cols-[1fr_1.5fr] gap-2">
          <div className="relative min-w-0">
            <select
              value={group.bankName}
              onChange={(e) => onChange({ ...group, bankName: e.target.value })}
              aria-label="은행 선택"
              className="w-full appearance-none rounded-xl border-[1.5px] border-line bg-white py-3 pr-[34px] pl-3.5 text-[15px] font-semibold outline-none"
            >
              <option value="">은행 선택</option>
              {BANK_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
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
              className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2"
            >
              <path d="m6 9 6 6 6-6" />
            </svg>
          </div>
          <input
            value={group.accountNumber}
            onChange={(e) =>
              onChange({
                ...group,
                accountNumber: e.target.value.replace(/[^0-9-]/g, ""),
              })
            }
            placeholder="송금받을 계좌번호"
            inputMode="numeric"
            className="w-full rounded-xl border-[1.5px] border-line bg-white px-3.5 py-3 text-[15px] outline-none placeholder:text-[#b5b3ad] focus:border-brand"
          />
        </div>
      )}

      {/* 항목 */}
      <div className="mt-2.5 mb-2 text-xs font-bold text-ink-soft">항목</div>
      {group.items.map((item) => {
        const count = item.participants.length;
        const amount = Number(item.amount) || 0;

        return (
          <div
            key={item.id}
            className="mb-2 animate-sl-up rounded-2xl bg-white px-3.5 pt-3 pb-3.5"
          >
            <div className="mb-2.5 flex items-center gap-2 border-b border-line pb-2.5">
              <input
                value={item.name}
                onChange={(e) =>
                  handleItemChange(item.id, { name: e.target.value })
                }
                placeholder="항목명"
                maxLength={30}
                className="min-w-0 flex-1 bg-transparent py-1.5 text-base font-bold outline-none placeholder:font-normal placeholder:text-[#b5b3ad]"
              />
              <input
                value={formatAmountInput(item.amount)}
                onChange={(e) =>
                  handleItemChange(item.id, {
                    amount: sanitizeAmountInput(e.target.value),
                  })
                }
                placeholder="0"
                inputMode="decimal"
                className="w-[44%] min-w-0 bg-transparent py-1.5 text-right text-lg font-extrabold tabular-nums outline-none placeholder:font-normal placeholder:text-[#b5b3ad]"
              />
              <span className="text-[15px] font-extrabold text-ink-soft">
                {unit}
              </span>
              {group.items.length > 1 && (
                <button
                  type="button"
                  aria-label="항목 삭제"
                  onClick={() =>
                    onChange({
                      ...group,
                      items: group.items.filter((i) => i.id !== item.id),
                    })
                  }
                  className="-mr-1 flex h-[26px] w-[26px] shrink-0 cursor-pointer items-center justify-center rounded-full bg-paper text-ink-soft"
                >
                  <svg
                    width="12"
                    height="12"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                  >
                    <path d="M18 6 6 18" />
                    <path d="m6 6 12 12" />
                  </svg>
                </button>
              )}
            </div>
            <div className="flex flex-wrap items-center gap-1.5">
              {members.map((member) => {
                const selected = item.participants.includes(member.memberId);

                return (
                  <button
                    key={member.memberId}
                    type="button"
                    onClick={() => toggleParticipant(item, member.memberId)}
                    className={`flex cursor-pointer items-center gap-1 rounded-full px-[11px] py-[7px] text-[13px] font-bold transition-colors active:scale-95 ${
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
              <span className="ml-auto text-xs font-bold whitespace-nowrap text-brand tabular-nums">
                {count
                  ? `${count}명 · 1인 ${(Math.ceil((amount / count) * 100) / 100).toLocaleString("ko-KR", { maximumFractionDigits: 2 })}${unit}`
                  : "선택 안 됨"}
              </span>
            </div>
          </div>
        );
      })}
      <button
        type="button"
        onClick={handleAddItem}
        className="flex w-full cursor-pointer items-center justify-center gap-1.5 rounded-[14px] border-[1.5px] border-dashed border-[#d3d1cb] bg-transparent p-[13px] text-sm font-bold text-ink-soft"
      >
        <svg
          width="15"
          height="15"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.5"
          strokeLinecap="round"
        >
          <path d="M12 5v14" />
          <path d="M5 12h14" />
        </svg>
        항목 추가
      </button>
    </BottomSheet>
  );
}
