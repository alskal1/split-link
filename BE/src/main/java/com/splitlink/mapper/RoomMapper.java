package com.splitlink.mapper;

import com.splitlink.dto.response.RoomDetailResponse;
import com.splitlink.dto.response.RoomSummaryResponse;
import com.splitlink.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 방 및 멤버 데이터에 접근하는 MyBatis Mapper 인터페이스
 */
@Mapper
public interface RoomMapper {

    /** 방 기본 정보 저장 */
    void insertRoom(Room room);

    /** 멤버 목록 일괄 저장 */
    void insertMembers(@Param("roomId") Long roomId, @Param("memberNames") List<String> memberNames);

    /** slug 기준 방 요약 정보 조회 */
    RoomSummaryResponse findSummaryBySlug(String slug);

    /** slug 기준 방 상세 정보 조회 */
    RoomDetailResponse findDetailBySlug(String slug);

    /** slug 기준 정답 입장코드 조회 */
    String findPinBySlug(String slug);
}
