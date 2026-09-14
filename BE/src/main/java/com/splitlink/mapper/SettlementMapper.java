package com.splitlink.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 정산(Settlement) 데이터에 접근하는 MyBatis Mapper 인터페이스
 */
@Mapper
public interface SettlementMapper {

    /** settlements 테이블 기준 유저의 보낼 돈 / 받을 돈 요약 조회 */
    SettlementSummary findSettlementSummary(@Param("roomId") Long roomId,
                                            @Param("memberId") Long memberId);

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
