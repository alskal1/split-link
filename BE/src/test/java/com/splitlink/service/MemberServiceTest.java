package com.splitlink.service;

import com.splitlink.dto.request.RoomCreateRequest;
import com.splitlink.dto.response.RoomCreateResponse;
import com.splitlink.dto.response.RoomDetailResponse;
import com.splitlink.dto.response.SelectMemberResponse;
import com.splitlink.mapper.RoomMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomMapper roomMapper;

    @Test
    @DisplayName("멤버 선택 성공 - is_active가 true로 변경되고, JWT 토큰이 발급된다.")
    void selectMemberSuccessTest() {
        // given
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
                .title("멤버 테스트방")
                .baseCurrency("KRW")
                .pin("1234")
                .memberNames(List.of("철수", "유리"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);
        String slug = createResponse.getSlug();

        RoomDetailResponse roomDetail = roomMapper.findDetailBySlug(slug);
        Long targetMemberId = roomDetail.getMembers().get(0).getMemberId(); // 철수

        // when: 멤버 선택 실행
        SelectMemberResponse response = memberService.selectMember(slug, targetMemberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getMemberId()).isEqualTo(targetMemberId);
        assertThat(response.getMemberName()).isEqualTo("철수");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 slug로 멤버 선택 시 예외 발생")
    void selectMemberFailInvalidSlugTest() {
        // given
        String invalidSlug = "invalid-slug-99999";
        Long memberId = 1L;

        // when & then
        assertThatThrownBy(() -> memberService.selectMember(invalidSlug, memberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 방이 존재하지 않습니다.");
    }

    @Test
    @DisplayName("해당 방에 속하지 않거나 존재하지 않는 memberId로 요청 시 예외 발생")
    void selectMemberFailInvalidMemberIdTest() {
        // given 1. 방 생성
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
                .title("멤버 실패 테스트방")
                .baseCurrency("KRW")
                .pin("1234")
                .memberNames(List.of("A", "B"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);
        String slug = createResponse.getSlug();

        // given 2. 방에 없는 임의의 memberId 지정
        Long wrongMemberId = 999999L;

        // when & then
        assertThatThrownBy(() -> memberService.selectMember(slug, wrongMemberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 방에 속하지 않은 멤버입니다.");
    }
}
