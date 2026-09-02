package com.splitlink.service;

import com.splitlink.common.jwt.JwtProvider;
import com.splitlink.dto.response.RoomDetailResponse;
import com.splitlink.dto.response.SelectMemberResponse;
import com.splitlink.mapper.MemberMapper;
import com.splitlink.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 방 참여 멤버의 활성화 및 인증 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final RoomMapper roomMapper;
    private final MemberMapper memberMapper;
    private final JwtProvider jwtProvider;

    /**
     * 방 참여자를 선택하여 활성화(is_active = true) 상태로 변경하고,
     * 방 접근 권한 인증을 위한 JWT 토큰을 발급합니다.
     */
    @Transactional
    public SelectMemberResponse selectMember(String slug, Long memberId) {
        log.info("MemberService:selectMember 진입 - slug: {}, memberId: {}", slug, memberId);

        // 방 정보 및 속한 멤버 목록 조회
        RoomDetailResponse roomDetail = roomMapper.findDetailBySlug(slug);
        if (roomDetail == null) {
            throw new IllegalArgumentException("해당 방이 존재하지 않습니다.");
        }

        // 해당 방에 속한 멤버인지 검증 및 객체 추출
        RoomDetailResponse.MemberResponse targetMember = roomDetail.getMembers().stream()
                .filter(m -> m.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 방에 속하지 않은 멤버입니다."));

        // 멤버 접속 상태 활성화
        int updatedRows = memberMapper.updateIsActive(memberId);
        if (updatedRows == 0) {
            throw new IllegalArgumentException("멤버 상태 변경에 실패했습니다. (존재하지 않는 멤버)");
        }

        String accessToken = jwtProvider.createToken(roomDetail.getRoomId(), memberId);

        return new SelectMemberResponse(
                accessToken,
                roomDetail.getRoomId(),
                roomDetail.getSlug(),
                roomDetail.getTitle(),
                targetMember.getMemberId(),
                targetMember.getName(),
                true
        );
    }
}
