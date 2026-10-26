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
 * [영속화] Member = Entity (board_member)
 * 1:N — Member(1) @OneToMany(mappedBy) 비소유 / Post(N) FK 주인
 */
@Entity
@Table(name = "board_member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String loginId;

    @Column(nullable = false, length = 20)
    private String nickname;

    /** mappedBy=주인 필드명. cascade/orphanRemoval: 컬렉션 변경 전파 */
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

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

    /** 양방향 맞춤: posts + post.setMember(this) */
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

    /** posts 제거 + member=null (orphanRemoval 시 DELETE) */
    public void removePost(Post post) {
        if (post == null) {
            throw new IllegalArgumentException("게시글은 null일 수 없습니다.");
        }
        if (this.posts.remove(post)) {
            post.setMember(null);
        }
    }
}
