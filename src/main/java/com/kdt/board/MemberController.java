package com.kdt.board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /members — Member 목록 평문 (Controller→Service→Repository) */
@RestController
public class MemberController {

    private final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/members")
    public String members() {
        StringBuilder lines = new StringBuilder();
        for (Member member : this.memberService.findAll()) {
            lines.append(member.getId())
                    .append(" ")
                    .append(member.getLoginId())
                    .append(" ")
                    .append(member.getNickname())
                    .append("\n");
        }
        return lines.toString();
    }
}
