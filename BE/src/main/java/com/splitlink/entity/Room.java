package com.splitlink.entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    private Long roomId;
    private String slug;
    private String title;
    private String baseCurrency;
    private String pin;
    private Boolean isClosed;
    private LocalDateTime createdAt;
}
