import { useEffect } from "react";
import type { MouseEvent, ReactNode } from "react";
import xGrayIcon from "../assets/x-gray.svg";

interface ModalProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
}

/**
 * 오버레이 + 흰 카드 + 헤더(제목, 닫기 버튼)를 담당하는 범용 모달
 * @param title 모달 제목
 * @param onClose 닫기 이벤트 (오버레이 클릭, X 버튼 클릭)
 * @param children 모달 내용
 */
export default function Modal({ title, onClose, children }: ModalProps) {
  // 모달이 떠있는 동안 배경(뒤쪽 페이지) 스크롤 막기
  useEffect(() => {
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, []);

  /**
   * 오버레이 클릭 시 닫기 (카드 내부 클릭은 무시)
   */
  const handleOverlayClick = (e: MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex h-dvh items-center justify-center bg-black/50 p-4"
      onClick={handleOverlayClick}
    >
      <div className="w-full max-w-[500px] max-h-[85vh] rounded-2xl bg-white overflow-hidden">
        <div className="max-h-[85vh] overflow-y-auto p-6">
          <div className="flex items-center justify-between">
            <div className="text-xl font-bold">{title}</div>
            <button
              type="button"
              className="w-8 h-8 flex items-center justify-center rounded-full badge-brand cursor-pointer shrink-0"
              aria-label="닫기"
              onClick={onClose}
            >
              <img src={xGrayIcon} alt="" className="w-4 h-4" />
            </button>
          </div>

          <div className="mt-6 flex flex-col space-y-6">{children}</div>
        </div>
      </div>
    </div>
  );
}
