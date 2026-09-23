package com.splitlink.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.splitlink.dto.response.RoomDetailResponse;
import com.splitlink.dto.response.RoomSummaryResponse;
import com.splitlink.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    /** slug, memberId 기준 방 PK 조회 */
    Long findRoomIdBySlugAndMemberId(@Param("slug") String slug,
                                     @Param("memberId") Long memberId);

    /** slug 기준 방 요약 정보 조회 */
    RoomSummaryResponse findSummaryBySlug(String slug);

    /** slug 기준 방 상세 정보 및 멤버 목록 조회 */
    RoomDetailResponse findDetailBySlug(String slug);

    /** slug 기준 정답 입장코드(PIN) 조회 */
    String findPinBySlug(String slug);

    /** slug, 입장코드(PIN) 검사 후 방 PK 조회 */
    Long findRoomIdBySlugAndPin(@Param("slug") String slug,
                                 @Param("pin") String pin);

    /** slug, memberId 기준 방 존재 여부 및 접근 권한 검증 후 Room 엔티티 조회 */
    Room findRoomBySlugAndMemberId(@Param("slug") String slug,
                                   @Param("memberId") Long memberId);

    /** 방 단위 비관적 락(줄서기) 조회 */
    Room findRoomByIdForUpdate(@Param("roomId") Long roomId);

    /** 방 기본 정보 수정 */
    int updateRoom(@Param("slug") String slug,
                   @Param("title") String title,
                   @Param("baseCurrency") String baseCurrency,
                   @Param("targetPin") String targetPin);

    /** 방의 지출 입력 마감(is_locked) 상태 변경 */
    int updateRoomLockStatus(@Param("roomId") Long roomId,
                             @Param("status") boolean status);

    /** slug 기준 방 정산 완료 상태 변경 */
    int updateIsClosedByRoomId(@Param("roomId") Long roomId,
                             @Param("isClosed") boolean isClosed);

    /** slug 기준 방 정산 완료 여부 조회 */
    Boolean findIsClosedBySlug(String slug);

    /** slug 기준 방 및 관련 데이터 삭제 */
    int deleteRoom(String slug);

    /** 지출 목록 상단 헤더 정보 조회 (방 제목, 사용자 이름, 정산 마감 여부) */
    ExpenseListHeaderData getExpenseListHeaderData(@Param("roomId") Long roomId,
                                                   @Param("memberId") Long memberId);

    /** slug 기준 지출 입력 마감 및 정산 완료 상태 조회 */
    RoomStatus findRoomStatusBySlug(@Param("slug") String slug);

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ExpenseListHeaderData {
        private String roomTitle;
        private String memberName;
        private boolean isLocked;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class RoomStatus {
        private boolean isLocked;
        private boolean isClosed;
    }
}
