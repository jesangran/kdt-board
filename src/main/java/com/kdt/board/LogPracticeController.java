package com.kdt.board;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * [06-SQL-로그와-N+1] 로그 실습 — DB 저장/조회 없음
 *
 * 노션 포인트: "로그를 남기는 코드" 와 "로그를 표시하는 설정" 을 구분
 *
 * 1) 남기는 코드 (여기)
 *    - System.out.println / log.debug / log.info
 *    - "이 메시지를 이 레벨로 남긴다" 만 결정
 *
 * 2) 표시하는 설정 (application.yaml 의 logging.level)
 *    - com.kdt.board: info 이면 → INFO 이상만 콘솔에 보임
 *    - DEBUG 로 낮추면 → log.debug 도 보임
 *    - 코드를 안 바꿔도 설정만으로 보이는 로그가 달라짐
 *
 * 확인: GET /logs/check
 *   - println 은 레벨과 무관하게 거의 항상 출력
 *   - debug 는 패키지 레벨이 DEBUG 일 때만
 *   - info  는 INFO 이상이면 출력 (지금 com.kdt.board: info → 보임)
 */
@RestController
public class LogPracticeController {

    // private static final: 이 클래스 전용 · 공유 · 불변 로거
    private static final Logger log = LoggerFactory.getLogger(LogPracticeController.class);

    @GetMapping("/logs/check")
    public String check() {
        // 표준 출력 — SLF4J 레벨 필터를 거치지 않음 (설정과 별개)
        System.out.println("### log practice println");
        // DEBUG: logging.level.com.kdt.board 가 DEBUG 여야 콘솔에 보임
        log.debug("### log practice debug");
        // INFO: 지금 설정(info)이면 보임
        log.info("### log practice info");

        return "logged";
    }
}
