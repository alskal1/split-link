package com.splitlink.mapper;

import com.splitlink.dto.response.ExpenseFormInitResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 멤버 데이터 영속성 처리를 담당하는 MyBatis Mapper 인터페이스
 */
@Mapper
public interface MemberMapper {

    /**
     * 특정 방(slug)의 전체 멤버 목록 조회
     */
    List<ExpenseFormInitResponse.MemberInfo> findRoomMembersBySlug(@Param("slug") String slug,
                                                                   @Param("memberId") Long memberId);

    /** 방 생성 시 초기 멤버 목록 일괄 저장 */
    void insertMembers(@Param("roomId") Long roomId,
                       @Param("memberNames") List<String> memberNames);

    /** 특정 방의 전체 멤버 일괄 삭제 */
    int deleteMembersByRoomId(Long roomId);

    /** 멤버 접속 선택 시 활성화 상태(is_active = true) 변경 */
    int updateIsActive(Long memberId);

    /** 멤버의 계좌 정보(은행명, 계좌번호) 조회 */
    Optional<ExpenseFormInitResponse.AccountInfo> findAccountInfoByMemberId(Long memberId);

    /** 결제자의 정산 계좌 정보(은행명, 계좌번호) 최신화 */
    void updateAccountInfo(@Param("memberId") Long memberId,
                          @Param("bankName") String bankName,
                          @Param("accountNumber") String accountNumber);

    /** 지정한 방(roomId)에 해당 멤버들(memberIds)이 모두 속해 있는지 개수 조회 */
    int countMembersByRoomIdAndMemberIds(@Param("roomId") Long roomId,
                                         @Param("memberIds") List<Long> memberIds);
}
