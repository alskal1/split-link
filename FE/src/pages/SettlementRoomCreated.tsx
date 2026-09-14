import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { clearCreatedRoom, loadCreatedRoom } from "../utils/createdRoomStorage";
import type { RoomStorage } from "../types/roomType";
import toast from "react-hot-toast";
import Button from "../components/Button";
import checkIcon from "../assets/check.svg";

export default function SettlementRoomCreated() {
  const navigate = useNavigate();
  const [room, setRoom] = useState<RoomStorage | null>(null);

  useEffect(() => {
    const stored = loadCreatedRoom();

    // 세션스토리지에 정보 없을 경우 메인 페이지로 이동
    if (!stored) {
      navigate("/", { replace: true });
      return;
    }

    setRoom(stored);
  }, [navigate]);

  if (!room) {
    return null;
  }

  // 방 url
  const inviteUrl = `${window.location.origin}/rooms/${room.slug}`;

  // 방 url 복사 버튼 클릭 이벤트
  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(inviteUrl);
      toast.success("초대 링크를 복사했어요");
    } catch {
      toast.error("복사에 실패했어요");
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
    <div className="flex flex-col items-center flex-1 text-center space-y-8 justify-center">
      <div className="w-20 h-20 flex items-center justify-center rounded-full badge-brand">
        <img src={checkIcon} alt="" className="w-9 h-9" />
      </div>

      <div className="flex flex-col space-y-2">
        <div className="text-xl font-bold">{room.title} 방이 만들어졌어요</div>
        <div className="explain-text">
          아래 링크를 공유하면 게스트가 앱 설치 없이 바로 참여할 수 있어요.
        </div>
      </div>

      <div className="w-full flex items-center justify-between bg-white rounded-[10px] px-4 py-3">
        <div className="flex-1 text-left text-[11pt] break-all">
          {inviteUrl}
        </div>
        <Button
          title="복사"
          bgColor="#000"
          textColor="#fff"
          className="rounded-[10px] shrink-0 ml-3"
          onClick={handleCopyLink}
        />
      </div>

      <div className="w-full flex items-center justify-center space-x-3 rounded-[10px] px-4 py-4 badge-brand">
        <span className="font-bold">입장코드</span>
        <span className="text-2xl font-bold tracking-[0.3em]">{room.pin}</span>
      </div>

      <div className="flex flex-wrap items-center justify-center gap-2">
        {room.memberNames.map((name) => (
          <div
            key={name}
            className="h-10 min-w-10 px-3 flex items-center justify-center rounded-full font-bold badge-brand ring-2 ring-[#fdf3eb]"
          >
            {name}
          </div>
        ))}
      </div>

      <Button
        title="정산방으로 이동"
        className="w-full rounded-2xl h-12.5"
        bgColor="#e85a48"
        textColor="#fff"
        onClick={handleEnterRoom}
      />
    </div>
  );
}
