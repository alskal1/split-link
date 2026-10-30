import { useEffect, useRef, useState } from "react";
import type { ChangeEvent, KeyboardEvent } from "react";
import toast from "react-hot-toast";
import { deleteRoom, updateRoom } from "../api/room";
import BottomSheet from "../components/BottomSheet";
import ConfirmModal from "../components/ConfirmModal";
import type { RoomMemberResponse } from "../types/roomType";
import type { ExpenseFormMemberInfo } from "../types/expenseType";

/**
 * 설정 모달에서 편집 중인 멤버
 * @param key 목록 렌더링용 로컬 키
 * @param memberId 기존 멤버 PK (신규 추가 멤버는 undefined)
 * @param name 멤버 이름
 */
interface EditableMember {
  key: string;
  memberId?: number;
  name: string;
  hasExpenses?: boolean;
}

interface SettlementRoomSettingProps {
  slug: string;
  title: string;
  members: ExpenseFormMemberInfo[];
  pin: string;
  baseCurrency: string;
  isLocked: boolean;
  expenseCount: number;
  expenseMemberNames: string[];
  onClose: () => void;
  onSaved: (
    updated: { title: string; baseCurrency: string; memberNames: string[] },
    newPin: string,
    savedMembers: RoomMemberResponse[],
  ) => void | Promise<void>;
  onDeleted: () => void;
}

/**
 * 결제내역 입력화면 설정 아이콘 클릭 시 뜨는 정산방 설정 모달
 * @param slug 방 슬러그
 * @param title 방 제목
 * @param members 참여 멤버 목록 (memberId 포함)
 * @param pin 기존 입장코드 (수정/삭제 권한 확인용)
 * @param baseCurrency 기준통화
 * @param isLocked 방 잠금 여부 (잠기면 참여 멤버 추가·삭제 불가)
 * @param expenseCount 결제내역 건수 (삭제 확인 문구용)
 * @param expenseMemberNames 결제자 또는 부담자로 지출 내역에 엮인 멤버 이름 목록 (멤버 삭제 제한용)
 * @param onClose 닫기 이벤트
 * @param onSaved 저장 완료 이벤트
 * @param onDeleted 삭제 완료 이벤트
 */
