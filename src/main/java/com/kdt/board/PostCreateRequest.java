package com.kdt.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
 * [09] PostCreateRequest — 생성 요청 DTO (POST /api/v1/posts @RequestBody)
 *
 * 요청(이 파일) title, body, memberId  ≠  응답(PostResponse) id, title, loginId, createdAt
 * Entity 직접 노출 대신 입력 모양만 받음. @Size 는 Post @Column(length) 와 맞춤.
 *
 * @NotBlank  문자열 null/빈문자/공백 불가
 * @NotNull   Long null 불가
 * @Size(max) 길이 상한 (title≤20, body≤100)
 * @Valid 실패 → 400  |  자원 없음(404) 과 구분
 *
 * record(Java 16+) + Jackson JSON 키 = 컴포넌트 이름
 */
public record PostCreateRequest(
        @NotBlank @Size(max = 20) String title,
        @NotBlank @Size(max = 100) String body,
        @NotNull Long memberId
) {
}
