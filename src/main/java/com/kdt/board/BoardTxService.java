package com.kdt.board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * =========================================================
 * [트랜잭션 + AOP] BoardTxService — 비즈니스 경계 (@Transactional)
 * =========================================================
 *
 * [개념] @Transactional = AOP 로 "트랜잭션 Advice" 를 붙이라는 표시
 * - 실제 시작/커밋/롤백: AOP 프록시 + JpaTransactionManager
 * - 기본 롤백: RuntimeException / Error
 * - jakarta.transaction.Transactional 과 패키지·기본값 다름 → Spring 쪽 사용
 * - 왜 Service? Repository=조각 / Service=유스케이스(트랜잭션 경계)
 *
 * [AOP 용어 → 이 클래스]
 * - 조인 포인트 : registerOK() 등 메서드 실행 순간
 * - 포인트컷   : @Transactional 메서드 (Spring) /
 *               TxProbeAspect 의 execution(...registerOK(..))
 * - 어드바이스 : 트랜잭션 begin → 원본 → commit/rollback /
 *               TxProbeAspect.beforeOk → "aspect registerOK" 출력
 * - 애스펙트   : Spring 트랜잭션 Aspect / TxProbeAspect.java
 * - /tx/ok 때: TxProbe(@Before) → 트랜잭션 Advice → registerOK 본문 순으로 겹침
 *
 * [프록시] 문지기 — Controller → [프록시 Advice] → 원본(이 클래스) 로직
 * - 기동: 원본(target) + 프록시 → 주입은 프록시 (/tx/proxy → $$SpringCGLIB$$)
 * - AOP 가 필요 없는 빈은 원본만
 * - this.메서드() = 원본 자기 호출 → 포인트컷 미적용 (/tx/self)
 * - open: 애노테이션 없음 → Advice 할 일 없음
 *
 * [프록시 ↔ 영속 ↔ LAZY]
 * - Advice 가 트랜잭션을 열어야 영속 유지 → 더티/flush/LAZY 가능
 * - findTitleWithLoginId: readOnly Advice 구간 안에서 getMember()
 * - AOP 프록시(이 빈) ≠ JPA 프록시(Post.member) — 구분 표는 BoardTxController / Post
 *
 * 실습) registerOK·Fail(적용) / failBySelf(우회) / registerOpen(無)
 *       registerDirty · registerFlushFail · findTitleWithLoginId
 *
 * [영속성 생명주기]
 *   비영속 ──save()/find()──▶ 영속 ──트랜잭션 끝──▶ 준영속
 * - 영속 구간을 만드는 것 = 프록시의 @Transactional Advice
 */
