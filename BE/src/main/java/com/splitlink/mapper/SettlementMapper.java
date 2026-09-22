package com.splitlink.mapper;

import com.splitlink.dto.response.RoomMySettlementResponse;
import com.splitlink.entity.Settlement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 정산(Settlement) 데이터에 접근하는 MyBatis Mapper 인터페이스
 */
@Mapper
public interface SettlementMapper {

    /** 계산된 정산 송금 내역 목록 다건 저장 */
    void insertSettlements(@Param("settlements") List<Settlement> settlements);

    /** settlements 테이블 기준 유저의 보낼 돈 / 받을 돈 요약 조회 */
    SettlementSummary findSettlementSummary(@Param("roomId") Long roomId,
                                            @Param("memberId") Long memberId);

    /** memberId 별 보낼 송금 리스트 */
    List<RoomMySettlementResponse.SendItem> findMySendSettlements(@Param("roomId") Long roomId,
                                                                  @Param("memberId") Long memberId);

    /** memberId 별 받은 송금 리스트 */
    List<RoomMySettlementResponse.ReceiveItem> findMyReceiveSettlements(@Param("roomId") Long roomId,
                                                                  @Param("memberId") Long memberId);

    /** 송금 여부 상태 변경 */
    int updateRemittanceStatus(@Param("roomId") Long roomId,
                               @Param("settlementId") Long settlementId,
                               @Param("memberId") Long memberId,
                               @Param("isDone") boolean isDone);

    /**
     * findSettlementSummary 전용 결과 매핑 클래스
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    class SettlementSummary {
        private BigDecimal totalSendAmount;
        private BigDecimal totalReceiveAmount;
    }
}