export default function SettlementRoomSetting({
  slug,
  title,
  members: initialMembers,
  pin: initialPin,
  baseCurrency,
  isLocked,
  expenseCount,
  expenseMemberNames,
  onClose,
  onSaved,
  onDeleted,
}: SettlementRoomSettingProps) {
  // 모임 이름
  const [roomName, setRoomName] = useState(title);
  // 입장코드
  const [pin, setPin] = useState(initialPin);
  // 참여 멤버
  const [members, setMembers] = useState<EditableMember[]>(() =>
    initialMembers.map((member) => ({
      key: `m-${member.memberId}`,
      memberId: member.memberId,
      name: member.name,
      hasExpenses: expenseMemberNames.includes(member.name),
    })),
  );
  // 신규 멤버 로컬 키 발급용
  const newKeySeq = useRef(0);
  // 참여 멤버 입력란
  const [memberInput, setMemberInput] = useState("");
  // 저장/삭제 진행 여부
  const [isSubmiting, setIsSubmiting] = useState(false);

  const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false);
  // 링크 복사 완료 표시 여부
  const [isLinkCopied, setIsLinkCopied] = useState(false);

  // 복사 완료 표시를 2초 뒤 원복
  useEffect(() => {
    if (!isLinkCopied) {
      return;
    }

    const timer = setTimeout(() => setIsLinkCopied(false), 2000);

    return () => clearTimeout(timer);
  }, [isLinkCopied]);

  // 방 링크
  const roomLink = `${window.location.origin}/rooms/${slug}`;

  /**
   * 입장코드 입력 변경 이벤트
   * @param e
   */
  const handlePinChange = (e: ChangeEvent<HTMLInputElement>) => {
    // 영대소문자 및 숫자만 입력 가능, 최대 10자 글자수 제한
    setPin(e.target.value.replace(/[^a-zA-Z0-9]/g, "").slice(0, 10));
  };

  /**
   * 참여멤버 추가
   */
  const handleAddMember = () => {
    if (isLocked) {
      return;
    }

    const name = memberInput.trim();

    if (!name) {
      toast.error("이름을 입력해주세요.");
      return;
    }

    // 동명이인 확인
    if (members.some((member) => member.name === name)) {
      setMemberInput("");
      toast.error("이미 등록된 이름이에요");
      return;
    }

    newKeySeq.current += 1;
    setMembers([...members, { key: `n-${newKeySeq.current}`, name }]);
    setMemberInput("");
  };

  /**
   * 참여멤버 입력란 keyDown 이벤트
   * @param e
   */
  const handleMemberInputKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" && !e.nativeEvent.isComposing) {
      e.preventDefault();
      handleAddMember();
    }
  };

  /**
   * 참여 멤버 삭제
   * @param key 참여 멤버 로컬 키
   */
  const handleRemoveMember = (key: string) => {
    if (isLocked) {
      return;
    }

    const target = members.find((member) => member.key === key);

    // 결제자 또는 부담자로 지출 내역에 엮인 기존 멤버는 삭제 불가 (저장 시 서버 검증과 동일 기준)
    if (target?.hasExpenses) {
      toast.error(`'${target.name}'님은 지출 내역이 존재하여 삭제할 수 없어요`);
      return;
    }

    setMembers(members.filter((member) => member.key !== key));
  };

  /**
   * 방 링크 복사
   */
  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(roomLink);
      setIsLinkCopied(true);
    } catch (error) {
      toast.error("링크 복사에 실패했어요");
    }
  };

  /**
   * 정산방 삭제
   */
  const handleDeleteRoom = async () => {
    if (isSubmiting) {
      return;
    }

    setIsDeleteConfirmOpen(false);

    setIsSubmiting(true);

    try {
      await deleteRoom(slug, { pin: initialPin });
      toast.success("정산방을 삭제했어요");
      onDeleted();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "정산방 삭제에 실패했어요",
      );
    } finally {
      setIsSubmiting(false);
    }
  };

  /**
   * 정산방 설정 저장
   */
  const handleSave = async () => {
    if (isSubmiting) {
      return;
    }

    const trimmedName = roomName.trim();

    if (!trimmedName) {
      toast.error("모임 이름을 입력해주세요");
      return;
    }

    if (members.length === 0) {
      toast.error("참여 멤버를 한 명 이상 등록해주세요");
      return;
    }

    setIsSubmiting(true);

    try {
      const changedPin = pin !== initialPin;
      const data = await updateRoom(slug, {
        title: trimmedName,
        baseCurrency,
        pin: initialPin,
        newPin: changedPin ? pin : undefined,
        members: members.map(({ memberId, name }) => ({ memberId, name })),
      });

      if (!data) {
        toast.error("정산방 설정 저장에 실패했어요");
        return;
      }

      toast.success("정산방 설정을 저장했어요");
      // 부모의 토큰 재발급이 끝날 때까지 isSubmiting을 유지해 중복 저장 방지
      await onSaved(
        {
          title: data.title,
          baseCurrency: data.baseCurrency,
          memberNames: data.members.map((member) => member.name),
        },
        changedPin ? pin : initialPin,
        data.members,
      );
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : "정산방 설정 저장에 실패했어요",
      );
    } finally {
      setIsSubmiting(false);
    }
  };

  const inputClass =
    "w-full rounded-xl border-[1.5px] border-line bg-white px-3.5 py-3 text-[15px] outline-none placeholder:text-[#b5b3ad] focus:border-brand disabled:opacity-50";
  const labelClass = "mb-2 text-xs font-bold text-ink-soft";

  return (
    <>
      <BottomSheet
        title="정산방 설정"
        onClose={onClose}
        footer={
          <div className="grid grid-cols-[1fr_1.4fr] gap-2">
            <button
              type="button"
              className="cursor-pointer rounded-[14px] border-[1.5px] border-line bg-transparent p-3.5 text-sm font-bold text-danger disabled:opacity-50"
              disabled={isSubmiting}
              onClick={() => setIsDeleteConfirmOpen(true)}
            >
              정산방 삭제
            </button>
            <button
              type="button"
              className="cursor-pointer rounded-[14px] bg-ink p-3.5 text-sm font-extrabold text-white disabled:opacity-50"
              disabled={isSubmiting}
              onClick={handleSave}
            >
              {isSubmiting ? "저장 중..." : "저장"}
            </button>
          </div>
        }
      >
        <div className="mb-[18px]">
          <div className={labelClass}>모임 이름</div>
          <input
            className={`${inputClass} font-semibold`}
            value={roomName}
            onChange={(e) => setRoomName(e.target.value)}
            maxLength={100}
          />
        </div>

        <div className="mb-[18px]">
          <div className={labelClass}>입장코드</div>
          <input
            className={`${inputClass} font-mono font-extrabold tracking-[0.15em]`}
            placeholder="입장코드"
            value={pin}
            onChange={handlePinChange}
            maxLength={10}
          />
        </div>

        <div className="mb-[18px]">
          <div className={labelClass}>참여 멤버</div>
          {members.length > 0 && (
            <div className="mb-2.5 flex flex-wrap gap-1.5">
              {members.map((member) => (
                <div
                  key={member.key}
                  className="flex items-center gap-1 rounded-full bg-white py-1.5 pr-1.5 pl-3.5 text-sm font-semibold"
                >
                  {member.name}
                  <button
                    type="button"
                    className="flex h-[22px] w-[22px] cursor-pointer items-center justify-center rounded-full text-ink-faint disabled:cursor-default disabled:opacity-40"
                    disabled={isLocked}
                    onClick={() => handleRemoveMember(member.key)}
                    aria-label={`${member.name} 삭제`}
                  >
                    <svg
                      width="12"
                      height="12"
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
              ))}
            </div>
          )}
          <div className="flex gap-2">
            <input
              className={`${inputClass} min-w-0 flex-1`}
              placeholder="이름 입력"
              value={memberInput}
              onChange={(e) => setMemberInput(e.target.value)}
              onKeyDown={handleMemberInputKeyDown}
              maxLength={50}
              disabled={isLocked}
            />
            <button
              type="button"
              className="cursor-pointer rounded-xl bg-ink px-[18px] text-sm font-bold text-white disabled:opacity-50"
              disabled={isLocked}
              onClick={handleAddMember}
            >
              추가
            </button>
          </div>
          {isLocked && (
            <div className="mt-2 text-xs text-ink-soft">
              정산이 시작되어 멤버를 변경할 수 없어요.
            </div>
          )}
        </div>

        <div>
          <div className={labelClass}>방 링크</div>
          <div className="flex items-center gap-2.5 rounded-xl bg-white py-2 pr-2 pl-3.5">
            <div className="min-w-0 flex-1 truncate text-sm font-semibold">
              {roomLink}
            </div>
            <button
              type="button"
              className={`shrink-0 cursor-pointer rounded-[9px] px-3.5 py-2 text-[13px] font-bold text-white transition-colors ${
                isLinkCopied ? "bg-brand" : "bg-ink"
              }`}
              onClick={handleCopyLink}
            >
              {isLinkCopied ? "✓ 복사됨" : "복사"}
            </button>
          </div>
        </div>
      </BottomSheet>
      {isDeleteConfirmOpen && (
        <ConfirmModal
          title="정산방을 삭제할까요?"
          message={
            expenseCount > 0
              ? `결제내역 ${expenseCount}건이 모두 사라지고 되돌릴 수 없어요.`
              : "되돌릴 수 없어요."
          }
          confirmText="삭제"
          variant="danger"
          disabled={isSubmiting}
          onConfirm={handleDeleteRoom}
          onCancel={() => setIsDeleteConfirmOpen(false)}
        />
      )}
    </>
  );
}
