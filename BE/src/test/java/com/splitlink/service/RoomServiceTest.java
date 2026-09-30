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
public class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private ExpenseMapper expenseMapper;

    /**
     * 방 생성 시나리오 테스트
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

        // isActive 필드가 초기값(false)으로 잘 매핑되었는지 검증
        assertThat(detailResponse.getMembers())
                .extracting("active") // boolean 필드는 getter명(isActive)에 따라 "active"로 추출
                .containsExactly(false, false);
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
    @DisplayName("방 정보 수정 성공 테스트 - 제목, 기준통화, 입장코드, 참여자 목록(지용 유지, 대성 추가, 태양 삭제)이 정상 수정된다")
    void updateRoomSuccessTest() {
        // given 1. 방 생성
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
                .title("원래 방제목")
                .baseCurrency("KRW")
                .pin("1234")
                .memberNames(List.of("지용", "태양"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);

        // given 2. 방 상세 조회를 통해 기존 '지용'의 memberId 획득
        RoomDetailResponse roomDetail = roomService.accessRoom(createResponse.getSlug(), RoomAccessRequest.builder().pin("1234").build());
        Long jiyongMemberId = roomDetail.getMembers().get(0).getMemberId(); // 지용

        // 수정 요청 DTO 준비 (지용: 기존 ID 전달하여 유지, 대성: memberId null로 신규 추가, 태양: 리스트에서 제외하여 삭제)
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

        // when: 방 수정 호출
        RoomDetailResponse response = roomService.updateRoom(createResponse.getSlug(), updateRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("수정된 방제목");
        assertThat(response.getBaseCurrency()).isEqualTo("USD");
        assertThat(response.getMembers()).extracting("name").containsExactly("지용", "대성");
    }

    /**
     * 방 정보 수정 실패 테스트 - 지출 참여자 삭제 시도 시 예외 발생
     */
    @Test
    @DisplayName("지출 내역(결제자)이 존재하는 멤버를 삭제하려고 하면 예외가 발생한다")
    void updateRoomFailDeleteMemberWithExpensesTest() {
        // given 1. 방 생성
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
                .title("지출 테스트방")
                .baseCurrency("KRW")
                .pin("1234")
                .memberNames(List.of("지용", "태양"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);
        String slug = createResponse.getSlug();

        // given 2. 방 상세 정보 조회를 통해 roomId와 결제자(memberId) 및 태양(memberId) 획득
        RoomDetailResponse roomDetail = roomService.accessRoom(slug, RoomAccessRequest.builder().pin("1234").build());
        Long roomId = roomDetail.getRoomId();
        Long payerId = roomDetail.getMembers().get(0).getMemberId(); // "지용"
        Long taeyangId = roomDetail.getMembers().get(1).getMemberId(); // "태양"

        // given 3. 지용이가 결제한 지출 1건 임의 등록
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

        // given 4. 지용이를 멤버 목록에서 제거하는 수정 요청 DTO
        // given 4. 지용이를 제외하고 태양만 유지하도록 수정 요청 DTO 작성 (지용 삭제 시도)
        RoomUpdateRequest.MemberRequest keepTaeyang = RoomUpdateRequest.MemberRequest.builder()
                .memberId(taeyangId)
                .name("태양")
                .build();

        RoomUpdateRequest invalidUpdateRequest = RoomUpdateRequest.builder()
                .title("수정 방제목")
                .baseCurrency("KRW")
                .pin("1234")
                .members(List.of(keepTaeyang)) // "지용" 삭제 시도
                .build();

        // when & then: 지출이 존재하는 멤버 제거 시 예외 발생 검증
        assertThatThrownBy(() -> roomService.updateRoom(slug, invalidUpdateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format("'%s'님은 지출 내역(결제 또는 참여)이 존재하여 삭제할 수 없습니다.", roomDetail.getMembers().get(0).getName()));
    }

    /**
     * 방 정보 수정 실패 테스트 - 존재하지 않는 slug로 요청 시 예외 발생
     */
    @Test
    @DisplayName("존재하지 않는 slug로 방 정보 수정 요청 시 예외 발생")
    void updateRoomFailRoomNotFoundTest() {
        // given: 존재하지 않는 임의의 slug 및 수정 요청 DTO 준비
        String invalidSlug = "non-existent-slug-12345";

        RoomUpdateRequest.MemberRequest m1 = RoomUpdateRequest.MemberRequest.builder().memberId(1L).name("지용").build();

        RoomUpdateRequest updateRequest = RoomUpdateRequest.builder()
                .title("수정된 방제목")
                .baseCurrency("KRW")
                .pin("1234")
                .members(List.of(m1))
                .build();

        // when & then: IllegalArgumentException 예외 발생 검증
        assertThatThrownBy(() -> roomService.updateRoom(invalidSlug, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 방이 존재하지 않거나 입장코드가 일치하지 않습니다.");
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

        RoomDetailResponse roomDetail = roomService.accessRoom(createResponse.getSlug(), RoomAccessRequest.builder().pin("1234").build());
        Long jiyongId = roomDetail.getMembers().get(0).getMemberId();

        // 중복된 이름이 포함된 수정 요청
        // 중복된 이름("지용", "지용")이 포함된 수정 요청
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

        RoomDetailResponse roomDetail = roomService.accessRoom(createResponse.getSlug(), RoomAccessRequest.builder().pin("1234").build());
        Long jiyongId = roomDetail.getMembers().get(0).getMemberId();

        RoomUpdateRequest.MemberRequest m1 = RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build();

        // 틀린 기존 PIN으로 수정 요청
        RoomUpdateRequest wrongPinRequest = RoomUpdateRequest.builder()
                .title("수정된 방제목")
                .baseCurrency("KRW")
                .pin("WrongPin") // 틀린 기존 PIN
                .members(List.of(m1))
                .build();

        // when & then
        assertThatThrownBy(() -> roomService.updateRoom(createResponse.getSlug(), wrongPinRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 방이 존재하지 않거나 입장코드가 일치하지 않습니다.");
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

        RoomDetailResponse roomDetail = roomService.accessRoom(createResponse.getSlug(), RoomAccessRequest.builder().pin("1234").build());
        Long jiyongId = roomDetail.getMembers().get(0).getMemberId();

        RoomUpdateRequest.MemberRequest m1 = RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build();

        // 잘못된 규격의 새 PIN 요청 (예: 3자리)
        RoomUpdateRequest invalidNewPinRequest = RoomUpdateRequest.builder()
                .title("수정된 방제목")
                .baseCurrency("KRW")
                .pin("1234")
                .newPin("123") // 4자리 미만
                .members(List.of(m1))
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

        RoomDetailResponse roomDetail = roomService.accessRoom(createResponse.getSlug(), RoomAccessRequest.builder().pin("1234").build());
        Long jiyongId = roomDetail.getMembers().get(0).getMemberId();
        Long taeyangId = roomDetail.getMembers().get(1).getMemberId();

        RoomUpdateRequest.MemberRequest m1 = RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build();
        RoomUpdateRequest.MemberRequest m2 = RoomUpdateRequest.MemberRequest.builder().memberId(taeyangId).name("태양").build();

        // newPin을 입력하지 않은 수정 요청
        RoomUpdateRequest updateRequestWithoutNewPin = RoomUpdateRequest.builder()
                .title("수정된 방제목")
                .baseCurrency("KRW")
                .pin("1234")
                .newPin(null) // 새 PIN 전달 안 함
                .members(List.of(m1, m2))
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
     * 1. 지출 없는 멤버 삭제가 성공하는지
     */
    @Test
    @DisplayName("[수정 성공] 지출 내역이 없는 멤버 삭제 성공 검증")
    void updateRoom_Success_DeleteMemberWithoutExpenses() {
        // given: 지용, 태양 방 생성
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

        // given: 지용만 남기고 태양 삭제 요청 DTO
        RoomUpdateRequest updateRequest = RoomUpdateRequest.builder()
                .title("테스트방")
                .baseCurrency("KRW")
                .pin("1234")
                .members(List.of(RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build()))
                .build();

        // when
        RoomDetailResponse updatedDetail = roomService.updateRoom(slug, updateRequest);

        // then: 태양이 삭제되어 멤버가 1명만 남았는지 검증
        assertThat(updatedDetail.getMembers()).hasSize(1);
        assertThat(updatedDetail.getMembers()).extracting("name").containsExactly("지용");
    }

    /**
     * 2. 기존 멤버의 memberId가 수정 후에도 유지되는지 (부분 수정의 핵심 보장)
     */
    @Test
    @DisplayName("[수정 성공] 수정 후에도 기존 멤버의 memberId가 변경되지 않고 유지되는지 검증")
    void updateRoom_Success_PreserveExistingMemberId() {
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

        // given: 제목 변경 요청
        RoomUpdateRequest updateRequest = RoomUpdateRequest.builder()
                .title("변경된 제목")
                .baseCurrency("KRW")
                .pin("1234")
                .members(List.of(RoomUpdateRequest.MemberRequest.builder().memberId(originalJiyongId).name("지용").build()))
                .build();

        // when
        RoomDetailResponse updatedDetail = roomService.updateRoom(slug, updateRequest);

        // then: 지용의 memberId가 수정 전과 동일한지 검증
        Long updatedJiyongId = updatedDetail.getMembers().get(0).getMemberId();
        assertThat(updatedJiyongId).isEqualTo(originalJiyongId);
    }

    /**
     * 3. 새 멤버만 추가할 때 (기존 멤버 삭제 없이 신규 등록)
     */
    @Test
    @DisplayName("[수정 성공] 기존 멤버 유지 + 새 멤버만 추가 성공 검증")
    void updateRoom_Success_OnlyAddNewMember() {
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

        // given: 지용(기존) + 대성(신규, memberId=null)
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

        // then: 멤버가 2명으로 늘었는지 검증
        assertThat(updatedDetail.getMembers()).hasSize(2);
        assertThat(updatedDetail.getMembers()).extracting("name").containsExactlyInAnyOrder("지용", "대성");
    }

    /**
     * 4. expense_shares에만 참여한 멤버(결제자가 아닌 경우) 삭제가 막히는지 (LEFT JOIN es 경로)
     */
    @Test
    @DisplayName("[수정 실패] 결제자가 아니고 부담금(expense_shares) 참여자에만 속한 멤버 삭제 시 예외 발생")
    void updateRoom_Fail_DeleteMemberInExpenseSharesOnly() {
        // given: 지용, 태양 방 생성
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

        // given 1: 지용이가 결제한 지출 1건 등록
        Expense dummyExpense = Expense.builder()
                .roomId(roomId)
                .payerId(jiyongId) // 결제자는 지용
                .title("공통 지출")
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .fxRate(new BigDecimal("1.0000"))
                .spentAt(LocalDateTime.now())
                .build();
        expenseMapper.insertExpense(dummyExpense);

        // given 2: ExpenseShareParam 객체를 생성하여 expense_shares에 태양(taeyangId) 추가
        ExpenseMapper.ExpenseShareParam shareParam = ExpenseMapper.ExpenseShareParam.builder()
                .expenseId(dummyExpense.getExpenseId())
                .memberId(taeyangId)
                .amount(new BigDecimal("5000"))
                .build();

        expenseMapper.insertExpenseShares(List.of(shareParam));

        // given 3: 결제자가 아닌 참여자 '태양'을 목록에서 제거하여 삭제 시도
        RoomUpdateRequest invalidUpdateRequest = RoomUpdateRequest.builder()
                .title("테스트방")
                .baseCurrency("KRW")
                .pin("1234")
                .members(List.of(RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build()))
                .build();

        // when & then: 예외 발생 검증
        assertThatThrownBy(() -> roomService.updateRoom(slug, invalidUpdateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지출 내역(결제 또는 참여)이 존재하여 삭제할 수 없습니다.");
    }

    /**
     * 5. 삭제와 추가를 동시에 하는 경우
     */
    @Test
    @DisplayName("[수정 성공] 멤버 삭제와 신규 멤버 추가가 한 번의 요청으로 동시에 성공하는지 검증")
    void updateRoom_Success_DeleteAndAddMembersConcurrently() {
        // given: 지용, 태양 방 생성
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

        // given: 태양 삭제 + 지용 유지 + 대성 추가
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

        // then: 태양은 빠지고 대성이 들어와 2명인지 검증
        assertThat(updatedDetail.getMembers()).hasSize(2);
        assertThat(updatedDetail.getMembers()).extracting("name").containsExactlyInAnyOrder("지용", "대성");
    }

    /**
     * 6. newPin 변경 검증
     */
    @Test
    @DisplayName("[수정 성공] newPin을 전달 시 입장코드가 정상적으로 변경되고 새 PIN으로 접근 가능한지 검증")
    void updateRoom_Success_ChangeNewPin() {
        // given: 기존 PIN "1234"
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

        // given: newPin "5678" 전달
        RoomUpdateRequest updateRequest = RoomUpdateRequest.builder()
                .title("테스트방")
                .baseCurrency("KRW")
                .pin("1234")
                .newPin("5678")
                .members(List.of(RoomUpdateRequest.MemberRequest.builder().memberId(jiyongId).name("지용").build()))
                .build();

        // when
        roomService.updateRoom(slug, updateRequest);

        // then: 변경된 새 PIN "5678"로 입장 성공하는지 검증
        RoomAccessRequest newPinAccessRequest = RoomAccessRequest.builder().pin("5678").build();
        RoomDetailResponse accessResponse = roomService.accessRoom(slug, newPinAccessRequest);
        assertThat(accessResponse).isNotNull();

        // 기존 PIN "1234"로는 접근 거부되는지 검증
        RoomAccessRequest oldPinAccessRequest = RoomAccessRequest.builder().pin("1234").build();
        assertThatThrownBy(() -> roomService.accessRoom(slug, oldPinAccessRequest))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 방 삭제 성공 테스트
     */
    @Test
    @DisplayName("지출이 없는 방은 정산 완료 여부와 관계없이 정상 삭제된다")
    void deleteRoomSuccessTest() {
        // given 1. 방 생성 (지출 0건)
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

        // when & then: 삭제 수행 시 예외 없이 정상 삭제
        assertThatCode(() -> roomService.deleteRoom(slug, accessRequest))
                .doesNotThrowAnyException();

        // 삭제 후 조회 시 방이 존재하지 않아야 함
        assertThatThrownBy(() -> roomService.getRoomSummary(slug))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 방 삭제 실패 테스트 - 존재하지 않는 방(slug)
     */
    @Test
    @DisplayName("존재하지 않는 방 삭제 시 '해당 방이 존재하지 않거나 입장코드가 일치하지 않습니다.' 예외 발생")
    void deleteRoomFailRoomNotFoundTest() {
        // given: 존재하지 않는 임의의 slug 및 요청 DTO 준비
        String slug = "wrong-slug";

        RoomAccessRequest accessRequest = RoomAccessRequest.builder()
                .pin("1234")
                .build();

        // when & then
        assertThatThrownBy(() -> roomService.deleteRoom(slug, accessRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 방이 존재하지 않거나 입장코드가 일치하지 않습니다.");
    }

    /**
     * 방 삭제 실패 테스트 - 입장코드가 틀린 경우
     */
    @Test
    @DisplayName("입장코드가 틀릴 시 '해당 방이 존재하지 않거나 입장코드가 일치하지 않습니다.' 예외 발생")
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

        // when & then
        assertThatThrownBy(() -> roomService.deleteRoom(slug, accessRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 방이 존재하지 않거나 입장코드가 일치하지 않습니다.");
    }

    /**
     * 방 삭제 실패 - 지출이 존재하고 정산 미완료 시 삭제 불가
     */
    @Test
    @DisplayName("지출 내역이 존재하는 방에서 정산 미완료 시 삭제 요청을 하면 예외가 발생한다")
    void deleteRoomFailNotClosedTest() {
        // given 1. 방 생성
        RoomCreateRequest createRequest = RoomCreateRequest.builder()
                .title("삭제 테스트방")
                .baseCurrency("KRW")
                .pin("1234")
                .memberNames(List.of("A", "B"))
                .build();
        RoomCreateResponse createResponse = roomService.createRoom(createRequest);
        String slug = createResponse.getSlug();

        // given 2. 방 상세 정보 조회를 통해 roomId와 결제자(memberId) 획득
        RoomDetailResponse roomDetail = roomService.accessRoom(slug, RoomAccessRequest.builder().pin("1234").build());
        Long roomId = roomDetail.getRoomId();
        Long payerId = roomDetail.getMembers().get(0).getMemberId();

        // given 3. 지출 1건 임의 등록 (is_closed = false 상태 유지)
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

        // when & then: 지출이 1건 이상 존재하고 정산 미완료 시 삭제 예외 검증
        assertThatThrownBy(() -> roomService.deleteRoom(slug, accessRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 방의 정산이 남았습니다. 모든 정산이 완료된 후 삭제할 수 있습니다.");
    }
}
