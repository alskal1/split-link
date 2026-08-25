package com.splitlink.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 전역 API 공통 응답 규격 Wrapper 클래스
 *
 * @param <T> 응답 데이터(data)의 데이터 타입
 */
@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    /** 성공 여부 (true / false) */
    private boolean success;

    /** HTTP 상태 코드 (200, 201, 400, 500) */
    private int status;

    /** 응답 메시지 ("SUCCESS" 또는 에러 내용) */
    private String message;

    /** 실제 응답 데이터 (성공 시에만 포함, 실패 시 null) */
    private T data;

    /**
     * 성공 응답 생성 (기본 HTTP 200 OK)
     *
     * @param data 클라이언트에 전달할 실제 데이터
     * @return 200 OK 성공 ApiResponse 객체
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, "SUCCESS", data);
    }

    /**
     * 커스텀 상태 코드를 가지는 성공 응답 생성 (예: 201 Created)
     *
     * @param status HTTP 상태 코드
     * @param data 클라이언트에 전달할 실제 데이터
     * @return 커스텀 상태 코드를 가지는 성공 ApiResponse 객체
     */
    public static <T> ApiResponse<T> success(int status, T data) {
        return new ApiResponse<>(true, status, "SUCCESS", data);
    }

    /**
     * 실패(에러) 응답 생성
     *
     * @param status HTTP 상태 코드 (예: 400, 404, 500)
     * @param message 에러 메세지
     * @return 에러 ApiResponse 객체
     */
    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(false, status, message, null);
    }

    /**
     * 상세 에러 데이터(예: Validation 필드 에러 목록)를 포함하는 실패 응답 생성
     *
     * @param status HTTP 상태 코드
     * @param message 에러 메세지
     * @param data 상세 에러 데이터 (예: CustomFieldError 목록)
     * @return 상세 에러 데이터를 포함하는 ApiResponse 객체
     */
    public static <T> ApiResponse<T> error(int status, String message, T data) {
        return new ApiResponse<>(false, status, message, data);
    }
}
