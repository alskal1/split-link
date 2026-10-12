import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { BASE_CURRENCY_CARDS } from "../constants/currency";
import { createSettlementRoom } from "../api/room";
import { saveCreatedRoomStorage } from "../utils/createdRoomStorage";
import type { ChangeEvent, KeyboardEvent } from "react";
import toast from "react-hot-toast";
import CurrencyCard from "../components/CurrencyCard";
import { AVATAR_PALETTE, PAGE_FONT_FAMILY } from "../constants/avatar";

// 입장코드 자동 생성에 쓰는 문자 (헷갈리는 문자 제외)
const PIN_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

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
  // 생성 진행 여부
  const [isSubmiting, setIsSubmiting] = useState(false);

  // 유효성 체크
  const isValid =
    roomName.trim().length > 0 &&
    (baseCurrency !== "ETC" || etcCurrencyCode.trim().length > 0) &&
    members.length > 0 &&
    entryCode.length >= 4 &&
    entryCode.length <= 10;

  const isEntryCodeLenValid = entryCode.length >= 4 && entryCode.length <= 10;
  const isCurrencyValid =
    baseCurrency !== "ETC" || etcCurrencyCode.trim().length > 0;
  // 입력 완료된 항목 수 (모임 이름, 기준 통화, 참여 멤버, 입장코드)
  const doneCount = [
    roomName.trim().length > 0,
    isCurrencyValid,
    members.length > 0,
    isEntryCodeLenValid,
  ].filter(Boolean).length;

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
   * 입장코드 자동 생성 (6자)
   */
  const handleGeneratePin = () => {
    let pin = "";
    for (let i = 0; i < 6; i++) {
      pin += PIN_CHARS[Math.floor(Math.random() * PIN_CHARS.length)];
    }
    setEntryCode(pin);
  };

  /**
   * 정산방 생성
   */
  const handleCreateSettlementRoom = async () => {
    // 유효성에 맞지 않거나 생성 진행중인 경우 리턴
    if (!isValid || isSubmiting) {
      return;
    }

    setIsSubmiting(true);

    try {
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
    } catch (error) {
      toast.error("방 생성에 실패했어요");
    } finally {
      setIsSubmiting(false);
    }
  };

  const pinChecks = [
    { label: "4~10자", ok: isEntryCodeLenValid },
    { label: "영문·숫자만", ok: entryCode.length > 0 },
  ];

  return (
    <div
      className={`min-h-screen bg-paper text-ink break-keep ${PAGE_FONT_FAMILY}`}
    >
      <div className="mx-auto max-w-[520px] px-5 pb-[140px]">
        {/* 헤더 */}
        <header className="flex h-16 items-center justify-between">
          <button
            type="button"
            aria-label="뒤로"
            onClick={() => navigate(-1)}
            className="-ml-2 flex h-10 w-10 cursor-pointer items-center justify-center rounded-xl text-ink active:bg-[#eae9e4]"
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
              <path d="m12 19-7-7 7-7" />
              <path d="M19 12H5" />
            </svg>
          </button>
          <div className="flex items-center gap-2 text-base font-extrabold tracking-[-0.04em]">
            <span className="h-2 w-2 rotate-45 rounded-[2px] bg-brand" />
            splitlink
          </div>
          <div className="w-8" />
        </header>

        <div className="pt-5 pb-8">
          <h1 className="mb-2.5 text-[length:clamp(30px,8vw,40px)] leading-[1.15] font-extrabold tracking-[-0.045em]">
            새 정산방
          </h1>
          <p className="text-[15px] leading-[1.6] text-ink-soft">
            이름, 통화, 멤버, 입장코드만 정하면 바로 시작해요.
          </p>
        </div>

        {/* 01 모임 이름 */}
        <section className="border-t-2 border-ink pt-6 pb-7">
          <div className="mb-3.5 flex items-baseline gap-2.5">
            <span className="text-xs font-bold text-brand tabular-nums">
              01
            </span>
            <span className="text-[15px] font-extrabold">모임 이름</span>
          </div>
          <input
            value={roomName}
            onChange={(e) => setRoomName(e.target.value)}
            placeholder="예: 발리 여행 2박3일"
            maxLength={100}
            className="w-full rounded-none border-0 border-b-2 border-line bg-transparent pt-1.5 pb-3 text-[length:clamp(22px,6vw,26px)] font-bold tracking-[-0.03em] transition-colors outline-none placeholder:text-[#b5b3ad] focus:border-brand"
          />
        </section>

        {/* 02 기준 통화 */}
        <section className="border-t border-line pt-6 pb-7">
          <div className="mb-3.5 flex items-baseline gap-2.5">
            <span className="text-xs font-bold text-brand">02</span>
            <span className="text-[15px] font-extrabold">기준 통화</span>
            <span className="ml-auto text-[13px] text-ink-soft">
              모든 지출이 이 통화로 환산돼요
            </span>
          </div>
          <div className="grid grid-cols-[repeat(auto-fill,minmax(104px,1fr))] gap-2">
            {BASE_CURRENCY_CARDS.map((c) => (
              <CurrencyCard
                key={c.value}
                symbol={c.symbol}
                label={c.label}
                selected={baseCurrency === c.value}
                onClick={() => setBaseCurrency(c.value)}
              />
            ))}
          </div>
          {baseCurrency === "ETC" && (
            <input
              value={etcCurrencyCode}
              onChange={(e) => setEtcCurrencyCode(e.target.value)}
              placeholder="통화 코드 입력 (예: PHP)"
              maxLength={3}
              className="mt-3 w-full animate-sl-up rounded-xl border-[1.5px] border-line bg-white px-4 py-3.5 text-[15px] outline-none placeholder:text-[#b5b3ad] focus:border-brand"
            />
          )}
        </section>

        {/* 03 참여 멤버 */}
        <section className="border-t border-line pt-6 pb-7">
          <div className="mb-3.5 flex items-baseline gap-2.5">
            <span className="text-xs font-bold text-brand">03</span>
            <span className="text-[15px] font-extrabold">참여 멤버</span>
            <span className="ml-auto text-[13px] font-bold text-ink-soft tabular-nums">
              {members.length}명
            </span>
          </div>
          <div className="flex items-center gap-2 rounded-[14px] border-[1.5px] border-line bg-white py-1.5 pr-1.5 pl-4 transition-colors focus-within:border-brand">
            <input
              value={memberInput}
              onChange={(e) => setMemberInput(e.target.value)}
              onKeyDown={handleMemberInputKeyDown}
              placeholder="이름 입력 후 추가"
              maxLength={50}
              className="min-w-0 flex-1 bg-transparent py-2 text-[15px] outline-none placeholder:text-[#b5b3ad]"
            />
            <button
              type="button"
              aria-label="멤버 추가"
              onClick={handleAddMember}
              className={`flex h-10 w-10 shrink-0 cursor-pointer items-center justify-center rounded-[10px] text-white transition-[background-color,transform] active:scale-[0.92] ${
                memberInput.trim() ? "bg-ink" : "bg-[#d3d1cb]"
              }`}
            >
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
              >
                <path d="M12 5v14" />
                <path d="M5 12h14" />
              </svg>
            </button>
          </div>
          {members.length > 0 && (
            <div className="mt-3.5 flex flex-wrap gap-2">
              {members.map((member, i) => {
                const [bg, color] = AVATAR_PALETTE[i % AVATAR_PALETTE.length];
                return (
                  <div
                    key={member}
                    className="flex animate-sl-chip items-center gap-2 rounded-full border border-line bg-white py-[5px] pr-1.5 pl-[5px]"
                  >
                    <span
                      className="flex h-[26px] w-[26px] items-center justify-center rounded-full text-xs font-extrabold"
                      style={{ backgroundColor: bg, color }}
                    >
                      {member.slice(0, 1)}
                    </span>
                    <span className="text-sm font-semibold">{member}</span>
                    <button
                      type="button"
                      aria-label={`${member} 삭제`}
                      onClick={() => handlekRemoveMember(member)}
                      className="flex h-[22px] w-[22px] cursor-pointer items-center justify-center rounded-full p-0 text-ink-faint hover:bg-[#edece7] hover:text-ink"
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
                );
              })}
            </div>
          )}
          <p className="mt-3 text-[13px] leading-[1.6] text-ink-soft">
            게스트는 앱 설치 없이 초대 링크만으로 바로 참여할 수 있어요.
          </p>
        </section>

        {/* 04 입장코드 */}
        <section className="border-t border-line pt-6 pb-2">
          <div className="mb-3.5 flex items-baseline gap-2.5">
            <span className="text-xs font-bold text-brand">04</span>
            <span className="text-[15px] font-extrabold">입장코드</span>
            <button
              type="button"
              onClick={handleGeneratePin}
              className="ml-auto flex cursor-pointer items-center gap-1 py-1 text-[13px] font-bold text-brand"
            >
              <svg
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M21 12a9 9 0 1 1-3-6.7L21 8" />
                <path d="M21 3v5h-5" />
              </svg>
              자동 생성
            </button>
          </div>
          <input
            value={entryCode}
            onChange={handleEntryCodeChange}
            placeholder="영대소문자 및 숫자 4~10자"
            maxLength={10}
            autoCapitalize="off"
            autoComplete="off"
            spellCheck={false}
            className={`w-full rounded-[14px] border-[1.5px] bg-white px-[18px] py-4 font-mono text-xl font-bold tracking-[0.18em] transition-colors outline-none placeholder:text-base placeholder:font-normal placeholder:tracking-normal placeholder:text-[#b5b3ad] focus:border-brand ${
              entryCode.length > 0 && entryCode.length < 4
                ? "border-danger"
                : "border-line"
            }`}
          />
          <div className="mt-3 flex flex-wrap gap-4">
            {pinChecks.map((pc) => (
              <div
                key={pc.label}
                className={`flex items-center gap-1.5 text-[13px] font-semibold transition-colors ${
                  pc.ok ? "text-brand-dark" : "text-ink-faint"
                }`}
              >
                <span
                  className={`flex h-4 w-4 items-center justify-center rounded-full transition-colors ${
                    pc.ok ? "bg-brand" : "bg-[#d3d1cb]"
                  }`}
                >
                  <svg
                    width="10"
                    height="10"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="#fff"
                    strokeWidth="3.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M20 6 9 17l-5-5" />
                  </svg>
                </span>
                {pc.label}
              </div>
            ))}
          </div>
          <p className="mt-3 text-[13px] leading-[1.6] text-ink-soft">
            게스트가 정산방에 들어올 때 필요한 코드예요.
          </p>
        </section>
      </div>

      {/* 하단 고정 버튼 */}
      <div className="fixed right-0 bottom-0 left-0 z-20 bg-gradient-to-t from-paper from-70% to-paper/0 px-5 pt-5 pb-[calc(16px+env(safe-area-inset-bottom))]">
        <div className="mx-auto max-w-[480px]">
          <button
            type="button"
            disabled={!isValid || isSubmiting}
            onClick={handleCreateSettlementRoom}
            className={`flex w-full items-center justify-center gap-2.5 rounded-[14px] py-[18px] text-base font-extrabold text-white transition-[background-color,transform] duration-200 active:scale-[0.98] ${
              isValid && !isSubmiting
                ? "cursor-pointer bg-ink"
                : "cursor-not-allowed bg-[#c9c7c1]"
            }`}
          >
            <span>{isSubmiting ? "생성 중..." : "정산방 만들기"}</span>
            <span className="text-[13px] font-semibold opacity-70">
              {doneCount}/4
            </span>
          </button>
        </div>
      </div>
    </div>
  );
}
