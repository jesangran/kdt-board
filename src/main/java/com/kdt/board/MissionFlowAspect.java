package com.kdt.board;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/*
 * =========================================================
 * [MissionFlowAspect] mission07 — Around 하나로 성공/실패/시간 관찰
 * =========================================================
 *
 * -------------------------------------------------
 * [요구사항 요약]
 * -------------------------------------------------
 * 1) Spring Bean 으로 등록 (@Aspect + @Component)
 * 2) Around 메서드는 observe(...) 한 개만
 * 3) 대상은 딱 두 메서드만 (|| 로 연결)
 *      - HelloService.message()
 *      - BoardTxService.registerFail(..)
 *    ※ 컨트롤러·패키지 전체·registerOK·registerOpen 은 제외
 * 4) 메서드명 → name, 시작 시각 → nanoTime, begin 로그
 * 5) try 안에서 proceed() 는 한 번만
 * 6) 정상 반환 → returned 로그 + 받은 결과 그대로 return
 *    (kimbap 등 새 값을 만들어 반환하지 않음)
 * 7) IllegalStateException catch → threw 로그 + 같은 ex 를 다시 throw
 *    (새 예외로 바꾸거나 null/성공 문자열 반환 금지)
 * 8) 다른 타입 예외는 catch 하지 않음 → throws 로 그대로 전달
 * 9) finally → elapsedNs 로그 (return / 새 throw / proceed 금지)
 * 10) 이 파일만 추가. 다른 Aspect·컨트롤러·트랜잭션 규칙은 수정하지 않음
 *
 * -------------------------------------------------
 * [개념] Around = try / catch / finally 를 Advice 안에 직접 쓰는 것
 * -------------------------------------------------
 *   @Before / @AfterReturning / @AfterThrowing / @After
 *     → 시점마다 Advice 를 따로 둠 (TxOutcomeAspect 방식)
 *
 *   @Around 한 개
 *     → 앞·정상·예외·마무리를 한 메서드에서 전부 처리
 *     → proceed() 가 "원래 메서드 실행" 스위치
 *
 *   비유)
 *     proceed() = 본 경기 시작 휘슬
 *     begin     = 경기 전 체크
 *     returned  = 정상 종료 + 점수 확인
 *     threw     = 파울(IllegalStateException) 기록 후 그대로 심판에게 넘김
 *     finally   = 성공/실패 상관없이 스톱워치 종료
 *
 * -------------------------------------------------
 * [포인트컷] execution 문법
 * -------------------------------------------------
 *   execution(* com.kdt.board.HelloService.message())
 *     || execution(* com.kdt.board.BoardTxService.registerFail(..))
 *
 *   *            → 반환 타입 무관
 *   message()    → 인자 없는 message 만
 *   registerFail(..) → 인자 개수·타입 무관
 *   ||           → 둘 중 하나만 맞아도 Advice 적용
 *
 * -------------------------------------------------
 * [확인]
 * -------------------------------------------------
 *   GET /hello   → message()      → begin → returned(kimbap) → finally
 *   GET /tx/fail → registerFail() → begin → threw → finally (예외는 그대로 전달)
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
