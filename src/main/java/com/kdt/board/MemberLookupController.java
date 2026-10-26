package com.kdt.board;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /members/sample — id로 단건 평문 조회 */
@RestController
public class MemberLookupController {

    private final MemberService memberService;

    public MemberLookupController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/members/sample")
    public String sample() {
        Long memberId = 1L;
        Member member = this.memberService.findById(memberId);
        if (member == null) {
            return "missing";
        }
        return member.getId() + " " + member.getLoginId() + " " + member.getNickname();
    }
}
