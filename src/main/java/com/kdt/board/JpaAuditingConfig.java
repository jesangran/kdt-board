package com.kdt.board;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/*
 * [영속화] JPA Auditing 스위치 (앱 전역 설정)
 *
 * @EnableJpaAuditing
 * - Spring Data JPA 의 "감사(Auditing)" 기능을 켭니다
 * - 이게 있어야 @CreatedDate / @LastModifiedDate 가 실제로 채워짐
 *
 * Post 와의 연관
 * 1) JpaAuditingConfig     → @EnableJpaAuditing  (기능 ON)
 * 2) Post                  → @EntityListeners(AuditingEntityListener.class)
 * 3) Post.createdAt        → @CreatedDate (Instant) — INSERT 때, 이후 불변
 * 4) Post.updatedAt        → @LastModifiedDate (Instant) — INSERT/UPDATE 때마다
 * 5) PostAuditUpdateRunner → body 수정 save 로 updatedAt 변화 확인
 *
 * 저장 흐름)
 *   [INSERT] postService.save(새 Post)
 *     → createdAt + updatedAt 에 Instant.now() (UTC)
 *   [UPDATE] postService.save(기존 Post)  ← PostAuditUpdateRunner
 *     → updatedAt 만 Instant.now() 로 갱신, createdAt 유지
 *
 * ★ Instant.now() 는 항상 UTC 라서 DB에도 UTC 기준으로 들어감
 *   한국 시간(UTC+9)으로 넣으려면 DateTimeProvider 를 Asia/Seoul 로
 *   따로 등록하는 식의 추가 설정이 필요함 (현재는 기본 UTC 그대로)
 *
 * ※ @EnableJpaAuditing 이 없으면
 *   @CreatedDate 를 붙여도 createdAt 이 null 로 남아 INSERT 실패할 수 있음
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

}
