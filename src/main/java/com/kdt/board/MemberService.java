package com.kdt.board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * [영속화] Service = 비즈니스 + Repository 호출 (3단계)
 *
 * Controller / Runner 는 Service만 알고,
 * 실제 DB 영속화는 Repository에 위임한다.
 *
 * save() 호출 시 흐름: (insert/update 메서드가 따로 없음!)
 * - JPA는 SQL처럼 insert/update를 나누지 않고 save() 하나로 처리
 * - 엔티티 상태로 자동 분기:
 *   1) id == null (비영속) → INSERT → 영속, id 부여
 *   2) id 있음           → UPDATE
 *
 * findAll() 호출 시 흐름:
 * DB SELECT → Member 목록 반환
 *
 * findById() + Optional / orElse
 * - repository.findById(id)의 반환 타입은 Member가 아니라 Optional<Member>
 * - Optional = "값이 있을 수도, 없을 수도 있다"를 감싼 상자
 *   · id 있음 → 상자 안에 Member
 *   · id 없음 → 빈 Optional (null이 아님)
 * - 메서드 반환이 Member인데 Optional을 그대로 return하면 타입 불일치 → 컴파일 에러
 * - orElse(null) = 상자에서 Member를 꺼냄 / 없으면 null 반환
 * - 실무에선 orElseThrow(...)로 "없으면 예외" 처리하기도 함
 */
@Service
public class MemberService {

    private final MemberRepository memberRepository;

    @Autowired // 생성자 주입: Repository Bean을 연결
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    // 영속화 진입점: 비영속 Member → DB 저장 → 영속 Member 반환
    public Member save(Member member) {
        return this.memberRepository.save(member);
    }

    // DB에 영속화된 Member 전체 조회
    public List<Member> findAll() {
        return this.memberRepository.findAll();
    }

    // Optional<Member> → Member로 꺼내기 위해 orElse 사용 (없으면 null) 있을수도 있고 없을 수도 있어서
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
