package com.kdt.board;

import java.time.Instant;

/*
 * [08·09] PostResponse — 성공 응답 DTO
 * 목록·단건·생성·수정 성공 시 동일 모양. (실패는 ApiError)
 * toResponse(Post) 에서 loginId 등 필요한 값만 담음.
 */
public record PostResponse(Long id, String title, String loginId, Instant createdAt) {
}
