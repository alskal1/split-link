package com.splitlink.service;

import com.splitlink.dto.request.RoomCreateRequest;
import com.splitlink.dto.response.RoomCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class RoomServiceTest {

    @Autowired
    private RoomService roomService;

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
        assertThat(response.getFullUrl()).isNotNull().contains(response.getSlug());
        assertThat(response.getMemberNames()).containsExactly("기영", "기철", "오덕");
    }
}
