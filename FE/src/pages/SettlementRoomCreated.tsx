import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { clearCreatedRoom, loadCreatedRoom } from "../utils/createdRoomStorage";
import { AVATAR_PALETTE, PAGE_FONT_FAMILY } from "../constants/avatar";
import type { RoomStorage } from "../types/roomType";
import toast from "react-hot-toast";

// 아바타 스택에 보여줄 최대 멤버 수
const MAX_STACK = 5;

export default function SettlementRoomCreated() {
  const navigate = useNavigate();
  const [room, setRoom] = useState<RoomStorage | null>(null);
  // 링크 복사 완료 표시 여부
  const [copied, setCopied] = useState(false);
  const copiedTimer = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    const stored = loadCreatedRoom();

    // 세션스토리지에 정보 없을 경우 메인 페이지로 이동
    if (!stored) {
      navigate("/", { replace: true });
      return;
    }

    setRoom(stored);
  }, [navigate]);

  useEffect(() => () => clearTimeout(copiedTimer.current), []);

  if (!room) {
    return null;
  }

  // 방 url
  const inviteUrl = `${window.location.origin}/rooms/${room.slug}`;
  const [firstMember, secondMember] = room.memberNames;
  const memberSummary =
    room.memberNames.length > 0
      ? `${[firstMember, secondMember].filter(Boolean).join(", ")}${
          room.memberNames.length > 2
            ? ` 외 ${room.memberNames.length - 2}명`
            : ""
        } 참여 예정`
      : "아직 추가된 멤버가 없어요";

  // 방 url 복사 버튼 클릭 이벤트
  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(inviteUrl);
      setCopied(true);
      clearTimeout(copiedTimer.current);
      copiedTimer.current = setTimeout(() => setCopied(false), 1600);
    } catch {
      toast.error("복사에 실패했어요");
    }
  };

  // 공유하기 (공유 시트를 지원하지 않으면 링크 복사)
  const handleShare = async () => {
    if (!navigator.share) {
      handleCopyLink();
      return;
    }

    try {
      await navigator.share({
        title: room.title,
        text: `입장코드: ${room.pin}`,
        url: inviteUrl,
      });
    } catch {
      // 사용자가 공유를 취소한 경우 별도 처리 없음
    }
  };

  // 정산방 이동
  const handleEnterRoom = async () => {
    // 세션스토리지 정보 제거
    clearCreatedRoom();
    // 방장은 입장코드를 이미 알고 있으므로 입장코드 입력은 건너뛰고 본인 멤버 선택 단계로 바로 이동
    navigate(`/rooms/${room.slug}`, {
      replace: true,
      state: { pin: room.pin },
    });
  };

  return (
    <div
      className={`min-h-screen bg-paper text-ink break-keep ${PAGE_FONT_FAMILY}`}
    >
      <div className="mx-auto max-w-[520px] px-5 pt-[72px] pb-10">
        <div className="mb-7 flex h-16 w-16 animate-[sl-pop_0.55s_cubic-bezier(0.3,1.4,0.5,1)_both] items-center justify-center rounded-[20px] bg-brand">
          <svg
            width="30"
            height="30"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#fff"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M20 6 9 17l-5-5" />
          </svg>
        </div>

        <div className="animate-[sl-up_0.5s_0.15s_ease_both]">
          <div className="mb-2 text-sm font-bold text-brand">
            정산방이 열렸어요
          </div>
          <h1 className="mb-3 text-[length:clamp(28px,7.5vw,36px)] leading-[1.2] font-extrabold tracking-[-0.045em]">
            {room.title}
          </h1>
          <p className="mb-8 text-[15px] leading-[1.6] text-ink-soft">
            링크와 입장코드를 멤버에게 보내주세요.
          </p>
        </div>

        <div className="mb-3 animate-[sl-up_0.5s_0.25s_ease_both] rounded-[20px] bg-white p-5">
          <div className="mb-2 text-xs font-bold text-ink-soft">초대 링크</div>
          <div className="flex items-center gap-2.5">
            <div className="min-w-0 flex-1 truncate text-[15px] font-semibold">
              {inviteUrl}
            </div>
            <button
              type="button"
              onClick={handleCopyLink}
              className={`shrink-0 cursor-pointer rounded-[10px] px-4 py-2.5 text-[13px] font-bold text-white transition-[background-color,transform] active:scale-95 ${
                copied ? "bg-brand" : "bg-ink"
              }`}
            >
              {copied ? "복사됨" : "복사"}
            </button>
          </div>
          <div className="my-[18px] h-px bg-line" />
          <div className="mb-2.5 text-xs font-bold text-ink-soft">입장코드</div>
          <div className="flex flex-wrap gap-1.5">
            {room.pin.split("").map((c, i) => (
              <span
                key={i}
                className="flex h-11 w-9 items-center justify-center rounded-[10px] bg-paper font-mono text-xl font-extrabold"
              >
                {c}
              </span>
            ))}
          </div>
        </div>

        <div className="flex animate-[sl-up_0.5s_0.35s_ease_both] items-center gap-2.5 px-1 pt-4 pb-8">
          <div className="flex">
            {room.memberNames.slice(0, MAX_STACK).map((name, i) => {
              const [bg, color] = AVATAR_PALETTE[i % AVATAR_PALETTE.length];
              return (
                <span
                  key={name}
                  className="-mr-2 flex h-[30px] w-[30px] items-center justify-center rounded-full border-2 border-paper text-xs font-extrabold"
                  style={{ backgroundColor: bg, color }}
                >
                  {name.slice(0, 1)}
                </span>
              );
            })}
          </div>
          <span className="ml-2 text-sm text-ink-soft">{memberSummary}</span>
        </div>

        <div className="grid animate-[sl-up_0.5s_0.45s_ease_both] grid-cols-[1fr_1.4fr] gap-2">
          <button
            type="button"
            onClick={handleShare}
            className="cursor-pointer rounded-[14px] border-[1.5px] border-line bg-white p-[17px] text-[15px] font-bold transition-transform active:scale-[0.97]"
          >
            공유하기
          </button>
          <button
            type="button"
            onClick={handleEnterRoom}
            className="cursor-pointer rounded-[14px] bg-ink p-[17px] text-[15px] font-extrabold text-white transition-[background-color,transform] hover:bg-brand-dark active:scale-[0.97]"
          >
            정산방으로 이동
          </button>
        </div>
      </div>
    </div>
  );
}
