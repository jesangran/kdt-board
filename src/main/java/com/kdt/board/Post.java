package com.kdt.board;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/*
 * =========================================================
 * [영속화] Post = Entity (board_post)
 * =========================================================
 * 영속화 = 자바 객체를 DB에 저장해서, 앱을 꺼도 데이터가 남게 하는 것
 *
 * Entity 생명주기
 * 1) 비영속 : new Post() 직후 → id=null
 * 2) 영속   : repository.save() 후 → JPA가 관리, id 부여
 * 3) 준영속 : 영속 컨텍스트에서 분리
 * 4) 삭제   : 삭제 예약
 *
 * [관련 소스]
 * 1. Post / Member     → 양방향 연관 매핑
 * 2. PostRepository    → DB 접근
 * 3. PostService       → save / findAll / findById
 * 4. PostController    → GET /posts, /posts/1
 * 5. BoardSeedRunner   → seed INSERT (createdAt 확인)
 * 6. PostAuditUpdateRunner → body UPDATE (updatedAt 확인)
 *
 * Member와의 관계 (양방향의 N쪽 = 연관 주인)
 * - @ManyToOne + @JoinColumn(member_id) → FK를 가진 쪽
 * - Member.posts 의 mappedBy = "member" 와 짝
 */
@Entity // JPA가 DB 테이블과 매핑하는 Entity
@Table(name = "board_post") // 매핑할 실제 테이블 이름
/*
 * @EntityListeners(AuditingEntityListener.class)
 * - 저장 직전 이벤트를 감지해 @CreatedDate / @LastModifiedDate 자동 세팅
 * - INSERT: createdAt + updatedAt
 * - UPDATE: updatedAt 만 갱신 (createdAt 은 updatable=false)
 * - 전제: JpaAuditingConfig 의 @EnableJpaAuditing
 */
@EntityListeners(AuditingEntityListener.class)
public class Post {

    // DB가 생성하는 게시글 ID (저장 전 null → 저장 후 AUTO_INCREMENT)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SQL 열의 NOT NULL·길이에 맞춘 매핑
    @Column(nullable = false, length = 20)
    private String title;

    @Column(nullable = false, length = 100)
    private String body;

    /*
     * 작성자(Member) — 양방향의 N쪽, 연관의 주인(FK 관리)
     * - @ManyToOne : Post(N) → Member(1)
     * - @JoinColumn(name = "member_id") : FK 컬럼
     *
     * [FetchType.LAZY] = JPA 프록시 (지연 로딩)
     * - Post 조회 시 member 는 실제 Member 가 아니라 LAZY 껍데기
     * - getMember()/getLoginId() 접근 시점에 board_member SELECT
     * - 영속·트랜잭션 밖이면 LazyInitializationException
     *
     * [구분] JPA 프록시 vs AOP 프록시
     * - JPA : 이 필드처럼 연관 엔티티를 나중에 로드 (Hibernate)
     * - AOP : BoardTxService 빈을 감싸 트랜잭션 Advice (/tx/proxy)
     * - alike: "대신 서는 객체" / 다름: 지연 로딩 vs Bean 가로채기
     * - 같이 씀: /posts/read-in-tx (AOP가 영속 연 뒤 → 여기 LAZY SELECT)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /*
     * 생성 시각
     * - Instant : UTC 절대 시각 (Auditing 기본도 Instant.now() → UTC)
     * - @CreatedDate : INSERT 때 자동 세팅
     * - updatable = false : 이후 UPDATE 로 바뀌지 않음
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /*
     * 마지막 수정 시각
     * - @LastModifiedDate : INSERT/UPDATE 때마다 Instant.now() 로 갱신
     * - setBody() 직후가 아니라 save()/flush 시점에 바뀜
     * - 확인: PostAuditUpdateRunner
     */
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
        // JPA가 조회한 행으로 엔티티를 만들 때 사용하는 생성자
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
