package com.splitlink.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 멤버 데이터에 접근하는 MyBatis Mapper 인터페이스
 */
@Mapper
public interface MemberMapper {

    /** 멤버 목록 일괄 저장 */
    void insertMembers(@Param("roomId") Long roomId, @Param("memberNames") List<String> memberNames);

    /** 해당 roomId의 멤버 전체 삭제 */
    int deleteMembersByRoomId(Long roomId);
}
