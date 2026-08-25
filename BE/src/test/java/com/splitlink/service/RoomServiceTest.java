package com.splitlink.service;

import com.splitlink.dto.request.RoomAccessRequest;
import com.splitlink.dto.request.RoomFormRequest;
import com.splitlink.dto.response.RoomCreateResponse;
import com.splitlink.dto.response.RoomDetailResponse;
import com.splitlink.dto.response.RoomSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RoomService 통합 테스트
 */
@SpringBootTest
@Transactional
public class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    /**
     * 방 생성 시나리오 테스트
     *
     * <p>
     *     입력값(방 제목, 기준 통화, 입장코드, 멤버 목록)을 바탕으로 방과 멤버가 정상적으로 생성되는지 검증
     * </p>
     */
    @Test
    @DisplayName("방 생성 테스트")
    void createRoomTest() {
        // given: 방 생성 요청 DTO 준비
        RoomFormRequest request = RoomFormRequest.builder()
                .title("테스트 방제목")
                .baseCurrency("KRW")
                .pin("Test11")
                .memberNames(List.of("기영", "기철", "오덕"))
                .build();

        // when: 방 생성 서비스 호출
        RoomCreateResponse response = roomService.createRoom(request);

        // then: 검증
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("테스트 방제목");
        assertThat(response.getPin()).isEqualTo("Test11");
        assertThat(response.getSlug()).isNotNull();
        assertThat(response.getMemberNames()).containsExactly("기영", "기철", "오덕");
    }

    /**
     * 방 요약 정보 조회 성공 테스트
     */
    @Test
    @DisplayName("slug 기반 방 요약 정보 조회 테스트")
    void getRoomSummaryTest() {
        // given
        RoomFormRequest createRequest = RoomFormRequest.builder()
                .title("요약 테스트방")
                .baseCurrency("KRW")
                .pin("Pass123")
                .memberNames(List.of("A", "B"))
                .build();

        RoomCreateResponse createResponse = roomService.createRoom(createRequest);

        // when
        RoomSummaryResponse summaryResponse = roomService.getRoomSummary(createResponse.getSlug());

        // then
        assertThat(summaryResponse).isNotNull();
        assertThat(summaryResponse.getTitle()).isEqualTo("요약 테스트방");
        assertThat(summaryResponse.getMemberCount()).isEqualTo(2);
        assertThat(summaryResponse.getMemberNames()).containsExactly("A", "B");
    }

    /**
     * 입장코드(PIN) 검증 성공 테스트
     */
    @Test
    @DisplayName("PIN 번호 일치 시 방 상세 정보 반환 테스트")
    void accessRoomSuccessTest() {
        // given
        RoomFormRequest createRequest = RoomFormRequest.builder()
                .title("입장 테스트방")
                .baseCurrency("KRW")
                .pin("Pass123")
                .memberNames(List.of("A", "B"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);

        RoomAccessRequest accessRequest = RoomAccessRequest.builder()
                .pin("Pass123")
                .build();

        // when
        RoomDetailResponse detailResponse = roomService.accessRoom(createResponse.getSlug(), accessRequest);

        // then
        assertThat(detailResponse).isNotNull();
        assertThat(detailResponse.getTitle()).isEqualTo("입장 테스트방");
        assertThat(detailResponse.getMembers()).hasSize(2);
    }

    /**
     * 입장코드(PIN) 검증 실패 테스트 (예외 발생)
     */
    @Test
    @DisplayName("PIN 번호 불일치 시 IllegalArgumentExcepion 예외 발생 테스트")
    void accessRoomFailWrongPinTest() {
        // given
        RoomFormRequest createRequest = RoomFormRequest.builder()
                .title("PIN 실패 테스트")
                .baseCurrency("KRW")
                .pin("Pass123")
                .memberNames(List.of("A", "B"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);

        RoomAccessRequest wrongAccessRequest = RoomAccessRequest.builder()
                .pin("WrongPin")
                .build();

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> roomService.accessRoom(createResponse.getSlug(), wrongAccessRequest));
    }
}
