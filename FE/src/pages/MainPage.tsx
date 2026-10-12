import { useEffect, useRef, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";

const CTA_LABEL = "지금 시작하기";
const TOTAL_AMOUNT = 241000;

const TRANSFERS = [
  { from: "민지", to: "나", amount: 120500 },
  { from: "준호", to: "나", amount: 86000 },
  { from: "하은", to: "나", amount: 34500 },
];

const FEATURES = [
  {
    num: "01",
    title: "자동 계산",
    desc: "누가 얼마 냈는지만 입력하면, 누가 누구에게 얼마를 보내야 하는지 정리돼요.",
  },
  {
    num: "02",
    title: "해외 결제 환율",
    desc: "결제일자와 현지 통화만 입력하면 그날 환율로 원화 환산해요.",
  },
  {
    num: "03",
    title: "계좌 복사 · 간편 송금",
    desc: "계좌번호를 복사해 직접 보내거나 토스로 바로 넘어가요.",
  },
  {
    num: "04",
    title: "링크로 참여",
    desc: "게스트는 앱 설치 없이 링크와 입장코드만으로 들어와요.",
  },
];

const STEPS = ["정산방 만들기", "지출 입력", "링크 공유", "송금 완료"];

const SECTION_X = "px-[clamp(20px,5vw,56px)]";

/**
 * 화면에 들어오면 아래에서 위로 떠오르는 reveal 래퍼
 * @param delay 순서에 따른 지연(ms)
 * @param onReveal 노출 시점에 호출되는 콜백
 */
function Reveal({
  children,
  delay = 0,
  className = "",
  as: Tag = "div",
  onReveal,
}: {
  children: ReactNode | ((visible: boolean) => ReactNode);
  delay?: number;
  className?: string;
  as?: "div" | "span";
  onReveal?: () => void;
}) {
  const ref = useRef<HTMLDivElement & HTMLSpanElement>(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) {
      return;
    }

    const io = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true);
          onReveal?.();
          io.disconnect();
        }
      },
      { threshold: 0.15 },
    );
    io.observe(el);
    return () => io.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <Tag
      ref={ref}
      className={`transition-[opacity,transform] duration-700 ease-out motion-reduce:transition-none ${
        visible ? "translate-y-0 opacity-100" : "translate-y-6 opacity-0"
      } ${className}`}
      style={{ transitionDelay: visible ? `${delay}ms` : "0ms" }}
    >
      {typeof children === "function" ? children(visible) : children}
    </Tag>
  );
}

/**
 * 히어로의 정산 현황 카드 (송금 완료가 순서대로 체크되는 데모)
 */
