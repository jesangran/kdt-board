package com.kdt.board;

import java.time.Instant;

/*
 * =========================================================
 * [08-JSON·DTO] PostResponse — 응답 전용 DTO (record)
 * =========================================================
 *
 * DTO(Data Transfer Object) = API로 주고받을 "모양"만 담는 객체
 *
 * Entity(Post)를 그대로 응답하면?
 * - body, updatedAt, Member 전체 등 불필요·민감할 수 있는 필드가 노출됨
 * - JSON 구조가 DB 테이블에 묶여 API가 깨지기 쉬움
 *
 * → 필요한 값만 골라 PostResponse 로 옮긴다.
 *   필드: id, title, loginId(작성자), createdAt
 *   (body / updatedAt / Member 엔티티 자체는 넣지 않음)
 *
 * record 란?
 * - 불변 데이터 운반용 짧은 클래스 (Java 16+)
 * - 컴파일러가 생성자·접근자(id(), title()…)·equals/hashCode/toString 자동 생성
 * - getId() 가 아니라 id() 형태. Jackson은 필드명(id, title…)으로 JSON 키를 만든다.
 *
 * 생성·사용 예)
 *   new PostResponse(1L, "closed", "ran", instant)
 *   → JSON: {"id":1,"title":"closed","loginId":"ran","createdAt":"..."}
 *
 * 변환 위치: PostService.toResponse(Post) → listResponse() → PostApiController
 */
public record PostResponse(Long id, String title, String loginId, Instant createdAt) {
}
