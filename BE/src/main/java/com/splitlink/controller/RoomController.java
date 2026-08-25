package com.splitlink.controller;

import com.splitlink.common.api.ApiResponse;
import com.splitlink.dto.request.RoomAccessRequest;
import com.splitlink.dto.request.RoomFormRequest;
import com.splitlink.dto.response.RoomCreateResponse;
import com.splitlink.dto.response.RoomDetailResponse;
import com.splitlink.dto.response.RoomSummaryResponse;
import com.splitlink.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
* 방(Room) 관련 API 요청을 처리하는 컨트롤러
* */
@Slf4j
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /**
     * 새로운 방 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RoomCreateResponse>> createRoom(@Valid @RequestBody RoomFormRequest request) {
        log.info("RoomController:createRoom 진입");

        RoomCreateResponse response = roomService.createRoom(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, response));
    }

    /**
     * 입장코드 입력 페이지용 방 요약 정보 조회
     */
    @GetMapping("/{slug}/summary")
    public ResponseEntity<ApiResponse<RoomSummaryResponse>> getRoomSummary(@PathVariable String slug) {
        log.info("RoomController:getRoomSummary 진입 - slug: {}", slug);

        RoomSummaryResponse response = roomService.getRoomSummary(slug);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 입장코드를 검증하고, 일치할 경우 방 상세 정보 반환한
     */
    @PostMapping("/{slug}/access")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> getRoomAccess(
            @PathVariable String slug,
            @Valid @RequestBody RoomAccessRequest request) {
        log.info("RoomController:getRoomAccess 진입 - slug: {}", slug);

        RoomDetailResponse response = roomService.accessRoom(slug, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 방 수정 (제목, 기준통화, 입장코드, 멤버)
     * 해당 값을 다 가지고 오며, 멤버는 이름 리스트로만 들어온다.
     * 이름 리스트는 전체 삭제 후 등록 진행.
     */
    @PatchMapping("/{slug}")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> updateRoom(
            @PathVariable String slug,
            @Valid @RequestBody RoomFormRequest request) {
        log.info("RoomController:updateRoom 진입 - slug: {}", slug);

        RoomDetailResponse response = roomService.updateRoom(slug, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
