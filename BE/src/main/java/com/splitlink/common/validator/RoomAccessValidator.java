package com.splitlink.common.validator;

import com.splitlink.mapper.MemberMapper;
import com.splitlink.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoomAccessValidator {

    private final RoomMapper roomMapper;
    private final MemberMapper memberMapper;

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

    /**
     * 전달받은 memberIds가 모두 해당 roomId에 소속되어 있는지 검증
     */
    public void validateMembersInRoom(Long roomId, List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        // 중복 제거 후 실제 소속 멤버 수 조회
        List<Long> distinctMemberIds = memberIds.stream().distinct().toList();
        int count = memberMapper.countMembersByRoomIdAndMemberIds(roomId, distinctMemberIds);

        // 요청한 멤버 수와 DB에서 조회된 멤버 수가 다르면 방 소속이 아닌 멤버가 포함된 것임
        if (count != distinctMemberIds.size()) {
            throw new IllegalArgumentException("해당 방에 속하지 않은 참여자가 포함되어 있습니다.");
        }
    }
}
