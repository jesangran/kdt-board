package com.kdt.board;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/*
 * [mission07] MissionFlowAspect — Around 하나로 성공/실패/시간
 *
 * 요구: @Aspect+@Component, observe 한 개,
 *   대상 HelloService.message() || BoardTxService.registerFail(..) 만
 *   begin → proceed 1회 → returned(결과 그대로) / threw(IllegalState만 catch·재throw)
 *   finally elapsedNs (return·새 throw·proceed 금지)
 *
 * Around = try/catch/finally 를 Advice에 직접 (TxOutcomeAspect 분리형과 대비)
 * /hello → begin·returned·finally / /tx/fail → begin·threw·finally
 */
@Aspect
@Component
public class MissionFlowAspect {

    @Around("execution(* com.kdt.board.HelloService.message()) || "
            + "execution(* com.kdt.board.BoardTxService.registerFail(..))")
    public Object observe(ProceedingJoinPoint joinPoint) throws Throwable {
        String name = joinPoint.getSignature().getName();
        long started = System.nanoTime();
        System.out.println("mission07 begin " + name);

        try {
            Object result = joinPoint.proceed();
            System.out.println("mission07 returned " + name + " " + result);
            return result;
        } catch (IllegalStateException ex) {
            System.out.println("mission07 threw " + name + " " + ex.getMessage());
            throw ex;
        } finally {
            long elapsed = System.nanoTime() - started;
            System.out.println("mission07 finally " + name + " elapsedNs=" + elapsed);
        }
    }
}
