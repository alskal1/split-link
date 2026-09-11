import api from "./axios";
import type {
  ApiResponse,
  RoomAccessRequest,
  RoomCreateRequest,
  RoomCreateResponse,
  RoomDetailResponse,
  RoomSummaryResponse,
  RoomUpdateRequest,
  SelectMemberResponse,
} from "../types/roomType";

/**
 * 정산방 생성
 * @param request
 * @returns
 */
export const createSettlementRoom = async (
  request: RoomCreateRequest,
): Promise<RoomCreateResponse | undefined> => {
  try {
    const { data } = await api.post<ApiResponse<RoomCreateResponse>>(
      "/rooms",
      request,
    );

    return data.data;
  } catch (error) {
    throw new Error("방 생성에 실패했어요");
  }
};

/**
 * 입장코드 입력 페이지용 방 요약 정보 조회
 * @param slug
 * @returns
 */
export const getRoomSummary = async (
  slug: string,
): Promise<RoomSummaryResponse | undefined> => {
  try {
    const { data } = await api.get<ApiResponse<RoomSummaryResponse>>(
      `/rooms/${slug}/summary`,
    );

    return data.data;
  } catch (error) {
    throw new Error("방 정보를 불러오지 못했어요");
  }
};

/**
 * 입장코드를 검증하고, 일치할 경우 방 상세 정보 조회
 * @param slug
 * @param request
 * @returns
 */
export const accessRoom = async (
  slug: string,
  request: RoomAccessRequest,
): Promise<RoomDetailResponse | undefined> => {
  try {
    const { data } = await api.post<ApiResponse<RoomDetailResponse>>(
      `/rooms/${slug}/access`,
      request,
    );

    return data.data;
  } catch (error) {
    throw new Error("입장코드가 올바르지 않아요");
  }
};

/**
 * 참여 멤버 중 본인을 선택하여 방 접근 인증 토큰 발급
 * @param slug
 * @param memberId
 * @returns
 */
export const selectMember = async (
  slug: string,
  memberId: number,
): Promise<SelectMemberResponse | undefined> => {
  try {
    const { data } = await api.post<ApiResponse<SelectMemberResponse>>(
      `/rooms/${slug}/members/${memberId}/select`,
    );

    return data.data;
  } catch (error) {
    throw new Error("멤버 선택에 실패했어요");
  }
};

/**
 * 방 수정 (제목, 기준통화, 입장코드, 멤버)
 * @param slug
 * @param request
 * @returns
 */
export const updateRoom = async (
  slug: string,
  request: RoomUpdateRequest,
): Promise<RoomDetailResponse | undefined> => {
  try {
    const { data } = await api.put<ApiResponse<RoomDetailResponse>>(
      `/rooms/${slug}`,
      request,
    );

    return data.data;
  } catch (error) {
    throw new Error("방 수정에 실패했어요");
  }
};

/**
 * 방 삭제 (기존 입장코드 검증 후 방 및 관련 데이터 일괄 삭제)
 * @param slug
 * @param request
 */
export const deleteRoom = async (
  slug: string,
  request: RoomAccessRequest,
): Promise<void> => {
  try {
    await api.delete<ApiResponse<void>>(`/rooms/${slug}`, {
      data: request,
    });
  } catch (error) {
    throw new Error("방 삭제에 실패했어요");
  }
};
