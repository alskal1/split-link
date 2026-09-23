package com.splitlink.service;

import com.splitlink.dto.request.RemittanceStatusUpdateRequest;
import com.splitlink.entity.Room;
import com.splitlink.entity.Settlement;
import com.splitlink.mapper.RoomMapper;
import com.splitlink.mapper.SettlementMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SettlementServiceIntegrationTest {

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private SettlementMapper settlementMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("동시성 검증: 두 스레드가 서로 다른 정산건을 동시에 완료 처리해도 is_closed가 정상적으로 true가 된다.")
    void updateRemittanceStatus_concurrency_success() throws InterruptedException {
        // 1. Given: Room 생성
        String slug = "concurrency-test-" + System.currentTimeMillis();
        Room room = Room.builder()
                .slug(slug)
                .title("동시성 테스트 방")
                .baseCurrency("KRW")
                .pin("1234")
                .build();
        roomMapper.insertRoom(room);
        Long roomId = room.getRoomId();

        // 2. Member 2명 생성 및 PK 획득
        jdbcTemplate.update("INSERT INTO members (room_id, name) VALUES (?, ?)", roomId, "회원1");
        Long member1Id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        jdbcTemplate.update("INSERT INTO members (room_id, name) VALUES (?, ?)", roomId, "회원2");
        Long member2Id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 3. Settlement 2건 생성 (1번: member1 -> member2, 2번: member2 -> member1)
        Settlement settlement1 = Settlement.builder()
                .roomId(roomId)
                .senderId(member1Id)
                .receiverId(member2Id)
                .amount(new BigDecimal("10000"))
                .isDone(false)
                .build();

        Settlement settlement2 = Settlement.builder()
                .roomId(roomId)
                .senderId(member2Id)
                .receiverId(member1Id)
                .amount(new BigDecimal("20000"))
                .isDone(false)
                .build();

        settlementMapper.insertSettlements(List.of(settlement1, settlement2));

        // DB에 실제 저장된 settlement_id 2건 조회
        List<Long> settlementIds = jdbcTemplate.queryForList(
                "SELECT settlement_id FROM settlements WHERE room_id = ? ORDER BY settlement_id ASC",
                Long.class,
                roomId
        );
        Long settlement1Id = settlementIds.get(0);
        Long settlement2Id = settlementIds.get(1);

        RemittanceStatusUpdateRequest request = new RemittanceStatusUpdateRequest(true);

        // 4. When: 2개의 멀티스레드로 동시 요청 실행
        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        executorService.submit(() -> {
            try {
                // member1Id가 자신의 정산건(settlement1Id) 완료 처리
                settlementService.updateRemittanceStatus(slug, settlement1Id, member1Id, request);
            } catch (Exception e) {
                System.err.println("=== 스레드 1 예외 발생 ===");
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                // member2Id가 자신의 정산건(settlement2Id) 완료 처리
                settlementService.updateRemittanceStatus(slug, settlement2Id, member2Id, request);
            } catch (Exception e) {
                System.err.println("=== 스레드 2 예외 발생 ===");
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        // 5. Then: 최종 방 마감 상태(is_closed)가 true인지 확인
        Boolean isClosed = roomMapper.findIsClosedBySlug(slug);
        assertThat(isClosed).isTrue();
    }
}