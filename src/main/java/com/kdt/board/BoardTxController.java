package com.kdt.board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * [05 트랜잭션 + AOP + 프록시] BoardTxController
 *
 * 트랜잭션: Member+Post 저장을 "한 묶음" — 성공 COMMIT / 예외 ROLLBACK
 * AOP: @Transactional·로그를 Service 코드 밖에 두고, 프록시가 끼워 넣음
 *
 * [AOP 프록시] ← Spring이 BoardTxService 빈을 감싼 문지기
 *   Controller → 프록시(Advice: 트랜잭션) → 원본 registerOK
 *   확인: GET /tx/proxy → 이름에 $$SpringCGLIB$$
 *   함정: this.registerFail() 자기호출 → 문지기 우회 (/tx/self)
 *
 * [JPA 프록시] ← Post.member LAZY 껍데기 (연관 지연 로딩)
 *   AOP 프록시와 다름! /posts/read-in-tx 에서
 *   AOP가 영속(트랜잭션) 연 뒤에야 JPA LAZY SELECT 성공
 *
 * /tx/ok·fail / self / open / proxy / dirty / flushed-fail / posts/read-in-tx
 */
@RestController
public class BoardTxController {

    private final BoardTxService boardTxService;

    @Autowired
    public BoardTxController(BoardTxService boardTxService) {
        // 주입되는 건 원본이 아니라 AOP 프록시 인스턴스
        this.boardTxService = boardTxService;
    }

    @GetMapping("/tx/ok")
    public String ok() {
        return this.boardTxService.registerOK();
    }

    /** RuntimeException → 프록시가 ROLLBACK */
    @GetMapping("/tx/fail")
    public String txFail() {
        try {
            return this.boardTxService.registerFail();
        } catch (IllegalStateException ex) {
            return "rolled " + ex.getMessage();
        }
    }

    /** self-invocation: 프록시 미경유 → 롤백 안 될 수 있음 */
    @GetMapping("/tx/self")
    public String self() {
        try {
            return this.boardTxService.failBySelf();
        } catch (IllegalStateException ex) {
            return "left " + ex.getMessage();
        }
    }

    /** @Transactional 없음 → 예외 전 save 가 남을 수 있음 */
    @GetMapping("/tx/open")
    public String open() {
        try {
            return this.boardTxService.registerOpen();
        } catch (IllegalStateException ex) {
            return "left " + ex.getMessage();
        }
    }

    /** AOP 프록시 FQCN 확인 */
    @GetMapping("/tx/proxy")
    public String proxy() {
        return this.boardTxService.getClass().getName();
    }

    /** 더티 체킹: 영속 set → 커밋 때 UPDATE */
    @GetMapping("/tx/dirty")
    public String dirty() {
        return this.boardTxService.registerDirty();
    }

    /** flush(SQL 전송) ≠ COMMIT. 예외 시 flush분도 ROLLBACK */
    @GetMapping("/tx/flushed-fail")
    public String flushedFail() {
        try {
            return this.boardTxService.registerFlushFail();
        } catch (IllegalStateException ex) {
            return "rolled " + ex.getMessage();
        }
    }

    /** AOP 프록시(영속 ON) + JPA 프록시(LAZY getMember) */
    @GetMapping("/posts/read-in-tx")
    public String readInTx() {
        return this.boardTxService.findTitleWithLoginId(1L);
    }
}
