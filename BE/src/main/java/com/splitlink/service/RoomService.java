package com.splitlink.service;

import com.splitlink.dto.request.RoomAccessRequest;
import com.splitlink.dto.request.RoomFormRequest;
import com.splitlink.dto.response.RoomCreateResponse;
import com.splitlink.dto.response.RoomDetailResponse;
import com.splitlink.dto.response.RoomSummaryResponse;
import com.splitlink.entity.Room;
import com.splitlink.mapper.MemberMapper;
import com.splitlink.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 방 생성, 조회, 입장코드 검증 등 핵심 비즈니스 로직을 수행하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomMapper roomMapper;
    private final MemberMapper memberMapper;

    /**
     * 방 및 초기 멤버를 생성
     *
     * @throws IllegalArgumentException 멤버 이름이 비어있거나 중복된 이름이 있을 경우
     */
    @Transactional
    public RoomCreateResponse createRoom(RoomFormRequest request) {
        log.info("RoomService:createRoom 진입");

        // 참여자 이름 유효성 검증
        List<String> sanitizedMembers = validateAndSanitizeMembers(request.getMemberNames());

        // 1. Slug 생성
        String slug = UUID.randomUUID().toString();

        // 2. Room 엔티티 조립 및 DB 저장
        // 기준 통화 코드를 대문자로 변환
        String sanitizedCurrency = request.getBaseCurrency().trim().toUpperCase();

        Room room = Room.builder()
                .title(request.getTitle())
                .baseCurrency(sanitizedCurrency)
                .pin(request.getPin())
                .slug(slug)
                .build();

        roomMapper.insertRoom(room);

        // 3. 참여자 등록
        memberMapper.insertMembers(room.getRoomId(), sanitizedMembers);

        // 4. 응답 반환
        return RoomCreateResponse.builder()
                .slug(slug)
                .title(room.getTitle())
                .baseCurrency(room.getBaseCurrency())
                .pin(room.getPin())
                .memberNames(sanitizedMembers)
                .build();
    }

    /**
     * slug 기반으로 방 요약 정보(제목, 멤버 수, 멤버 목록) 조회
     *
     * @throws IllegalArgumentException 존재하지 않는 방일 경우
     */
    @Transactional(readOnly = true)
    public RoomSummaryResponse getRoomSummary(String slug) {
        // slug 기준 요약 정보 조회
        RoomSummaryResponse response = roomMapper.findSummaryBySlug(slug);

        // 존재하지 않는 방이라면 예외 발생
        if (response == null) {
            throw new IllegalArgumentException("존재하지 않는 방입니다.");
        }

        int count = (response.getMemberNames() != null) ? response.getMemberNames().size() : 0;
        response.setMemberCount(count);

        return response;
    }

    /**
     * 입장코드 일치 여부를 검증한 후 방 상세 정보를 반환
     *
     * @throws IllegalArgumentException 방이 존재하지 않거나 입장코드가 일치하지 않을 경우
     */
    @Transactional(readOnly = true)
    public RoomDetailResponse accessRoom(String slug, RoomAccessRequest request) {
        log.info("RoomService:accessRoom 진입- slug: {}", slug);

        // 입장코드 검증을 위한 DB의 정답 입장코드 조회
        String realPin = roomMapper.findPinBySlug(slug);

        // 해당 방 존재 여부 확인
        if (realPin == null) {
            throw new IllegalArgumentException("존재하지 않는 방입니다.");
        }

        // 입장코드 일치 여부 확인
        if (!realPin.equals(request.getPin())) {
            throw new IllegalArgumentException("입장 코드가 일치하지 않습니다.");
        }

        // 상세 정보 조회
        RoomDetailResponse response = roomMapper.findDetailBySlug(slug);

        if (response == null) {
            throw new IllegalArgumentException("방 상세 정보를 불러올 수 없습니다.");
        }

        return response;
    }

    /**
     * 방 정보 수정
     * (제목, 기준통화, 입장코드, 멤버)
     */
    @Transactional
    public RoomDetailResponse updateRoom(String slug, RoomFormRequest request) {
        log.info("RoomService:updateRoom 진입 - slug: {}", slug);

        // 해당 방이 있는가
        Long roomId = roomMapper.findRoomIdBySlug(slug);
        if (roomId == null) {
            throw new IllegalArgumentException("해당 방이 없습니다.");
        }

        // 참여자 이름 목록 유효성 검사
        List<String> memberNames = validateAndSanitizeMembers(request.getMemberNames());

        // 기준 통화 코드를 대문자로 변환
        String sanitizedCurrency = request.getBaseCurrency().trim().toUpperCase();

        // 방 정보 수정
        int updatedRows = roomMapper.updateRoom(slug, sanitizedCurrency, request);
        if (updatedRows == 0) {
            throw new IllegalArgumentException("존재하지 않거나 수정할 수 없는 방입니다.");
        }

        // 참여자 목록 삭제
        memberMapper.deleteMembersByRoomId(roomId);

        // 참여자 목록 재등록
        memberMapper.insertMembers(roomId, memberNames);

        // 최신 상태 재조회
        return roomMapper.findDetailBySlug(slug);
    }

    /**
     * 멤버 이름 목록의 공백을 제거하고 유효성 및 중복 여부를 검증
     */
    private List<String> validateAndSanitizeMembers(List<String> rawMemberNames) {
        // 1. null 또는 빈 리스트 우선 검증
        if (rawMemberNames == null || rawMemberNames.isEmpty()) {
            throw new IllegalArgumentException("멤버 이름을 최소 한 명 이상 입력해야 합니다.");
        }

        // 2. 공백 제거 및 공백 문자열 필터링
        List<String> sanitizedMembers = rawMemberNames.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();

        // 3. 공백 문자열만 입력되어 모두 필터링된 경우 검증
        if (sanitizedMembers.isEmpty()) {
            throw new IllegalArgumentException("멤버 이름을 최소 한 명 이상 입력해야 합니다.");
        }

        // 4. 중복 이름 검증
        long uniqueCount = sanitizedMembers.stream().distinct().count();
        if (uniqueCount != sanitizedMembers.size()) {
            throw new IllegalArgumentException("방 멤버 이름은 중복될 수 없습니다.");
        }

        return sanitizedMembers;
    }
}
