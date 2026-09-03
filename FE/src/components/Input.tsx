import type { ChangeEvent, KeyboardEvent } from "react";

interface inputProps {
  className?: string;
  width?: string;
  height?: string;
  bgColor?: string;
  placeholder?: string;
  value?: string;
  onChange?: (e: ChangeEvent<HTMLInputElement>) => void;
  onKeyDown?: (e: KeyboardEvent<HTMLInputElement>) => void;
  maxLength?: number;
}

/**
 *
 * @param className 클래스 속성
 * @param width 너비
 * @param height 높이
 * @param bgColor 배경색상
 * @param placeholder 힌트 텍스트
 * @param value 입력값
 * @param onChange 입력값 변경 이벤트
 * @param onKeyDown 키 입력 이벤트
 * @param maxLength 최대 입력 길이
 * @returns
 */
export default function Input({
  className = "",
  width = "",
  height = "44px",
  bgColor = "#fff",
  placeholder = "",
  value,
  onChange,
  onKeyDown,
  maxLength,
}: inputProps) {
  return (
    <input
      className={`${className} border border-[#E6DFD9] rounded-[10px] p-2`}
      placeholder={placeholder}
      value={value}
      onChange={onChange}
      onKeyDown={onKeyDown}
      maxLength={maxLength}
      style={{ width: width, height: height, backgroundColor: bgColor }}
    />
  );
}
