package com.kdt.board;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/*
 * =========================================================
 * [AOP / Around 용어 한줄 정리]
 * =========================================================
 * 1. AOP(관점 지향 프로그래밍)
 *    - 본래 비즈니스 로직과 "곁다리 관심사"(로그, 트랜잭션, 보안 등)를 분리
 *    - HelloService는 message()만 신경 쓰고, 앞·뒤 처리는 Aspect가 담당
 *
 * 2. Aspect(애스펙트)
 *    - "언제(포인트컷) + 무엇을(어드바이스)" 묶은 모듈
 *    - 이 클래스 전체가 Aspect. @Aspect 필수
 *
 * 3. Advice(어드바이스)
 *    - 실제로 끼워 넣는 코드
 *    - 종류: @Before / @After / @Around / @AfterReturning / @AfterThrowing
 *    - @Around = 메서드 실행 전·후를 모두 감싸서 제어 (가장 강력)
 *
 * 4. Pointcut(포인트컷)
 *    - "어느 조인포인트에 Advice를 적용할지" 고르는 표현식
 *    - 여기: execution(* com.kdt.board.HelloService.message())
 *
 * 5. Join Point(조인포인트)
 *    - Advice가 끼어들 수 있는 지점 (Spring AOP에서는 주로 메서드 실행)
 *    - 예: HelloService.message() 가 실제로 호출되는 순간
 *
 * 6. ProceedingJoinPoint
 *    - @Around 전용. 대상 메서드를 직접 실행할 수 있는 핸들
 *    - joinPoint.proceed() = 원래 메서드(message)를 이어서 호출
 *    - proceed()를 안 부르면 본문 메서드는 실행되지 않음
 *
 * 7. Proxy(프록시)
 *    - Spring AOP는 대상 Bean을 감싼 프록시를 만들어 호출을 가로챔
 *    - Controller → (프록시) → Aspect 앞 → 실제 Service → Aspect 뒤
 *    - 같은 클래스 안 this.message() 자기 호출은 프록시를 안 타서 AOP 미적용
 *
 * 8. @Component
 *    - Aspect도 Spring Bean이어야 컨테이너가 인식·적용함
 *    - @Aspect만 있고 Bean이 아니면 동작하지 않음
 *
 * 9. 성능
 *    - 호출마다 프록시 경유 + Advice 실행 → 약간의 오버헤드 가능
 *    - 보통은 비즈니스 로직·DB·네트워크에 비해 미미함
 *    - 다만 포인트컷이 너무 넓거나(@Around 남발), Advice 안이 무거우면
 *      (로깅 I/O, 복잡한 연산 등) 체감 저하가 날 수 있음
 *    - 실무 팁: 필요한 조인포인트만 좁게 잡고, Advice는 가볍게 유지
 *
 * =========================================================
 * [포인트컷 표현식 분해]
 *   execution(* com.kdt.board.HelloService.message())
 * =========================================================
 *   execution(...)  → 메서드 "실행" 시점에 매칭
 *   *                → 반환 타입 무관 (String, void, ...)
 *   HelloService     → 대상 클래스 (FQCN)
 *   message()        → 인자 없는 message 메서드만
 *
 * =========================================================
 * [호출 흐름]
 * =========================================================
 *   1) HelloController 가 helloService.message() 호출
 *   2) Spring 프록시가 가로챔 → aroundMessage() 진입
 *   3) "around begin" 출력  (본문 실행 전)
 *   4) joinPoint.proceed() → 실제 HelloService.message() 실행 → "kimbap"
 *   5) "around end" 출력    (본문 실행 후)
 *   6) return body          → 컨트롤러로 "kimbap" 전달
 */
@Aspect
@Component
public class HelloAroundAspect {

    @Around("execution(* com.kdt.board.HelloService.message())")
    public Object aroundMessage(ProceedingJoinPoint joinPoint) throws Throwable {
        // 본문(message) 실행 전
        System.out.println("around begin");

        // 원래 메서드 실행. 반환값이 body에 담김 ("kimbap")
        Object body = joinPoint.proceed();

        // 본문(message) 실행 후
        System.out.println("around end");

        // 호출자(Controller)에게 원래 반환값 그대로 전달
        return body;
    }
}
