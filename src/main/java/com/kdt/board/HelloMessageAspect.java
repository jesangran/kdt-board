package com.kdt.board;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** Around HelloService.message2 — /testhello */
@Aspect
@Component
@Slf4j
public class HelloMessageAspect {

    @Around("execution(* com.kdt.board.HelloService.message2(..))")
    public Object aroundMessage2(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("AOP 시작: " + joinPoint.getSignature().getName());
        Object body = joinPoint.proceed();
        log.info("aop end");
        return body;
    }
}
