package com.splitlink.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 상태 확인(Health Check) API 컨트롤러
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * 서버 정상 동작 여부 확인
     */
    @GetMapping
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
