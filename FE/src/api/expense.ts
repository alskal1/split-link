import axios from "axios";
import api from "./axios";
import type { ApiResponse } from "../types/roomType";
import type {
  ExpenseBatchCreateRequest,
  ExpenseDetailResponse,
  ExpenseFormInitResponse,
  ExpenseListResponse,
  ExpenseUpdateFormResponse,
  ExpenseUpdateRequest,
} from "../types/expenseType";

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
 * 지출 내역 목록 및 정산 요약 정보 조회
 * @param slug
 * @returns
 */
export const getExpenseList = async (
  slug: string,
): Promise<ExpenseListResponse | undefined> => {
  try {
    const { data } = await api.get<ApiResponse<ExpenseListResponse>>(
      `/rooms/${slug}/expenses`,
    );

    return data.data;
  } catch (error) {
    throw new Error(extractErrorMessage(error, "지출 목록을 불러오지 못했어요"));
  }
};

/**
 * 지출 입력 폼 초기 데이터 조회 (계좌 정보 및 방 멤버 목록)
 * @param slug
 * @returns
 */
export const getExpenseFormInit = async (
  slug: string,
): Promise<ExpenseFormInitResponse | undefined> => {
  try {
    const { data } = await api.get<ApiResponse<ExpenseFormInitResponse>>(
      `/rooms/${slug}/expenses/new`,
    );

    return data.data;
  } catch (error) {
    throw new Error(
      extractErrorMessage(error, "지출 입력 정보를 불러오지 못했어요"),
    );
  }
};

/**
 * 지출 내역 일괄 등록
 * @param slug
 * @param request
 */
export const createExpenses = async (
  slug: string,
  request: ExpenseBatchCreateRequest,
): Promise<void> => {
  try {
    await api.post<ApiResponse<void>>(`/rooms/${slug}/expenses`, request);
  } catch (error) {
    throw new Error(extractErrorMessage(error, "지출 등록에 실패했어요"));
  }
};

/**
 * 지출 내역 상세 조회
 * @param slug
 * @param expenseId
 * @returns
 */
export const getExpenseDetail = async (
  slug: string,
  expenseId: number,
): Promise<ExpenseDetailResponse | undefined> => {
  try {
    const { data } = await api.get<ApiResponse<ExpenseDetailResponse>>(
      `/rooms/${slug}/expenses/${expenseId}`,
    );

    return data.data;
  } catch (error) {
    throw new Error(
      extractErrorMessage(error, "지출 상세 정보를 불러오지 못했어요"),
    );
  }
};

/**
 * 지출 내역 수정 폼 조회
 * @param slug
 * @param expenseId
 * @returns
 */
export const getExpenseUpdateForm = async (
  slug: string,
  expenseId: number,
): Promise<ExpenseUpdateFormResponse | undefined> => {
  try {
    const { data } = await api.get<ApiResponse<ExpenseUpdateFormResponse>>(
      `/rooms/${slug}/expenses/${expenseId}/edit`,
    );

    return data.data;
  } catch (error) {
    throw new Error(
      extractErrorMessage(error, "지출 수정 정보를 불러오지 못했어요"),
    );
  }
};

/**
 * 지출 내역 수정
 * @param slug
 * @param expenseId
 * @param request
 */
export const updateExpense = async (
  slug: string,
  expenseId: number,
  request: ExpenseUpdateRequest,
): Promise<void> => {
  try {
    await api.put<ApiResponse<void>>(
      `/rooms/${slug}/expenses/${expenseId}`,
      request,
    );
  } catch (error) {
    throw new Error(extractErrorMessage(error, "지출 수정에 실패했어요"));
  }
};

/**
 * 지출 내역 삭제
 * @param slug
 * @param expenseId
 */
export const deleteExpense = async (
  slug: string,
  expenseId: number,
): Promise<void> => {
  try {
    await api.delete<ApiResponse<void>>(`/rooms/${slug}/expenses/${expenseId}`);
  } catch (error) {
    throw new Error(extractErrorMessage(error, "지출 삭제에 실패했어요"));
  }
};
