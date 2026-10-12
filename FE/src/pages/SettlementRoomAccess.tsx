import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { accessRoom, getRoomSummary, selectMember } from "../api/room";
import type { ChangeEvent, KeyboardEvent } from "react";
import toast from "react-hot-toast";
import { AVATAR_PALETTE, PAGE_FONT_FAMILY } from "../constants/avatar";
import {
  loadMemberAccess,
  saveMemberAccess,
} from "../utils/memberAccessStorage";
import type { RoomMemberResponse } from "../types/roomType";

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
  // 인증에 사용된 입장코드 (정산방 설정에서 재사용하기 위해 보관)
  const [verifiedPin, setVerifiedPin] = useState<string | null>(null);
  // 인증 완료 후 받은 기준통화 (정산방 설정에서 재사용하기 위해 보관)
  const [baseCurrency, setBaseCurrency] = useState<string | null>(null);
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

    // 응답 도착 전에 slug가 바뀌거나 언마운트되면 이후 상태 갱신을 막기 위한 플래그
    let cancelled = false;

    (async () => {
      try {
        // 방 요약 정보 조회
        const summary = await getRoomSummary(slug);

        if (cancelled) {
          return;
        }

        // 방 요약 정보 없을 경우 메인 페이지로 이동
        if (!summary) {
          navigate("/", { replace: true });
          return;
        }

        setTitle(summary.title);
      } catch (error) {
        if (cancelled) {
          return;
        }

        toast.error("방 정보를 불러오지 못했어요");
        navigate("/", { replace: true });
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [slug, navigate]);

  useEffect(() => {
    // 방장 플로우: 전달받은 입장코드로 자동 인증하여 입장코드 입력 화면을 건너뜀
    if (!autoPin || !slug) {
      return;
    }

    let cancelled = false;

    verifyPin(autoPin, () => cancelled);

    return () => {
      cancelled = true;
    };
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
   * @param isCancelled 응답 도착 시점에 이 호출이 여전히 유효한지 확인하는 콜백 (자동 인증 useEffect에서만 사용)
   */
  const verifyPin = async (
    value: string,
    isCancelled: () => boolean = () => false,
  ) => {
    if (!slug || !value || isSubmiting) {
      return;
    }

    setIsSubmiting(true);

    try {
      const data = await accessRoom(slug, { pin: value });

      if (isCancelled() || !data) {
        return;
      }

      // 인증 완료 후 본인 멤버 선택 단계로 이동
      setMembers(data.members);
      setVerifiedPin(value);
      setBaseCurrency(data.baseCurrency);
      setStep("select-member");
    } catch (error) {
      if (!isCancelled()) {
        toast.error("입장코드가 올바르지 않아요");
      }
    } finally {
      if (!isCancelled()) {
        setIsSubmiting(false);
        setAutoVerifyDone(true);
      }
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
    if (
      !slug ||
      selectedMemberId === null ||
      isSubmiting ||
      !verifiedPin ||
      !baseCurrency
    ) {
      return;
    }

    setIsSubmiting(true);

    try {
      const data = await selectMember(slug, selectedMemberId);

      if (!data) {
        return;
      }

      saveMemberAccess({ ...data, pin: verifiedPin, baseCurrency });
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

  const selectedName =
    members.find((m) => m.memberId === selectedMemberId)?.name ?? null;
  const canEnter = selectedMemberId !== null && !isSubmiting;

  return (
    <div
      className={`min-h-screen bg-paper text-ink break-keep ${PAGE_FONT_FAMILY}`}
    >
      <div className="mx-auto max-w-[520px] px-5 pb-[140px]">
        <header className="flex h-16 items-center justify-center">
          <div className="max-w-[60%] truncate text-sm font-bold text-ink-soft">
            {title}
          </div>
        </header>

        {step === "select-member" ? (
          <>
            <div className="animate-[sl-up_0.45s_ease_both] pt-5 pb-7">
              <h1 className="mb-2.5 text-[length:clamp(28px,7.5vw,36px)] leading-[1.2] font-extrabold tracking-[-0.045em]">
                어떤 멤버로
                <br />
                입장할까요?
              </h1>
              <p className="text-[15px] leading-[1.6] text-ink-soft">
                선택한 이름 기준으로 내가 보낼 돈, 받을 돈이 표시돼요.
              </p>
            </div>

            <div className="flex flex-col gap-2">
              {members.map((member, i) => {
                const isSelected = selectedMemberId === member.memberId;
                const [bg, color] = AVATAR_PALETTE[i % AVATAR_PALETTE.length];

                return (
                  <button
                    key={member.memberId}
                    type="button"
                    disabled={member.isActive}
                    onClick={() => setSelectedMemberId(member.memberId)}
                    style={{ animationDelay: `${i * 50}ms` }}
                    className={`flex w-full animate-[sl-up_0.4s_ease_both] items-center gap-3.5 rounded-2xl border-[1.5px] px-4 py-3.5 text-left transition-[background-color,border-color] duration-200 active:scale-[0.98] ${
                      member.isActive
                        ? "cursor-not-allowed border-transparent bg-white/40"
                        : isSelected
                          ? "cursor-pointer border-brand bg-white"
                          : "cursor-pointer border-transparent bg-white/55"
                    }`}
                  >
                    <span
                      className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-[15px] font-extrabold ${
                        member.isActive ? "opacity-50" : ""
                      }`}
                      style={{ backgroundColor: bg, color }}
                    >
                      {member.name.slice(0, 1)}
                    </span>
                    <span
                      className={`flex-1 text-base font-bold ${
                        member.isActive ? "text-ink-faint" : "text-ink"
                      }`}
                    >
                      {member.name}
                    </span>
                    {member.isActive ? (
                      <span className="text-[13px] font-semibold text-ink-faint">
                        참여중
                      </span>
                    ) : (
                      <span
                        className={`flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-full border-2 transition-[background-color,border-color] duration-200 ${
                          isSelected
                            ? "border-brand bg-brand"
                            : "border-[#d3d1cb] bg-transparent"
                        }`}
                      >
                        <svg
                          width="12"
                          height="12"
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
                    )}
                  </button>
                );
              })}
            </div>
          </>
        ) : (
          <>
            <div className="animate-[sl-up_0.45s_ease_both] pt-5 pb-7">
              <div className="mb-2 text-sm font-bold text-brand">
                모임정산 초대
              </div>
              <h1 className="mb-2.5 text-[length:clamp(28px,7.5vw,36px)] leading-[1.2] font-extrabold tracking-[-0.045em]">
                {title}
              </h1>
              <p className="text-[15px] leading-[1.6] text-ink-soft">
                방장에게 받은 입장코드를 입력해주세요.
              </p>
            </div>
            <input
              value={pin}
              onChange={handlePinChange}
              onKeyDown={handleKeyDown}
              placeholder="입장코드"
              maxLength={10}
              autoCapitalize="off"
              autoComplete="off"
              spellCheck={false}
              className="w-full rounded-[14px] border-[1.5px] border-line bg-white px-[18px] py-4 text-center font-mono text-xl font-bold tracking-[0.18em] transition-colors outline-none placeholder:text-base placeholder:font-normal placeholder:tracking-normal placeholder:text-[#b5b3ad] focus:border-brand"
            />
          </>
        )}
      </div>

      {/* 하단 고정 버튼 */}
      <div className="fixed right-0 bottom-0 left-0 z-20 bg-gradient-to-t from-paper from-70% to-paper/0 px-5 pt-5 pb-[calc(16px+env(safe-area-inset-bottom))]">
        <div className="mx-auto max-w-[480px]">
          {step === "select-member" ? (
            <button
              type="button"
              disabled={!canEnter}
              onClick={handleEnterRoom}
              className={`block w-full rounded-[14px] py-[18px] text-center text-base font-extrabold text-white transition-[background-color,transform] duration-200 active:scale-[0.98] ${
                canEnter
                  ? "cursor-pointer bg-ink"
                  : "cursor-not-allowed bg-[#c9c7c1]"
              }`}
            >
              {isSubmiting
                ? "이동 중..."
                : selectedName
                  ? `${selectedName}(으)로 입장`
                  : "멤버를 선택해주세요"}
            </button>
          ) : (
            <button
              type="button"
              disabled={pin.length < 4 || isSubmiting}
              onClick={handleSubmit}
              className={`block w-full rounded-[14px] py-[18px] text-center text-base font-extrabold text-white transition-[background-color,transform] duration-200 active:scale-[0.98] ${
                pin.length < 4 || isSubmiting
                  ? "cursor-not-allowed bg-[#c9c7c1]"
                  : "cursor-pointer bg-ink"
              }`}
            >
              확인
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
