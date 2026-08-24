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
}
