package com.splitlink.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success; // 성공 여부 (true / false)
    private int status;      // HTTP 상태 코드 (200, 400, 500 등)
    private String message;  // 응답 메시지 ("SUCCESS" 또는 에러 내용)
    private T data;          // 실제 데이터 (성공 시에만 담김)

    // 성공했을 때 쓰는 정적 팩토리 메서드
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, "SUCCESS", data);
    }

    // 실패(에러)했을 때 쓰는 정적 팩토리 메서드
    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(false, status, message, null);
    }
}
