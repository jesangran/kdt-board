package com.kdt.board;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/*
 * [07 AOP] TxProbeAspect — @Before
 *
 * 위빙(Weaving): 기동 시 이 Advice를 BoardTxService 프록시에 "꿰매는" 일
 * 설계도(이 클래스) ≠ 위빙(기동) ≠ 실행(/tx/ok 요청)
 *
 * /tx/ok: TxProbe @Before → @Transactional Advice → registerOK
 * execution(...registerOK) 만 대상 (registerFail 에는 안 붙음)
 */
@Aspect
@Component
public class TxProbeAspect {

    @Before("execution(* com.kdt.board.BoardTxService.registerOK(..))")
    public void beforeOk(JoinPoint joinPoint) {
        System.out.println("aspect " + joinPoint.getSignature().getName());
    }

    @Before("@within(org.springframework.stereotype.Service)")
    public void beforeOk2(JoinPoint joinPoint) {
        System.out.println("aspect " + joinPoint.getSignature().getName());
    }

    /** Controller.ok 만 — GET /tx/ok → "probe ok" */
    @Before("execution(* com.kdt.board.BoardTxController.ok(..))")
    public void beforeOkName(JoinPoint joinPoint) {
        System.out.println("probe " + joinPoint.getSignature().getName());
    }
}
