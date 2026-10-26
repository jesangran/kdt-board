package com.kdt.board;

/*
 * ============================================================================
 * [10] KimbapPrice — 작은 계산 규칙 (11차시 TDD 워밍업)
 * ============================================================================
 * HTTP/DB 없이 "가격·수량"만. 테스트(KimbapPriceTest)를 먼저 쓰는 연습용.
 *
 * won()   → 단가 4500
 * total(n) → n >= 1 이면 4500 * n / 아니면 IllegalArgumentException("count")
 * ============================================================================
 */
public class KimbapPrice {

    /** 김밥 1줄 단가(원) */
    public int won() {
        return 4500;
    }

    /**
     * 수량만큼 합계. 단가 변경 시 won()만 고치면 되도록 won() * count 사용.
     *
     * @param count 1 이상
     * @throws IllegalArgumentException count &lt; 1 일 때 메시지 "count"
     */
    public int total(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("count");
        }
        return this.won() * count;
    }
}
