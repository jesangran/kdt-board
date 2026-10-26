package com.kdt.board;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * [JPA 프록시·LAZY 관찰] GET /posts/load-check
 * Post 조회 → board_post만 / getLoginId() → board_member SELECT (지연)
 * member 자리는 "껍데기 프록시" — AOP 프록시와 다름 (Post·BoardTxController)
 * OSIV true(기본)면 Controller에서도 LAZY 가능
 */
@RestController
public class PostLoadController {

    private final PostService postService;

    public PostLoadController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/posts/load-check")
    public String loadCheck() {
        Long postId = 1L;
        System.out.println("load: before post lookup");
        Post post = this.postService.findById(postId);
        if (post == null) {
            return "missing";
        }
        System.out.println("load: post loaded, title=" + post.getTitle());
        Member member = post.getMember();
        System.out.println("load: member reference obtained");
        System.out.println("load: member id=" + member.getId());
        System.out.println("load: before loginId");
        String loginId = member.getLoginId();
        System.out.println("load: after loginId=" + loginId);
        return post.getTitle() + " by " + loginId;
    }
}
