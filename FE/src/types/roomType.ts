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
export interface roomStorage extends RoomCreateResponse {
  expiresAt: number;
}

/**
 * 입장코드 입력 페이지용 방 요약 정보 응답
 * @param title 방 제목
 * @param memberCount 참여 멤버 수
 * @param memberNames 참여 멤버 이름 목록
 */
export interface RoomSummaryResponse {
  title: string;
  memberCount: number;
  memberNames: string[];
}

/**
 * 방 입장코드 검증 요청 바디
 * @param pin 입장코드
 */
export interface RoomAccessRequest {
  pin: string;
}

/**
 * 방 상세 정보 응답용 멤버 단건
 * @param memberId 멤버 PK
 * @param name 멤버 이름
 * @param isActive 방 최초 접속 여부
 */
export interface RoomMemberResponse {
  memberId: number;
  name: string;
  active: boolean;
}

/**
 * 방 상세 정보 및 멤버 목록 응답
 * @param roomId 방 PK
 * @param slug 슬러그
 * @param title 방 제목
 * @param baseCurrency 기준통화
 * @param members 등록된 멤버 상세 목록
 */
export interface RoomDetailResponse {
  roomId: number;
  slug: string;
  title: string;
  baseCurrency: string;
  members: RoomMemberResponse[];
}

/**
 * 멤버 선택(본인 지정) 응답
 * @param accessToken 선택한 멤버로 정산방에 접근하기 위한 인증 토큰
 * @param roomId 방 PK
 * @param slug 슬러그
 * @param title 방 제목
 * @param memberId 선택한 멤버 PK
 * @param memberName 선택한 멤버 이름
 * @param isActive 방 최초 접속 여부
 */
export interface SelectMemberResponse {
  accessToken: string;
  roomId: number;
  slug: string;
  title: string;
  memberId: number;
  memberName: string;
  isActive: boolean;
}

/**
 * 방 수정 요청 바디
 * 멤버는 이름 리스트로만 들어오며, 전체 삭제 후 재등록된다.
 * @param title 방 제목
 * @param baseCurrency 기준통화
 * @param pin 기존 입장코드 (권한 확인용)
 * @param newPin 새 입장코드 (선택, 미입력 시 유지)
 * @param memberNames 참여멤버
 */
export interface RoomUpdateRequest {
  title: string;
  baseCurrency: string;
  pin: string;
  newPin?: string;
  memberNames: string[];
}
