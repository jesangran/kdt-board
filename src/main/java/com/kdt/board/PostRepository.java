package com.kdt.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*
 * [영속화] Repository = DB 접근 창구 (2단계)
 *
 * JpaRepository<Post, Long>
 * - Post : 영속화 대상 Entity
 * - Long : PK 타입 (Post.id)
 *
 * Spring Data JPA가 구현체를 자동 생성 → Bean으로 등록
 * 주요 메서드:
 * - save(entity)   : INSERT/UPDATE를 하나로 처리, 저장 후 id 채워짐
 * - findAll()           : Post 만 조회 (Member LAZY → N+1 가능)
 * - findAllWithMember() : join fetch 로 Post+Member 한 번에 (N+1 해결)
 * - countPosts()        : COUNT 만 → 엔티티 미생성 (가벼움)
 * - findById(id)        : PK로 단건 조회 → Optional<Post>
 * - deleteById(id)      : 삭제
 *
 * [관점] "동작한다" ≠ "비용이 싸다" — 조회 API 뒤의 비용을 구분해서 볼 것
 * - 메모리   : 엔티티(Post/Member 객체)를 몇 개 만드는지
 * - DB/네트워크 : SQL 횟수·왕복 (N+1 이면 1+N 번)
 * - 시간     : 위 둘이 합쳐져 응답이 느려짐
 * 예) countPosts = 숫자만 / findAll().size() = 객체 N개+SELECT 전부
 * 예) findAll+LAZY 루프 = 쿼리 많음 / join fetch = 쿼리 적음(같은 응답)
 *
 * ★ 왜 insert() / update()가 없고 save()만 있나?
 * - SQL은 INSERT/UPDATE를 나누지만, JPA는 "저장(save)" 중심
 * - 메서드 이름이 아니라 엔티티 상태로 자동 분기함
 *   · id == null (비영속) → INSERT
 *   · id 있음 (이미 DB 행) → UPDATE
 *
 * 호출 주체: PostService (저장/조회), PostController (목록 조회 시 직접 호출)
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /*
     * [N+1 해결] fetch join
     * - join fetch p.member : Post 조회 시 Member 도 같이 로딩
     * - 일반 join 과 달리 "연관 엔티티를 영속 상태로 채움" (LAZY 프록시 대신 실제 데이터)
     * - 생성 SQL 예: SELECT ... FROM board_post p JOIN board_member m ON ...
     * - 호출: PostService.findAllWithMember() → GET /posts/joined
     *
     * [비용] findAll() + 루프 getMember() 와 비교
     * - DB/네트워크 : SQL ≈ 1번 (N+1 의 1+N 왕복 제거) ← 핵심 이득
     * - 메모리       : Post+Member 는 어차피 응답에 필요하면 로드됨
     *                 (절약은 "불필요한 추가 쿼리", 객체 개수 자체는 비슷할 수 있음)
     * - 시간         : 왕복 감소 → 목록+작성자 조회가 빨라짐
     */
    @Query("select p from Post p join fetch p.member")
    List<Post> findAllWithMember();

    /*
     * [개수만 조회] COUNT 집계 — 비용 관점의 정석
     * - 엔티티를 로드하지 않고 DB 가 건수만 계산
     * - PostService.countPosts() 에서 사용
     * - findAll().size() (countLoadedPosts) 보다 훨씬 가벼움
     *
     * [비용] countLoadedPosts(findAll → size) 와 비교
     * - 메모리       : long 숫자 1개 vs Post 객체 N개  ← 여기선 메모리 차이가 핵심
     * - DB/네트워크 : COUNT 결과 한 줄 vs 행 전체 SELECT
     * - 시간         : 데이터·객체 생성량이 적어 빠름
     * → 결과는 같아도 "필요할 만큼만" 가져오면 비용이 싸다
     */
    @Query("select count(p) from Post p")
    long countPosts();

    /*
     * [바인딩] 이름 있는 파라미터 (Named Parameter) — 노션: 집계 조건에 입력값
     *
     * JPQL 의 :title 은 "구멍(플레이스홀더)"
     * → 메서드 인자 값이 그 구멍에 채워짐 = 바인딩 (binding)
     *
     * 연결 고리)
     *   @Param("title") String title  →  JPQL :title 자리에 title 값
     *   ※ @Param 문자열과 :이름 이 같아야 함 ("title" ↔ :title)
     *
     * 실제 JDBC SQL 로는 ? 로 바뀌고, 값은 따로 전달됨
     *   예) SELECT COUNT(*) FROM board_post WHERE title = ?
     *       binding parameter (1) <- [closed]   ← /posts/title-count/closed
     *   → application.yaml 의 org.hibernate.orm.jdbc.bind: TRACE 로 확인
     *
     * 왜 문자열을 쿼리에 직접 안 붙이나? ( "..." + title )
     * - SQL 인젝션 위험 / 캐시·최적화에 불리
     * - 바인딩 = 쿼리 뼈대와 값을 분리해서 안전하게 전달
     *
     * 호출: PostService.countByTitle(title) → GET /posts/title-count/closed
     */
    @Query("select count(p) from Post p where p.title = :title")
    long countByTitle(@Param("title") String title);
}
