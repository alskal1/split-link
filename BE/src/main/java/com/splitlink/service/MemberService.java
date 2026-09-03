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

        // 1. 방 및 멤버 소속 검증
        RoomDetailResponse roomDetail = roomMapper.findDetailBySlug(slug);
        if (roomDetail == null) {
            throw new IllegalArgumentException("해당 방이 없습니다.");
        }

        RoomDetailResponse.MemberResponse targetMember = roomDetail.getMembers().stream()
                .filter(m -> m.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 방에 속하지 않은 멤버입니다."));

        if (targetMember.isActive()) {
            throw new IllegalArgumentException("이미 선택되어 접속 중인 멤버입니다.");
        }

        // 2. 상태 변경 실행 (앞에서 검증했으므로 검사 생략)
        memberMapper.updateIsActive(memberId);

        // 3. 토큰 발급 및 리턴
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
