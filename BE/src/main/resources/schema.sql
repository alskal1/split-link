-- 1. rooms (방 테이블)
CREATE TABLE rooms (
                       room_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '방 고유 ID',
                       slug VARCHAR(36) NOT NULL UNIQUE COMMENT 'URL 공유용 고유 문자열',
                       title VARCHAR(100) NOT NULL COMMENT '방 이름',
                       base_currency VARCHAR(3) NOT NULL DEFAULT 'KRW' COMMENT '기준 통화 (예: KRW, USD)',
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '방 생성 일시',
                       pin VARCHAR(10) NOT NULL COMMENT '방 접속용 핀 번호',
                       is_locked BOOLEAN NOT NULL DEFAULT FALSE COMMENT '지출 입력 마감(잠금) 여부',
                       is_closed BOOLEAN NOT NULL DEFAULT FALSE COMMENT '정산 완료 여부'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='정산 방 정보';

-- 2. members (참여자 테이블)
CREATE TABLE members (
                         member_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '참여자 고유 ID',
                         room_id BIGINT NOT NULL COMMENT '소속 방 ID',
                         name VARCHAR(50) NOT NULL COMMENT '참여자 이름(닉네임)',
                         bank_name VARCHAR(50) COMMENT '은행명',
                         account_number VARCHAR(50) COMMENT '계좌번호',
                         is_active BOOLEAN NOT NULL DEFAULT FALSE COMMENT '해당 방 접속 상태 여부',
                         CONSTRAINT fk_members_room FOREIGN KEY (room_id)
                             REFERENCES rooms(room_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='방 참여자 정보';

-- 3. expenses (지출 내역 테이블)
CREATE TABLE expenses (
                          expense_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '지출 고유 ID',
                          room_id BIGINT NOT NULL COMMENT '소속 방 ID',
                          payer_id BIGINT NOT NULL COMMENT '실제 결제한 멤버 ID',
                          title VARCHAR(100) NOT NULL COMMENT '지출 항목명',
                          amount DECIMAL(12,2) NOT NULL COMMENT '결제 금액',
                          currency VARCHAR(3) NOT NULL COMMENT '결제 화폐 단위',
                          fx_rate DECIMAL(10,4) NOT NULL DEFAULT 1.0000 COMMENT '기준 통화로 환산하기 위한 환율',
                          spent_at TIMESTAMP NOT NULL COMMENT '지출 일시',
                          CONSTRAINT fk_expenses_room FOREIGN KEY (room_id)
                              REFERENCES rooms(room_id) ON DELETE CASCADE,
                          CONSTRAINT fk_expenses_payer FOREIGN KEY (payer_id)
                              REFERENCES members(member_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='지출 내역';

-- 4. expense_shares (지출별 멤버 부담금 테이블)
CREATE TABLE expense_shares (
                                expense_id BIGINT NOT NULL COMMENT '지출 ID',
                                member_id BIGINT NOT NULL COMMENT '참여자 ID',
                                amount DECIMAL(12,2) NOT NULL COMMENT '부담해야 할 최종 금액',
                                PRIMARY KEY (expense_id, member_id),
                                INDEX idx_shares_member (member_id),
                                CONSTRAINT fk_shares_expense FOREIGN KEY (expense_id)
                                    REFERENCES expenses(expense_id) ON DELETE CASCADE,
                                CONSTRAINT fk_shares_member FOREIGN KEY (member_id)
                                    REFERENCES members(member_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='지출별 멤버 부담 금액';

-- 5. settlements (정산 송금 목록 테이블)
CREATE TABLE settlements (
                             settlement_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '정산 ID',
                             room_id BIGINT NOT NULL COMMENT '소속 방 ID',
                             sender_id BIGINT NOT NULL COMMENT '돈을 보내야 하는 사람 (송금자 ID)',
                             receiver_id BIGINT NOT NULL COMMENT '돈을 받아야 하는 사람 (수신자 ID)',
                             amount DECIMAL(12,2) NOT NULL COMMENT '송금할 금액',
                             is_done BOOLEAN NOT NULL DEFAULT FALSE COMMENT '송금 완료 여부',
                             CONSTRAINT fk_settlements_room FOREIGN KEY (room_id)
                                 REFERENCES rooms(room_id) ON DELETE CASCADE,
                             CONSTRAINT fk_settlements_sender FOREIGN KEY (sender_id)
                                 REFERENCES members(member_id) ON DELETE CASCADE,
                             CONSTRAINT fk_settlements_receiver FOREIGN KEY (receiver_id)
                                 REFERENCES members(member_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='최종 정산 송금 목록';