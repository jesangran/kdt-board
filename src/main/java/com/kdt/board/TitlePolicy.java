package com.kdt.board;

/*
 * TitlePolicy — 게시글 제목 규칙 (TDD 연습)
 * 검사 순서: blank(null/공백) → 길이 초과 → ok
 * isBlank(): "" 뿐 아니라 공백만 있는 문자열도 true (isEmpty()는 길이 0만)
 */
public class TitlePolicy {

    /** 허용 최대 길이(포함). 초과 시 "title is long" */
    private static final int MAX_TITLE_LENGTH = 20;

    /**
     * @return "ok" — 규칙을 통과한 경우
     * @throws IllegalArgumentException blank → "title is blank" / 길이 초과 → "title is long"
     */
    public String check(String title) {
        // 1) null 또는 blank(공백만 포함) → 거부
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is blank");
        }

        // 2) 길이 초과 → 거부 (blank 검사 이후라 공백만 긴 문자열은 blank로 먼저 걸림)
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("title is long");
        }

        // 3) 통과
        return "ok";
    }
}
