package com.kdt.board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * =========================================================
 * [트랜잭션 + AOP] BoardTxController — HTTP 진입점
 * =========================================================
 *
 * [개념] 트랜잭션
 * - 여러 DB 작업을 "전부 성공 또는 전부 실패"로 묶는 단위
 * - COMMIT / ROLLBACK — JpaTransactionManager (logging DEBUG 로 확인)
 *
 * [개념] AOP (Aspect-Oriented Programming, 관점 지향)
 * - 핵심 로직(회원·글 저장)과 횡단 관심사(트랜잭션·로그)를 분리
 * - Spring 은 AOP 프록시로 메서드 호출을 가로채 Advice 를 끼워 넣음
 *
 * [AOP 용어] — 강의 표와 이 프로젝트 매핑
 * ┌──────────────┬────────────────────────────┬─────────────────────────────┐
 * │ 용어         │ 의미                       │ 이번 예시                   │
 * ├──────────────┼────────────────────────────┼─────────────────────────────┤
 * │ 조인 포인트  │ 공통 로직을 끼울 수 있는   │ registerOK() 메서드 실행    │
 * │ (Join point) │ 실행 지점 (Spring=메서드)  │                             │
 * ├──────────────┼────────────────────────────┼─────────────────────────────┤
 * │ 포인트컷     │ 어떤 호출을 대상으로 할지  │ @Transactional 메서드 /     │
 * │ (Pointcut)   │ 고르는 조건                │ TxProbeAspect 의            │
 * │              │                            │ execution(...registerOK)    │
 * ├──────────────┼────────────────────────────┼─────────────────────────────┤
 * │ 어드바이스   │ 골라낸 호출에 실행할       │ 트랜잭션 begin/commit /     │
 * │ (Advice)     │ 공통 로직 (전·후·주변)     │ TxProbe: 메서드명 println   │
 * ├──────────────┼────────────────────────────┼─────────────────────────────┤
 * │ 애스펙트     │ 포인트컷 + 어드바이스를    │ Spring 트랜잭션 Aspect /    │
 * │ (Aspect)     │ 묶은 단위(클래스)          │ TxProbeAspect.java          │
 * └──────────────┴────────────────────────────┴─────────────────────────────┘
 * ※ /tx/ok → TxProbeAspect("aspect registerOK") + @Transactional Advice 둘 다 동작
 *
 * [프록시] @Transactional = 표시 / 프록시 = 실행
 * - 주입되는 boardTxService 는 원본이 아니라 AOP 프록시
 * - 외부 호출만 가로챔 → this.메서드() 는 미적용 (/tx/self)
 * - 왜 Service 에? Controller=HTTP / Service=유스케이스 경계
 * - 프록시가 연 영속 안에서만 더티·flush·LAZY (/posts/read-in-tx)
 *
 * [구분] AOP 프록시 vs JPA 프록시  ← 이름만 같고 역할이 다름
 * - AOP : Service 빈을 감쌈 → 트랜잭션 Advice (/tx/proxy → $$SpringCGLIB$$)
 * - JPA : 연관 엔티티 LAZY 껍데기 → getXxx() 때 SELECT (Post.member 주석)
 * - alike: 둘 다 "대신 서는 객체" / 다름: Bean 가로채기 vs 지연 로딩
 * - 같이 씀: /posts/read-in-tx — AOP가 영속 열기 → 그 안 JPA LAZY SELECT
 *
 * 호출 순서) Controller → 프록시(Advice) → 원본 Service → COMMIT/ROLLBACK
 * 실습) /tx/ok·fail(프록시O) / self(우회) / open(애노테이션無) / proxy(FQCN)
 *       /tx/dirty · flushed-fail · /posts/read-in-tx
 */
@RestController
public class BoardTxController {

    private final BoardTxService boardTxService;

    @Autowired
    public BoardTxController(BoardTxService boardTxService) {
        // Spring 이 주입하는 건 원본이 아니라 AOP 프록시 인스턴스
        this.boardTxService = boardTxService;
    }

