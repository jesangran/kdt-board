package com.kdt.board;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
 * [09] ApiExceptionAdvice — REST 예외 → HTTP 응답 (공통)
 * @RestControllerAdvice + @ExceptionHandler
 * NotFoundException → 404 + ApiError
 * ResponseEntity = 상태 코드 + 본문 (기본 return DTO 는 보통 200)
 * ※ AOP(@Aspect)와 다름: 여기는 "예외→JSON/상태" 웹 공통 처리
 * ※ @Valid 실패(MethodArgumentNotValidException)는 별도/기본 400
 */
@RestControllerAdvice
public class ApiExceptionAdvice {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handlerNotFound(NotFoundException ex) {
        ApiError body = new ApiError("NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
