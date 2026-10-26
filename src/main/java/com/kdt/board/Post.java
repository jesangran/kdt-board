package com.kdt.board;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/*
 * [영속화] Post = Entity (board_post)
 *
 * 영속성 = 자바 객체를 DB에 저장해 앱을 꺼도 데이터가 남게 하는 것
 *
 * 생명주기 (초보)
 *   1) 비영속 : new Post() — JPA가 모름, id=null, DB와 무관
 *   2) 영속   : repository.save() 후 — JPA가 관리, INSERT/UPDATE, id 부여
 *   3) 준영속 : 트랜잭션 끝 — 관리 종료. 이후 set해도 자동 UPDATE 없음
 *   4) 삭제   : delete — DB에서 제거 예약
 *
 * 더티 체킹: "영속"일 때만. setTitle() 후 커밋 때 값이 바뀌었으면 UPDATE
 *   → PostService.update() 가 save() 없이도 DB 반영되는 이유
 *
 * 양방향 N쪽(연관 주인): @ManyToOne + member_id FK
 *   ↔ Member.posts 의 mappedBy = "member"
 *
 * [Auditing] JpaAuditingConfig(@EnableJpaAuditing) + Listener
 *   INSERT: createdAt·updatedAt 자동 / UPDATE: updatedAt만
 */
@Entity
@Table(name = "board_post")
@EntityListeners(AuditingEntityListener.class)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** DB NOT NULL·길이 — PostCreateRequest @Size 와 맞춤 */
    @Column(nullable = false, length = 20)
    private String title;

    @Column(nullable = false, length = 100)
    private String body;

    /*
     * [JPA 프록시 · LAZY] — AOP 프록시와 이름만 같고 역할이 다름!
     *
     * Post 조회 직후 member 자리에 앉는 건 "껍데기(프록시)"
     * getLoginId() 하는 순간 → board_member SELECT (지연 로딩)
     *
     * 전제: 아직 영속·트랜잭션이 열려 있어야 함
     * 닫힌 뒤 접근 → LazyInitializationException
     *
     * 구분)
     *   JPA 프록시 = 연관 데이터를 나중에 가져옴 (이 필드)
     *   AOP 프록시 = Service 빈 호출을 가로채 트랜잭션/로그 (BoardTxController)
     * 함께: /posts/read-in-tx — AOP가 영속 연 뒤 → 여기 LAZY SELECT
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 생성 시각 — INSERT 때만, 이후 UPDATE로 안 바뀜 */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /** 수정 시각 — INSERT/UPDATE 때 Auditing이 자동 갱신 */
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Post() {
    }

    public Long getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Member getMember() {
        return this.member;
    }

    public void setMember(Member member) {
        this.member = member;
    }
}