function HeroCard() {
  const [step, setStep] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => setStep((s) => (s + 1) % 6), 1500);
    return () => clearInterval(timer);
  }, []);

  const settled = Math.min(step, 3);
  const received = TRANSFERS.slice(0, settled).reduce(
    (sum, t) => sum + t.amount,
    0,
  );
  const justIdx = step >= 1 && step <= 3 ? step - 1 : -1;
  const toast =
    justIdx >= 0
      ? `${TRANSFERS[justIdx].from}님이 ${TRANSFERS[justIdx].amount.toLocaleString("ko-KR")}원을 보냈어요`
      : "";

  return (
    <div className="relative mx-auto w-full max-w-[480px] justify-self-center rounded-[20px] bg-white p-[clamp(22px,3vw,32px)] shadow-[0_1px_0_rgba(40,28,24,0.06),0_24px_48px_-24px_rgba(40,28,24,0.18)]">
      {/* 송금 완료 토스트 */}
      <div
        className={`pointer-events-none absolute -top-[26px] right-4 left-4 z-[2] flex items-center gap-2.5 rounded-[14px] bg-ink px-3.5 py-[11px] text-sm font-semibold text-white shadow-[0_14px_30px_-12px_rgba(18,18,18,0.45)] transition-[opacity,transform] duration-500 ${
          justIdx >= 0
            ? "translate-y-0 scale-100 opacity-100"
            : "translate-y-2.5 scale-[0.96] opacity-0"
        }`}
      >
        <span className="flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-full bg-brand">
          <svg
            width="13"
            height="13"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#fff"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M20 6 9 17l-5-5" />
          </svg>
        </span>
        <span className="truncate">{toast}</span>
      </div>

      <div className="flex items-baseline justify-between gap-3 border-b border-dashed border-line pb-[18px]">
        <div>
          <div className="mb-1 text-[13px] text-ink-soft">
            제주 2박 3일 · 4명
          </div>
          <div className="text-[length:clamp(26px,3.4vw,32px)] font-extrabold tracking-[-0.03em] tabular-nums">
            482,000원
          </div>
        </div>
        <div className="text-[13px] font-bold whitespace-nowrap text-brand">
          1인 120,500원
        </div>
      </div>

      <div className="pt-4 pb-1.5">
        <div className="mb-2 flex justify-between text-[13px] text-ink-soft tabular-nums">
          <span>받은 금액</span>
          <span className="shrink-0 whitespace-nowrap">
            <b className="text-ink">{received.toLocaleString("ko-KR")}원</b> /{" "}
            {TOTAL_AMOUNT.toLocaleString("ko-KR")}원
          </span>
        </div>
        <div className="h-1.5 overflow-hidden rounded-md bg-[#EDECE7]">
          <div
            className="h-full rounded-md bg-brand transition-[width] duration-700 ease-out"
            style={{ width: `${Math.round((received / TOTAL_AMOUNT) * 100)}%` }}
          />
        </div>
      </div>

      <div className="flex flex-col">
        {TRANSFERS.map((t, i) => {
          const isDone = i < settled;
          const justDone = i === settled - 1 && step <= 3;
          return (
            <div
              key={t.from}
              className={`-mx-3 flex items-center justify-between gap-3 rounded-[10px] border-b border-line px-3 py-4 transition-colors duration-[600ms] ${
                justDone ? "bg-brand-soft/50" : "bg-transparent"
              }`}
            >
              <div className="flex min-w-0 items-center gap-2 text-[15px] font-semibold">
                <span>{t.from}</span>
                <span className="text-ink-faint">→</span>
                <span>{t.to}</span>
              </div>
              <div className="flex shrink-0 items-center gap-2.5">
                <span className="text-[15px] font-bold tabular-nums">
                  {t.amount.toLocaleString("ko-KR")}원
                </span>
                <span
                  className={`rounded-md px-2 py-1 text-xs font-bold transition-[background-color,color,transform] duration-[400ms] ${
                    isDone
                      ? "bg-brand-soft text-brand-dark"
                      : "bg-[#EDECE7] text-ink-soft"
                  } ${justDone ? "scale-[1.12]" : "scale-100"}`}
                >
                  {isDone ? "완료" : "대기"}
                </span>
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-5 grid grid-cols-2 gap-2">
        <div className="rounded-[10px] bg-paper p-[13px] text-center text-sm font-bold">
          계좌 복사
        </div>
        <div className="rounded-[10px] bg-ink p-[13px] text-center text-sm font-bold text-white">
          토스
        </div>
      </div>
    </div>
  );
}

/**
 * 기능 항목 (노출 시 상단 포인트 라인이 채워짐)
 */
function FeatureItem({
  num,
  title,
  desc,
  index,
}: (typeof FEATURES)[number] & { index: number }) {
  return (
    <Reveal
      delay={index * 90}
      className="group relative border-b border-line py-6 pr-6 pb-7 transition-[padding] duration-300 hover:pl-2.5 active:pl-2.5"
    >
      {(visible) => (
        <>
          <div
            className="absolute -top-0.5 left-0 h-0.5 bg-brand transition-[width] duration-1000 ease-out"
            style={{
              width: visible ? "100%" : "0%",
              transitionDelay: visible ? `${index * 90 + 250}ms` : "0ms",
            }}
          />
          <div className="mb-3.5 text-[13px] font-bold text-brand tabular-nums">
            {num}
          </div>
          <h3 className="mb-2.5 text-[19px] font-extrabold tracking-[-0.02em]">
            {title}
          </h3>
          <p className="text-[15px] leading-[1.65] text-pretty text-ink-soft">
            {desc}
          </p>
        </>
      )}
    </Reveal>
  );
}

/**
 * 이용방법 단계 (노출 시 번호 원이 포인트 컬러로 채워짐)
 */
function StepItem({ num, title }: { num: number; title: string }) {
  const [filled, setFilled] = useState(false);
  const [popped, setPopped] = useState(false);

  const handleReveal = () => {
    setTimeout(
      () => {
        setFilled(true);
        setPopped(true);
        setTimeout(() => setPopped(false), 320);
      },
      450 + (num - 1) * 90,
    );
  };

  return (
    <Reveal
      delay={(num - 1) * 90}
      onReveal={handleReveal}
      className="flex items-center gap-3.5 rounded-[14px] bg-white px-5 py-[18px] hover:-translate-y-[3px] hover:shadow-[0_12px_24px_-14px_rgba(18,18,18,0.25)] active:scale-[0.97]"
    >
      <div
        className={`flex h-[30px] w-[30px] shrink-0 items-center justify-center rounded-full text-sm font-extrabold transition-[background-color,color,transform] duration-[400ms] ${
          filled ? "bg-brand text-white" : "bg-brand-soft text-brand-dark"
        } ${popped ? "scale-[1.18]" : "scale-100"}`}
      >
        {num}
      </div>
      <div className="text-base font-bold">{title}</div>
    </Reveal>
  );
}

export default function MainPage() {
  const navigate = useNavigate();
  const [stuck, setStuck] = useState(false);

  /**
   * 모바일에서 일정 이상 스크롤하면 하단 고정 CTA 노출
   */
  useEffect(() => {
    const onScroll = () =>
      setStuck(window.scrollY > 480 && window.innerWidth < 760);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", onScroll);
    return () => {
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", onScroll);
    };
  }, []);

  /**
   * 지금 시작하기 버튼 클릭 이벤트
   */
  const handleStartNow = () => {
    navigate("/new-settlement-room");
  };

  return (
    <div className="min-h-screen w-full overflow-x-hidden bg-paper font-[Pretendard_Variable,Pretendard,-apple-system,sans-serif] break-keep text-ink">
      {/* 헤더 */}
      <header
        className={`mx-auto flex max-w-[1200px] items-center justify-between gap-4 py-5 ${SECTION_X}`}
      >
        <div className="flex items-center gap-2.5 text-xl font-extrabold tracking-[-0.04em]">
          <span className="h-2.5 w-2.5 rotate-45 rounded-[3px] bg-brand" />
          splitlink
        </div>
        <button
          type="button"
          onClick={handleStartNow}
          className="cursor-pointer rounded-[10px] bg-ink px-[18px] py-2.5 text-sm font-bold whitespace-nowrap text-white transition-colors hover:bg-brand-dark"
        >
          {CTA_LABEL}
        </button>
      </header>

      {/* 히어로 */}
      <section
        className={`mx-auto grid max-w-[1200px] grid-cols-[repeat(auto-fit,minmax(min(100%,420px),1fr))] items-center gap-[clamp(40px,6vw,80px)] pt-[clamp(40px,8vw,96px)] pb-[clamp(56px,8vw,112px)] ${SECTION_X}`}
      >
        <div>
          <h1 className="mb-6 text-[length:clamp(40px,7.2vw,84px)] leading-[1.08] font-extrabold tracking-[-0.045em] text-balance">
            <Reveal as="span" className="block">
              누가 누구에게
            </Reveal>
            <Reveal as="span" delay={90} className="block">
              <span className="text-brand">얼마</span> 보내면
            </Reveal>
            <Reveal as="span" delay={180} className="block">
              되는지.
            </Reveal>
          </h1>
          <Reveal delay={270}>
            <p className="mb-8 max-w-[440px] text-[length:clamp(16px,1.6vw,19px)] leading-[1.65] text-pretty text-ink-soft">
              결제한 사람과 금액만 적으면 끝나요. 해외 결제는 그날 환율로
              바꿔드리고, 송금은 계좌 복사나 토스로.
            </p>
            <div className="flex flex-wrap items-center gap-x-5 gap-y-3">
              <button
                type="button"
                onClick={handleStartNow}
                className="cursor-pointer rounded-xl bg-brand px-[30px] py-[17px] text-base font-bold text-white transition-[transform,background-color] duration-200 hover:-translate-y-0.5 hover:bg-brand-dark active:scale-[0.96]"
              >
                {CTA_LABEL}
              </button>
              <span className="text-sm text-ink-soft">
                설치 없이 링크로 참여 · 무료
              </span>
            </div>
          </Reveal>
        </div>
        <Reveal>
          <HeroCard />
        </Reveal>
      </section>

      {/* 기능 */}
      <section
        className={`mx-auto max-w-[1200px] pb-[clamp(64px,9vw,120px)] ${SECTION_X}`}
      >
        <h2 className="mb-[clamp(28px,4vw,48px)] text-[length:clamp(26px,3.6vw,40px)] leading-[1.25] font-extrabold tracking-[-0.035em] text-balance">
          필요한 것만 넣었어요
        </h2>
        <div className="grid grid-cols-[repeat(auto-fit,minmax(min(100%,250px),1fr))] border-t-2 border-ink">
          {FEATURES.map((f, i) => (
            <FeatureItem key={f.num} {...f} index={i} />
          ))}
        </div>
      </section>

      {/* 이용방법 */}
      <section
        className={`mx-auto max-w-[1200px] pb-[clamp(64px,9vw,120px)] ${SECTION_X}`}
      >
        <h2 className="mb-[clamp(24px,3vw,36px)] text-[length:clamp(26px,3.6vw,40px)] font-extrabold tracking-[-0.035em]">
          이렇게 써요
        </h2>
        <div className="grid grid-cols-[repeat(auto-fit,minmax(min(100%,220px),1fr))] gap-2.5">
          {STEPS.map((title, i) => (
            <StepItem key={title} num={i + 1} title={title} />
          ))}
        </div>
      </section>

      {/* 하단 CTA */}
      <section className="mx-auto max-w-[1200px] px-[clamp(12px,3vw,56px)] pb-[clamp(40px,6vw,72px)]">
        <Reveal className="flex flex-wrap items-center justify-between gap-7 rounded-3xl bg-ink px-[clamp(24px,5vw,64px)] py-[clamp(40px,7vw,80px)] text-white">
          <h2 className="m-0 flex-[1_1_320px] text-[length:clamp(28px,4.4vw,48px)] leading-[1.2] font-extrabold tracking-[-0.04em]">
            다음 모임 정산,
            <br />
            오늘 끝내요.
          </h2>
          <button
            type="button"
            onClick={handleStartNow}
            className="cursor-pointer rounded-xl bg-brand px-[30px] py-[17px] text-base font-bold whitespace-nowrap text-white transition-colors hover:bg-white hover:text-ink"
          >
            {CTA_LABEL}
          </button>
        </Reveal>
      </section>

      {/* 푸터 */}
      <footer
        className={`mx-auto flex max-w-[1200px] flex-wrap justify-between gap-3 pt-6 pb-[110px] text-[13px] text-ink-soft ${SECTION_X}`}
      >
        <div>© 2026 splitlink</div>
        {/*<div className="flex gap-5">
          <a href="#" className="text-ink-soft">
            이용약관
          </a>
          <a href="#" className="text-ink-soft">
            개인정보처리방침
          </a>
        </div>*/}
      </footer>

      {/* 모바일 하단 고정 CTA */}
      <div
        className={`fixed right-3 bottom-[calc(12px+env(safe-area-inset-bottom))] left-3 z-50 flex items-center justify-between gap-3 rounded-2xl bg-ink py-2.5 pr-2.5 pl-[18px] text-white shadow-[0_18px_40px_-16px_rgba(18,18,18,0.5)] transition-[transform,opacity] duration-[450ms] ${
          stuck
            ? "pointer-events-auto translate-y-0 opacity-100"
            : "pointer-events-none translate-y-[160%] opacity-0"
        }`}
      >
        <span className="text-sm font-bold">오늘 모임, 바로 정산</span>
        <button
          type="button"
          onClick={handleStartNow}
          className="cursor-pointer rounded-[11px] bg-brand px-[18px] py-3 text-sm font-bold whitespace-nowrap text-white active:scale-95"
        >
          {CTA_LABEL}
        </button>
      </div>
    </div>
  );
}
