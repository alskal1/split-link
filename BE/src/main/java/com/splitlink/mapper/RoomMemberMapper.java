package com.splitlink.mapper;

import com.splitlink.dto.response.ExpenseFormInitResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoomMemberMapper {

    /**
     * 특정 방(slug)의 전체 멤버 목록 조회
     */
    List<ExpenseFormInitResponse.MemberInfo> findRoomMembersBySlug(@Param("slug") String slug,
                                                                   @Param("memberId") Long memberId);
}
