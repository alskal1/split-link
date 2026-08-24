interface ButtonProps {
  title: string;
  textColor?: string;
  bgColor?: string;
  className?: string;
  onClick?: () => void;
}

/**
 *
 * @param title 버튼명
 * @param textColor 버튼명 색상
 * @param bgcolor 버튼 배경 색상
 * @param className 클래스 속성
 * @param onClick 클릭 이벤트
 * @returns
 */
export default function Button({
  title,
  textColor = "#000",
  bgColor,
  className = "",
  onClick,
}: ButtonProps) {
  return (
    <button
      className={`${className} cursor-pointer font-bold px-5 py-2`}
      style={{ color: textColor, backgroundColor: bgColor }}
      onClick={onClick}
    >
      {title}
    </button>
  );
}
