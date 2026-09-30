import Modal from "./Modal";

interface ConfirmModalProps {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm: () => void;
  onCancel: () => void;
}

/**
 * 확인/취소 선택을 받는 confirm 메시지박스
 * @param title 제목
 * @param message 안내 문구
 * @param confirmText 확인 버튼명 (기본: 확인)
 * @param cancelText 취소 버튼명 (기본: 취소)
 * @param onConfirm 확인 클릭 이벤트
 * @param onCancel 취소 이벤트 (취소 버튼, X 버튼, 오버레이 클릭)
 */
export default function ConfirmModal({
  title,
  message,
  confirmText = "확인",
  cancelText = "취소",
  onConfirm,
  onCancel,
}: ConfirmModalProps) {
  return (
    <Modal title={title} onClose={onCancel}>
      <div className="explain-text whitespace-pre-line">{message}</div>
      <div className="flex gap-2">
        <button
          type="button"
          className="flex-1 h-11 rounded-[10px] font-bold bg-gray-100 cursor-pointer"
          onClick={onCancel}
        >
          {cancelText}
        </button>
        <button
          type="button"
          className="btn-brand flex-1 h-11 rounded-[10px] font-bold"
          onClick={onConfirm}
        >
          {confirmText}
        </button>
      </div>
    </Modal>
  );
}
