package com.splitlink.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 멤버 데이터 영속성 처리를 담당하는 MyBatis Mapper 인터페이스
 */
@Mapper
public interface MemberMapper {

    /** 방 생성 시 초기 멤버 목록 일괄 저장 */
    void insertMembers(@Param("roomId") Long roomId,
                       @Param("memberNames") List<String> memberNames);

    /** 특정 방의 전체 멤버 일괄 삭제 */
    int deleteMembersByRoomId(Long roomId);

    /** 멤버 접속 선택 시 활성화 상태(is_active = true) 변경 */
    int updateIsActive(Long memberId);
}
