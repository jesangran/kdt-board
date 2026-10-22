package com.kdt.board;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MemberLookupController {
    private final MemberService memberService;

    public MemberLookupController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/members/sample")
    public String sample() {
        Long memberId = 1L; // 확인한 실제 kdt01의 ID로 변경합니다.
        Member member = this.memberService.findById(memberId);
        if (member == null) {
            return "missing";
        }
        return member.getId() + " "
                + member.getLoginId() + " "
                + member.getNickname();
    }
}
