package com.kdt.board;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/*
 * =========================================================
 * [TxOutcomeAspect] 메서드가 "어떻게 끝났는지" 보는 AOP 연습
 * =========================================================
 *
 * 대상 메서드)
 *   BoardTxService.registerOK(..)   → 정상적으로 String 반환
 *   BoardTxService.registerFail(..) → 중간에 예외를 던짐
 *
 * 이 클래스에서 쓰는 Advice 4종류)
 *   @AfterReturning → 정상 반환했을 때만 실행 (반환값도 받을 수 있음)
 *   @AfterThrowing  → 예외가 났을 때만 실행 (예외 객체도 받을 수 있음)
 *   @After          → 성공이든 실패든, 메서드가 끝나면 항상 실행 (finally 느낌)
 *   @Around         → 메서드 앞·뒤를 감싸서 직접 제어 (여기선 registerFail만)
 *
 * 쉬운 비유)
 *   registerOK/Fail = 시험 보는 학생
 *   @AfterReturning = 시험 통과했을 때 "점수 몇 점?" 확인
 *   @AfterThrowing  = 시험 중 사고 났을 때 "무슨 사고?" 확인
 *   @After          = 시험장 나와서 "끝났네" 체크 (통과/사고 둘 다)
 *   @Around         = 시험 시작 스톱워치 누르고, 끝나면 시간 재기
 *
 * ★ 순서 감각 (성공일 때)
 *   (Around 있으면) Around 앞 → 본문 → Around 정상 끝
 *   → @AfterReturning → @After → (Around 의 finally)
 *
 * ★ 순서 감각 (예외일 때)
 *   (Around 있으면) Around 앞 → 본문 예외
 *   → @AfterThrowing → @After → (Around 의 finally) → 예외는 위로 다시 전달
 *
 * /tx/ok   → registerOK   → returned + ok 로그
 * /tx/fail → registerFail → around + failed + ok 로그 (returned 는 안 탐)
 */
@Component // Spring Bean 으로 등록해야 Aspect 가 동작함
@Aspect    // "이 클래스는 AOP Aspect 야" 표시
@Slf4j     // log.info(...) 쓰려고 Lombok 이 logger 를 만들어 줌
public class TxOutcomeAspect {

    /*
     * [AfterReturning] 정상 종료(return) 했을 때만 호출
     *
     * pointcut : registerOK 또는 registerFail 실행
     * returning = "seen" : 메서드가 return 한 값을 파라미터 seen 으로 받음
     *   → 파라미터 이름(seen)과 returning 값이 같아야 연결됨
     *
     * registerFail 은 예외를 던지므로 여기로 안 옴
     * registerOK 은 "ok" 같은 문자열을 반환 → seen 에 그 값이 들어옴
     */
    @AfterReturning(pointcut = "execution (* com.kdt.board.BoardTxService.registerOK(..)) || " +
            "execution(* com.kdt.board.BoardTxService.registerFail(..))", returning = "seen")
    public void returned(JoinPoint joinPoint, Object seen) {
        // joinPoint = "방금 실행된 메서드 정보" (이름, 인자 등)
        // seen      = 그 메서드의 반환값
        log.info("outcome returned " + joinPoint.getSignature().getName()+ " " + seen);

    }

    /*
     * [AfterThrowing] 예외가 발생했을 때만 호출
     *
     * throwing = "ex" : 던져진 예외를 파라미터 ex 로 받음
     * 파라미터 타입이 IllegalStateException → 이 타입(및 하위) 예외일 때만 이 Advice 실행
     *   (다른 종류의 예외면 이 메서드는 안 탐)
     *
     * registerOK 은 정상 반환이라 여기로 안 옴
     * registerFail 은 IllegalStateException 을 던지므로 여기로 옴
     */
    @AfterThrowing(pointcut = "execution (* com.kdt.board.BoardTxService.registerOK(..)) || " +
            "execution(* com.kdt.board.BoardTxService.registerFail(..))", throwing = "ex")
    public void failed(JoinPoint joinPoint, IllegalStateException ex) {
        log.info("outcome threw" + joinPoint.getSignature().getName()+ " " + ex.getMessage());
    }

    /*
     * [After] 성공/실패 상관없이 메서드가 끝나면 항상 호출
     *   = try-finally 의 finally 와 비슷한 위치
     *
     * 주의: After 는 "끝났다"만 알려 줌
     *       반환값(@AfterReturning)이나 예외(@AfterThrowing)는 여기서 못 봄
     */
    @After("execution (* com.kdt.board.BoardTxService.registerOK(..)) || " +
            "execution(* com.kdt.board.BoardTxService.registerFail(..))")
    public void ok(JoinPoint joinPoint) {
        log.info("outcome finished" + joinPoint.getSignature().getName());
    }

    /*
     * [Around] registerFail 만 감쌈 (앞·뒤 + 소요 시간 측정)
     *
     * ProceedingJoinPoint.proceed()
     *   - 원래 메서드(registerFail)를 이어서 실행하는 스위치
     *   - 이걸 안 부르면 본문은 실행되지 않음
     *
     * try / finally
     *   - registerFail 은 예외를 던지므로 "failure around normal end" 는 보통 안 찍힘
     *   - 그래도 finally 는 항상 실행 → 걸린 시간(elapsedNs)은 예외여도 찍힘
     *   - 예외는 그대로 밖으로 다시 나가서 @AfterThrowing / @After 도 이어서 동작
     */
    @Around("execution(* com.kdt.board.BoardTxService.registerFail(..))")
    public Object aroundFail(ProceedingJoinPoint joinPoint) throws Throwable {
        long started = System.nanoTime(); // 시작 시각 (나노초)
        log.info("failure around begin");

        try {
            Object result = joinPoint.proceed(); // 실제 registerFail() 실행
            log.info("failure around normal end"); // 예외 없이 끝나면 여기
            return result;
        }finally {
            // 성공이든 예외든 무조건 실행
            long elapsed = System.nanoTime() - started;
            log.info("failure around finally elapsedNs " + elapsed + " ns");
        }
    }

}
