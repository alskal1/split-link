import { PAGE_FONT_FAMILY } from "../constants/avatar";

interface ConfirmModalProps {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  variant?: "brand" | "danger";
  disabled?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/**
 * 확인/취소 선택을 받는 화면 중앙 팝업 (아이콘 + 제목 + 안내 문구 + 취소/확인 버튼)
 * @param title 제목
 * @param message 안내 문구
 * @param confirmText 확인 버튼명 (기본: 확인)
 * @param cancelText 취소 버튼명 (기본: 취소)
 * @param variant danger: 삭제 등 위험 동작(휴지통 아이콘, 빨간 버튼), brand: 일반 동작(계산기 아이콘, 초록 버튼) (기본: brand)
 * @param disabled 확인 버튼 비활성화 여부 (요청 진행 중)
 * @param onConfirm 확인 클릭 이벤트
 * @param onCancel 취소 이벤트 (취소 버튼, 팝업 바깥 클릭)
 */
export default function ConfirmModal({
  title,
  message,
  confirmText = "확인",
  cancelText = "취소",
  variant = "brand",
  disabled = false,
  onConfirm,
  onCancel,
}: ConfirmModalProps) {
  const isDanger = variant === "danger";

  return (
    <div
      className={`fixed inset-0 z-[60] flex animate-sl-fade items-center justify-center bg-ink/45 p-6 ${PAGE_FONT_FAMILY}`}
      onClick={onCancel}
    >
      <div
        className="w-full max-w-[320px] rounded-3xl bg-white px-6 pt-7 pb-5 text-center text-ink"
        onClick={(e) => e.stopPropagation()}
      >
        <div
          className={`mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full ${
            isDanger ? "bg-[#fbeeea] text-danger" : "bg-brand-soft text-brand"
          }`}
        >
          <svg
            width="22"
            height="22"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            {isDanger ? (
              <>
                <path d="M3 6h18" />
                <path d="M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2" />
                <path d="M19 6l-1 14a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1L5 6" />
                <path d="M10 11v6" />
                <path d="M14 11v6" />
              </>
            ) : (
              <>
                <rect width="16" height="20" x="4" y="2" rx="2" />
                <path d="M8 6h8" />
                <path d="M8 11h.01M12 11h.01M16 11h.01" />
                <path d="M8 15h.01M12 15h.01M16 15h.01" />
              </>
            )}
          </svg>
        </div>
        <div className="mb-2 text-lg font-extrabold tracking-[-0.02em]">
          {title}
        </div>
        <div className="mb-6 whitespace-pre-line text-sm leading-relaxed text-ink-soft">
          {message}
        </div>
        <div className="grid grid-cols-2 gap-2">
          <button
            type="button"
            className="cursor-pointer rounded-xl bg-paper p-3.5 text-sm font-bold"
            onClick={onCancel}
          >
            {cancelText}
          </button>
          <button
            type="button"
            className={`cursor-pointer rounded-xl p-3.5 text-sm font-extrabold text-white disabled:opacity-50 ${
              isDanger ? "bg-danger" : "bg-brand"
            }`}
            disabled={disabled}
            onClick={onConfirm}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}
