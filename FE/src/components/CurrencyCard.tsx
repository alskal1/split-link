interface CurrencyCardProps {
  symbol: string;
  label: string;
  selected?: boolean;
  onClick?: () => void;
}

/**
 * 기준 통화 선택 카드
 * @param symbol 통화 기호
 * @param label 통화명
 * @param selected 선택 여부
 * @param onClick 클릭 이벤트
 * @returns
 */
export default function CurrencyCard({
  symbol,
  label,
  selected = false,
  onClick,
}: CurrencyCardProps) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={onClick}
      className={`flex cursor-pointer flex-col items-start gap-0.5 rounded-xl border-[1.5px] px-3.5 py-3 text-left transition-[background-color,border-color,color] duration-200 active:scale-[0.96] ${
        selected
          ? "border-ink bg-ink text-white"
          : "border-line bg-white text-ink"
      }`}
    >
      <span className="text-[17px] font-extrabold">{symbol}</span>
      <span className="text-xs font-semibold opacity-85">{label}</span>
    </button>
  );
}
