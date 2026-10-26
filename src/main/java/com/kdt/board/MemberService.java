package com.kdt.board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * [영속화] MemberService — Repository 위임
 * save: id null→INSERT / 있음→UPDATE
 * findById: Optional.orElse(null)
 */
@Service
public class MemberService {

    private final MemberRepository memberRepository;

    @Autowired
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member save(Member member) {
        return this.memberRepository.save(member);
    }

    public List<Member> findAll() {
        return this.memberRepository.findAll();
    }

    public Member findById(Long id) {
        return this.memberRepository.findById(id).orElse(null);
    }

    public Member findByLoginId(String loginId) {
        return this.memberRepository.findByLoginId(loginId).orElse(null);
    }

    public List<Member> findByNickname(String nickname) {
        List<Member> members = this.memberRepository.findByNickname(nickname);
        if (members.isEmpty()) {
            System.out.println("no");
            return null;
        }
        return members;
    }
}
