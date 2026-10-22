package com.kdt.board;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * [영속화] Controller = HTTP 진입점 (4단계)
 * [06-SQL-로그와-N+1] 조회·로그 관찰 (기존 회원·게시글 수정 없음)
 *
 * 노션 학습 흐름)
 *   관찰: /posts/with-member(일반) vs /posts/joined(fetch join) — 응답 같고 SQL 횟수 다름
 *   집계: /posts/count vs /posts/list-count — 개수만 vs 목록 로드 후 size
 *         /posts/title-count/closed — 조건+바인딩
 *
 * 공통 흐름:
 * PostController → PostService → PostRepository → DB
 *
 * GET /posts
 *   → postService.findAll()
 *   → board_post SELECT
 *   → id / title / createdAt 을 한 줄씩 문자열로 응답
 *   ※ createdAt 은 @CreatedDate + Auditing 으로 저장 시 자동 세팅된 값
 *   ※ Member 를 안 건드림 → N+1 없음 (board_post 1번만)
 *
 * GET /posts/1
 *   → postService.findById(1L)
 *   → 없으면 "missing"
 *   → 있으면 title + 작성자 loginId
 *   ※ post.getMember().getLoginId() 는 LAZY 로딩
 *     - open-in-view: true  → Controller 에서 가능
 *     - open-in-view: false → LazyInitializationException 가능
 *       (이 경우 Service 트랜잭션 안에서 Member 를 미리 읽거나 fetch join 필요)
 *   ※ 게시글 1건 + Member 1건 → N+1 이슈의 "맛보기" 정도 (목록이 아니라 체감 작음)
 *
 * GET /posts/with-member  ★ N+1 문제 실습 (비교용 normal)
 *   → findAll() 로 Post N건 조회 (SQL 1번)
 *   → 루프에서 글마다 getMember().getLoginId() (LAZY → Member SELECT N번)
 *   → 총 SQL ≈ 1 + N 번  ← 이게 N+1
 *   콘솔: "sql compare normal begin" ~ "end" 사이 SQL 개수 확인
 *
 * GET /posts/joined  ★ N+1 해결 실습 (비교용 joined)
 *   → findAllWithMember() = JPQL join fetch
 *   → Post + Member 를 JOIN 한 번에 조회 (SQL ≈ 1번)
 *   → 루프에서 getMember().getLoginId() 해도 추가 SELECT 없음
 *   콘솔: "sql compare joined begin" ~ "end" 와 with-member 비교
 *
 * [개념] N+1 문제
 * - 목록 조회 1번 + 연관 엔티티를 건당 추가 조회 N번
 * - 원인: LAZY + for 루프 안에서 연관 객체 접근
 * - 글 100개면 board_post 1 + board_member 100 = 101번 → DB 부담·느림
 * - 해결: fetch join / @EntityGraph 등으로 Post+Member 를 한 번에 조회
 *   → 이 프로젝트: /posts/with-member(문제) vs /posts/joined(해결)
 *
 * [비용 관점] 메모리 + DB/네트워크 + 시간 — "동작"과 "싸게 동작"을 구분
 * - N+1 (with-member) : DB 왕복 1+N ↑ → 시간 ↑ (메모리에 Member 올리는 건 필요할 수 있음)
 * - fetch join (joined): 같은 데이터를 SQL ≈ 1번으로 → 왕복·시간 ↓
 * - countPosts vs countLoadedPosts : 개수만 필요할 때
 *   · COUNT = 숫자만 (메모리 거의 없음)
 *   · findAll+size = Post N개 로드 후 셈 (메모리·SELECT 비용 ↑) ← 메모리 차이가 핵심
 *
 * GET /posts/count            → countPosts()        COUNT 집계만
 * GET /posts/list-count       → countLoadedPosts()  findAll + size (비교용)
 * GET /posts/title-count/closed → countByTitle("closed")  조건+Named 바인딩
 *
 * 여기서는 저장하지 않고 조회만 함 (06차시 본문: 조회·로그 관찰)
 */
@RestController
public class PostController {

    // private: 이 클래스만 / static: 인스턴스마다 안 만듦 / final: 재할당 불가
    private static final Logger log = LoggerFactory.getLogger(PostController.class);
    private final PostService postService;

    // 생성자 주입: Service Bean 연결 (Controller 는 Repository 를 직접 모름)
    public PostController(PostService postService) {
        this.postService = postService;
    }

    // /posts: 게시글마다 ID·제목·생성 시각을 한 줄로 구성합니다.
    // Member 미접근 → findAll() 의 board_post SELECT 1번만 (N+1 없음)
    @GetMapping("/posts")
    public String posts() {
        StringBuilder lines = new StringBuilder(); // 문자열을 순서대로 누적
        for (Post post : this.postService.findAll()) {
            lines.append(post.getId())
                    .append(" ")
                    .append(post.getTitle())
                    .append(" ")
                    .append(post.getCreatedAt()) // Auditing 으로 채워진 Instant (UTC)
                    .append(" ")
                    .append(post.getUpdatedAt())
                    .append("\n");
        }
        return lines.toString();
    }

