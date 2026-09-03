package com.splitlink.controller;

import com.splitlink.common.api.ApiResponse;
import com.splitlink.dto.response.SelectMemberResponse;
import com.splitlink.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 방 멤버 관련 API 요청을 처리하는 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/rooms/{slug}/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 방 참여자 선택 및 접속 활성화 (JWT 토큰 발급)
     */
    @PostMapping("/{memberId}/select")
    public ResponseEntity<ApiResponse<SelectMemberResponse>> selectMember(
            @PathVariable String slug,
            @PathVariable Long memberId) {
        log.info("MemberController:selectMember 진입 - slug: {}, memberId: {}", slug, memberId);

        SelectMemberResponse response = memberService.selectMember(slug, memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
