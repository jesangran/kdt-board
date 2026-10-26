package com.kdt.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * ============================================================================
 * [10] KimbapPriceTest — 계산 규칙 + 잘못된 수량 (순수 단위 테스트)
 * ============================================================================
 * TDD 순서 연습: 실패하는 테스트 → 최소 구현 → 통과 → 다음 규칙 → 리팩터
 * @BeforeEach: 각 @Test 전에 price 새로 준비 (테스트 간 상태 격리)
 * ============================================================================
 */
class KimbapPriceTest {

    private KimbapPrice price;

    @BeforeEach
    void setUp() {
        this.price = new KimbapPrice();
    }

    @Test
    void won_is_4500() {
        assertEquals(4500, this.price.won());
    }

    @Test
    void two_packs_is_9000() {
        assertEquals(9000, this.price.total(2));
    }

    /** 0·음수는 같은 분기(count &lt; 1). assertThrows + 메시지 "count" */
    @Test
    void total_rejects_non_positive_counts() {
        IllegalArgumentException zero = assertThrows(
                IllegalArgumentException.class, () -> this.price.total(0));
        assertEquals("count", zero.getMessage());
        IllegalArgumentException negative = assertThrows(
                IllegalArgumentException.class, () -> this.price.total(-1));
        assertEquals("count", negative.getMessage());
    }
}
