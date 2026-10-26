package com.kdt.board;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/*
 * [07 AOP] TxOutcomeAspect — 종료 시점별 Advice
 * 대상: BoardTxService.registerOK / registerFail
 *
 * @AfterReturning  정상 return 만 (+반환값)
 * @AfterThrowing   예외만 (+예외 객체)
 * @After           성공/실패 항상 (finally 느낌)
 * @Around          앞·뒤 제어 (여기선 registerFail + 시간)
 *
 * 성공: Around앞 → 본문 → Around끝 → Returning → After
 * 예외: Around앞 → 예외 → Throwing → After → (Around finally) → 예외 전파
 * /tx/ok → returned / /tx/fail → around+threw (returned 없음)
 */
@Component
@Aspect
@Slf4j
public class TxOutcomeAspect {

    @AfterReturning(pointcut = "execution (* com.kdt.board.BoardTxService.registerOK(..)) || "
            + "execution(* com.kdt.board.BoardTxService.registerFail(..))", returning = "seen")
    public void returned(JoinPoint joinPoint, Object seen) {
        log.info("outcome returned " + joinPoint.getSignature().getName() + " " + seen);
    }

    @AfterThrowing(pointcut = "execution (* com.kdt.board.BoardTxService.registerOK(..)) || "
            + "execution(* com.kdt.board.BoardTxService.registerFail(..))", throwing = "ex")
    public void failed(JoinPoint joinPoint, IllegalStateException ex) {
        log.info("outcome threw" + joinPoint.getSignature().getName() + " " + ex.getMessage());
    }

    @After("execution (* com.kdt.board.BoardTxService.registerOK(..)) || "
            + "execution(* com.kdt.board.BoardTxService.registerFail(..))")
    public void ok(JoinPoint joinPoint) {
        log.info("outcome finished" + joinPoint.getSignature().getName());
    }

    @Around("execution(* com.kdt.board.BoardTxService.registerFail(..))")
    public Object aroundFail(ProceedingJoinPoint joinPoint) throws Throwable {
        long started = System.nanoTime();
        log.info("failure around begin");
        try {
            Object result = joinPoint.proceed();
            log.info("failure around normal end");
            return result;
        } finally {
            long elapsed = System.nanoTime() - started;
            log.info("failure around finally elapsedNs " + elapsed + " ns");
        }
    }
}
