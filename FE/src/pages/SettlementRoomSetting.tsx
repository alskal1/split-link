import { useState } from "react";
import type { ChangeEvent, KeyboardEvent } from "react";
import toast from "react-hot-toast";
import { deleteRoom, updateRoom } from "../api/room";
import Modal from "../components/Modal";
import Button from "../components/Button";
import Input from "../components/Input";
import xIcon from "../assets/x.svg";

interface SettlementRoomSettingProps {
  slug: string;
  title: string;
  members: string[];
  pin: string;
  baseCurrency: string;
  onClose: () => void;
  onSaved: (
    updated: { title: string; baseCurrency: string; memberNames: string[] },
    newPin: string,
  ) => void;
  onDeleted: () => void;
}

const INPUT_BG_COLOR = "#fdf3eb";

/**
 * 결제내역 입력화면 설정 아이콘 클릭 시 뜨는 정산방 설정 모달
 * @param slug 방 슬러그
 * @param title 방 제목
 * @param members 참여 멤버 목록
 * @param pin 기존 입장코드 (수정/삭제 권한 확인용)
 * @param baseCurrency 기준통화
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
  onClose,
  onSaved,
  onDeleted,
}: SettlementRoomSettingProps) {
  // 모임 이름
  const [roomName, setRoomName] = useState(title);
  // 입장코드
  const [pin, setPin] = useState(initialPin);
  // 참여 멤버
  const [members, setMembers] = useState<string[]>(initialMembers);
  // 참여 멤버 입력란
  const [memberInput, setMemberInput] = useState("");
  // 저장/삭제 진행 여부
  const [isSubmiting, setIsSubmiting] = useState(false);

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
    const name = memberInput.trim();

    if (!name) {
      toast.error("이름을 입력해주세요.");
      return;
    }

    // 동명이인 확인
    if (members.includes(name)) {
      setMemberInput("");
      toast.error("이미 등록된 이름이에요");
      return;
    }

    setMembers([...members, name]);
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
   * @param name 참여 멤버 이름
   */
  const handleRemoveMember = (name: string) => {
    setMembers(members.filter((member) => member !== name));
  };

  /**
   * 방 링크 복사
   */
  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(roomLink);
      toast.success("링크가 복사됐어요");
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

    if (!window.confirm("정산방을 삭제할까요? 되돌릴 수 없어요.")) {
      return;
    }

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
        memberNames: members,
      });

      if (!data) {
        return;
      }

      toast.success("정산방 설정을 저장했어요");
      onSaved(
        {
          title: data.title,
          baseCurrency: data.baseCurrency,
          memberNames: data.members.map((member) => member.name),
        },
        changedPin ? pin : initialPin,
      );
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "정산방 설정 저장에 실패했어요",
      );
    } finally {
      setIsSubmiting(false);
    }
  };

  return (
    <Modal title="정산방 설정" onClose={onClose}>
      <div className="flex flex-col space-y-2">
        <div className="font-bold">모임 이름</div>
        <Input
          className="w-full"
          bgColor={INPUT_BG_COLOR}
          value={roomName}
          onChange={(e) => setRoomName(e.target.value)}
          maxLength={100}
        />
      </div>

      <div className="flex flex-col space-y-2">
        <div className="font-bold">입장코드</div>
        <Input
          className="w-full font-bold"
          bgColor={INPUT_BG_COLOR}
          placeholder="입장코드"
          value={pin}
          onChange={handlePinChange}
          maxLength={10}
        />
      </div>

      <div className="flex flex-col space-y-2">
        <div className="font-bold">참여 멤버</div>
        {members.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {members.map((member) => (
              <div
                key={member}
                className="flex items-center gap-2 rounded-full pl-3 pr-1.5 py-1.5 badge-brand"
              >
                <span className="font-semibold">{member}</span>
                <button
                  type="button"
                  className="w-5 h-5 flex items-center justify-center rounded-full bg-[#c53829] cursor-pointer shrink-0"
                  onClick={() => handleRemoveMember(member)}
                  aria-label={`${member} 삭제`}
                >
                  <img src={xIcon} alt="삭제" className="w-2.5 h-2.5" />
                </button>
              </div>
            ))}
          </div>
        )}
        <div className="flex items-center space-x-2">
          <Input
            className="flex-1"
            bgColor={INPUT_BG_COLOR}
            placeholder="이름 입력 후 추가"
            value={memberInput}
            onChange={(e) => setMemberInput(e.target.value)}
            onKeyDown={handleMemberInputKeyDown}
            maxLength={50}
          />
          <Button
            title="추가"
            bgColor="#000"
            textColor="#fff"
            className="rounded-[10px]"
            onClick={handleAddMember}
          />
        </div>
      </div>

      <div className="flex flex-col space-y-2">
        <div className="font-bold">방 링크</div>
        <div className="flex items-center space-x-2">
          <div
            className="min-w-0 flex-1 truncate rounded-[10px] border border-[#e6dfd9] p-2 text-[10pt]"
            style={{ backgroundColor: INPUT_BG_COLOR }}
          >
            {roomLink}
          </div>
          <Button
            title="복사"
            bgColor="#000"
            textColor="#fff"
            className="rounded-[10px]"
            onClick={handleCopyLink}
          />
        </div>
      </div>

      <hr className="border-[#e6dfd9]" />

      <div className="flex items-center space-x-3">
        <Button
          title="정산방 삭제"
          textColor="#c53829"
          bgColor="#fff"
          className="flex-1 rounded-[10px] border border-[#c53829]"
          disabled={isSubmiting}
          onClick={handleDeleteRoom}
        />
        <Button
          title={isSubmiting ? "저장 중..." : "저장"}
          bgColor="#000"
          textColor="#fff"
          className="flex-1 rounded-[10px]"
          disabled={isSubmiting}
          onClick={handleSave}
        />
      </div>
    </Modal>
  );
}
