package com.kdt.board;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/*
 * [07 AOP] HelloAroundAspect — 초보용 Around
 *
 * AOP = 본업(message→kimbap)과 곁다리(로그)를 분리
 *
 * 용어)
 *   Aspect    : 이 클래스 전체 (@Aspect + @Component 로 Bean)
 *   Advice    : aroundMessage() — 실제로 끼워 넣는 코드
 *   Pointcut  : execution(* ...HelloService.message()) — 어디에 붙일지
 *   JoinPoint : 지금 가로챈 그 message() 호출
 *   Proxy     : HelloService 빈을 감싼 문지기. Controller는 프록시를 호출
 *   proceed() : "원래 message() 실행" 스위치 (안 부르면 kimbap 안 나옴)
 *
 * 흐름) /hello → 프록시 → begin → proceed(kimbap) → end → 응답
 * 함정) 같은 클래스 this.message() 는 프록시 안 탐 → AOP 미적용
 *
 * ※ @Transactional 도 AOP Advice (트랜잭션 시작/커밋) — BoardTxService
 */
@Aspect
@Component
public class HelloAroundAspect {

    @Around("execution(* com.kdt.board.HelloService.message())")
    public Object aroundMessage(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("around begin");
        Object body = joinPoint.proceed();
        System.out.println("around end");
        return body;
    }
}
