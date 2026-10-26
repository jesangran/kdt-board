package com.kdt.board;

/*
 * static import(정적 임포트)란?
 * ----------------------------------------------------------------------------
 * 보통은 Assertions.assertEquals(...)처럼 클래스 이름을 붙여 호출합니다.
 * `import static org.junit.jupiter.api.Assertions.assertEquals;`를 쓰면
 * assertEquals(...)만으로 호출할 수 있습니다.
 *
 * 테스트 코드에서는 검증 구문이 반복되기 때문에 static import를 쓰면
 * "무엇을 검증하는지"가 더 잘 보입니다.
 * (IDE의 자동 완성으로 static import를 추가할 수 있습니다.)
 */
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/*
 * ============================================================================
 * [10] HelloServiceTest
 * HelloService의 message()가 "kimbap"을 돌려주는지 확인하는 순수 단위 테스트
 * ============================================================================
 *
 * [위치] src/test/java — 앱 코드(src/main)와 분리. 패키지명은 같아도 폴더는 다름.
 *
 * [단위 테스트(unit test)란?]
 *   하나의 작은 단위(여기서는 HelloService의 메서드 하나)만 떼어 내서 검증합니다.
 *   스프링, DB, 웹 서버, 네트워크를 전혀 사용하지 않습니다.
 *   그래서 매우 빠르고(수 밀리초), 실패 원인이 명확합니다.
 *
 * [JUnit 5]
 *   @Test 메서드를 찾아 대신 실행합니다. main() 불필요.
 *   assertEquals(기대, 실제) — 다르면 테스트 실패(AssertionFailedError).
 *
 * [AAA 패턴]
 *   Arrange(준비) → Act(실행) → Assert(검증)
 *
 * [실행] .\gradlew.bat test --tests "com.kdt.board.HelloServiceTest"
 * ============================================================================
 */
class HelloServiceTest {

    /*
     * @Test — JUnit이 이 메서드를 테스트로 인식.
     * 이름 message_returns_kimbap = "message() → kimbap" 계약을 문장처럼 표현.
     */
    @Test
    void message_returns_kimbap() {
        // Arrange(준비): 스프링 없이 실제 서비스를 직접 생성
        HelloService helloService = new HelloService();

        // Act + Assert: assertEquals(기대값, 실제값) — 순서 주의
        assertEquals("kimbap", helloService.message());
    }
}
