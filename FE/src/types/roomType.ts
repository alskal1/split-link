/**
 * 서버 공통 응답 포맷
 * @param success 성공 여부
 * @param status 상태코드
 * @param message 메세지
 * @param data 실제 데이터
 */
export interface ApiResponse<T> {
  success: boolean;
  status: number;
  message: string;
  data: T;
}

/**
 * 정산방 생성 요청 바디
 * @param title 방 제목
 * @param baseCurrency 기준통화
 * @param pin 입장코드
 * @param memberNames 참여멤버
 */
export interface RoomCreateRequest {
  title: string;
  baseCurrency: string;
  pin: string;
  memberNames: string[];
}

// 정산방 생성 응답. slug는 방 접근용 URL 식별자
/**
 * 정산방 생성 응답
 * @param slug 슬러그
 * @param title 방 제목
 * @param baseCurrency 기준통화
 * @param pin 입장코드
 * @param memberNames 참여멤버
 */
export interface RoomCreateResponse {
  slug: string;
  title: string;
  baseCurrency: string;
  pin: string;
  memberNames: string[];
}

/**
 * 정산방 생성 완료 화면용 세션 저장 데이터
 * sessionStorage에 저장되어 탭을 닫으면 사라지며, expiresAt으로 노출 시간을 추가로 제한한다.
 * @param expiresAt 만료 시각(epoch ms)
 */
export interface CreatedRoomStorage extends RoomCreateResponse {
  expiresAt: number;
}
