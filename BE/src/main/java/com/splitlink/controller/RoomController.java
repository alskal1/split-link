package com.splitlink.controller;

import com.splitlink.common.api.ApiResponse;
import com.splitlink.dto.request.RoomCreateRequest;
import com.splitlink.dto.response.RoomCreateResponse;
import com.splitlink.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // 방 생성 (Create)
    @PostMapping
    public ResponseEntity<ApiResponse<RoomCreateResponse>> createRoom(@Valid @RequestBody RoomCreateRequest request) {
        log.info("RoomController.createRoom 진입");

        RoomCreateResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, response));
    }
//
//    // 방 단건 조회 (Read)
//    @GetMapping("/{roomId}")
//    public ApiResponse<RoomResponseDto> getRoom(@PathVariable Long roomId) {
//        RoomResponseDto room = roomService.getRoom(roomId);
//        return ApiResponse.success(room);
//    }
//
//    // 방 수정 (Update)
//    @PatchMapping("/{roomId}")
//    public ApiResponse<Void> updateRoom(@PathVariable Long roomId, @RequestBody RoomUpdateRequest request) {
//        roomService.modifyRoom(roomId, request);
//        return ApiResponse.success(null);
//    }
//
//    // 방 삭제 (Delete)
//    @DeleteMapping("/{roomId}")
//    public ApiResponse<Void> deleteRoom(@PathVariable Long roomId) {
//        roomService.removeRoom(roomId);
//        return ApiResponse.success(null);
//    }
}