    // 경로의 1과 findById의 1L을 확인한 실제 closed ID로 맞춥니다.
    @GetMapping("/posts/1")
    public String onePost() {
        Post post = this.postService.findById(1L);
        if (post == null) {
            return "missing";
        }
        // 작성자 객체에서 loginId를 읽어 제목과 함께 응답합니다. (LAZY: getLoginId 시점에 Member SELECT)
        return post.getTitle()
                + " by "
                + post.getMember().getLoginId();
    }

    /*
     * [/posts/with-member] N+1 발생 경로 (비교용 "normal")
     *
     * 흐름)
     *   1) findAll()           → SELECT board_post          … 1번
     *   2) for 각 Post
     *        getMember()
     *        .getLoginId()     → SELECT board_member WHERE id=?  … Post 개수(N)번
     *   3) 합계 SQL ≈ 1 + N
     *
     * 확인 방법)
     *   begin ~ end 로그 사이에 board_member SELECT 가 rows 만큼 반복되면 N+1
     *   (/posts 는 Member 를 안 써서 board_post 만 나옴 → 비교)
     */
    @GetMapping("/posts/with-member")
    public String withMember() {
        log.info("sql compare normal begin");

        int rows = 0;
        StringBuilder lines = new StringBuilder();
        // Post 만 먼저 로드한 뒤, 루프에서 LAZY Member 를 건당 접근 → N+1
        for (Post post : this.postService.findAll()) {
            lines.append(post.getTitle())
                    .append(" by ")
                    // LAZY: 이 줄 실행 순간마다 Member SELECT 1번 추가
                    .append(post.getMember().getLoginId())
                    .append("\n");
            rows++;
        }
        log.info("sql compare normal end rows={}", rows);

        return lines.toString();
    }

    /*
     * [/posts/joined] N+1 해결 경로 (비교용 "joined")
     *
     * 흐름)
     *   1) findAllWithMember()
     *        → JPQL: select p from Post p join fetch p.member
     *        → SQL: board_post JOIN board_member … 대략 1번
     *   2) for 각 Post
     *        getMember().getLoginId()
     *        → 이미 fetch 로 채워짐 → 추가 Member SELECT 없음
     *   3) 합계 SQL ≈ 1  (with-member 의 1+N 과 대비)
     *
     * 확인 방법)
     *   begin ~ end 사이에 board_member 단독 SELECT 가 반복되지 않으면 성공
     *   응답 문자열 형태는 with-member 와 같고, SQL 횟수만 다름
     *
     * ※ open-in-view:false 여도 fetch 로 Member 가 이미 로드되어
     *    Controller 에서 getLoginId() 가능
     */
    @GetMapping("/posts/joined")
    public String joined() {
        log.info("sql compare joined begin");

        int rows = 0;
        StringBuilder lines = new StringBuilder();
        // fetch join 으로 Post+Member 를 한 번에 가져온 뒤 루프 → N+1 없음
        for (Post post : this.postService.findAllWithMember()) {
            lines.append(post.getTitle())
                    .append(" by ")
                    // Member 이미 로드됨 → 이 줄에서 추가 SELECT 안 나감
                    .append(post.getMember().getLoginId())
                    .append("\n");
            rows++;
        }
        log.info("sql compare joined end rows={}", rows);

        return lines.toString();
    }

    /*
     * [/posts/count] 개수만 — COUNT (노션: 전체 목록 대신 개수)
     * - SQL: SELECT COUNT(...) FROM board_post
     * - Post 객체를 안 만듦 → 메모리 비용 작음
     * - /posts/list-count 와 숫자는 같아도 SQL·메모리 비용이 다름
     */
    @GetMapping("/posts/count")
    public String count() {
        return "" + this.postService.countPosts();
    }

    /*
     * [/posts/list-count] 목록 로드 후 size — 비교·학습용 (비효율)
     * - findAll() 로 Post 전부 메모리에 올린 뒤 size()
     * - 개수만 필요할 때 이렇게 하면 비용이 큼 → /posts/count 와 로그 비교
     */
    @GetMapping("/posts/list-count")
    public String listCount() {
        return "" + this.postService.countLoadedPosts();
    }

    /*
     * [/posts/title-count/closed] 제목 조건 집계 + 바인딩
     * - countByTitle("closed") → JPQL :title 자리에 "closed" 바인딩
     * - 콘솔 TRACE(jdbc.bind): binding parameter ... <- [closed]
     * - 경로의 closed 와 인자 "closed" 가 같은 조건 값 (실습용 고정 문자열)
     */
    @GetMapping("/posts/title-count/closed")
    public String countClosed() {
        return "" + this.postService.countByTitle("closed");
    }
}