@Service
public class BoardTxService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    @Autowired
    public BoardTxService(MemberRepository memberRepository, PostRepository postRepository) {
        this.memberRepository = memberRepository;
        this.postRepository = postRepository;
    }

    /*
     * [/tx/ok] 정상 커밋 — AOP 조인 포인트 (registerOK 실행)
     * - TxProbeAspect: @Before execution(...registerOK) → "aspect registerOK"
     * - @Transactional: 트랜잭션 Advice → 시작~COMMIT
     * - Member + Post 를 한 트랜잭션으로 묶음
     */
    @Transactional
    public String registerOK() {
        // System.currentTimeMillis(): 1970-01-01 UTC 기준 밀리초 → loginId 유니크용
        String loginId = "kdt" + System.currentTimeMillis();

        Member member = new Member();
        member.setLoginId(loginId);
        member.setNickname("kimbap");
        Member saved = this.memberRepository.save(member);

        Post post = new Post();
        post.setTitle("closed");
        post.setBody("no class");
        post.setMember(saved);
        this.postRepository.save(post);

        return "ok" + loginId;
    }

    /*
     * [/tx/fail] 예외 → 롤백
     * [개념] save 가 두 번 있어도 같은 트랜잭션이면 예외 시 전부 취소
     *        IllegalStateException = RuntimeException → Spring 기본 롤백 대상
     * 흐름)
     *   1) 프록시: 트랜잭션 시작
     *   2) Member + Post save
     *   3) throw → 프록시 ROLLBACK (DB에 안 남음)
     */
    @Transactional
    public String registerFail() {
        String loginId = "kdt" + System.currentTimeMillis();

        Member member = new Member();
        member.setLoginId(loginId);
        member.setNickname("kimbap");
        Member saved = this.memberRepository.save(member);

        Post post = new Post();
        post.setTitle("closed");
        post.setBody("no class");
        post.setMember(saved);
        this.postRepository.save(post);

        System.out.println("tx fail" + loginId);

        throw new IllegalStateException(loginId);
    }

    /*
     * [/tx/self] self-invocation
     * [개념] 같은 클래스 안에서 this 로 @Transactional 메서드를 부르면
     *        프록시를 건너뛰어 트랜잭션이 시작되지 않음
     *        → registerFail 의 @Transactional 이 있어도 여기선 무효
     * 흐름)
     *   1) failBySelf 자체는 @Transactional 없음
     *   2) this.registerFail() → 프록시 미경유
     *   3) 롤백 안 됨 → 데이터가 남을 수 있음
     */
    public String failBySelf() {
        return this.registerFail();
    }

    /*
     * [/tx/open] 트랜잭션 선언 없음
     * [개념] @Transactional 이 없으면 "전부 성공/전부 실패" 보장이 없음
     *        예외 전에 실행된 save 는 커밋되어 남을 수 있음 (부분 반영)
     * 흐름)
     *   1) Member save
     *   2) throw IllegalStateException
     *   3) 묶인 트랜잭션 없음 → Member 가 DB에 남을 수 있음
     */
    public String registerOpen() {
        String loginId = "kdt" + System.currentTimeMillis();

        Member member = new Member();
        member.setLoginId(loginId);
        member.setNickname("kimbap");
        this.memberRepository.save(member);
        System.out.println("tx open" + loginId);

        throw new IllegalStateException(loginId);
    }

    /*
     * [/tx/dirty] 더티 체킹 + 영속성
     *
     * -------------------------------------------------
     * [영속화란?]
     * 자바 객체(Member)를 DB에 저장해서, 앱을 꺼도 데이터가 남게 하는 것
     *
     * [영속성 생명주기 전체 흐름]
     *   비영속 ──save()/find()──▶ 영속 ──트랜잭션 끝/detach──▶ 준영속
     *                              │
     *                              └──remove()──▶ 삭제
     *
     * [이 메서드에서의 상태 변화]
     * 1) 비영속 (new Member...)
     *    - 그냥 new 한 자바 객체. JPA가 아직 모름. DB와도 무관
     * 2) 영속   (save 직후 saved)
     *    - JPA(영속 컨텍스트)가 "이 객체는 내가 관리한다"고 붙잡음
     *    - DB와 연결, 처음 값을 스냅샷으로 기억
     *    - ★ 더티 체킹은 여기서만 동작
     * 3) setNickname → 여전히 영속 (상태 안 바뀜, 값만 바뀜)
     * 4) 메서드/트랜잭션 종료 후 → (보통) 준영속
     *    - 관리에서 벗어남. 이후 set 해도 자동 UPDATE 안 됨
     * 5) (참고) 삭제: remove()/delete() 하면 삭제 예약 → 커밋 때 DELETE
     *    - 이 메서드에선 사용 안 함
     *
     * ★ 더티 체킹 = 영속일 때만. 비영속·준영속에서는 set ≠ UPDATE
     * -------------------------------------------------
     *
     * [더티 체킹이란? — 한 줄]
     * JPA가 관리 중인(영속) 객체의 값이 처음이랑 달라졌는지 보고,
     * 끝나면 알아서 UPDATE SQL 을 날리는 것
     * (우리가 save() 를 한 번 더 안 불러도 됨)
     *
     * [쉬운 비유]
     * save = 시험지 제출(INSERT)
     * 영속 = 선생님이 아직 시험지를 붙잡고 채점 전
     * setNickname = 연필로 답만 고침 (이 순간엔 DB UPDATE 없음)
     * 커밋 = 제출 마감 → 선생님이 "원래랑 다르네" 하고 고친 답으로 반영(UPDATE)
     *
     * [이 코드 흐름]
     * 1) @Transactional → 영속 컨텍스트가 메서드 끝날 때까지 열려 있음
     * 2) new Member(...) → 비영속
     * 3) save(member) → INSERT + 영속 전환 (스냅샷: nickname=first)
     * 4) setNickname("second") → 자바 객체만 second (DB는 아직 first, SQL 없음)
     * 5) return 후 커밋 → JPA가 스냅샷과 비교 → 다름(dirty) → UPDATE → COMMIT
     *
     * [콘솔에서 보이는 순서]
     * Creating transaction → INSERT → dirty beforefirst → dirty aftersecond
     * → UPDATE (println 다음! set 직후가 아님) → Committing
     *
     * [헷갈리는 점]
     * - set 하는 순간에 UPDATE 가 나가는 게 아님 → 커밋 직전
     * - save 를 안 불러도 UPDATE 되는 이유 = 영속이라 JPA가 지켜보고 있어서
     * - 트랜잭션/영속이 아니면 set 만으로는 DB 안 바뀜
     */
    @Transactional
    public String registerDirty() {
        String loginId = "dirty" + System.currentTimeMillis();

        // 비영속 → (save) → 영속 → (커밋 후) → 준영속
        Member member = new Member(loginId, "first"); // 비영속
        Member saved = this.memberRepository.save(member); // 영속 + INSERT
        System.out.println("dirty before" + saved.getNickname());

        saved.setNickname("second"); // 여전히 영속, 값만 변경 → 커밋 때 UPDATE
        System.out.println("dirty after" + saved.getNickname());

        return "dirty " + saved.getLoginId(); // 종료 후 flush/COMMIT → 이후 준영속
    }

    /*
     * [/tx/flush-fail] flush ≠ 커밋 + 영속성 / 더티 체킹
     *
     * -------------------------------------------------
     * [flush 란? — 한 줄]
     * 영속 컨텍스트에 쌓여 있던 변경(INSERT/UPDATE)을
     * "지금 당장" DB로 SQL 전송하는 것
     * 단, 트랜잭션은 아직 안 끝남 → COMMIT 이 아님!
     *
     * [flush vs COMMIT vs ROLLBACK]
     * - flush   : SQL 을 DB에 보냄 (같은 트랜잭션 안, 아직 확정 X)
     * - COMMIT  : 트랜잭션 성공 종료 → 변경 확정
     * - ROLLBACK: 트랜잭션 실패 종료 → flush 로 보낸 것도 전부 취소
     *
     * [쉬운 비유]
     * save/set  = 시험지에 답 적기 (메모리/영속 컨텍스트)
     * flush     = 선생님에게 중간 제출 (DB에 SQL 감) — 아직 성적 확정 X
     * 예외/롤백 = "취소!" → 중간 제출도 없던 일로
     * 커밋      = 성적표 확정
     * -------------------------------------------------
     *
     * [영속성 생명주기 — 이 메서드]
     *   비영속 ──save──▶ 영속 ──set──▶ 영속(dirty) ──flush──▶ 영속(DB에 SQL 반영됨)
     *                                                              │
     *                                              throw ──▶ ROLLBACK (DB 원상복구)
     * 1) new Member → 비영속
     * 2) save → 영속 + INSERT (보통 이 시점 또는 flush 때 SQL)
     * 3) setNickname("after") → 여전히 영속, 값만 변경(dirty) — 아직 UPDATE 안 나갈 수 있음
     * 4) flush() → 더티 체킹 즉시 실행 → UPDATE SQL 을 DB로 보냄 (커밋은 아님)
     * 5) throw → 프록시 ROLLBACK → flush 한 INSERT/UPDATE 도 취소 → DB에 안 남음
     *
     * [registerDirty 와 비교]
     * - dirty : set 후 메서드 끝(커밋)에 UPDATE → 성공하면 DB에 남음
     * - flush-fail : set 후 flush 로 UPDATE를 먼저 보내지만, 예외라 ROLLBACK → DB에 안 남음
     * - 콘솔에 UPDATE 가 보여도 ≠ 최종 저장. flush 로그 ≠ 커밋 로그
     *
     * [이 코드 흐름]
     * 1) @Transactional → 트랜잭션 + 영속 컨텍스트 시작
     * 2) save → 영속
     * 3) setNickname → dirty
     * 4) flush → SQL 즉시 전송 (INSERT/UPDATE 콘솔에 보임)
     * 5) println → flush 는 정상 반환됐다는 확인
     * 6) throw IllegalStateException → ROLLBACK (flush 내용 포함 전부 취소)
     *
     * [콘솔에서 보이는 순서]
     * Creating transaction → INSERT → (flush 시) UPDATE
     * → flush returned ... → 예외 → Rolling back / rollback
     * → DB 조회하면 해당 loginId 없음 (flush 했어도 롤백됨)
     *
     * [헷갈리는 점]
     * - flush 했다고 저장된 게 아님 → COMMIT 해야 확정
     * - flush 후 예외 = SQL 로그는 있어도 최종 DB 결과는 없음
     * - 더티 체킹 자체는 dirty 와 같음. 차이점은 "언제 SQL을 보내느냐"(커밋 vs flush)
     */
    @Transactional
    public String registerFlushFail() {
        String loginId = "flush " + System.currentTimeMillis();

        // 비영속 → save → 영속
        Member saved = this.memberRepository.save(new Member(loginId, "before"));
        saved.setNickname("after"); // 영속 + dirty (아직 UPDATE는 flush/커밋 때)

        // flush: 지금 SQL 전송 (커밋 아님!). 더티 체킹으로 UPDATE 즉시 발생 가능
        this.memberRepository.flush();
        System.out.println("flush returned " + loginId);

        // 예외 → ROLLBACK → 위에서 flush 한 변경도 전부 취소
        throw new IllegalStateException(loginId);
    }

    /*
     * [/posts/read-in-tx] @Transactional(readOnly = true) — 조회 전용
     *
     * -------------------------------------------------
     * [readOnly = true 란? — 한 줄]
     * "이 트랜잭션은 읽기만 한다"고 Spring/JPA 에 알려 주는 옵션
     * (쓰기 save/UPDATE 용 registerOK 등과 역할이 다름)
     *
     * [왜 쓰나?]
     * 1) 의도 표현: 조회 메서드임을 코드로 명시
     * 2) 성능 힌트: Hibernate 가 불필요한 flush/더티 체킹을 줄임
     * 3) DB 힌트: 가능하면 읽기 전용 커넥션/최적화에 활용
     * ※ 절대적인 "쓰기 금지 락"은 아님. 그래도 조회 메서드에선 save 하지 말 것
     *
     * [영속성 / LAZY 와의 관계] ★ open-in-view:false 일 때 중요
     * - application.yaml: open-in-view: false
     *   → Service 트랜잭션이 끝나면 영속 컨텍스트 종료
     * - Post.member 는 FetchType.LAZY
     *   → getMember() 하려면 아직 영속(트랜잭션 안)이어야 함
     * - 그래서 조회도 @Transactional 로 감싸야
     *   findById + getMember().getLoginId() 가 같은 영속 컨텍스트 안에서 가능
     * - readOnly=true 면 "조회만 할 건데 트랜잭션은 필요"할 때 잘 맞음
     *
     * [AOP 프록시 + JPA 프록시 — 이 메서드가 다리]
     * - AOP 프록시: @Transactional 실행 → 트랜잭션·영속 열림 (문지기)
     * - JPA 프록시: member LAZY 껍데기 → getLoginId() 때 SELECT
     * - 흐름:
     *   Controller → AOP 프록시(readOnly ON, 영속 열림)
     *     → findById (Post 영속, member = JPA 프록시만)
     *     → getMember() LAZY SELECT ← 아직 AOP가 연 트랜잭션 안
     *   → AOP 프록시 OFF, 영속 닫힘
     * - @Transactional 없이 Controller 에서 getMember → LazyInitializationException
     *
     * [쉬운 비유]
     * AOP 프록시     = 열람실 문 열어 주는 사람
     * readOnly=true  = "열람만" 좌석
     * JPA 프록시     = 책장에 꽂힌 "나중에 펼칠" 쪽지(연관 미로딩)
     * -------------------------------------------------
     *
     * [영속성 생명주기 — 이 메서드]
     * 1) 프록시: @Transactional(readOnly=true) → 읽기 트랜잭션 + 영속 시작
     * 2) findById → Post 영속 (SELECT board_post) / member 는 LAZY 미로딩
     * 3) getMember().getLoginId() → LAZY 로딩 (SELECT board_member)
     *    - 프록시가 아직 트랜잭션을 유지 중이라 성공
     * 4) return → 프록시가 트랜잭션 종료 → 영속 닫힘 → 준영속
     *    - 이 시점 이후 Controller 에서 다시 getMember() 하면 실패
     *
     * [registerDirty / registerOK 와 비교]
     * - OK/Dirty : 쓰기(INSERT/UPDATE), readOnly 없음(기본 false)
     * - 여기     : 읽기만, readOnly=true, save/set 없음
     *
     * [이 코드 흐름]
     * 1) 프록시: readOnly 트랜잭션 시작
     * 2) Post 조회 (없으면 "missing")
     * 3) title + member.loginId 조합 (LAZY SELECT)
     * 4) 정상 종료 → COMMIT(변경 없음) / Closing
     *
     * [콘솔에서 보이는 것]
     * Creating new transaction ... findTitleWithLoginId (read-only 힌트일 수 있음)
     * → SELECT board_post ...
     * → SELECT board_member ...  (getMember 시점, LAZY)
     * → Committing / Closing
     * → INSERT/UPDATE 는 없음
     *
     * [헷갈리는 점]
     * - readOnly 여도 @Transactional 은 필요 (LAZY·영속 컨텍스트 유지)
     * - readOnly = "쓰기 안 함" 선언이지, 트랜잭션 자체를 없애는 게 아님
     * - Controller 에서 LAZY 하면 open-in-view:false 에선 예외
     *   → 조회+연관 접근은 Service 트랜잭션 안에서 (이 메서드처럼)
     */
    @Transactional(readOnly = true)
    public String findTitleWithLoginId(Long id) {
        // 영속 컨텍스트 안에서 Post 조회 (readOnly 트랜잭션)
        Post post = this.postRepository.findById(id).orElse(null);
        if (post == null) {
            return "missing";
        }
        // LAZY: getMember() 시점에 board_member SELECT — 같은 트랜잭션이라 가능
        return post.getTitle() + "by" + post.getMember().getLoginId();
    }
}
