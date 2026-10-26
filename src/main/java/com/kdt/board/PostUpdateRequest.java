package com.kdt.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 * [09] PostUpdateRequest — 수정 요청 DTO (PUT /api/v1/posts/{id} @RequestBody)
 * title·body 만 (memberId 변경 없음). 검증 규칙은 PostCreateRequest 와 동일 계열.
 */
public record PostUpdateRequest(
        @NotBlank @Size(max = 20) String title,
        @NotBlank @Size(max = 100) String body
) {
}
