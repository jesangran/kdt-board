package com.kdt.board;

/*
 * [static import 모음]
 *   given(...)           → Mockito BDD 스텁
 *   get(...)             → MockMvc GET 요청 빌더
 *   content() / status() → 응답 본문·상태 검증
 */
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/*
 * ============================================================================
 * [10] HelloControllerTest
 * HelloController의 GET /hello 를 검증하는 웹 슬라이스 테스트
 * ============================================================================
 *
 * [슬라이스 테스트 @WebMvcTest]
 *   웹 계층만 띄움: 컨트롤러·Jackson·검증 등
 *   제외: @Service·@Repository·DB·트랜잭션
 *   → @SpringBootTest 보다 빠르고 MySQL 없이 실행
 *
 * [MockMvc]
 *   진짜 톰캣/포트 없이 HTTP 요청·응답을 흉내 냄 ("가짜 브라우저").
 *   이름에 Mock이 있어도 컨트롤러는 실제 객체. 서버만 안 띄운다.
 *
 * [실물 vs 모의]
 *   테스트 대상(HelloController) = 실제
 *   의존성(HelloService) = @MockitoBean 모의 → given으로 반환값만 지정
 *   → 이 테스트는 "컨트롤러 연결·응답"만 확인. 서비스 구현은 HelloServiceTest 가 담당.
 *
 * [흐름]
 *   perform(GET /hello) → DispatcherServlet → HelloController
 *   → mock HelloService.message() → 스텁 "kimbap" → andExpect 상태·본문
 * ============================================================================
 */
@WebMvcTest(HelloController.class)
class HelloControllerTest {

    /** 스프링이 준비한 MockMvc 주입 (테스트에서는 필드 주입이 흔함) */
    @Autowired
    private MockMvc mockMvc;

    /*
     * @MockitoBean — Boot 4.x (@MockBean 대체)
     * 컨텍스트의 HelloService 를 Mockito mock 으로 교체.
     * 스텁 없으면 String 기본값 null.
     */
    @MockitoBean
    private HelloService helloService;

    @Test
    void hello_returns_kimbap() throws Exception {
        // Arrange: 모의 서비스 반환값 준비 (when(...).thenReturn 과 동일 계열)
        given(this.helloService.message()).willReturn("kimbap");

        // Act: GET /hello 실행 (네트워크 X)
        this.mockMvc.perform(get("/hello"))
                // Assert: 200 + 본문 "kimbap"
                .andExpect(status().isOk())
                .andExpect(content().string("kimbap"));
    }
}
