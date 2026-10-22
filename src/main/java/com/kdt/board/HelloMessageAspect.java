package com.kdt.board;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class HelloMessageAspect {

    @Around("execution(* com.kdt.board.HelloService.message2(..))")
    public Object aroundMessage2(ProceedingJoinPoint joinPoint) throws Throwable {
       // System.out.println("AOP 실행 전: " + joinPoint.getSignature().getName());
        log.info("AOP 시작: " + joinPoint.getSignature().getName());
        Object body = joinPoint.proceed();
        // System.out.println("AOP 실행 후 : aop end");
        log.info("aop end");
        return body;
    }
}
