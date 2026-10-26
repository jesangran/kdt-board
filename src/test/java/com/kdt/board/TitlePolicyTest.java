package com.kdt.board;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * TitlePolicy 동작을 검증하는 테스트.
 * TDD(Test-Driven Development, 테스트 주도 개발): 테스트를 먼저 쓰고, 통과하도록 구현한다.
 * - Red: 테스트 실패 (기대값 ≠ 실제값, 또는 기대 예외가 안 남)
 * - Green: 구현을 맞춰 테스트 통과
 * - Refactor: 통과한 채로 코드 정리
 */
public class TitlePolicyTest {

    // 정상: title이 "closed"이면 "ok"
    @Test
    void closedTitleOk() {
        // 1) 준비(Arrange)
        TitlePolicy policy = new TitlePolicy();
        // 2) 실행(Act) + 3) 검증(Assert): assertEquals(기대값, 실제값)
        assertEquals("ok", policy.check("closed"));
    }

    // 예외: 빈 문자열 "" → blank
    @Test
    void blankTitleIsRejected() {
        // 1) 준비(Arrange)
        TitlePolicy policy = new TitlePolicy();
        // 2)~3) assertThrows가 람다 (() -> ...) 를 감싸 실행
        //    기대 타입의 예외가 나면 error에 담고 통과, 아니면 실패
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> policy.check(""));
        // 4) 메시지까지 검증
        assertEquals("title is blank", error.getMessage());
    }

    // 예외: 공백만 → isBlank() true (isEmpty()였다면 통과했을 케이스)
    @Test
    void spaceOnlyIsBlank() {
        TitlePolicy policy = new TitlePolicy();
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> policy.check(" "));
        assertEquals("title is blank", error.getMessage());
    }

    // 예외: null → blank와 동일 메시지
    @Test
    void nullTitleIsBlank() {
        TitlePolicy policy = new TitlePolicy();
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> policy.check(null));
        assertEquals("title is blank", error.getMessage());
    }

    // 예외: 21자 → 길이 초과
    @Test
    void longTitleIsReject() {
        String title = "123456789012345678901";
        assertEquals(21, title.length());

        TitlePolicy policy = new TitlePolicy();
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> policy.check(title));
        assertEquals("title is long", error.getMessage());
    }

    // 정상: 19자 → ok (경계 아래)
    @Test
    void nineteenLetterIsOk() {
        String title = "abcdefghijklmnopqrs";
        assertEquals(19, title.length());

        TitlePolicy policy = new TitlePolicy();
        assertEquals("ok", policy.check(title));
    }

    // 정상: 20자 → ok (최대 길이 포함)
    @Test
    void twentyLetterIsOk() {
        String title = "abcdefghijklmnopqrst";
        assertEquals(20, title.length());

        TitlePolicy policy = new TitlePolicy();
        assertEquals("ok", policy.check(title));
    }

    // 예외 우선순위: 공백만 21자 → length보다 blank를 먼저 검사
    @Test
    void twentyOneSpacesPreferBlankError() {
        String title = "          " + "          " + " ";
        assertEquals(21, title.length());

        TitlePolicy policy = new TitlePolicy();
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> policy.check(title));
        assertEquals("title is blank", error.getMessage());
    }

    // 정상: 19자 + 공백 = 20자 → ok (끝 공백도 길이에 포함, blank은 아님)
    @Test
    void nineteenLetterAndSpaceIsOk() {
        String title = "abcdefghijklmnopqrs" + " ";
        assertEquals(20, title.length());

        TitlePolicy policy = new TitlePolicy();
        assertEquals("ok", policy.check(title));
    }

    // 예외: 20자 + 공백 = 21자 → long
    @Test
    void twentyLetterAndSpaceIsLong() {
        String title = "abcdefghijklmnopqrst" + " ";
        assertEquals(21, title.length());

        TitlePolicy policy = new TitlePolicy();
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> policy.check(title));
        assertEquals("title is long", error.getMessage());
    }
}
