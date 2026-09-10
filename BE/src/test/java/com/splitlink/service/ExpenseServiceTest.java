package com.splitlink.service;

import com.splitlink.common.validator.RoomAccessValidator;
import com.splitlink.dto.response.ExpenseFormInitResponse;
import com.splitlink.mapper.MemberMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {

    @InjectMocks
    private ExpenseService expenseService;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private RoomAccessValidator roomAccessValidator;

    @Test
    @DisplayName("성공: 등록된 계좌가 있는 회원인 경우 계좌 정보와 방 멤버 목록을 정상 반환한다.")
    void getExpenseFormInitSuccessWithAccount() {
        // given
        String slug = "test-room-slug";
        Long memberId = 1L;
        Long roomId = 10L;

        ExpenseFormInitResponse.AccountInfo accountInfo = ExpenseFormInitResponse.AccountInfo.builder()
                .bankName("카카오뱅크")
                .accountNumber("3333-12-345678")
                .build();

        ExpenseFormInitResponse.MemberInfo member1 = ExpenseFormInitResponse.MemberInfo.builder()
                .memberId(1L).name("스폰지밥").isActive(true).isSelf(true).build();
        ExpenseFormInitResponse.MemberInfo member2 = ExpenseFormInitResponse.MemberInfo.builder()
                .memberId(2L).name("뚱이").isActive(false).isSelf(false).build();

        given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);
        given(memberMapper.findAccountInfoByMemberId(memberId)).willReturn(Optional.of(accountInfo));
        given(memberMapper.findRoomMembersBySlug(slug, memberId)).willReturn(List.of(member1, member2));

        // when
        ExpenseFormInitResponse response = expenseService.getExpenseFormInit(slug, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCurrentMemberId()).isEqualTo(memberId);

        // 계좌 검증
        assertThat(response.getDefaultAccount()).isNotNull();
        assertThat(response.getDefaultAccount().getBankName()).isEqualTo("카카오뱅크");

        // 멤버 목록 검증
        assertThat(response.getRoomMembers()).hasSize(2);
        assertThat(response.getRoomMembers().get(0).isSelf()).isTrue();
        assertThat(response.getRoomMembers().get(0).isActive()).isTrue();
        assertThat(response.getRoomMembers().get(1).isSelf()).isFalse();
        assertThat(response.getRoomMembers().get(1).isActive()).isFalse();

        verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
        verify(memberMapper).findAccountInfoByMemberId(memberId);
        verify(memberMapper).findRoomMembersBySlug(slug, memberId);
    }

    @Test
    @DisplayName("성공: DB에 계좌 정보(Member)가 아예 없는 경우(Optional.empty)에도 defaultAccount는 null을 반환한다.")
    void getExpenseFormInitSuccessNoAccount() {
        // given
        String slug = "test-room-slug";
        Long memberId = 1L;
        Long roomId = 10L;

        // Validator는 정상 통과하지만, 계좌/회원 조회 결과가 Optional.empty인 상황
        given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);
        given(memberMapper.findAccountInfoByMemberId(memberId)).willReturn(Optional.empty());
        given(memberMapper.findRoomMembersBySlug(slug, memberId)).willReturn(List.of());

        // when
        ExpenseFormInitResponse response = expenseService.getExpenseFormInit(slug, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDefaultAccount()).isNull(); // 예외가 발생하는 대신 null이어야 함!

        verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
        verify(memberMapper).findAccountInfoByMemberId(memberId);
    }

    @Test
    @DisplayName("예외: 존재하지 않는 회원 ID로 요청 시 IllegalArgumentException 예외가 발생한다.")
    void getExpenseFormInitThrowExceptionWhenMemberNotFound() {
        // given
        String slug = "test-room-slug";
        Long invalidMemberId = 999L;

        // RoomAccessValidator에서 예외가 발생하는 상황 모킹
        given(roomAccessValidator.validateAndGetRoomId(slug, invalidMemberId))
                .willThrow(new IllegalArgumentException("해당 방이 존재하지 않거나, 해당 방에 접근 권한이 없습니다."));

        // when & then
        assertThatThrownBy(() -> expenseService.getExpenseFormInit(slug, invalidMemberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 방이 존재하지 않거나, 해당 방에 접근 권한이 없습니다.");
    }
}
