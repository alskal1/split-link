import {
  CREATED_ROOM_STORAGE_KEY,
  CREATED_ROOM_TTL_MS,
} from "../constants/room";
import type { CreatedRoomStorage, RoomCreateResponse } from "../types/roomType";

/**
 * 정산방 생성 완료 정보를 세션에 저장
 * @param data 정산방 생성 응답
 */
export const saveCreatedRoomStorage = (data: RoomCreateResponse): void => {
  const payload: CreatedRoomStorage = {
    slug: data.slug,
    title: data.title,
    baseCurrency: data.baseCurrency,
    pin: data.pin,
    memberNames: data.memberNames,
    expiresAt: Date.now() + CREATED_ROOM_TTL_MS,
  };

  try {
    sessionStorage.setItem(CREATED_ROOM_STORAGE_KEY, JSON.stringify(payload));
  } catch (error) {
    console.error(error);
  }
};

/**
 * 저장된 정산방 생성 완료 정보
 * @returns 유효한 저장 데이터 또는 null
 */
export const loadCreatedRoom = (): CreatedRoomStorage | null => {
  // sessionStorage에서 읽어온 원본 문자열을 담을 변수
  let raw: string | null = null;
  try {
    // 세션스토리지 정보 조회
    raw = sessionStorage.getItem(CREATED_ROOM_STORAGE_KEY);
  } catch (error) {
    console.error(error);
    return null;
  }

  // 저장된 값 없으면 더 진행하지 않음
  if (!raw) {
    return null;
  }

  try {
    // 문자열을 객체로 파싱
    const parsed = JSON.parse(raw) as Partial<CreatedRoomStorage>;

    if (
      Array.isArray(parsed.memberNames) ||
      typeof parsed.title === "string" ||
      typeof parsed.baseCurrency === "string" ||
      typeof parsed.slug !== "string" ||
      typeof parsed.pin !== "string" ||
      typeof parsed.expiresAt !== "number" ||
      parsed.expiresAt <= Date.now()
    ) {
      // 유효하지 않은 데이터는 제거
      clearCreatedRoom();
      return null;
    }

    // 검증을 통과한 데이터를 CreatedRoomStorage로 반환
    return parsed as CreatedRoomStorage;
  } catch (error) {
    console.error(error);
    clearCreatedRoom();
    return null;
  }
};

/**
 * 저장된 정산방 생성 완료 정보 제거
 */
export const clearCreatedRoom = (): void => {
  try {
    sessionStorage.removeItem(CREATED_ROOM_STORAGE_KEY);
  } catch (error) {
    console.error(error);
  }
};
