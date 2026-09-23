package com.splitlink.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 송금 여부 상태 변경을 위한 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RemittanceStatusUpdateRequest {

    @JsonProperty("isDone")
    @NotNull(message = "송금 여부 상태값(isDone)은 필수입니다.")
    private Boolean isDone;
}
