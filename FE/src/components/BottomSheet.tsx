import { useEffect } from "react";
import type { ReactNode } from "react";
import { PAGE_FONT_FAMILY } from "../constants/avatar";

interface BottomSheetProps {
  title?: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
}

/**
 * 하단에서 올라오는 시트 (오버레이 + 핸들 + 헤더(제목, 닫기, 제목이 있을 때만) + 스크롤 영역 + 하단 고정 영역)
 * @param title 시트 제목 (생략하면 헤더(제목, 닫기 버튼) 없이 핸들만 표시)
 * @param onClose 닫기 이벤트 (오버레이 클릭, X 버튼 클릭)
 * @param children 시트 내용 (스크롤 영역)
 * @param footer 하단 고정 영역
 */
export default function BottomSheet({
  title,
  onClose,
  children,
  footer,
}: BottomSheetProps) {
  // 시트가 떠있는 동안 배경(뒤쪽 페이지) 스크롤 막기
  useEffect(() => {
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, []);

  return (
    <div
      className={`fixed inset-0 z-50 flex animate-sl-fade items-end justify-center bg-ink/45 ${PAGE_FONT_FAMILY}`}
      onClick={onClose}
    >
      <div
        className="flex max-h-[92vh] w-full max-w-[520px] animate-sl-sheet flex-col overflow-hidden rounded-t-3xl bg-paper text-ink"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="shrink-0 px-5 pt-2.5 pb-1">
          <div className="mx-auto mb-3.5 h-1 w-10 rounded bg-[#d3d1cb]" />
          {title && (
            <div className="flex items-center justify-between gap-3">
              <div className="min-w-0 flex-1 truncate text-xl font-extrabold tracking-[-0.03em]">
                {title}
              </div>
              <button
                type="button"
                aria-label="닫기"
                onClick={onClose}
                className="flex h-[34px] w-[34px] shrink-0 cursor-pointer items-center justify-center rounded-full bg-white"
              >
                <svg
                  width="16"
                  height="16"
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
            </div>
          )}
        </div>

        <div className="flex-1 overflow-y-auto px-5 pt-4 pb-5">{children}</div>

        {footer && (
          <div className="shrink-0 border-t border-line bg-paper px-5 pt-3 pb-[calc(14px+env(safe-area-inset-bottom))]">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}
