package com.kdt.board;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/*
 * =========================================================
 * [영속화] Member = Entity (board_member)
 * =========================================================
 * 영속화 = 자바 객체를 DB에 저장해서, 앱을 꺼도 데이터가 남게 하는 것
 *
 * Entity 생명주기
 * 1) 비영속 : new Member() 직후 → id=null
 * 2) 영속   : repository.save() 후 → id 부여
 * 3) 준영속 / 4) 삭제
 *
 * [관련 소스]
 * 1. Member / Post     → 양방향 연관 매핑
 * 2. MemberRepository  → DB 접근
 * 3. MemberService     → save / findAll / findById ...
 * 4. MemberController  → GET /members
 * 5. BoardSeedRunner   → kdt01 seed 후 Post.setMember(saved)
 *
 * Post와의 관계 (양방향)
 * - Member(1) : Post(N)
 * - Member 쪽: @OneToMany(mappedBy = "member") → posts (비소유)
 * - Post 쪽  : @ManyToOne + member_id FK → 연관의 주인
 * - FK는 board_post.member_id 에만 존재
 */
@Entity
@Table(name = "board_member")
public class Member {

    // DB AUTO_INCREMENT PK (저장 전 null → 저장 후 값)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20) // NOT NULL, UNIQUE
    private String loginId;

    @Column(nullable = false, length = 20)
    private String nickname;

    /*
     * 이 회원이 작성한 게시글 목록 (1:N의 "1" 쪽, 연관의 비소유)
     * - mappedBy = "member" : Post.member 가 FK(주인)
     * - LAZY : getPosts() 호출 시점에 board_post SELECT
     * - cascade = ALL : Member 저장/삭제 시 연관 Post 도 전파
     * - orphanRemoval = true : posts 에서 제거된 Post 는 DB 에서도 삭제
     *
     * 양방향 편의 메서드 addPost / removePost 로
     * 컬렉션(posts)과 주인(post.member)을 함께 맞춰 줌
     */
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    // JPA 기본 생성자 (필수)
    public Member() {
    }

    public Member(String loginId, String nickname) {
        this.loginId = loginId;
        this.nickname = nickname;
    }

    public Long getId() { return this.id; }
    public String getLoginId() { return this.loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getNickname() { return this.nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public List<Post> getPosts() { return this.posts; }

    /*
     * 양방향 편의 메서드: 게시글 추가
     * - posts 컬렉션에 넣고
     * - post.setMember(this) 로 주인(FK) 쪽도 맞춤
     * - 한쪽만 세팅하면 객체 그래프와 DB FK 가 어긋날 수 있음
     */
    public void addPost(Post post) {
        if (post == null) {
            throw new IllegalArgumentException("게시글은 null일 수 없습니다.");
        }
        if (post.getMember() != null && post.getMember() != this) {
            throw new IllegalArgumentException("다른 회원의 게시글을 이전하지 않습니다.");
        }
        if (!this.posts.contains(post)) {
            this.posts.add(post);
        }
        post.setMember(this);
    }

    /*
     * 양방향 편의 메서드: 게시글 제거
     * - posts 에서 빼고 post.member = null
     * - orphanRemoval=true 이면 이후 flush 때 DB 에서도 DELETE
     */
    public void removePost(Post post) {
        if (post == null) {
            throw new IllegalArgumentException("게시글은 null일 수 없습니다.");
        }
        if (this.posts.remove(post)) {
            post.setMember(null);
        }
    }
}
