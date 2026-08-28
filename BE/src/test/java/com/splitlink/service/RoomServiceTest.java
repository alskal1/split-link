package com.splitlink.service;

import com.splitlink.dto.request.RoomAccessRequest;
import com.splitlink.dto.request.RoomCreateRequest;
import com.splitlink.dto.request.RoomUpdateRequest;
import com.splitlink.dto.response.RoomCreateResponse;
import com.splitlink.dto.response.RoomDetailResponse;
import com.splitlink.dto.response.RoomSummaryResponse;
import com.splitlink.mapper.RoomMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RoomService 통합 테스트
 *
 * [ AssertJ 예외 검증 패턴 정리 ]
 * 1. 성공 케이스 (예외 미발생 검증):
 *    assertThatCode(() -> 실행할_메서드)
 *        .doesNotThrowAnyException();
 *
 * 2. 실패 케이스 (예외 발생 검증):
 *    assertThatThrownBy(() -> 실행할_메서드)
 *        .isInstanceOf(예외클래스.class)
 *        .hasMessage("서비스에서_던지는_예외_메시지");
 */
@SpringBootTest
@Transactional
public class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomMapper roomMapper;

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
        RoomCreateRequest request = RoomCreateRequest.builder()
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
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
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
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
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
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
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
        assertThrows(IllegalArgumentException.class,
                () -> roomService.accessRoom(createResponse.getSlug(), wrongAccessRequest));
    }

    /**
     * 방 정보 수정 성공 테스트 (PUT)
     */
    @Test
    @DisplayName("방 정보 수정 성공 테스트 - 제목, 기준통화, 입장코드, 참여자 목록이 정상 수정된다")
    void updateRoomSuccessTest() {
        // given
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
                .title("원래 방제목")
                .baseCurrency("KRW")
                .pin("1234")
                .memberNames(List.of("지용", "태양"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);

        // 수정 요청 DTO 준비
        RoomUpdateRequest updateRequest = RoomUpdateRequest.builder()
                .title("수정된 방제목")
                .baseCurrency("usd")
                .pin("1234")
                .newPin("NewPass12")
                .memberNames(List.of("지용", "대성"))
                .build();

        // when: 방 수정 호출
        RoomDetailResponse response = roomService.updateRoom(createResponse.getSlug(), updateRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("수정된 방제목");
        assertThat(response.getBaseCurrency()).isEqualTo("USD");
        assertThat(response.getMembers()).extracting("name").containsExactly("지용", "대성");
    }

    /**
     * 방 정보 수정 실패 테스트 - 존재하지 않는 slug로 요청 시 예외 발생
     */
    @Test
    @DisplayName("존재하지 않는 slug로 방 정보 수정 요청 시 예외 발생")
    void updateRoomFailRoomNotFoundTest() {
        // given: 존재하지 않는 임의의 slug 및 수정 요청 DTO 준비
        String invalidSlug = "non-existent-slug-12345";

        RoomUpdateRequest updateRequest = RoomUpdateRequest.builder()
                .title("수정된 방제목")
                .baseCurrency("KRW")
                .pin("1234")
                .memberNames(List.of("지용", "태양"))
                .build();

        // when & then: IllegalArgumentException 예외 발생 검증
        assertThatThrownBy(() -> roomService.updateRoom(invalidSlug, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 방이 없습니다.");
    }

    /**
     * 방 정보 수정 실패 테스트 - 멤버 이름이 중복
     */
    @Test
    @DisplayName("방 정보 수정 시 멤버 이름에 중복이 있으면 400 예외 발생")
    void updateRoomFailDuplicateMemberTest() {
        // given
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
                .title("원래 방제목")
                .baseCurrency("KRW")
                .pin("1234")
                .memberNames(List.of("지용", "태양"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);

        // 중복된 이름이 포함된 수정 요청
        RoomUpdateRequest invalidUpdateRequest = RoomUpdateRequest.builder()
                .title("수정된 방제목")
                .baseCurrency("KRW")
                .pin("1234")
                .newPin(null)
                .memberNames(List.of("지용", "지용"))
                .build();

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> roomService.updateRoom(createResponse.getSlug(), invalidUpdateRequest));
    }

    /**
     * 방 정보 수정 실패 테스트 - 기존 PIN 번호 불일치
     */
    @Test
    @DisplayName("방 정보 수정 시 기존 PIN 번호가 일치하지 않으면 예외 발생")
    void updateRoomFailWrongPinTest() {
        // given
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
                .title("원래 방제목")
                .baseCurrency("KRW")
                .pin("1234")
                .memberNames(List.of("지용", "태양"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);

        // 틀린 기존 PIN으로 수정 요청
        RoomUpdateRequest wrongPinRequest = RoomUpdateRequest.builder()
                .title("수정된 방제목")
                .baseCurrency("KRW")
                .pin("WrongPin") // 틀린 기존 PIN
                .memberNames(List.of("지용", "태양"))
                .build();

        // when & then
        assertThatThrownBy(() -> roomService.updateRoom(createResponse.getSlug(), wrongPinRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("기존 입장코드가 일치하지 않습니다.");
    }

    /**
     * 방 정보 수정 실패 테스트 - 새 PIN 규격 미달
     */
    @Test
    @DisplayName("새 PIN 번호가 규격(영대소문자/숫자 4~10자리)에 맞지 않으면 예외 발생")
    void updateRoomFailInvalidNewPinTest() {
        // given
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
                .title("원래 방제목")
                .baseCurrency("KRW")
                .pin("1234")
                .memberNames(List.of("지용", "태양"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);

        // 잘못된 규격의 새 PIN 요청 (예: 3자리)
        RoomUpdateRequest invalidNewPinRequest = RoomUpdateRequest.builder()
                .title("수정된 방제목")
                .baseCurrency("KRW")
                .pin("1234")
                .newPin("123") // 4자리 미만
                .memberNames(List.of("지용", "태양"))
                .build();

        // when & then
        assertThatThrownBy(() -> roomService.updateRoom(createResponse.getSlug(), invalidNewPinRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("입장코드는 영대소문자와 숫자 조합으로 4~10자리여야 합니다.");
    }

    /**
     * 방 정보 수정 성공 테스트 - 새 PIN 미입력 시 기존 PIN 유지
     */
    @Test
    @DisplayName("새 PIN 번호를 입력하지 않으면 기존 PIN 번호를 유지하고 수정 성공")
    void updateRoomSuccessKeepOriginalPinTest() {
        // given
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
                .title("원래 방제목")
                .baseCurrency("KRW")
                .pin("1234")
                .memberNames(List.of("지용", "태양"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);

        // newPin을 입력하지 않은 수정 요청
        RoomUpdateRequest updateRequestWithoutNewPin = RoomUpdateRequest.builder()
                .title("수정된 방제목")
                .baseCurrency("KRW")
                .pin("1234")
                .newPin(null) // 새 PIN 전달 안 함
                .memberNames(List.of("지용", "태양"))
                .build();

        // when
        RoomDetailResponse response = roomService.updateRoom(createResponse.getSlug(), updateRequestWithoutNewPin);

        // then: 수정은 성공하고 기존 PIN으로 입장 조회되는지 확인
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("수정된 방제목");

        // 기존 PIN으로 입장 성공하는지 최종 검증
        RoomAccessRequest accessRequest = RoomAccessRequest.builder().pin("1234").build();
        RoomDetailResponse accessResponse = roomService.accessRoom(createResponse.getSlug(), accessRequest);
        assertThat(accessResponse).isNotNull();
    }

    /**
     * 방 삭제 성공 테스트
     */
    @Test
    @DisplayName("정산이 완료된 방 삭제 성공 테스트")
    void deleteRoomSuccessTest() {
        // given 1. 방 생성
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
                .title("삭제 테스트방")
                .baseCurrency("KRW")
                .pin("1234")
                .memberNames(List.of("A", "B"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);
        String slug = createResponse.getSlug();

        // 2. DB에서 정산 완료 상태로 변경
        roomMapper.updateIsClosedBySlug(slug, true);

        RoomAccessRequest accessRequest = RoomAccessRequest.builder()
                .pin("1234")
                .build();

        // 3. 삭제 수행 및 검증
        assertThatCode(() -> roomService.deleteRoom(slug, accessRequest))
                .doesNotThrowAnyException();
    }

    /**
     * 방 삭제 실패 테스트 - 존재하지 않는 방(slug)
     */
    @Test
    @DisplayName("존재하지 않는 방 삭제 시 '해당 방이 없습니다.' 예외 발생")
    void deleteRoomFailRoomNotFoundTest() {
        // given: 존재하지 않는 임의의 slug 및 요청 DTO 준비
        String slug = "wrong-slug";

        RoomAccessRequest accessRequest = RoomAccessRequest.builder()
                .pin("1234")
                .build();

        // 3. 삭제 수행 및 검증
        assertThatThrownBy(() -> roomService.deleteRoom(slug, accessRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 방이 없습니다.");
    }

    /**
     * 방 삭제 실패 테스트 - 입장코드가 틀린 경우
     */
    @Test
    @DisplayName("입장코드가 틀릴 시 '기존 입장코드가 일치하지 않습니다.' 예외 발생")
    void deleteRoomFailWrongPinTest() {
        // given 1. 방 생성
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
                .title("삭제 테스트방")
                .baseCurrency("KRW")
                .pin("1234")
                .memberNames(List.of("A", "B"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);
        String slug = createResponse.getSlug();

        // 2. 잘못된 pin 제공
        RoomAccessRequest accessRequest = RoomAccessRequest.builder()
                .pin("wrongpin")
                .build();

        // 3. 삭제 수행 및 검증
        assertThatThrownBy(() -> roomService.deleteRoom(slug, accessRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("기존 입장코드가 일치하지 않습니다.");
    }

    /**
     * 방 삭제 실패 - 정산 미완료 시 삭제
     */
    @Test
    @DisplayName("정산 미완료 삭제 시 '해당 방의 정산이 남았습니다. 모든 정산이 완료된 후 삭제할 수 있습니다.' 예외 발생")
    void deleteRoomFailNotClosedTest() {
        // given 1. 방 생성 (기본값 is_closed = false)
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
                .title("삭제 테스트방")
                .baseCurrency("KRW")
                .pin("1234")
                .memberNames(List.of("A", "B"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);
        String slug = createResponse.getSlug();

        RoomAccessRequest accessRequest = RoomAccessRequest.builder()
                .pin("1234")
                .build();

        // 2. 정산 미완료 상태로 삭제 수행 및 검증
        assertThatThrownBy(() -> roomService.deleteRoom(slug, accessRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 방의 정산이 남았습니다. 모든 정산이 완료된 후 삭제할 수 있습니다.");
    }
}
