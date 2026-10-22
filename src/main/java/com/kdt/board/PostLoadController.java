package com.kdt.board;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * [영속화] FetchType.LAZY 동작 확인용 Controller
 *
 * Post.member 가 LAZY 이므로:
 * - Post 조회 시점에는 board_post 만 SELECT
 * - Member 는 프록시(가짜 참조)로만 들고 있음
 * - Member 의 실제 컬럼(loginId 등)에 접근할 때 board_member SELECT
 *
 * GET /posts/load-check 실행 후 콘솔 로그 + SQL 순서를 보면 LAZY가 보인다.
 *
 * 왜 Controller 에서도 getMember() 가 되는가?
 * - Spring Boot 기본값: spring.jpa.open-in-view=true (OSIV)
 * - HTTP 요청이 끝날 때까지 영속 컨텍스트가 열려 있어서
 *   트랜잭션 밖처럼 보여도 LAZY 로딩이 가능함
 * - OSIV 를 끄면 여기서 LazyInitializationException 이 날 수 있음
 */
@RestController
public class PostLoadController {

    private final PostService postService;

    public PostLoadController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/posts/load-check")
    public String loadCheck() {
        Long postId = 1L; // 확인한 실제 closed의 ID로 변경합니다.

        // ① 아직 DB 조회 전
        System.out.println("load: before post lookup");

        // ② 이 시점에 board_post SELECT (Member 는 아직 SELECT 안 함 = LAZY)
        Post post = this.postService.findById(postId);
        if (post == null) {
            return "missing";
        }

        // ③ Post 자체 필드(title)만 사용 → 추가 SQL 없음
        System.out.println("load: post loaded, title=" + post.getTitle());

        /*
         * ④ getMember()
         * - 보통 이 줄에서는 board_member SELECT 가 아직 안 나감
         * - LAZY 프록시 객체만 받아 옴
         */
        Member member = post.getMember();
        System.out.println("load: member reference obtained");

        /*
         * ⑤ getId()
         * - FK(member_id) 값은 Post 조회 때 이미 알고 있어서
         * - 프록시 id 만 읽으면 SELECT 없이 끝나는 경우가 많음
         */
        Long memberId = member.getId();
        System.out.println("load: member id=" + memberId);

        // ⑥ loginId 는 Member 테이블 컬럼 → 여기서 board_member SELECT (LAZY 로딩 발생!)
        System.out.println("load: before loginId");
        String loginId = member.getLoginId();
        System.out.println("load: after loginId=" + loginId);

        return post.getTitle() + " by " + loginId;
    }
}
