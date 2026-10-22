package com.kdt.board;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/*
 * =========================================================
 * [AOP] TxProbeAspect — 학습용 애스펙트
 * =========================================================
 *
 * ★ 왜 코드는 짧은데 개념이 많나?
 *   이 파일은 "할 일(println)"보다
 *   "언제·어디에 붙일지"를 선언하는 설정에 가깝다.
 *   실제 연결(위빙)과 실행은 앱 기동·/tx/ok 요청 때 일어난다.
 *
 * -------------------------------------------------
 * 1) 이 파일에 "적혀 있는 것" (정적 = 설계도)
 * -------------------------------------------------
 *   @Aspect + @Component  → 애스펙트 빈으로 등록
 *   @Before("execution(...registerOK..)") → 포인트컷 + 시점(직전)
 *   beforeOk(JoinPoint)   → 어드바이스 메서드 (본문은 println뿐)
 *
 * -------------------------------------------------
 * 2) 런타임에 "일어나는 것" (동적)
 * -------------------------------------------------
 *   [위빙 Weaving] Advice 를 대상 호출 경로에 엮어 넣는 과정
 *   - Spring AOP = 보통 런타임 위빙
 *   - 기동 시: BoardTxService 원본 위 프록시 생성
 *             → 이 Aspect·@Transactional 을 프록시에 연결 (= 위빙)
 *   - 요청 시: /tx/ok → 프록시가 Advice 실행 → 원본 registerOK
 *
 *   비유) 설계도(이 클래스) ≠ 옷에 실 꿰매기(위빙) ≠ 입고 나가기(/tx/ok)
 *
 * -------------------------------------------------
 * 3) 용어 한 줄씩 (이 클래스 기준)
 * -------------------------------------------------
 *   애스펙트   : TxProbeAspect 전체
 *   포인트컷   : execution(* ...BoardTxService.registerOK(..))
 *   어드바이스 : beforeOk() — registerOK 직전 이름 출력
 *   조인포인트 : 실제로 가로챈 그 registerOK 호출
 *   위빙       : 위 규칙을 프록시에 연결하는 일 (기동 시)
 *
 *   execution 읽기)
 *     *  = 반환 타입 무관 / (..) = 인자 0개 이상
 *     registerOK 만 대상 → registerFail 에는 안 붙음
 *
 * -------------------------------------------------
 * 4) /tx/ok 확인 순서
 * -------------------------------------------------
 *   Controller → AOP 프록시
 *     → TxProbeAspect (@Before)  "aspect registerOK"
 *     → @Transactional Advice    Creating… / Committing…
 *     → 원본 registerOK 본문
 *
 * [의존] build.gradle → spring-boot-starter-aspectj
 * @Transactional(트랜잭션 Aspect) 과 별개지만, 둘 다 AOP·위빙으로 붙음
 */
@Aspect
@Component
public class TxProbeAspect {

    /*
     * beforeOk — 어드바이스 본체 (직접 하는 일은 println 한 줄)
     *
     * 층만 구분하면 됨:
     *   @Before + execution  → 언제·어느 메서드 (위빙될 규칙)
     *   beforeOk             → 그때 실행할 코드
     *   JoinPoint            → 프레임워크가 넣는 "지금 호출 정보" (new 안 함)
     *   getName()            → 메서드 이름만 꺼냄 → "registerOK"
     *
     * 한 줄: registerOK 들어가기 전에 이름 찍기 (위빙된 @Before Advice)
     */
    @Before("execution(* com.kdt.board.BoardTxService.registerOK(..))")
    public void beforeOk(JoinPoint joinPoint) {
        System.out.println("aspect " + joinPoint.getSignature().getName());
    }

    @Before("@within(org.springframework.stereotype.Service)")
    public void beforeOk2(JoinPoint joinPoint) {
        System.out.println("aspect " + joinPoint.getSignature().getName());
    }

    /*
     * beforeOkName — BoardTxController.ok 만 (Service 저장 메서드 아님)
     * GET /tx/ok → "probe ok"
     */
    @Before("execution(* com.kdt.board.BoardTxController.ok(..))")
    public void beforeOkName(JoinPoint joinPoint) {
        System.out.println("probe " + joinPoint.getSignature().getName());
    }
}
