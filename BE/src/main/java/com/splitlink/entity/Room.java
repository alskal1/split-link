package com.splitlink.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 방(rooms) 테이블과 매핑되는 Entity 클래스
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    private Long roomId;
    private String slug;
    private String title;
    private String baseCurrency;
    private String pin;
    private boolean isLocked;
    private boolean isClosed;
    private LocalDateTime createdAt;
}
