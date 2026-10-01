package com.splitlink.service;

import com.splitlink.dto.request.RoomAccessRequest;
import com.splitlink.dto.request.RoomCreateRequest;
import com.splitlink.dto.request.RoomUpdateRequest;
import com.splitlink.dto.response.RoomCreateResponse;
import com.splitlink.dto.response.RoomDetailResponse;
import com.splitlink.dto.response.RoomSummaryResponse;
import com.splitlink.entity.Expense;
import com.splitlink.mapper.ExpenseMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RoomService 통합 테스트
 */
@SpringBootTest
@Transactional
@DisplayName("RoomService 통합 테스트")
public class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private ExpenseMapper expenseMapper;

    @Nested
    @DisplayName("방 생성 (createRoom)")
    class CreateRoomTest {

        @Test
        @DisplayName("성공: 입력값(방 제목, 기준 통화, 입장코드, 멤버 목록)을 바탕으로 방과 멤버를 정상 생성한다.")
        void createRoomSuccess() {
            // given
            RoomCreateRequest request = RoomCreateRequest.builder()
                    .title("테스트 방제목")
                    .baseCurrency("KRW")
                    .pin("Test11")
                    .memberNames(List.of("기영", "기철", "오덕"))
                    .build();

            // when
            RoomCreateResponse response = roomService.createRoom(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("테스트 방제목");
            assertThat(response.getPin()).isEqualTo("Test11");
            assertThat(response.getSlug()).isNotNull();
            assertThat(response.getMemberNames()).containsExactly("기영", "기철", "오덕");
        }
    }

    @Nested
    @DisplayName("방 요약 및 상세 정보 접근 (getRoomSummary / accessRoom)")
    class GetRoomSummaryAndAccessTest {

        @Test
        @DisplayName("성공: 올바른 slug 요청 시 방 요약 정보를 정상 반환한다.")
        void getRoomSummarySuccess() {
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

        @Test
        @DisplayName("성공: PIN 번호가 일치하면 방 상세 정보 및 초기 멤버 상태(active=false)를 반환한다.")
        void accessRoomSuccess() {
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
            assertThat(detailResponse.getMembers())
                    .extracting("active")
                    .containsExactly(false, false);
        }

        @Test
        @DisplayName("예외: PIN 번호가 불일치하면 IllegalArgumentException 예외가 발생한다.")
        void accessRoomThrowExceptionWhenWrongPin() {
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
    }

    @Nested
    @DisplayName("방 정보 수정 (updateRoom)")
    class UpdateRoomTest {

        @Test
        @DisplayName("성공: 제목, 기준통화, 입장코드, 참여자 목록(지용 유지, 대성 추가, 태양 삭제)이 정상 수정된다.")
        void updateRoomSuccess() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("원래 방제목")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("지용", "태양"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);

            RoomDetailResponse roomDetail = roomService.accessRoom(createResponse.getSlug(), RoomAccessRequest.builder().pin("1234").build());
            Long jiyongMemberId = roomDetail.getMembers().get(0).getMemberId();

            RoomUpdateRequest.MemberRequest keepMember = RoomUpdateRequest.MemberRequest.builder()
                    .memberId(jiyongMemberId)
                    .name("지용")
                    .build();

            RoomUpdateRequest.MemberRequest newMember = RoomUpdateRequest.MemberRequest.builder()
                    .memberId(null)
                    .name("대성")
                    .build();

            RoomUpdateRequest updateRequest = RoomUpdateRequest.builder()
                    .title("수정된 방제목")
                    .baseCurrency("usd")
                    .pin("1234")
                    .newPin("NewPass12")
                    .members(List.of(keepMember, newMember))
                    .build();

            // when
            RoomDetailResponse response = roomService.updateRoom(createResponse.getSlug(), updateRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("수정된 방제목");
            assertThat(response.getBaseCurrency()).isEqualTo("USD");
            assertThat(response.getMembers()).extracting("name").containsExactly("지용", "대성");
        }

        @Test
        @DisplayName("성공: 지출 내역이 없는 멤버 삭제가 정상적으로 처리된다.")
        void updateRoomSuccessDeleteMemberWithoutExpenses() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("지용", "태양"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);
            String slug = createResponse.getSlug();

            RoomDetailResponse initialDetail = roomService.accessRoom(slug, RoomAccessRequest.builder().pin("1234").build());
            Long jiyongId = initialDetail.getMembers().stream().filter(m -> m.getName().equals("지용")).findFirst().get().getMemberId();

            RoomUpdateRequest updateRequest = RoomUpdateRequest.builder()
                    .title("테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .members(List.of(RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build()))
                    .build();

            // when
            RoomDetailResponse updatedDetail = roomService.updateRoom(slug, updateRequest);

            // then
            assertThat(updatedDetail.getMembers()).hasSize(1);
            assertThat(updatedDetail.getMembers()).extracting("name").containsExactly("지용");
        }

        @Test
        @DisplayName("성공: 수정 후에도 기존 멤버의 memberId가 변하지 않고 유지된다.")
        void updateRoomSuccessPreserveExistingMemberId() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("지용"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);
            String slug = createResponse.getSlug();

            RoomDetailResponse initialDetail = roomService.accessRoom(slug, RoomAccessRequest.builder().pin("1234").build());
            Long originalJiyongId = initialDetail.getMembers().get(0).getMemberId();

            RoomUpdateRequest updateRequest = RoomUpdateRequest.builder()
                    .title("변경된 제목")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .members(List.of(RoomUpdateRequest.MemberRequest.builder().memberId(originalJiyongId).name("지용").build()))
                    .build();

            // when
            RoomDetailResponse updatedDetail = roomService.updateRoom(slug, updateRequest);

            // then
            Long updatedJiyongId = updatedDetail.getMembers().get(0).getMemberId();
            assertThat(updatedJiyongId).isEqualTo(originalJiyongId);
        }

        @Test
        @DisplayName("성공: 기존 멤버를 유지하면서 새 멤버만 추가 등록이 수행된다.")
        void updateRoomSuccessOnlyAddNewMember() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("지용"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);
            String slug = createResponse.getSlug();

            RoomDetailResponse initialDetail = roomService.accessRoom(slug, RoomAccessRequest.builder().pin("1234").build());
            Long jiyongId = initialDetail.getMembers().get(0).getMemberId();

            RoomUpdateRequest updateRequest = RoomUpdateRequest.builder()
                    .title("테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .members(List.of(
                            RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build(),
                            RoomUpdateRequest.MemberRequest.builder().memberId(null).name("대성").build()
                    ))
                    .build();

            // when
            RoomDetailResponse updatedDetail = roomService.updateRoom(slug, updateRequest);

            // then
            assertThat(updatedDetail.getMembers()).hasSize(2);
            assertThat(updatedDetail.getMembers()).extracting("name").containsExactlyInAnyOrder("지용", "대성");
        }

        @Test
        @DisplayName("성공: 멤버 삭제와 신규 멤버 추가가 한 번의 요청으로 동시에 수행된다.")
        void updateRoomSuccessDeleteAndAddMembersConcurrently() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("지용", "태양"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);
            String slug = createResponse.getSlug();

            RoomDetailResponse initialDetail = roomService.accessRoom(slug, RoomAccessRequest.builder().pin("1234").build());
            Long jiyongId = initialDetail.getMembers().stream().filter(m -> m.getName().equals("지용")).findFirst().get().getMemberId();

            RoomUpdateRequest updateRequest = RoomUpdateRequest.builder()
                    .title("테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .members(List.of(
                            RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build(),
                            RoomUpdateRequest.MemberRequest.builder().memberId(null).name("대성").build()
                    ))
                    .build();

            // when
            RoomDetailResponse updatedDetail = roomService.updateRoom(slug, updateRequest);

            // then
            assertThat(updatedDetail.getMembers()).hasSize(2);
            assertThat(updatedDetail.getMembers()).extracting("name").containsExactlyInAnyOrder("지용", "대성");
        }

        @Test
        @DisplayName("성공: newPin 전달 시 입장코드가 변경되고 새 PIN으로 접근이 가능해진다.")
        void updateRoomSuccessChangeNewPin() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("지용"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);
            String slug = createResponse.getSlug();

            RoomDetailResponse initialDetail = roomService.accessRoom(slug, RoomAccessRequest.builder().pin("1234").build());
            Long jiyongId = initialDetail.getMembers().get(0).getMemberId();

            RoomUpdateRequest updateRequest = RoomUpdateRequest.builder()
                    .title("테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .newPin("5678")
                    .members(List.of(RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build()))
                    .build();

            // when
            roomService.updateRoom(slug, updateRequest);

            // then
            RoomAccessRequest newPinAccessRequest = RoomAccessRequest.builder().pin("5678").build();
            RoomDetailResponse accessResponse = roomService.accessRoom(slug, newPinAccessRequest);
            assertThat(accessResponse).isNotNull();

            RoomAccessRequest oldPinAccessRequest = RoomAccessRequest.builder().pin("1234").build();
            assertThatThrownBy(() -> roomService.accessRoom(slug, oldPinAccessRequest))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("성공: 새 PIN 번호를 입력하지 않으면 기존 PIN 번호를 유지한다.")
        void updateRoomSuccessKeepOriginalPin() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("원래 방제목")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("지용", "태양"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);

            RoomDetailResponse roomDetail = roomService.accessRoom(createResponse.getSlug(), RoomAccessRequest.builder().pin("1234").build());
            Long jiyongId = roomDetail.getMembers().get(0).getMemberId();
            Long taeyangId = roomDetail.getMembers().get(1).getMemberId();

            RoomUpdateRequest.MemberRequest m1 = RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build();
            RoomUpdateRequest.MemberRequest m2 = RoomUpdateRequest.MemberRequest.builder().memberId(taeyangId).name("태양").build();

            RoomUpdateRequest updateRequestWithoutNewPin = RoomUpdateRequest.builder()
                    .title("수정된 방제목")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .newPin(null)
                    .members(List.of(m1, m2))
                    .build();

            // when
            RoomDetailResponse response = roomService.updateRoom(createResponse.getSlug(), updateRequestWithoutNewPin);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("수정된 방제목");

            RoomAccessRequest accessRequest = RoomAccessRequest.builder().pin("1234").build();
            RoomDetailResponse accessResponse = roomService.accessRoom(createResponse.getSlug(), accessRequest);
            assertThat(accessResponse).isNotNull();
        }

        @Test
        @DisplayName("예외: 지출 내역(결제자)이 존재하는 멤버를 삭제하려고 하면 예외가 발생한다.")
        void updateRoomThrowExceptionWhenDeletePayerMember() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("지출 테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("지용", "태양"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);
            String slug = createResponse.getSlug();

            RoomDetailResponse roomDetail = roomService.accessRoom(slug, RoomAccessRequest.builder().pin("1234").build());
            Long roomId = roomDetail.getRoomId();
            Long payerId = roomDetail.getMembers().get(0).getMemberId();
            Long taeyangId = roomDetail.getMembers().get(1).getMemberId();

            Expense dummyExpense = Expense.builder()
                    .roomId(roomId)
                    .payerId(payerId)
                    .title("테스트 지출")
                    .amount(new BigDecimal("10000"))
                    .currency("KRW")
                    .fxRate(new BigDecimal("1.0000"))
                    .spentAt(LocalDateTime.now())
                    .build();
            expenseMapper.insertExpense(dummyExpense);

            RoomUpdateRequest.MemberRequest keepTaeyang = RoomUpdateRequest.MemberRequest.builder()
                    .memberId(taeyangId)
                    .name("태양")
                    .build();

            RoomUpdateRequest invalidUpdateRequest = RoomUpdateRequest.builder()
                    .title("수정 방제목")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .members(List.of(keepTaeyang))
                    .build();

            // when & then
            assertThatThrownBy(() -> roomService.updateRoom(slug, invalidUpdateRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(String.format("'%s'님은 지출 내역(결제 또는 참여)이 존재하여 삭제할 수 없습니다.", roomDetail.getMembers().get(0).getName()));
        }

        @Test
        @DisplayName("예외: 결제자가 아니고 부담금(expense_shares) 참여자에만 속한 멤버 삭제 시 예외가 발생한다.")
        void updateRoomThrowExceptionWhenDeleteMemberInExpenseSharesOnly() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("지용", "태양"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);
            String slug = createResponse.getSlug();

            RoomDetailResponse initialDetail = roomService.accessRoom(slug, RoomAccessRequest.builder().pin("1234").build());
            Long roomId = initialDetail.getRoomId();
            Long jiyongId = initialDetail.getMembers().stream().filter(m -> m.getName().equals("지용")).findFirst().get().getMemberId();
            Long taeyangId = initialDetail.getMembers().stream().filter(m -> m.getName().equals("태양")).findFirst().get().getMemberId();

            Expense dummyExpense = Expense.builder()
                    .roomId(roomId)
                    .payerId(jiyongId)
                    .title("공통 지출")
                    .amount(new BigDecimal("10000"))
                    .currency("KRW")
                    .fxRate(new BigDecimal("1.0000"))
                    .spentAt(LocalDateTime.now())
                    .build();
            expenseMapper.insertExpense(dummyExpense);

            ExpenseMapper.ExpenseShareParam shareParam = ExpenseMapper.ExpenseShareParam.builder()
                    .expenseId(dummyExpense.getExpenseId())
                    .memberId(taeyangId)
                    .amount(new BigDecimal("5000"))
                    .build();

            expenseMapper.insertExpenseShares(List.of(shareParam));

            RoomUpdateRequest invalidUpdateRequest = RoomUpdateRequest.builder()
                    .title("테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .members(List.of(RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build()))
                    .build();

            // when & then
            assertThatThrownBy(() -> roomService.updateRoom(slug, invalidUpdateRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("지출 내역(결제 또는 참여)이 존재하여 삭제할 수 없습니다.");
        }

        @Test
        @DisplayName("예외: 존재하지 않는 slug로 방 정보 수정 요청 시 예외가 발생한다.")
        void updateRoomThrowExceptionWhenRoomNotFound() {
            // given
            String invalidSlug = "non-existent-slug-12345";
            RoomUpdateRequest.MemberRequest m1 = RoomUpdateRequest.MemberRequest.builder().memberId(1L).name("지용").build();

            RoomUpdateRequest updateRequest = RoomUpdateRequest.builder()
                    .title("수정된 방제목")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .members(List.of(m1))
                    .build();

            // when & then
            assertThatThrownBy(() -> roomService.updateRoom(invalidSlug, updateRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 방이 존재하지 않거나 입장코드가 일치하지 않습니다.");
        }

        @Test
        @DisplayName("예외: 멤버 이름에 중복이 있으면 예외가 발생한다.")
        void updateRoomThrowExceptionWhenDuplicateMember() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("원래 방제목")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("지용", "태양"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);

            RoomDetailResponse roomDetail = roomService.accessRoom(createResponse.getSlug(), RoomAccessRequest.builder().pin("1234").build());
            Long jiyongId = roomDetail.getMembers().get(0).getMemberId();

            RoomUpdateRequest.MemberRequest m1 = RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build();
            RoomUpdateRequest.MemberRequest m2 = RoomUpdateRequest.MemberRequest.builder().memberId(null).name("지용").build();

            RoomUpdateRequest invalidUpdateRequest = RoomUpdateRequest.builder()
                    .title("수정된 방제목")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .newPin(null)
                    .members(List.of(m1, m2))
                    .build();

            // when & then
            assertThrows(IllegalArgumentException.class,
                    () -> roomService.updateRoom(createResponse.getSlug(), invalidUpdateRequest));
        }

        @Test
        @DisplayName("예외: 기존 PIN 번호가 일치하지 않으면 예외가 발생한다.")
        void updateRoomThrowExceptionWhenWrongPin() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("원래 방제목")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("지용", "태양"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);

            RoomDetailResponse roomDetail = roomService.accessRoom(createResponse.getSlug(), RoomAccessRequest.builder().pin("1234").build());
            Long jiyongId = roomDetail.getMembers().get(0).getMemberId();

            RoomUpdateRequest.MemberRequest m1 = RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build();

            RoomUpdateRequest wrongPinRequest = RoomUpdateRequest.builder()
                    .title("수정된 방제목")
                    .baseCurrency("KRW")
                    .pin("WrongPin")
                    .members(List.of(m1))
                    .build();

            // when & then
            assertThatThrownBy(() -> roomService.updateRoom(createResponse.getSlug(), wrongPinRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 방이 존재하지 않거나 입장코드가 일치하지 않습니다.");
        }

        @Test
        @DisplayName("예외: 새 PIN 번호가 규격(영대소문자/숫자 4~10자리)에 맞지 않으면 예외가 발생한다.")
        void updateRoomThrowExceptionWhenInvalidNewPin() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("원래 방제목")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("지용", "태양"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);

            RoomDetailResponse roomDetail = roomService.accessRoom(createResponse.getSlug(), RoomAccessRequest.builder().pin("1234").build());
            Long jiyongId = roomDetail.getMembers().get(0).getMemberId();

            RoomUpdateRequest.MemberRequest m1 = RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build();

            RoomUpdateRequest invalidNewPinRequest = RoomUpdateRequest.builder()
                    .title("수정된 방제목")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .newPin("123")
                    .members(List.of(m1))
                    .build();

            // when & then
            assertThatThrownBy(() -> roomService.updateRoom(createResponse.getSlug(), invalidNewPinRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("입장코드는 영대소문자와 숫자 조합으로 4~10자리여야 합니다.");
        }
    }

    @Nested
    @DisplayName("방 삭제 (deleteRoom)")
    class DeleteRoomTest {

        @Test
        @DisplayName("성공: 지출이 없는 방은 정산 완료 여부와 관계없이 정상 삭제된다.")
        void deleteRoomSuccess() {
            // given
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

            // when & then
            assertThatCode(() -> roomService.deleteRoom(slug, accessRequest))
                    .doesNotThrowAnyException();

            assertThatThrownBy(() -> roomService.getRoomSummary(slug))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("예외: 존재하지 않는 방 삭제 시 예외가 발생한다.")
        void deleteRoomThrowExceptionWhenRoomNotFound() {
            // given
            String slug = "wrong-slug";

            RoomAccessRequest accessRequest = RoomAccessRequest.builder()
                    .pin("1234")
                    .build();

            // when & then
            assertThatThrownBy(() -> roomService.deleteRoom(slug, accessRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 방이 존재하지 않거나 입장코드가 일치하지 않습니다.");
        }

        @Test
        @DisplayName("예외: 입장코드가 틀릴 시 예외가 발생한다.")
        void deleteRoomThrowExceptionWhenWrongPin() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("삭제 테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("A", "B"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);
            String slug = createResponse.getSlug();

            RoomAccessRequest accessRequest = RoomAccessRequest.builder()
                    .pin("wrongpin")
                    .build();

            // when & then
            assertThatThrownBy(() -> roomService.deleteRoom(slug, accessRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 방이 존재하지 않거나 입장코드가 일치하지 않습니다.");
        }

        @Test
        @DisplayName("예외: 지출 내역이 존재하는 방에서 정산 미완료 시 삭제 요청을 하면 예외가 발생한다.")
        void deleteRoomThrowExceptionWhenNotClosed() {
            // given
            RoomCreateRequest createRequest = RoomCreateRequest.builder()
                    .title("삭제 테스트방")
                    .baseCurrency("KRW")
                    .pin("1234")
                    .memberNames(List.of("A", "B"))
                    .build();
            RoomCreateResponse createResponse = roomService.createRoom(createRequest);
            String slug = createResponse.getSlug();

            RoomDetailResponse roomDetail = roomService.accessRoom(slug, RoomAccessRequest.builder().pin("1234").build());
            Long roomId = roomDetail.getRoomId();
            Long payerId = roomDetail.getMembers().get(0).getMemberId();

            Expense dummyExpense = Expense.builder()
                    .roomId(roomId)
                    .payerId(payerId)
                    .title("테스트 카페 지출")
                    .amount(new BigDecimal("15000"))
                    .currency("KRW")
                    .fxRate(new BigDecimal("1.0000"))
                    .spentAt(LocalDateTime.now())
                    .build();
            expenseMapper.insertExpense(dummyExpense);

            RoomAccessRequest accessRequest = RoomAccessRequest.builder()
                    .pin("1234")
                    .build();

            // when & then
            assertThatThrownBy(() -> roomService.deleteRoom(slug, accessRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 방의 정산이 남았습니다. 모든 정산이 완료된 후 삭제할 수 있습니다.");
        }
    }
}