package com.kdt.board;

/*
 * [09] NotFoundException — 자원 없음
 * Service 가 throw → ApiExceptionAdvice 가 404 + ApiError
 * 400(@Valid 실패) 과 구분: 요청 형식은 OK, 해당 id 만 DB에 없음
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
