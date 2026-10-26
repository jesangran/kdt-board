package com.kdt.board;

/*
 * [09] ApiError — 실패 응답 JSON { code, message }
 * code=분기용 고정키("NOT_FOUND"), message=설명
 * HTTP 404 + 이 본문 ← ApiExceptionAdvice
 */
public record ApiError(String code, String message) {
}
