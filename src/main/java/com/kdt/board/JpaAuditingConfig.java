package com.kdt.board;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/*
 * [Auditing] 감사 — "누가/언제 만들었·고쳤는지"를 자동 기록
 *
 * @EnableJpaAuditing = 앱 전역 스위치 ON
 * 없으면 Post의 @CreatedDate / @LastModifiedDate 가 안 채워짐(null → INSERT 실패 가능)
 *
 * 짝: Post @EntityListeners(AuditingEntityListener) + @CreatedDate / @LastModifiedDate
 * INSERT → createdAt + updatedAt (Instant, 기본 UTC)
 * UPDATE → updatedAt 만 갱신 (createdAt 은 updatable=false)
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
