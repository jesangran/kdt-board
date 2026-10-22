package com.kdt.board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * [영속화] Controller = HTTP 진입점 (4단계)
 *
 * GET /members 요청 흐름:
 * MemberController.members()
 *   → MemberService.findAll()
 *   → MemberRepository.findAll()
 *   → DB에서 board_member SELECT
 *   → 영속화된 Member들을 문자열로 만들어 응답
 *
 * 여기서는 저장(영속화)하지 않고, 이미 저장된 데이터를 조회만 함
 */
@RestController
public class MemberController {

    private final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/members")
    public String members() {
        StringBuilder lines = new StringBuilder(); //문자열을 순서대로 누적하는 객체
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

    @GetMapping("/test/members")
    public String testMembers() {
        String a = "kdt01";
        this.memberService.findByNickname(a);
        return "ok";
    }
}
