package com.kdt.board;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * [06] PostController — 평문 조회·SQL 관찰 (JSON은 PostApiController)
 *
 * 흐름: Controller → PostService → PostRepository → DB
 *
 * [N+1 · LAZY · JPA 프록시]
 *   Post.member 는 LAZY → 목록에서 글마다 getLoginId() 하면
 *   board_post 1번 + board_member N번 = N+1 (느리고 DB 부담)
 *   해결: join fetch (/posts/joined) ≈ SQL 1번
 *
 * [엔드포인트]
 *   /posts                  Member 안 건드림 → N+1 없음
 *   /posts/1                단건 LAZY (OSIV true면 Controller OK)
 *   /posts/with-member      N+1 문제 (비교용)
 *   /posts/joined           N+1 해결
 *   /posts/count            COUNT만 (가벼움)
 *   /posts/list-count       findAll+size (무거움)
 *   /posts/title-count/closed  조건+바인딩
 *
 * 비용: 메모리·SQL 횟수·시간 — "동작한다" ≠ "싸게 동작한다"
 */
@RestController
public class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/posts")
    public String posts() {
        StringBuilder lines = new StringBuilder();
        for (Post post : this.postService.findAll()) {
            lines.append(post.getId())
                    .append(" ")
                    .append(post.getTitle())
                    .append(" ")
                    .append(post.getCreatedAt())
                    .append(" ")
                    .append(post.getUpdatedAt())
                    .append("\n");
        }
        return lines.toString();
    }

    @GetMapping("/posts/1")
    public String onePost() {
        Post post = this.postService.findById(1L);
        if (post == null) {
            return "missing";
        }
        // LAZY: getLoginId 시점에 Member SELECT
        return post.getTitle() + " by " + post.getMember().getLoginId();
    }

    /** N+1: findAll 1 + getMember N — begin~end 사이 SQL 개수 확인 */
    @GetMapping("/posts/with-member")
    public String withMember() {
        log.info("sql compare normal begin");
        int rows = 0;
        StringBuilder lines = new StringBuilder();
        for (Post post : this.postService.findAll()) {
            lines.append(post.getTitle())
                    .append(" by ")
                    .append(post.getMember().getLoginId())
                    .append("\n");
            rows++;
        }
        log.info("sql compare normal end rows={}", rows);
        return lines.toString();
    }

    /** fetch join ≈ 1쿼리 — with-member 와 응답 같고 SQL만 다름 */
    @GetMapping("/posts/joined")
    public String joined() {
        log.info("sql compare joined begin");
        int rows = 0;
        StringBuilder lines = new StringBuilder();
        for (Post post : this.postService.findAllWithMember()) {
            lines.append(post.getTitle())
                    .append(" by ")
                    .append(post.getMember().getLoginId())
                    .append("\n");
            rows++;
        }
        log.info("sql compare joined end rows={}", rows);
        return lines.toString();
    }

    @GetMapping("/posts/count")
    public String count() {
        return "" + this.postService.countPosts();
    }

    @GetMapping("/posts/list-count")
    public String listCount() {
        return "" + this.postService.countLoadedPosts();
    }

    @GetMapping("/posts/title-count/closed")
    public String countClosed() {
        return "" + this.postService.countByTitle("closed");
    }
}
