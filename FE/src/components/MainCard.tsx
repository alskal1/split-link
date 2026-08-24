import arrowRightLeft from "../assets/arrow-right-left.svg";
import calculator from "../assets/calculator.svg";
import globe from "../assets/globe.svg";
import link from "../assets/link.svg";

const ICONS = {
  "arrow-right-left": arrowRightLeft,
  calculator,
  globe,
  link,
} as const;

type IconName = keyof typeof ICONS;

interface mainCardProps {
  icon: IconName;
  iconBgColor: string;
  title: string;
  contents: string;
  pageImg: string;
}

/**
 *
 * @param icon 아이콘명
 * @param iconBgColor 아이콘 뒷배경 색상
 * @param title 카드 제목
 * @param contents 카드 내용
 * @param pageImg 관련 페이지 이미지
 * @returns
 */
export default function MainCard({
  icon,
  iconBgColor,
  title,
  contents,
  pageImg = "",
}: mainCardProps) {
  return (
    <div className="flex flex-col bg-white rounded-2xl space-y-3 p-10">
      <div
        className="flex w-10 h-10 items-center justify-center rounded-[10px] p-2"
        style={{ backgroundColor: iconBgColor }}
      >
        <img src={ICONS[icon]} alt="아이콘" className="w-5 h-5" />
      </div>
      <div className="text-[1.1rem] font-bold">{title}</div>
      <div className="text-[10pt] text-[#281C18]">{contents}</div>
      <img src={`${pageImg}`} alt="이미지" />
    </div>
  );
}
