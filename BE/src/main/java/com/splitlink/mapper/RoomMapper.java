package com.splitlink.mapper;

import com.splitlink.dto.response.RoomDetailResponse;
import com.splitlink.dto.response.RoomSummaryResponse;
import com.splitlink.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 방 데이터에 접근하는 MyBatis Mapper 인터페이스
 */
@Mapper
public interface RoomMapper {

    /** 방 기본 정보 저장 */
    void insertRoom(Room room);

    /** slug 기준 방 PK 조회 */
    Long findRoomIdBySlug(String slug);

    /** slug 기준 방 요약 정보 조회 */
    RoomSummaryResponse findSummaryBySlug(String slug);

    /** slug 기준 방 상세 정보 및 멤버 목록 조회 */
    RoomDetailResponse findDetailBySlug(String slug);

    /** slug 기준 정답 입장코드(PIN) 조회 */
    String findPinBySlug(String slug);

    /** 방 기본 정보 수정 */
    int updateRoom(@Param("slug") String slug,
                   @Param("title") String title,
                   @Param("baseCurrency") String baseCurrency,
                   @Param("targetPin") String targetPin);

    /** slug 기준 방 정산 완료 상태 변경 */
    int updateIsClosedBySlug(@Param("slug") String slug,
                             @Param("isClosed") Boolean isClosed);

    /** slug 기준 방 정산 완료 여부 조회 */
    Boolean findIsClosedBySlug(String slug);

    /** slug 기준 방 및 관련 데이터 삭제 */
    int deleteRoom(String slug);
}
