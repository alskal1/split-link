import { MEMBER_ACCESS_STORAGE_KEY } from "../constants/room";
import type { SelectMemberResponse } from "../types/roomType";

/**
 * 방(slug)별로 분리된 저장 키 생성 (여러 정산방 동시 참여 시 충돌 방지)
 * @param slug 방 슬러그
 */
const getStorageKey = (slug: string): string =>
  `${MEMBER_ACCESS_STORAGE_KEY}:${slug}`;

/**
 * 멤버 선택(본인 지정) 완료 정보를 로컬에 저장
 * @param data 멤버 선택 응답
 */
export const saveMemberAccess = (data: SelectMemberResponse): void => {
  try {
    localStorage.setItem(getStorageKey(data.slug), JSON.stringify(data));
  } catch (error) {
    console.error(error);
  }
};

/**
 * 저장된 멤버 선택 정보 조회
 * @param slug 현재 방 슬러그 (다른 방의 저장 정보 오사용 방지)
 * @returns 유효한 저장 데이터 또는 null
 */
export const loadMemberAccess = (
  slug: string,
): SelectMemberResponse | null => {
  let raw: string | null = null;
  try {
    raw = localStorage.getItem(getStorageKey(slug));
  } catch (error) {
    console.error(error);
    return null;
  }

  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as Partial<SelectMemberResponse>;

    if (
      typeof parsed.accessToken !== "string" ||
      typeof parsed.slug !== "string" ||
      parsed.slug !== slug ||
      typeof parsed.memberId !== "number"
    ) {
      clearMemberAccess(slug);
      return null;
    }

    return parsed as SelectMemberResponse;
  } catch (error) {
    console.error(error);
    clearMemberAccess(slug);
    return null;
  }
};

/**
 * 저장된 멤버 선택 정보 제거
 * @param slug 현재 방 슬러그
 */
export const clearMemberAccess = (slug: string): void => {
  try {
    localStorage.removeItem(getStorageKey(slug));
  } catch (error) {
    console.error(error);
  }
};
