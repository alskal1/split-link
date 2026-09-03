interface ButtonProps {
  title: string;
  textColor?: string;
  bgColor?: string;
  className?: string;
  onClick?: () => void;
  disabled?: boolean;
}

/**
 *
 * @param title 버튼명
 * @param textColor 버튼명 색상
 * @param bgcolor 버튼 배경 색상
 * @param className 클래스 속성
 * @param onClick 클릭 이벤트
 * @param disabled 비활성화 여부
 * @returns
 */
export default function Button({
  title,
  textColor,
  bgColor,
  className = "",
  onClick,
  disabled = false,
}: ButtonProps) {
  return (
    <button
      className={`${className} inline-flex items-center justify-center font-bold px-5 py-2 ${
        disabled ? "cursor-not-allowed opacity-50" : "cursor-pointer"
      }`}
      style={{ color: textColor, backgroundColor: bgColor }}
      onClick={onClick}
      disabled={disabled}
    >
      {title}
    </button>
  );
}
