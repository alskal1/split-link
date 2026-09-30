package com.splitlink.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitlink.dto.request.RoomAccessRequest;
import com.splitlink.dto.request.RoomCreateRequest;
import com.splitlink.dto.response.RoomCreateResponse;
import com.splitlink.dto.response.RoomDetailResponse;
import com.splitlink.dto.response.SelectMemberResponse;
import com.splitlink.mapper.RoomMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MemberService 통합 테스트
 */
@SpringBootTest
@Transactional
@DisplayName("MemberService 통합 테스트")
public class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("멤버 선택 (selectMember)")
    class SelectMemberTest {

        @Test
        @DisplayName("성공: 올바른 slug와 memberId로 요청 시 isActive가 true로 변경되고 토큰 및 멤버 정보가 정상 반환된다.")
        void selectMemberSuccess() {
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

            // when
            SelectMemberResponse response = memberService.selectMember(slug, targetMemberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isNotNull();
            assertThat(response.getMemberId()).isEqualTo(targetMemberId);
            assertThat(response.getMemberName()).isEqualTo("철수");
            assertThat(response.isActive()).isTrue();
        }

        @Test
        @DisplayName("예외: 존재하지 않는 slug로 멤버 선택 시 IllegalArgumentException 예외가 발생한다.")
        void selectMemberThrowExceptionWhenInvalidSlug() {
            // given
            String invalidSlug = "invalid-slug-99999";
            Long memberId = 1L;

            // when & then
            assertThatThrownBy(() -> memberService.selectMember(invalidSlug, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 방이 없습니다.");
        }

        @Test
        @DisplayName("예외: 해당 방에 속하지 않거나 존재하지 않는 memberId로 요청 시 IllegalArgumentException 예외가 발생한다.")
        void selectMemberThrowExceptionWhenInvalidMemberId() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("멤버 실패 테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("A", "B"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);
            String slug = createResponse.getSlug();

            Long wrongMemberId = 999999L;

            // when & then
            assertThatThrownBy(() -> memberService.selectMember(slug, wrongMemberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 방에 속하지 않은 멤버입니다.");
        }

        @Test
        @DisplayName("예외: 이미 활성화(isActive=true)된 멤버를 다시 선택 시 IllegalArgumentException 예외가 발생한다.")
        void selectMemberThrowExceptionWhenAlreadyActive() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("중복 선택 테스트방")
                    .baseCurrency("KRW")
                    .pin("Pass123")
                    .memberNames(List.of("참여자A", "참여자B"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);

            RoomAccessRequest accessRequest = RoomAccessRequest.builder().pin("Pass123").build();
            RoomDetailResponse detailResponse = roomService.accessRoom(createResponse.getSlug(), accessRequest);
            Long targetMemberId = detailResponse.getMembers().get(0).getMemberId();

            // 1차 선택: 정상 활성화 (isActive -> true)
            memberService.selectMember(createResponse.getSlug(), targetMemberId);

            // when & then: 2차 중복 선택 시 예외 발생 검증
            assertThatThrownBy(() -> memberService.selectMember(createResponse.getSlug(), targetMemberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 선택되어 접속 중인 멤버입니다.");
        }
    }
}