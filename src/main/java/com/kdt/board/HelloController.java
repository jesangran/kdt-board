package com.kdt.board;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 * =========================================================
 * [Spring DI 용어 한줄 정리]
 * =========================================================
 * 1. DI(의존성 주입)
 *    - 필요한 부품을 직접 new 하지 않고, 밖에서 넣어주는 것
 *    - 예: 자동차가 엔진을 직접 만들지 않고 생성자로 받음
 *
 * 2. Bean(빈)
 *    - Spring이 대신 만들어서 관리해주는 객체
 *    - @Service, @RestController, @Component 가 붙으면 빈 후보
 *
 * 3. Container(컨테이너)
 *    - 빈들을 보관하는 창고
 *    - 등록해두고, 필요할 때 꺼내서 주입해 줌
 *
 * 4. 불변성 / final
 *    - 한번 정하면 바꿀 수 없게 만드는 것
 *    - private final HelloService → 생성 후 교체 불가 → 안전
 *
 * 5. NPE(NullPointerException)
 *    - null인 걸 쓰려고 해서 나는 에러
 *    - 생성자 주입은 주입 실패 시 객체 생성 자체가 안 됨 → 조기 발견
 *
 * 6. 순환 의존성
 *    - A가 B 필요, B가 A 필요 → 닭-달걀 상황
 *    - 생성자 주입: 기동 시 바로 에러 / 필드·Setter: 늦게 발견될 수 있음
 *
 * 7. 3단계 캐시
 *    - Spring이 순환 의존성을 억지로 푸는 임시 저장소
 *    - Setter/필드 주입에서만 동작. 설계 문제를 숨기는 꼼수에 가까움
 *
 * 8. @Lazy
 *    - 진짜 필요할 때까지 빈 생성을 미룸 (순환 의존성 임시 해결)
 *    - 근본 해결은 설계를 고치는 것
 *
 * 9. SRP(단일 책임 원칙)
 *    - 한 클래스는 한 가지 일만 하자
 *    - 의존성 5개 이상 → 책임 과다 신호
 *
 * 10. 보일러플레이트
 *     - 매번 반복해야 하는 지루한 코드 (생성자 대입문 등)
 *
 * 11. Lombok / @RequiredArgsConstructor
 *     - 반복 코드(보일러플레이트)를 자동 생성해 주는 도구
 *     - @RequiredArgsConstructor = final 필드를 모아 생성자를 자동 생성 → 생성자 주입
 *     - ★ 런타임이 아니라 "컴파일 시점"에 생성됨 (.java → .class 변환 때)
 *       소스에는 생성자가 없어도, 컴파일된 .class에는 생성자가 들어감
 *       예: public HelloController(HelloService helloService) { ... }
 *     - Spring은 그 생성자를 보고 DI를 수행함
 *     - IDE에 Lombok 플러그인이 없으면 "생성자 없음" 빨간줄이 날 수 있음
 *       (실제 빌드/실행은 annotation processor가 처리하므로 동작은 함)
 *
 * 12. Setter / Getter
 *     - Setter: 값 넣기 / Getter: 값 꺼내기
 *     - Setter 주입 = Setter로 의존성을 나중에 넣는 방식
 *
 * 13. 단위 테스트
 *     - 딱 하나의 기능만 따로 떼서 검사
 *     - 생성자 주입이면 new HelloController(mock) 처럼 Spring 없이 가능
 *
 * 14. mock
 *     - 테스트용 가짜 객체 (진짜 동작 대신 "한 척")
 *
 * =========================================================
 * [DI 방식 비교] 생성자 vs Setter vs 필드
 * =========================================================
 * ★ 생성자 주입 (권장, 현재 방식)
 *   - final 불변 / 의존관계 명확 / NPE·순환의존성 조기 발견 / 테스트 용이
 *   - 실무: @RequiredArgsConstructor + private final
 *
 * ○ Setter 주입
 *   - 선택적 의존성에만. final 불가, 누락 시 NPE 위험
 *
 * ✕ 필드 주입 (@Autowired private ...)
 *   - 짧지만 테스트·불변성·의존관계 파악에 불리 → 지양
 *
 * 결론:
 * "생성자 주입 = 안전하고 테스트하기 좋음.
 *  Setter 주입 = 가끔 필요할 때만.
 *  필드 주입 = 옛날 방식, 쓰지 말자."
 *
 * 흐름: Component 등록 → Container가 Bean 보관 → DI로 연결
 */
@RestController // API 요청을 받아 결과 데이터를 응답으로 주는 Bean/Component
@RequiredArgsConstructor // 컴파일 시 final 필드용 생성자 자동 생성 → 생성자 주입
public class HelloController {

    // DI 대상. final → 불변 (생성 후 변경 불가)
    // Lombok이 이 필드를 받는 생성자를 컴파일 때 만들어 줌
    private final HelloService helloService;

    /*
    // [참고1] 생성자 주입을 직접 쓴 형태
    // @RequiredArgsConstructor가 컴파일 시 아래와 같은 코드를 .class에 생성함
    public HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    // [참고2] Setter 주입 예시 (선택적 의존성용, 현재 미사용)
    // @Autowired
    // public void setHelloService(HelloService helloService) {
    //     this.helloService = helloService;
    // }

    // [참고3] 필드 주입 예시 (지양, 현재 미사용)
    // @Autowired
    // private HelloService helloService;
    */

    @GetMapping("/hello") // 데이터를 조회할 때 쓰는 매핑
    public String hello() {
        // 주입받은 helloService 사용 (직접 new 하지 않음 = DI)
        return this.helloService.message();
    }

    @GetMapping("/testhello")
    public String hello2() {
        List<String> strings = this.helloService.message2("good","h2");
        String ab = strings.get(0) + strings.get(1);
        return ab;
    }
}
