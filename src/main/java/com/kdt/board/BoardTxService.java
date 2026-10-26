package com.kdt.board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * [05] BoardTxService — @Transactional = "트랜잭션 AOP Advice 붙여 줘"
 *
 * Service = 유스케이스 경계. 프록시가 메서드 앞뒤로 트랜잭션을 연다/닫는다.
 * 기본 롤백: RuntimeException / Error
 * 외부에서 boardTxService.xxx() 호출할 때만 프록시 적용
 *   → failBySelf 안의 this.registerFail() 은 원본 직행 (/tx/self)
 */
@Service
public class BoardTxService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    @Autowired
    public BoardTxService(MemberRepository memberRepository, PostRepository postRepository) {
        this.memberRepository = memberRepository;
        this.postRepository = postRepository;
    }

    /** 정상: Member+Post 한 트랜잭션 → COMMIT */
    @Transactional
    public String registerOK() {
        String loginId = "kdt" + System.currentTimeMillis();
        Member saved = this.memberRepository.save(member(loginId));
        Post post = new Post();
        post.setTitle("closed");
        post.setBody("no class");
        post.setMember(saved);
        this.postRepository.save(post);
        return "ok" + loginId;
    }

    /** save 후 예외 → 같은 트랜잭션 전부 ROLLBACK */
    @Transactional
    public String registerFail() {
        String loginId = "kdt" + System.currentTimeMillis();
        Member saved = this.memberRepository.save(member(loginId));
        Post post = new Post();
        post.setTitle("closed");
        post.setBody("no class");
        post.setMember(saved);
        this.postRepository.save(post);
        System.out.println("tx fail" + loginId);
        throw new IllegalStateException(loginId);
    }

    /** this.호출 = AOP 프록시 우회 */
    public String failBySelf() {
        return this.registerFail();
    }

    /** 트랜잭션 선언 없음 → 부분 반영 가능 */
    public String registerOpen() {
        String loginId = "kdt" + System.currentTimeMillis();
        this.memberRepository.save(member(loginId));
        System.out.println("tx open" + loginId);
        throw new IllegalStateException(loginId);
    }

    /*
     * 더티 체킹 (영속성)
     * new → 비영속 / save → 영속+INSERT / setNickname → 아직 SQL 없음
     * 메서드 끝 커밋 → 스냅샷과 비교 → 다르면 UPDATE
     * "set 하는 순간 DB가 바뀌는 게 아님"
     */
    @Transactional
    public String registerDirty() {
        String loginId = "dirty" + System.currentTimeMillis();
        Member saved = this.memberRepository.save(new Member(loginId, "first"));
        System.out.println("dirty before" + saved.getNickname());
        saved.setNickname("second");
        System.out.println("dirty after" + saved.getNickname());
        return "dirty " + saved.getLoginId();
    }

    /*
     * flush = 지금 SQL을 DB로 보냄 (COMMIT 아님!)
     * flush 후 예외 → ROLLBACK → flush한 것도 취소. 콘솔 SQL ≠ 최종 저장
     */
    @Transactional
    public String registerFlushFail() {
        String loginId = "flush " + System.currentTimeMillis();
        Member saved = this.memberRepository.save(new Member(loginId, "before"));
        saved.setNickname("after");
        this.memberRepository.flush();
        System.out.println("flush returned " + loginId);
        throw new IllegalStateException(loginId);
    }

    /*
     * readOnly=true: 조회용. 그래도 @Transactional 은 필요
     * → 프록시가 영속을 열어 둬야 getMember() LAZY(JPA 프록시) 가 성공
     */
    @Transactional(readOnly = true)
    public String findTitleWithLoginId(Long id) {
        Post post = this.postRepository.findById(id).orElse(null);
        if (post == null) {
            return "missing";
        }
        return post.getTitle() + "by" + post.getMember().getLoginId();
    }

    private static Member member(String loginId) {
        Member member = new Member();
        member.setLoginId(loginId);
        member.setNickname("kimbap");
        return member;
    }
}
