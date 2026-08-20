package com.splitlink.service;

import com.splitlink.dto.request.RoomCreateRequest;
import com.splitlink.dto.response.RoomCreateResponse;
import com.splitlink.entity.Room;
import com.splitlink.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Room room = Room.builder()
                .title(request.getTitle())
                .baseCurrency(request.getBaseCurrency())
                .pin(request.getPin())
                .slug(slug)
                .build();

        roomMapper.insertRoom(room);

        // 3. 방금 채워진 roomId를 꺼내서 멤버들 일괄 저장
        // 일단 멤버별 color 은 제외함
        Long generatedRoomId = room.getRoomId();
        roomMapper.insertMembers(generatedRoomId, request.getMemberNames());

        // 4. Full URL 생성 및 응답 반환
        String fullUrl = "https://splitlink.com/rooms/" + slug;

        return RoomCreateResponse.builder()
                .slug(slug)
                .fullUrl(fullUrl)
                .title(room.getTitle())
                .pin(room.getPin())
                .memberNames(request.getMemberNames())
                .build();
    }
}
