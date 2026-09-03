import { useState } from "react";
import type { ChangeEvent, KeyboardEvent } from "react";
import { useNavigate } from "react-router-dom";
import Button from "../components/Button";
import Input from "../components/Input";
import Select from "../components/Select";
import toast from "react-hot-toast";
import arrowLeft from "../assets/arrow-left.svg";
import xIcon from "../assets/x.svg";
import { CURRENCY_OPTIONS } from "../constants/currency";
import { createSettlementRoom } from "../api/room";
import { saveCreatedRoomStorage } from "../utils/createdRoomStorage";

export default function NewSettlementRoom() {
  const navigate = useNavigate();
  // 방 제목
  const [roomName, setRoomName] = useState("");
  // 기준통화
  const [baseCurrency, setBaseCurrency] = useState("KRW");
  // 기타통화
  const [etcCurrencyCode, setEtcCurrencyCode] = useState("");
  // 참여멤버 입력란
  const [memberInput, setMemberInput] = useState("");
  // 참여멤버
  const [members, setMembers] = useState<string[]>([]);
  // 입장코드
  const [entryCode, setEntryCode] = useState("");

  // 유효성 체크
  const isValid =
    roomName.trim().length > 0 &&
    (baseCurrency !== "ETC" || etcCurrencyCode.trim().length > 0) &&
    members.length > 0 &&
    entryCode.length >= 4 &&
    entryCode.length <= 10;

  /**
   * 참여멤버 추가
   * @returns
   */
  const handleAddMember = () => {
    // 입력된 이름 공백 제거
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

    // 참여멤버 추가
    setMembers([...members, name]);
    setMemberInput("");
  };

  /**
   * 참여멤버 입력란 keyDown 이벤트
   * @param e
   */
  const handleMemberInputKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    // Enter 키 입력 시 참여멤버 추가 이벤트 발생
    if (e.key === "Enter" && !e.nativeEvent.isComposing) {
      e.preventDefault();
      handleAddMember();
    }
  };

  /**
   * 입장코드 입력 변경 이벤트
   * @param e
   */
  const handleEntryCodeChange = (e: ChangeEvent<HTMLInputElement>) => {
    // 영대소문자 및 숫자만 입력 가능, 최대 10자 글자수 제한
    setEntryCode(e.target.value.replace(/[^a-zA-Z0-9]/g, "").slice(0, 10));
  };

  /**
   * 참여 멤버 삭제
   * @param name 참여 멤버 이름
   */
  const handlekRemoveMember = (name: string) => {
    setMembers(members.filter((member) => member !== name));
  };

  /**
   * 정산방 생성
   */
  const handleCreateSettlementRoom = async () => {
    if (!isValid) {
      return;
    }

    // 정산방 생성 API 요청
    const data = await createSettlementRoom({
      title: roomName.trim(),
      baseCurrency:
        baseCurrency === "ETC" ? etcCurrencyCode.trim() : baseCurrency,
      pin: entryCode,
      memberNames: members,
    });

    if (!data) {
      return;
    }

    // 세션스토리지 저장
    saveCreatedRoomStorage(data);
    navigate("/settlement-room-created", { replace: true });
  };

  return (
    <div className="flex flex-col space-y-8">
      <div className="flex items-center space-x-2">
        <div className="w-7 h-7 flex justify-center items-center bg-white rounded-4xl">
          <img src={arrowLeft} alt="뒤로가기" className="w-4 h-4" />
        </div>
        <div className="font-bold">정산방 만들기</div>
      </div>
      <div className="flex flex-col space-y-2">
        <div className="font-bold">모임 이름</div>
        <Input
          placeholder="예: 발리 여행 2박3일"
          value={roomName}
          onChange={(e) => setRoomName(e.target.value)}
          maxLength={100}
        />
      </div>
      <div className="flex flex-col space-y-2">
        <div className="font-bold">기준 통화</div>
        <Select
          options={CURRENCY_OPTIONS}
          option={baseCurrency}
          onChange={setBaseCurrency}
        />
        {baseCurrency === "ETC" && (
          <Input
            placeholder="통화 코드 입력"
            value={etcCurrencyCode}
            onChange={(e) => setEtcCurrencyCode(e.target.value)}
            maxLength={3}
          />
        )}
        <div className="explain-text">
          모든 지출이 이 통화로 환산되어 정산돼요.
        </div>
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
                  className="w-5 h-5 flex items-center justify-center rounded-full bg-[#C53829] cursor-pointer shrink-0"
                  onClick={() => handlekRemoveMember(member)}
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
        <div className="explain-text">
          게스트는 앱 설치 없이 초대 링크만으로 바로 참여할 수 있어요.
        </div>
      </div>
      <div className="flex flex-col space-y-2">
        <div className="font-bold">입장 코드</div>
        <Input
          placeholder="영대소문자 및 숫자 4~10자"
          value={entryCode}
          onChange={handleEntryCodeChange}
          maxLength={10}
        />
        {entryCode.length < 4 && entryCode.length != 0 ? (
          <div className="text-[10pt] text-[#C53829]">
            영대소문자와 숫자만 사용해 4~10자로 입력해주세요.
          </div>
        ) : (
          <div className="explain-text">
            게스트가 정산방에 들어올 때 필요한 코드예요.
          </div>
        )}
      </div>
      <Button
        title="정산방 만들기"
        bgColor="#000"
        textColor="#fff"
        className="rounded-[10px]"
        disabled={!isValid}
        onClick={handleCreateSettlementRoom}
      />
    </div>
  );
}
