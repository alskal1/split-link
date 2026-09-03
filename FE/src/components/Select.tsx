interface SelectOption {
  label: string;
  value: string;
}

interface SelectProps {
  className?: string;
  width?: string;
  height?: string;
  bgColor?: string;
  options: SelectOption[];
  option?: string;
  onChange?: (value: string) => void;
}

/**
 *
 * @param className 클래스 속성
 * @param width 너비
 * @param height 높이
 * @param bgColor 배경색상
 * @param options 목록
 * @param option 선택값
 * @param onChange 값 변경 이벤트
 * @returns
 */
export default function Select({
  className = "",
  width = "",
  height = "",
  bgColor = "#fff",
  options,
  option = "",
  onChange,
}: SelectProps) {
  return (
    <select
      className={`${className} appearance-none border border-[#E6DFD9] rounded-[10px] p-2`}
      style={{ width: width, height: height, backgroundColor: bgColor }}
      defaultValue={option}
      onChange={(e) => onChange?.(e.target.value)}
    >
      {options.map((opt) => (
        <option key={opt.value} value={opt.value}>
          {opt.label}
        </option>
      ))}
    </select>
  );
}
