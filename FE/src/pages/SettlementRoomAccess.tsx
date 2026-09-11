import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { accessRoom, getRoomSummary, selectMember } from "../api/room";
import type { ChangeEvent, KeyboardEvent } from "react";
import toast from "react-hot-toast";
import Button from "../components/Button";
import Input from "../components/Input";
import {
  loadMemberAccess,
  saveMemberAccess,
} from "../utils/memberAccessStorage";
import type { RoomMemberResponse } from "../types/roomType";
import checkIcon from "../assets/check.svg";

export default function SettlementRoomAccess() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  // 방장 플로우에서 전달된 입장코드 (자동 인증용)
  const autoPin = (location.state as { pin?: string } | null)?.pin ?? null;

  // 현재 단계 (입장코드 입력 → 본인 멤버 선택)
  const [step, setStep] = useState<"pin" | "select-member">("pin");
  // 방 제목 (안내용)
  const [title, setTitle] = useState<string | null>(null);
  // 입장코드 입력값
  const [pin, setPin] = useState("");
  // 인증 진행 여부
  const [isSubmiting, setIsSubmiting] = useState(false);
  // 인증 완료 후 받은 참여 멤버 목록
  const [members, setMembers] = useState<RoomMemberResponse[]>([]);
  // 선택한 본인 멤버
  const [selectedMemberId, setSelectedMemberId] = useState<number | null>(null);
  // 방장 플로우 자동 인증 시도 완료 여부 (실패 시 수동 입력 폼으로 대체 노출)
  const [autoVerifyDone, setAutoVerifyDone] = useState(!autoPin);

  useEffect(() => {
    // slug가 없을 경우 메인 페이지로 이동
    if (!slug) {
      navigate("/", { replace: true });
      return;
    }

    // 이미 본인 멤버로 접근한 로컬 세션이 있으면 선택 화면을 건너뛰고 바로 입장
    if (loadMemberAccess(slug)) {
      navigate(`/rooms/${slug}/room`, { replace: true });
      return;
    }

    (async () => {
      try {
        // 방 요약 정보 조회
        const summary = await getRoomSummary(slug);

        // 방 요약 정보 없을 경우 메인 페이지로 이동
        if (!summary) {
          navigate("/", { replace: true });
          return;
        }

        setTitle(summary.title);
      } catch (error) {
        toast.error("방 정보를 불러오지 못했어요");
        navigate("/", { replace: true });
      }
    })();
  }, [slug, navigate]);

  useEffect(() => {
    // 방장 플로우: 전달받은 입장코드로 자동 인증하여 입장코드 입력 화면을 건너뜀
    if (autoPin && slug) {
      verifyPin(autoPin);
    }
  }, [autoPin, slug]);

  /**
   * 입장코드 입력 변경 이벤트
   * @param e
   */
  const handlePinChange = (e: ChangeEvent<HTMLInputElement>) => {
    // 영대소문자 및 숫자만 입력 가능, 최대 10자 글자수 제한
    setPin(e.target.value.replace(/[^a-zA-Z0-9]/g, "").slice(0, 10));
  };

  /**
   * 입장코드 검증
   * @param value 검증할 입장코드
   */
  const verifyPin = async (value: string) => {
    if (!slug || !value || isSubmiting) {
      return;
    }

    setIsSubmiting(true);

    try {
      const data = await accessRoom(slug, { pin: value });

      if (!data) {
        return;
      }

      // 인증 완료 후 본인 멤버 선택 단계로 이동
      setMembers(data.members);
      setStep("select-member");
    } catch (error) {
      toast.error("입장코드가 올바르지 않아요");
    } finally {
      setIsSubmiting(false);
      setAutoVerifyDone(true);
    }
  };

  /**
   * 입장코드 확인 버튼 클릭 이벤트
   */
  const handleSubmit = () => verifyPin(pin);

  /**
   * 입장코드 입력란 keyDown 이벤트
   * @param e
   */
  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" && !e.nativeEvent.isComposing) {
      e.preventDefault();
      handleSubmit();
    }
  };

  /**
   * 본인 멤버 선택 후 정산방 입장
   */
  const handleEnterRoom = async () => {
    if (!slug || selectedMemberId === null || isSubmiting) {
      return;
    }

    setIsSubmiting(true);

    try {
      const data = await selectMember(slug, selectedMemberId);

      if (!data) {
        return;
      }

      saveMemberAccess(data);
      navigate(`/rooms/${slug}/room`, { replace: true });
    } catch (error) {
      toast.error("이미 다른 사람이 선택한 멤버예요");
    } finally {
      setIsSubmiting(false);
    }
  };

  if (!title || !autoVerifyDone) {
    return null;
  }

  if (step === "select-member") {
    return (
      <div className="flex flex-col items-center flex-1 text-center space-y-8 justify-center">
        <div className="w-20 h-20 flex items-center justify-center rounded-full badge-brand">
          <img src={checkIcon} alt="" className="w-9 h-9" />
        </div>

        <div className="flex flex-col space-y-2">
          <div className="text-2xl font-bold">{title}</div>
          <div className="explain-text">참여 멤버 중 본인을 선택해주세요</div>
        </div>

        <div className="w-full flex flex-col space-y-3">
          {members.map((member) => {
            const isSelected = selectedMemberId === member.memberId;

            return (
              <button
                key={member.memberId}
                type="button"
                className={`w-full flex items-center justify-between rounded-2xl p-4 text-left border ${
                  member.active
                    ? "bg-gray border-[#e6dfd9] cursor-not-allowed"
                    : isSelected
                      ? "bg-[#ffdcd4] border-[#c53829] cursor-pointer"
                      : "bg-white border-[#e6dfd9] cursor-pointer"
                }`}
                disabled={member.active}
                onClick={() => setSelectedMemberId(member.memberId)}
              >
                <span className="font-bold">{member.name}</span>
                {member.active ? (
                  <span className="text-[10pt] text-[#9c8f86]">참여중</span>
                ) : (
                  <span
                    className={`w-6 h-6 rounded-full flex items-center justify-center ${
                      isSelected ? "bg-[#c53829]" : "border-2 border-[#e6dfd9]"
                    }`}
                  />
                )}
              </button>
            );
          })}
        </div>

        <Button
          title={isSubmiting ? "이동 중..." : "정산방 들어가기"}
          bgColor="#e85a48"
          textColor="#fff"
          className="w-full rounded-2xl h-12.5"
          disabled={selectedMemberId === null || isSubmiting}
          onClick={handleEnterRoom}
        />
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center flex-1 text-center space-y-8 justify-center">
      <div className="flex flex-col space-y-2">
        <div className="font-bold text-[#c53829]">모임정산 초대</div>
        <div className="text-2xl font-bold">{title}</div>
        <div className="explain-text">
          방장에게 받은 입장코드를 입력해주세요
        </div>
      </div>

      <Input
        className="w-full text-center font-bold"
        height="56px"
        placeholder="입장코드"
        value={pin}
        onChange={handlePinChange}
        onKeyDown={handleKeyDown}
        maxLength={10}
      />

      <Button
        title="확인"
        bgColor="#000"
        textColor="#fff"
        className="w-full rounded-2xl h-12.5"
        disabled={pin?.length < 4 || isSubmiting}
        onClick={handleSubmit}
      />
    </div>
  );
}
