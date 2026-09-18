package com.splitlink.service;

import com.splitlink.common.validator.RoomAccessValidator;
import com.splitlink.dto.request.ExpenseBatchCreateRequest;
import com.splitlink.dto.request.ExpenseUpdateRequest;
import com.splitlink.dto.response.ExpenseDetailResponse;
import com.splitlink.dto.response.ExpenseFormInitResponse;
import com.splitlink.dto.response.ExpenseListResponse;
import com.splitlink.dto.response.ExpenseUpdateFormResponse;
import com.splitlink.entity.Expense;
import com.splitlink.mapper.ExpenseMapper;
import com.splitlink.mapper.MemberMapper;
import com.splitlink.mapper.RoomMapper;
import com.splitlink.mapper.SettlementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 지출 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final MemberMapper memberMapper;
    private final RoomMapper roomMapper;
    private final ExpenseMapper expenseMapper;
    private final SettlementMapper settlementMapper;
    private final RoomAccessValidator roomAccessValidator;

    /**
     * 지출 입력 폼 초기화에 필요한 데이터 조회 (계좌 정보 + 방 멤버 목록)
     *
     * @param slug     방 식별자
     * @param memberId 현재 접속한 회원 PK
     * @return 지출 폼 초기화 응답 DTO
     */
    @Transactional(readOnly = true)
    public ExpenseFormInitResponse getExpenseFormInit(String slug, Long memberId) {

        roomAccessValidator.validateAndGetRoomId(slug, memberId);

        // 현재 접속한 사용자 존재 여부 확인 및 계좌 정보 조회
        ExpenseFormInitResponse.AccountInfo defaultAccount = memberMapper.findAccountInfoByMemberId(memberId)
                .orElse(null);

        // TODO: [보안] 향후 계좌번호 AES-256 암복호화 유틸리티 적용 예정
        // 회원은 존재하나 계좌 등록을 안한 경우 (bank_name이 null) DTO를 null로 치환하여 전달
        if (defaultAccount != null && defaultAccount.getBankName() == null) {
            defaultAccount = null;
        }

        // 해당 방에 속한 전체 멤버 목록 조회
        List<ExpenseFormInitResponse.MemberInfo> roomMembers = memberMapper.findRoomMembersBySlug(slug, memberId);

        // 최종 DTO 생성 및 반환
        return ExpenseFormInitResponse.builder()
                .currentMemberId(memberId)
                .defaultAccount(defaultAccount)
                .roomMembers(roomMembers)
                .build();
    }

    /**
     * 지출 내역 일괄 등록 (결제자 계좌 업데이트 + 지출 메인 생성 + 참여자 분할 저장)
     *
     * @param slug     방 식별자 (UUID/Slug)
     * @param memberId 현재 JWT 인증된 사용자 PK (요청자 검증용)
     * @param request  지출 일괄 등록 요청 DTO
     */
    @Transactional
    public void createExpenses(String slug, Long memberId, ExpenseBatchCreateRequest request) {

        // 요청자 본인의 방 접근 권한 및 roomId 조회
        Long roomId = roomAccessValidator.validateAndGetRoomId(slug, memberId);

        // 요청 데이터 내의 모든 payerId와 targetMemberIds 수집
        Set<Long> allRequestMemberIds = new HashSet<>();
        for (ExpenseBatchCreateRequest.ExpenseGroupRequest group : request.getExpenseGroups()) {
            allRequestMemberIds.add(group.getPayerId());
            for (ExpenseBatchCreateRequest.ExpenseItemRequest item : group.getItems()) {
                allRequestMemberIds.addAll(item.getTargetMemberIds());
            }
        }

        // 요청된 모든 memberId가 실제 해당 방 소속인지 일괄 검증 (IDOR 방지)
        roomAccessValidator.validateMembersInRoom(roomId, new ArrayList<>(allRequestMemberIds));

        // 결제자 그룹 단위 처리
        for (ExpenseBatchCreateRequest.ExpenseGroupRequest group : request.getExpenseGroups()) {

            // 결제자 최신 계좌번호 업데이트
            memberMapper.updateAccountInfo(group.getPayerId(), group.getBankName(), group.getAccountNumber());

            // TODO: [환율] 추후 다국어/외화 결제 지원 시 고도화 예정 (현재 원화 KRW 1.0 고정)
            // 통화 및 환율 세팅 (원화 전용)
            String currency = (group.getCurrency() == null) ? "KRW" : group.getCurrency().trim().toUpperCase();
            BigDecimal fxRate = BigDecimal.ONE; // 원화니까 환율은 1.0 고정

            // 결제자 그룹 내 세부 지출 항목 등록
            for (ExpenseBatchCreateRequest.ExpenseItemRequest item : group.getItems()) {

                Expense expense = Expense.builder()
                        .roomId(roomId)
                        .payerId(group.getPayerId())
                        .title(item.getTitle())
                        .amount(item.getAmount())
                        .currency(currency)
                        .fxRate(fxRate)
                        .spentAt(group.getSpentAt())
                        .build();

                // 메인 지출 데이터 저장 (MyBatis useGeneratedKeys를 통해 expenseId PK 주입됨)
                expenseMapper.insertExpense(expense);

                // 오차 보정된 멤버별 부담금 리스트 계산 (별도 메서드 호출)
                List<ExpenseMapper.ExpenseShareParam> shares = calculateShares(
                        expense.getExpenseId(),
                        item.getAmount(),
                        item.getTargetMemberIds()
                );

                // 지출 부담 참여자 Bulk Insert
                expenseMapper.insertExpenseShares(shares);
            }
        }
    }

    /**
     * 지출 내역 목록 및 상단 정산 요약 정보 조회
     *
     * @param slug     방 식별자
     * @param memberId 현재 접속한 회원 PK
     * @return 지출 내역 및 정산 정보 응답 DTO
     */
    @Transactional(readOnly = true)
    public ExpenseListResponse getExpenseList(String slug, Long memberId) {

        // 방 존재 및 접근 권한 검증 + roomId 가져오기
        Long roomId = roomAccessValidator.validateAndGetRoomId(slug, memberId);

        // 상단 헤더 데이터 조회 (방 제목, 사용자 이름, isLocked)
        RoomMapper.ExpenseListHeaderData headerData = roomMapper.getExpenseListHeaderData(roomId, memberId);

        // 방 전체 총 지출 금액 조회 (null 방지 처리)
        // 일단은 한화로 진행
        BigDecimal totalExpenseAmount = expenseMapper.findTotalExpenseAmountByRoomId(roomId);
        if (totalExpenseAmount == null) {
            totalExpenseAmount = BigDecimal.ZERO;
        }

        // 내 정산 상태 및 정산 금액 연산 (isLocked 여부에 따른 분기)
        ExpenseListResponse.SettlementStatus settlementStatus;
        BigDecimal mySettlementAmount;

        if (headerData.isLocked()) {
            // [isLocked = true] 이미 정산하기 버튼을 눌러 settlements 테이블에 결과가 들어있는 상태
            // settlements 테이블에서 sender/receiver 조회 (Single Query)
            SettlementMapper.SettlementSummary summary = settlementMapper.findSettlementSummary(roomId, memberId);

            BigDecimal sendAmount = (summary != null) ? summary.getTotalSendAmount() : BigDecimal.ZERO;
            BigDecimal receiveAmount = (summary != null) ?  summary.getTotalReceiveAmount() : BigDecimal.ZERO;

            // 받을 돈에서 보낼 돈을 뺀 순액(Net Amount) 계산
            BigDecimal netAmount = receiveAmount.subtract(sendAmount);

            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 순액이 양수 (+) -> 최종적으로 돈을 받아야 함
                settlementStatus = ExpenseListResponse.SettlementStatus.RECEIVE;
                mySettlementAmount = netAmount;
            } else if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
                // 순액이 음수 (-) -> 최종적으로 돈을 보내야 함 (음수를 양수로 변환하기 위해 abs() 사용)
                settlementStatus = ExpenseListResponse.SettlementStatus.SEND;
                mySettlementAmount = netAmount.abs();
            } else {
                // 순액이 0원 -> 낼 것도 받을 것도 없음
                settlementStatus = ExpenseListResponse.SettlementStatus.ZERO;
                mySettlementAmount = BigDecimal.ZERO;
            }
        } else {
            // [isLocked = false] 지출 입력 중이라 정산 미마감 상태
            settlementStatus = ExpenseListResponse.SettlementStatus.PENDING;
            mySettlementAmount = null;
        }

        // 하단 지출 내역 목록 조회 (결제자 이름, 참여자 수, 본인 결제 여부 포함)
        List<ExpenseListResponse.ExpenseItemResponse> expenses = expenseMapper.findExpenseItems(roomId, memberId);

        // 반환
        return ExpenseListResponse.builder()
                .roomTitle(headerData.getRoomTitle())
                .currentMemberName(headerData.getMemberName())
                .isLocked(headerData.isLocked())
                .totalExpenseAmount(totalExpenseAmount)
                .settlementStatus(settlementStatus)
                .mySettlementAmount(mySettlementAmount)
                .expenses(expenses)
                .build();
    }

    /**
     * 지출 단건 상세 조회
     *
     * @param slug      방 식별자 (UUID/Slug)
     * @param expenseId 지출 PK
     * @param memberId  현재 접속한 회원 PK
     * @return 지출 상세 응답 DTO
     */
    @Transactional(readOnly = true)
    public ExpenseDetailResponse getExpenseDetail(String slug, Long expenseId, Long memberId) {

        // 해당 방 존재 여부 및 사용자 접근 권한 검증 -> roomId 가져오기
        Long roomId = roomAccessValidator.validateAndGetRoomId(slug, memberId);

        // 해당 방(roomId)에 속한 지출(expenseId) 기본 정보 및 결제자 정보 조회 (없을 경우 예외 처리)
        ExpenseDetailResponse detail = expenseMapper.findExpenseDetailById(expenseId, roomId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지출 내역이 존재하지 않습니다."));

        // 지출 부담 참여자 목록 및 1/N 보정된 개인별 금액 조회
        List<ExpenseDetailResponse.TargetMemberDetail> targetMembers =
                expenseMapper.findExpenseSharesByExpenseId(expenseId, roomId, memberId);

        // 참여자 목록을 세팅하여 최종 DTO 반환
        return ExpenseDetailResponse.builder()
                .expenseId(detail.getExpenseId())
                .title(detail.getTitle())
                .amount(detail.getAmount())
                .currency(detail.getCurrency())
                .fxRate(detail.getFxRate())
                .spentAt(detail.getSpentAt())
                .payerId(detail.getPayerId())
                .payerName(detail.getPayerName())
                .bankName(detail.getBankName())
                .accountNumber(detail.getAccountNumber())
                .isMyPayment(detail.isMyPayment())
                .targetMembers(targetMembers)
                .build();
    }

    @Transactional(readOnly = true)
    public ExpenseUpdateFormResponse getExpenseUpdateForm(String slug, Long expenseId, Long memberId) {

        // 방 접근 권한 검증 및 roomId 가져오기
        Long roomId = roomAccessValidator.validateAndGetRoomId(slug, memberId);

        // 방 마감 상태(isLocked, isClosed) 검증
        RoomMapper.RoomStatus status = roomMapper.findRoomStatusBySlug(slug);
        if (status == null) {
            throw new IllegalArgumentException("존재하지 않는 방입니다.");
        }
        if (status.isClosed()) {
            throw new IllegalArgumentException("이미 정산이 완료된 방의 지출은 수정할 수 없습니다.");
        }
        if (status.isLocked()) {
            throw new IllegalArgumentException("이미 지출 입력이 잠긴 방의 지출은 수정할 수 없습니다.");
        }

        // 지출 기본 정보 조회 (제목, 금액, 결제일시, 결제자ID)
        ExpenseUpdateFormResponse form = expenseMapper.findExpenseUpdateFormById(expenseId, roomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지출 내역이 존재하지 않습니다."));

        // 지출에 선택되어 있던 참여자 ID 목록 조회
        List<Long> targetMemberIds = expenseMapper.findTargetMemberIdsByExpenseId(expenseId);

        // 방 소속 전체 멤버 목록 조회
        List<ExpenseFormInitResponse.MemberInfo> roomMembers = memberMapper.findRoomMembersBySlug(slug, memberId);

        // MemberInfo 타입 매핑 (ExpenseFormInitResponse.MemberInfo -> ExpenseUpdateFormResponse.MemberInfo)
        List<ExpenseUpdateFormResponse.MemberInfo> mappedMembers = roomMembers.stream()
                .map(m -> ExpenseUpdateFormResponse.MemberInfo.builder()
                        .memberId(m.getMemberId())
                        .name(m.getName())
                        .isActive(m.isActive())
                        .build())
                .toList();

        // 최종 수정 폼 DTO 조립 및 반환
        return ExpenseUpdateFormResponse.builder()
                .expenseId(form.getExpenseId())
                .title(form.getTitle())
                .amount(form.getAmount())
                .currency(form.getCurrency())
                .spentAt(form.getSpentAt())
                .payerId(form.getPayerId())
                .targetMemberIds(targetMemberIds)
                .roomMembers(mappedMembers)
                .build();
    }

    /**
     * 지출 내역 수정
     *
     * @param slug      방 식별자 (UUID/Slug)
     * @param expenseId 수정할 지출 PK
     * @param memberId  현재 JWT 인증된 회원 PK
     * @param request   지출 수정 요청 DTO
     */
    @Transactional
    public void updateExpense(String slug, Long expenseId, Long memberId, ExpenseUpdateRequest request) {

        // 방 접근 권한 및 방 존재 검증 후 roomId 반환
        Long roomId = roomAccessValidator.validateAndGetRoomId(slug, memberId);

        // 방 마감 상태(isLocked, isClosed) 검증
        RoomMapper.RoomStatus status = roomMapper.findRoomStatusBySlug(slug);
        if (status == null) {
            throw new IllegalArgumentException("존재하지 않는 방입니다.");
        }
        if (status.isClosed()) {
            throw new IllegalArgumentException("이미 정산이 완료된 방의 지출은 수정할 수 없습니다.");
        }
        if (status.isLocked()){
            throw new IllegalArgumentException("이미 지출 입력이 완료된 방의 지출은 수정할 수 없습니다.");
        }

        // 요청 바디의 payerId 및 targetMemberIds가 해당 방 소속 멤버인지 일괄 검증 (IDOR 방지)
        Set<Long> requestMemberIds = new HashSet<>(request.getTargetMemberIds());
        requestMemberIds.add(request.getPayerId());
        roomAccessValidator.validateMembersInRoom(roomId, new ArrayList<>(requestMemberIds));

        // 메인 지출 데이터 수정
        int updatedRows = expenseMapper.updateExpense(
                expenseId,
                roomId,
                request.getPayerId(),
                request.getTitle(),
                request.getAmount(),
                request.getSpentAt()
        );
        if (updatedRows == 0) {
            throw new IllegalArgumentException("해당 방에 존재하지 않는 지출이거나 이미 삭제된 지출입니다.");
        }

        // 기존 부담금(expense_shares) 삭제 후 1/N 오차 보정하여 새 부담금 재등록
        expenseMapper.deleteExpenseSharesByExpenseId(expenseId);

        List<ExpenseMapper.ExpenseShareParam> newShares = calculateShares(
                expenseId,
                request.getAmount(),
                request.getTargetMemberIds()
        );
        expenseMapper.insertExpenseShares(newShares);
    }

    /**
     * 지출 내역 삭제
     *
     * @param slug      방 식별자 (UUID/Slug)
     * @param expenseId 삭제할 지출 PK
     * @param memberId  현재 접속한 회원 PK
     */
    @Transactional
    public void deleteExpense(String slug, Long expenseId, Long memberId) {

        // 방 접근 권한 및 방 존재 검증 후 roomId 반환
        Long roomId = roomAccessValidator.validateAndGetRoomId(slug, memberId);

        // 방 마감 상태(isLocked, isClosed) 쿼리 1개로 조회 및 각각 검증
        RoomMapper.RoomStatus status = roomMapper.findRoomStatusBySlug(slug);

        if (status == null) {
            throw new IllegalArgumentException("존재하지 않는 방입니다.");
        }
        if (status.isClosed()) {
            throw new IllegalArgumentException("이미 정산이 완료된 방의 지출은 삭제할 수 없습니다.");
        }
        if (status.isLocked()) {
            throw new IllegalArgumentException("이미 지출 입력이 잠긴 방의 지출은 삭제할 수 없습니다.");
        }

        // 지출 삭제 (expenses 삭제 시 FK ON DELETE CASCADE 조건으로 expense_shares 자동 삭제)
        int deletedRows = expenseMapper.deleteExpenseById(expenseId, roomId);
        if (deletedRows == 0) {
            throw new IllegalArgumentException("해당 방에 존재하지 않는 지출이거나 이미 삭제된 지출입니다.");
        }
    }

    /**
     * 1/N 부담금 계산 및 1원 오차 보정 헬퍼 메서드
     * (소수점 버림 후 남은 차액은 첫 번째 참여자에게 가산)
     */
    private List<ExpenseMapper.ExpenseShareParam> calculateShares(Long expenseId, BigDecimal totalAmount, List<Long> memberIds) {
        int memberCount = memberIds.size();

        // 소수점 아래 버림 처리 (예: 10,000 / 3 = 3,333)
        BigDecimal baseAmount = totalAmount.divide(new BigDecimal(memberCount), 0, RoundingMode.DOWN);

        // 남은 오차 금액 계산 (예: 10,000 - (3,333 * 3) = 1)
        BigDecimal remainder = totalAmount.subtract(baseAmount.multiply(new BigDecimal(memberCount)));

        List<ExpenseMapper.ExpenseShareParam> shares = new ArrayList<>();

        for (int i = 0; i < memberCount; i++) {
            Long memberId = memberIds.get(i);

            // 첫 번째 사람(i == 0)에게 남은 오차(1원 등)를 몰아줌
            BigDecimal finalAmount = (i == 0) ? baseAmount.add(remainder) : baseAmount;

            shares.add(new ExpenseMapper.ExpenseShareParam(expenseId, memberId, finalAmount));
        }

        return shares;
    }
}