    /*
     * [/tx/ok] 정상 커밋 + TxProbeAspect 확인
     * - beforeOkName → "probe ok" (Controller.ok)
     * - beforeOk → "aspect registerOK" (Service)
     * - @Transactional → registerOK 본문 → COMMIT
     */
    @GetMapping("/tx/ok")
    public String ok() {
        return this.boardTxService.registerOK();
    }

    /*
     * [/tx/fail] 롤백 확인
     * [개념] Spring @Transactional 기본: RuntimeException 이면 ROLLBACK
     *        (jakarta.transaction.Transactional 은 기본이 모든 Exception — 패키지 다름)
     * 흐름) 프록시 시작 → registerFail → save 후 예외 → ROLLBACK
     *      → catch → "rolled {loginId}" (DB에는 안 남음)
     */
    @GetMapping("/tx/fail")
    public String txFail() {
        try {
            return this.boardTxService.registerFail();
        } catch (IllegalStateException ex) {
            return "rolled " + ex.getMessage();
        }
    }

    /*
     * [/tx/self] self-invocation (자기 호출) 함정
     * [개념] @Transactional 은 프록시가 가로챌 때만 동작
     *        failBySelf → this.registerFail() 은 같은 객체 내부 호출
     *        → 프록시 미경유 → 애노테이션 무시 → 롤백 안 됨
     * 흐름) 프록시(failBySelf, tx 없음) → this.registerFail() → 데이터 남을 수 있음
     *      → catch → "left {loginId}"
     */
    @GetMapping("/tx/self")
    public String self() {
        try {
            return this.boardTxService.failBySelf();
        } catch (IllegalStateException ex) {
            return "left " + ex.getMessage();
        }
    }

    /*
     * [/tx/open] 트랜잭션 미사용
     * [개념] @Transactional 이 없으면 "한 일"로 묶이지 않음
     *        save 가 각자 처리되어 예외 전에 넣은 데이터가 남을 수 있음 (부분 반영)
     * 흐름) registerOpen → Member save → 예외 → "left {loginId}"
     */
    @GetMapping("/tx/open")
    public String open() {
        try {
            return this.boardTxService.registerOpen();
        } catch (IllegalStateException ex) {
            return "left " + ex.getMessage();
        }
    }

    /*
     * [/tx/proxy] AOP 프록시 존재 확인
     * [개념]
     * - getClass() : 객체의 런타임 Class 메타데이터
     * - getName()  : FQCN 문자열 (패키지+클래스명)
     * - BoardTxService$$SpringCGLIB$$... 이면 CGLIB 프록시가 끼어 있는 상태
     * - 프록시가 있으므로 외부 호출만 @Transactional 적용, this.메서드() 는 미적용
     * 흐름) boardTxService.getClass().getName() → 프록시 FQCN(Fully Qualified Class Name) 문자열 응답
     */
    @GetMapping("/tx/proxy")
    public String proxy() {
        return this.boardTxService.getClass().getName();
    }

    /*
     * [/tx/dirty] 더티 체킹 — 프록시가 연 영속(Advice 구간) 안에서 set 만으로 UPDATE
     */
    @GetMapping("/tx/dirty")
    public String dirty() {
        return this.boardTxService.registerDirty();
    }

    /*
     * [/tx/flushed-fail] flush 후 예외 → 프록시 Advice 가 ROLLBACK (flush 분도 취소)
     */
    @GetMapping("/tx/flushed-fail")
    public String flushedFail() {
        try {
            return this.boardTxService.registerFlushFail();
        } catch (IllegalStateException ex) {
            return "rolled " + ex.getMessage();
        }
    }

    /*
     * [/posts/read-in-tx] AOP 프록시 + JPA 프록시가 같이 쓰이는 다리
     *
     * 1) AOP 프록시: @Transactional(readOnly) Advice → 트랜잭션·영속 열림
     * 2) JPA 프록시: post.getMember() LAZY 껍데기 → getLoginId() 때 SELECT
     * → AOP가 문을 연 동안에만 JPA LAZY 가 성공
     *
     * open-in-view:false 면 Controller 단독 LAZY 는 예외
     * → Service @Transactional(이 경로) 필요
     */
    @GetMapping("/posts/read-in-tx")
    public String readInTx() {
        Long postId = 1L;
        return this.boardTxService.findTitleWithLoginId(postId);
    }
}
