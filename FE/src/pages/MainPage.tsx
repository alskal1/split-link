import { useRef } from "react";
import { useNavigate } from "react-router-dom";
import Button from "../components/Button";
import MainCard from "../components/MainCard";
import mockup from "../assets/hero-mockup.png";

export default function MainPage() {
  const featureRef = useRef<HTMLDivElement>(null);
  const usageRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  /**
   * 메뉴바에서 메뉴 클릭 시 스크롤 이동
   */
  const scrollTo = (ref: React.RefObject<HTMLDivElement>) => {
    ref.current?.scrollIntoView({ behavior: "smooth" });
  };

  /**
   * 지금 시작하기 버튼 클릭 이벤트
   */
  const onClickStartBtn = () => {
    navigate("/new-settlement-room");
  };

  return (
    <div>
      {/* 헤더 */}
      <div className="flex justify-between items-center mb-20">
        <div className="font-bold text-3xl">SplitLink</div>
        <div className="flex items-center space-x-10">
          <Button title="기능" onClick={() => scrollTo(featureRef)} />
          <Button title="이용방법" onClick={() => scrollTo(usageRef)} />
          <Button
            title="지금 시작하기"
            textColor="#fff"
            bgColor="#E85A48"
            className="rounded-3xl"
            onClick={onClickStartBtn}
          />
        </div>
      </div>
      <div className="flex flex-col space-y-20">
        <div className="flex w-full">
          <div className="flex flex-col w-1/2 space-y-5">
            <div className="bg-[#F4E8BB] text-[#996700] text-[10pt] font-bold w-fit rounded-2xl px-2 py-1">
              여행부터 회식까지, 정산 한 번에
            </div>
            <div className="text-5xl font-bold leading-14">
              더치페이 계산은
              <br />
              그만, SplitLink가
              <br />
              대신 해드려요
            </div>
            <div className="text-[12pt] text-[#281C18] leading-6">
              누가 얼마 냈는지만 입력하면 얼마씩 보내야 하는지 자동으로
              계산하고,
              <br />
              계좌번호 복사부터 토스연결까지 바로 할 수 있어요.
            </div>
            <Button
              title="지금 시작하기"
              bgColor="#000"
              textColor="#fff"
              className="w-fit rounded-2xl mt-5"
              onClick={onClickStartBtn}
            />
          </div>
          <div className="w-1/2">
            <img src={mockup} alt="" className="h-[350px]" />
          </div>
        </div>
        {/* 기능 */}
        <div className="flex flex-col items-center" ref={featureRef}>
          <div className="text-[10pt] text-[#C53829] font-bold mb-2">기능</div>
          <div className="text-3xl font-bold mb-12">
            정산부터 송금까지, 한 화면에서
          </div>
          <div className="w-full grid grid-cols-2 gap-6">
            <MainCard
              title="정산 자동 계산"
              icon="calculator"
              iconBgColor="#FFDCD4"
              pageImg=""
              contents="누가 얼마 냈는지만 입력하면 1/N, 항목별, 인당 다르게까지 원하는
              방식으로 자동 계산돼요. 복잡한 엑셀은 이제 필요 없어요."
            />
            <MainCard
              title="간편 송금"
              icon="arrow-right-left"
              iconBgColor="#F4E8BB"
              pageImg=""
              contents="계산이 끝나면 계좌번호를 복사해서 직접 보내거나, 토스로
              바로 넘겨서 송금할 수 있어요."
            />
            <MainCard
              title="해외여행 환율 자동 반영"
              icon="globe"
              iconBgColor="#FFC4B9"
              pageImg=""
              contents="결제일자와 현지 통화만 입력하면 그날 환율을 반영해서 원화 기준으로
              정산해드려요."
            />
            <MainCard
              title="링크로 초대"
              icon="link"
              iconBgColor="#F4E8BB"
              pageImg=""
              contents="게스트는 앱을 따로 설치할 필요 없이, 초대 링크만 누르면 바로
              모임에 참여하고 정산 내역을 확인할 수 있어요."
            />
          </div>
        </div>
        {/* 이용방법 */}
        <div
          className="flex flex-col items-center rounded-3xl bg-white py-10"
          ref={usageRef}
        >
          <div className="text-[10pt] text-[#C53829] font-bold mb-2">
            이용방법
          </div>
          <div className="text-3xl font-bold">이렇게 간단해요</div>
          <div className="grid grid-cols-4 gap-4 my-20 px-16">
            <div className="flex flex-col space-y-3">
              <div className="text-3xl font-bold">
                <span className="text-[#FFDCD4]">0</span>
                <span className="text-[#FFC4B9]">1</span>
              </div>
              <div className="font-bold">모임 만들기</div>
              <div className="text-[10pt] text-[#281C18]">
                모임 이름을 정하고 정산할 멤버를 초대해요.
              </div>
            </div>
            <div className="flex flex-col space-y-3">
              <div className="text-3xl font-bold">
                <span className="text-[#FFDCD4]">0</span>
                <span className="text-[#FFC4B9]">2</span>
              </div>
              <div className="font-bold">지출 기록하기</div>
              <div className="text-[10pt] text-[#281C18]">
                누가 얼마 결제했는지 하나씩 입력해요.
              </div>
            </div>{" "}
            <div className="flex flex-col space-y-3">
              <div className="text-3xl font-bold">
                <span className="text-[#FFDCD4]">0</span>
                <span className="text-[#FFC4B9]">3</span>
              </div>
              <div className="font-bold">자동 계산</div>
              <div className="text-[10pt] text-[#281C18]">
                누가 얼마 내고 받아야 하는지 알아서 계산해요.
              </div>
            </div>{" "}
            <div className="flex flex-col space-y-3">
              <div className="text-3xl font-bold">
                <span className="text-[#FFDCD4]">0</span>
                <span className="text-[#FFC4B9]">4</span>
              </div>
              <div className="font-bold">바로 송금</div>
              <div className="text-[10pt] text-[#281C18]">
                계좌번호를 복사하거나 토스로 넘겨서 송금을 마무리해요.
              </div>
            </div>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center p-20">
          <div className="text-center text-3xl font-bold">
            다음 모임 정산은
            <br />
            미루지 마세요
          </div>
          <div className="text-[#281C18] mt-2 mb-5">
            지금 시작하고 첫 정산을 3분 안에 끝내보세요
          </div>
          <Button
            title="지금 시작하기"
            textColor="#fff"
            bgColor="#E85A48"
            className="rounded-2xl h-12.5"
            onClick={onClickStartBtn}
          />
        </div>
      </div>
    </div>
  );
}
