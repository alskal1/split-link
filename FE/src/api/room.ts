import api from "./axios";
import type {
  ApiResponse,
  RoomCreateRequest,
  RoomCreateResponse,
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
