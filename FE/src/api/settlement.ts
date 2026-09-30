import axios from "axios";
import api from "./axios";
import type { ApiResponse } from "../types/roomType";
import type { RoomMySettlementResponse } from "../types/settlementType";

/**
 * 에러에서 서버가 내려준 메시지를 꺼내고, 없으면 기본 메시지로 대체
 * @param error catch로 잡힌 에러
 * @param fallback 서버 메시지가 없을 때 사용할 기본 메시지
 */
const extractErrorMessage = (error: unknown, fallback: string): string => {
  if (axios.isAxiosError(error)) {
    const message = (error.response?.data as ApiResponse<unknown> | undefined)
      ?.message;

    if (message) {
      return message;
    }
  }

  return fallback;
};

/**
 * 정산 실행 (방 잠금 + 최소 송금 알고리즘 계산 + 정산 내역 저장)
 * @param slug
 */
export const executeSettlement = async (slug: string): Promise<void> => {
  try {
    await api.post<ApiResponse<void>>(`/rooms/${slug}/settlements`);
  } catch (error) {
    throw new Error(extractErrorMessage(error, "정산 실행에 실패했어요"));
  }
};

/**
 * 내 정산 내역 조회 (보낼 금액 / 받을 금액 목록)
 * @param slug
 * @returns
 */
export const getMySettlement = async (
  slug: string,
): Promise<RoomMySettlementResponse | undefined> => {
  try {
    const { data } = await api.get<ApiResponse<RoomMySettlementResponse>>(
      `/rooms/${slug}/settlements/me`,
    );

    return data.data;
  } catch (error) {
    throw new Error(
      extractErrorMessage(error, "정산 내역을 불러오지 못했어요"),
    );
  }
};

/**
 * 개별 송금 완료 상태 변경
 * @param slug
 * @param settlementId 정산 내역 PK
 * @param isDone 완료 여부
 */
export const updateRemittanceStatus = async (
  slug: string,
  settlementId: number,
  isDone: boolean,
): Promise<void> => {
  try {
    await api.patch<ApiResponse<void>>(
      `/rooms/${slug}/settlements/${settlementId}`,
      { isDone },
    );
  } catch (error) {
    throw new Error(
      extractErrorMessage(error, "송금 완료 상태 변경에 실패했어요"),
    );
  }
};
