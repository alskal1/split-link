package com.splitlink.common.validator;

import com.splitlink.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomAccessValidator {

    private final RoomMapper roomMapper;

    /**
     * 방 존재 여부 및 해당 유저의 방 참여 권한을 검증하고 roomId를 반환
     */
    public Long validateAndGetRoomId(String slug, Long memberId) {
        Long roomId = roomMapper.findRoomIdBySlugAndMemberId(slug, memberId);
        if (roomId == null) {
            throw new IllegalArgumentException("해당 방이 존재하지 않거나, 해당 방에 접근 권한이 없습니다.");
        }
        return roomId;
    }
}
