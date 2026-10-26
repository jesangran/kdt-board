package com.kdt.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*
 * [영속화] PostRepository — DB 접근 (JpaRepository)
 *
 * save만 있는 이유 (초보): SQL은 INSERT/UPDATE를 나누지만
 * JPA는 엔티티 상태로 분기 — id==null → INSERT / id 있음 → UPDATE
 *
 * 비용 (노션 06): 동작 ≠ 싸게 동작
 *   메모리=엔티티 개수 / DB=SQL 횟수 / 시간=합
 *   countPosts(COUNT) ≪ findAll().size()
 *   findAll+LAZY 루프(1+N) ≫ join fetch(≈1)
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /** N+1 해결: Post+Member 한 쿼리 → /posts/joined */
    @Query("select p from Post p join fetch p.member")
    List<Post> findAllWithMember();

    /** 개수만 COUNT — 엔티티 미생성 → /posts/count */
    @Query("select count(p) from Post p")
    long countPosts();

    /*
     * Named 파라미터 바인딩: @Param("title") ↔ JPQL :title
     * 문자열 이어붙이기 금지(인젝션). TRACE jdbc.bind 로 값 확인
     * → /posts/title-count/closed , /api/v1/posts/count-by-title
     */
    @Query("select count(p) from Post p where p.title = :title")
    long countByTitle(@Param("title") String title);
}
