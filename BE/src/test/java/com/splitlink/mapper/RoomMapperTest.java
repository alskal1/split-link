package com.splitlink.mapper;

import com.splitlink.entity.Room;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("RoomMapper 쿼리 검증 테스트")
class RoomMapperTest {

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private MemberMapper memberMapper;

    @Test
    @DisplayName("findRoomIdBySlugAndPin: 정상 조회 시 roomId를 반환하고, 타 방 멤버나 틀린 PIN은 null을 반환한다.")
    void findRoomIdBySlugAndPinTest() {
        // given: 방 A와 방 B 생성 및 각각 멤버 등록
        String slugA = UUID.randomUUID().toString();
        Room roomA = Room.builder()
                .title("방 A")
                .baseCurrency("KRW")
                .pin("1234")
                .slug(slugA)
                .build();
        roomMapper.insertRoom(roomA);
        memberMapper.insertMembers(roomA.getRoomId(), List.of("멤버A"));

        String slugB = UUID.randomUUID().toString();
        Room roomB = Room.builder()
                .title("방 B")
                .baseCurrency("KRW")
                .pin("5678")
                .slug(slugB)
                .build();
        roomMapper.insertRoom(roomB);
        memberMapper.insertMembers(roomB.getRoomId(), List.of("멤버B"));

        Long memberAId = memberMapper.findMemberUsageStatusesByRoomId(roomA.getRoomId()).get(0).getMemberId();
        Long memberBId = memberMapper.findMemberUsageStatusesByRoomId(roomB.getRoomId()).get(0).getMemberId();

        // when & then
        // 1. 정상이면 roomId가 나오는지
        Long validRoomId = roomMapper.findRoomIdBySlugAndPin(slugA, memberAId, "1234");
        assertThat(validRoomId).isEqualTo(roomA.getRoomId());

        // 2. 다른 방 멤버로 조회하면 null이 나오는지
        Long otherMemberResult = roomMapper.findRoomIdBySlugAndPin(slugA, memberBId, "1234");
        assertThat(otherMemberResult).isNull();

        // 3. PIN이 틀리면 null이 나오는지
        Long wrongPinResult = roomMapper.findRoomIdBySlugAndPin(slugA, memberAId, "9999");
        assertThat(wrongPinResult).isNull();
    }
}