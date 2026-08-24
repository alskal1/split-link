package com.splitlink.service;

import com.splitlink.dto.request.RoomCreateRequest;
import com.splitlink.dto.response.RoomCreateResponse;
import com.splitlink.entity.Room;
import com.splitlink.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomMapper roomMapper;

    @Transactional
    public RoomCreateResponse createRoom(RoomCreateRequest request) {
        log.info("RoomService:createRoom 진입");

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

        // 3. 방금 채워진 roomId를 꺼내서 멤버들 일괄 저장
        Long generatedRoomId = room.getRoomId();

        // 공백 제거 및 중복 검증 예시
        List<String> sanitizedMembers = request.getMemberNames().stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();

        if (sanitizedMembers.isEmpty()) {
            throw new IllegalArgumentException("멤버 이름을 최소 한 명 이상 입력해야 합니다.");
        }

        long uniqueCount = sanitizedMembers.stream().distinct().count();
        if (uniqueCount != sanitizedMembers.size()) {
            throw new IllegalArgumentException("방 멤버 이름은 중복될 수 없습니다.");
        }

        roomMapper.insertMembers(generatedRoomId, sanitizedMembers);

        // 4. 응답 반환
        return RoomCreateResponse.builder()
                .slug(slug)
                .title(room.getTitle())
                .baseCurrency(room.getBaseCurrency())
                .pin(room.getPin())
                .memberNames(sanitizedMembers)
                .build();
    }
}
