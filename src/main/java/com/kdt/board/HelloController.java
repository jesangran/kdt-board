package com.kdt.board;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 * [DI] HelloController — 생성자 주입 (@RequiredArgsConstructor + final)
 *
 * Bean=Spring 관리 객체 / Container=빈 창고 / DI=밖에서 넣어 줌(new 금지)
 * 생성자 주입 권장: final 불변, 순환·누락 조기 발견, 테스트 용이
 * Setter=선택적만 / 필드 @Autowired=지양
 * Lombok @RequiredArgsConstructor → 컴파일 시 final 필드 생성자 생성
 *
 * GET /hello → HelloService.message() (+ HelloAroundAspect)
 * GET /testhello → message2 (+ HelloMessageAspect)
 */
@RestController
@RequiredArgsConstructor
public class HelloController {

    private final HelloService helloService;

    @GetMapping("/hello")
    public String hello() {
        return this.helloService.message();
    }

    @GetMapping("/testhello")
    public String hello2() {
        List<String> strings = this.helloService.message2("good", "h2");
        return strings.get(0) + strings.get(1);
    }
}
