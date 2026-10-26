package com.kdt.board;

/*
 * ============================================================================
 * PostApiControllerTest
 * 게시글 REST API(PostApiController)의 웹 계층을 검증하는 슬라이스 테스트
 * ============================================================================
 *
 * [테스트 대상]
 *   PostApiController  (기본 경로: /api/v1/posts)
 *     GET    /api/v1/posts                  전체 목록
 *     GET    /api/v1/posts/{id}             단건 조회
 *     GET    /api/v1/posts/count-by-title   제목별 개수
 *     POST   /api/v1/posts                  생성
 *     PUT    /api/v1/posts/{id}             수정
 *     DELETE /api/v1/posts/{id}             삭제
 *
 * [테스트 종류: 슬라이스 테스트(slice test)]
 *   @WebMvcTest는 스프링 전체를 띄우지 않고 "웹 계층"만 잘라서 띄웁니다.
 *     - 포함: 컨트롤러, JSON 변환(Jackson), @Valid 검증, 예외 처리기 등
 *     - 제외: @Service, @Repository, DB, 트랜잭션
 *   그래서 실제 PostService 대신 @MockitoBean으로 만든 가짜 객체를 사용합니다.
 *   DB가 없어도 실행되고, @SpringBootTest보다 훨씬 빠릅니다.
 *
 * [왜 컨트롤러를 따로 테스트하나?]
 *   서비스 로직이 맞더라도 URL, HTTP 메서드, 상태 코드, JSON 필드 모양이 틀리면
 *   클라이언트 입장에서는 버그입니다. 이 테스트는 API의 "계약(contract)"을
 *   코드로 고정합니다.
 *     - 성공: 200/201/204 와 정해진 JSON
 *     - 잘못된 입력: 400 (서비스까지 가지 않음)
 *     - 없는 데이터: 404 + {"code":"NOT_FOUND", "message":"..."}
 *
 * [전체 흐름]
 *   MockMvc(가짜 브라우저)
 *     -> DispatcherServlet
 *       -> URL/HTTP 메서드에 맞는 컨트롤러 메서드 선택
 *         -> @Valid 검증 (실패하면 여기서 400, 컨트롤러 호출 안 됨)
 *           -> PostService (mock: 미리 정한 값 또는 예외 반환)
 *             -> PostApiController가 응답(ResponseEntity/JSON) 생성
 *   -> andExpect(...)로 상태 코드/헤더/본문 검증
 *
 * [등장인물]
 *   - PostResponse: record(id, title, loginId, createdAt)
 *       -> Jackson이 JSON 객체로 직렬화. record라서 값이 같으면 equals도 true.
 *   - PostCreateRequest: record(title, body, memberId) + @NotBlank/@Size/@NotNull
 *   - PostUpdateRequest: record(title, body) + @NotBlank/@Size
 *   - ApiExceptionAdvice: @RestControllerAdvice. NotFoundException을 잡아
 *       404 + ApiError(code, message) JSON으로 바꿔 줍니다.
 *   - MockMvc: 진짜 서버 없이 요청/응답을 흉내 내는 도구.
 *
 * [실행 방법]
 *   .\gradlew.bat test --tests "com.kdt.board.PostApiControllerTest"
 *   특정 메서드만:
 *   .\gradlew.bat test --tests "com.kdt.board.PostApiControllerTest.one_returns_200_json"
 * ============================================================================
 */

/*
 * [import 읽는 법]
 * ----------------------------------------------------------------------------
 * MockMvc 테스트는 static import가 많아 처음에는 복잡해 보입니다.
 * 크게 세 종류로 나눠 보면 쉽습니다.
 *
 *   1. 요청 만들기
 *      MockMvcRequestBuilders.get/post/put/delete(...)
 *   2. 응답 검증하기
 *      MockMvcResultMatchers.status()/content()/jsonPath()/header()
 *   3. 가짜 객체 다루기
 *      Mockito.given/willThrow/verify/verifyNoInteractions,
 *      ArgumentMatchers.any/eq
 *
 * 아래에 verifyNoInteractions가 두 번 import 되어 있는데, 같은 이름을 중복
 * import해도 컴파일에는 문제가 없습니다. 정리하고 싶다면 한 줄을 지워도 됩니다.
 */

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/*
 * @WebMvcTest(PostApiController.class)
 * ----------------------------------------------------------------------------
 * 웹 계층만 올리고, 그중 PostApiController를 테스트합니다.
 * 다른 컨트롤러는 등록하지 않아 테스트 범위가 좁고 빠릅니다.
 *
 * @Import(ApiExceptionAdvice.class)
 * ----------------------------------------------------------------------------
 * 예외 처리기를 테스트 컨텍스트에 명시적으로 포함합니다.
 * 이렇게 해야 NotFoundException이 발생했을 때 실제 애플리케이션과 같은 방식으로
 * 404 + ApiError JSON 응답이 만들어지는지 검증할 수 있습니다.
 * (예외 처리기를 포함하지 않으면 예외가 그대로 밖으로 나가 테스트가 실패하거나
 *  500으로 처리되어, 우리가 원하는 "404 계약"을 검증할 수 없습니다.)
 *
 * @MockitoBean private PostService postService;
 * ----------------------------------------------------------------------------
 * 컨트롤러가 필요로 하는 PostService를 가짜 객체로 교체합니다.
 * 이렇게 하면 DB/비즈니스 로직과 분리해서 "웹 계층만" 검증할 수 있습니다.
 */
@WebMvcTest(PostApiController.class)
@Import(ApiExceptionAdvice.class)
class PostApiControllerTest {

    /*
     * MockMvc: 진짜 톰캣 없이 HTTP 요청을 흉내 내는 객체입니다.
     * 스프링 테스트 컨텍스트가 만들어 준 것을 @Autowired로 주입받습니다.
     */
    @Autowired
    private MockMvc mockMvc;

    /*
     * PostService의 가짜 객체입니다.
     * 테스트마다 given(...)으로 원하는 반환값/예외를 정해 줍니다.
     */
    @MockitoBean
    private PostService postService;

    /*
     * ========================================================================
     * 1) 단건 조회 성공
     *    GET /api/v1/posts/1  ->  200 OK + 게시글 JSON
     * ========================================================================
     *
     * [검증하는 계약]
     *   - 상태 코드 200
     *   - Content-Type이 application/json 계열
     *   - JSON 필드가 id, title, loginId, createdAt 딱 4개
     *   - 각 필드의 값이 서비스가 돌려준 값과 일치
     *
     * [흐름]
     *   given(...)으로 서비스가 반환할 PostResponse를 미리 정함
     *   -> GET 요청
     *   -> 컨트롤러가 postService.findResponse(1L) 호출
     *   -> mock이 준비된 PostResponse 반환
     *   -> Jackson이 record를 JSON으로 변환
     *   -> jsonPath로 필드 검증
     */
    @Test
    void one_returns_200_json() throws Exception {
        // Arrange: 서비스가 돌려줄 응답을 만듭니다.
        // Instant.parse("...Z")는 UTC 기준 시각을 만듭니다.
        // Jackson은 Instant를 "2026-08-21T00:00:00Z" 같은 ISO-8601 문자열로
        // 직렬화합니다(Spring Boot가 JavaTimeModule을 자동 등록).
        PostResponse row = new PostResponse(1L, "closed", "kdt01",
                                            Instant.parse("2026-08-21T00:00:00Z"));

        // Stub: findResponse(1L)이 호출되면 row를 반환하라고 지정합니다.
        // 인자 1L은 실제 요청 경로의 id와 equals로 비교됩니다.
        given(this.postService.findResponse(1L)).willReturn(row);

        // Act + Assert: GET /api/v1/posts/1 요청을 보내고 응답을 검증합니다.
        // {id} 자리에는 두 번째 인자 1L이 들어가 "/api/v1/posts/1"이 됩니다.
        this.mockMvc.perform(get("/api/v1/posts/{id}", 1L))

                    // 200 OK
                    .andExpect(status().isOk())

                    // 응답 Content-Type이 application/json과 호환되는지 확인.
                    // "application/json;charset=UTF-8"처럼 파라미터가 붙어도 통과합니다.
                    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))

                    // jsonPath는 JSON 본문에서 값을 꺼내는 표현식입니다.
                    //   $        : 문서 전체(root)
                    //   $.id     : root의 id 필드
                    // value(1)은 숫자 1과 비교합니다.
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("closed"))
                    .andExpect(jsonPath("$.loginId").value("kdt01"))
                    .andExpect(jsonPath("$.createdAt").value("2026-08-21T00:00:00Z"))

                    // 객체의 필드 개수가 4개인지 확인합니다.
                    // 응답에 의도하지 않은 필드(예: body, memberId)가 추가로
                    // 새어 나가지 않도록 지키는 계약 테스트입니다.
                    .andExpect(jsonPath("$.length()").value(4));

        // verify: 컨트롤러가 서비스를 정확히 한 번, 올바른 인자로 호출했는지 확인.
        // (verify를 쓰지 않으면 "응답은 맞는데 서비스 호출이 잘못된" 경우를 놓칠 수 있음)
        verify(this.postService).findResponse(1L);
    }

    /*
     * ========================================================================
     * 2) 단건 조회 실패 - 없는 게시글
     *    GET /api/v1/posts/999999  ->  404 + 에러 JSON
     * ========================================================================
     *
     * [검증하는 계약]
     *   - 상태 코드 404
     *   - 본문이 {"code":"NOT_FOUND", "message":"post missing"}
     *
     * [흐름]
     *   서비스가 NotFoundException을 던짐
     *   -> ApiExceptionAdvice의 @ExceptionHandler가 잡음
     *   -> ApiError(code, message)를 JSON으로 만들어 404 응답
     *
     * [왜 예외를 JSON으로 바꾸나?]
     *   클라이언트가 일관되게 처리할 수 있도록 "기계가 읽을 수 있는" 에러 형식을
     *   정해 두는 것입니다. 스택 트레이스나 HTML 에러 페이지가 나가면 안 됩니다.
     */
    @Test
    void missing_post_returns_404_json() throws Exception {
        // willThrow: 이 호출이 일어나면 예외를 던지라고 스텁합니다.
        given(this.postService.findResponse(999999L))
                .willThrow(new NotFoundException("post missing"));

        this.mockMvc.perform(get("/api/v1/posts/{id}", 999999L))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))

                    // ApiError record의 두 필드를 확인합니다.
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("post missing"));

        verify(this.postService).findResponse(999999L);
    }

    /*
     * ========================================================================
     * 3) 생성 실패 - 제목이 빈 문자열
     *    POST /api/v1/posts  ->  400, 서비스는 호출되지 않음
     * ========================================================================
     *
     * [검증하는 계약]
     *   - @Valid 검증이 요청 본문을 먼저 검사한다.
     *   - title이 ""이면 @NotBlank 위반 -> 400 Bad Request
     *   - 검증에서 걸렸으므로 서비스(비즈니스 로직/DB)까지 가지 않는다.
     *
     * [왜 "서비스 미호출"까지 확인하나?]
     *   400만 확인하면, 검증이 통과된 뒤 서비스에서 예외가 나서 400이 된 것인지
     *   구분할 수 없습니다. verifyNoInteractions로 "컨트롤러 입구에서 막혔다"는
     *   사실을 명확히 증명합니다. 불필요한 DB 접근을 막는 성능/안전 측면도 있습니다.
     */
    @Test
    void blank_title_post_returns_400_before_service() throws Exception {
        this.mockMvc.perform(post("/api/v1/posts")
                                     // 요청 본문이 JSON임을 알려 주는 헤더입니다.
                                     .contentType(APPLICATION_JSON)

                                     // 실제 요청 본문(JSON 문자열).
                                     // Jackson이 PostCreateRequest record로 변환합니다.
                                     // title="" -> @NotBlank 위반
                                     .content("{\"title\":\"\",\"body\":\"menu\",\"memberId\":7}"))
                    .andExpect(status().isBadRequest());

        // 서비스의 어떤 메서드도 호출되지 않았음을 확인합니다.
        verifyNoInteractions(this.postService);
    }

    /*
     * ========================================================================
     * 4) 생성 성공
     *    POST /api/v1/posts  ->  201 Created + Location 헤더 + 응답 JSON
     * ========================================================================
     *
     * [검증하는 계약]
     *   - 상태 코드 201
     *   - Location 헤더가 새 리소스의 주소(/api/v1/posts/42)
     *   - 응답 JSON 4개 필드의 값
     *   - 응답에 body/memberId 같은 내부 필드가 노출되지 않음
     *   - 서비스에 전달된 요청 값이 원본 JSON과 일치
     *
     * [201 Created와 Location]
     *   생성 성공을 나타내는 표준 방식입니다. Location 헤더를 보고 클라이언트가
     *   새로 만들어진 리소스를 바로 조회할 수 있습니다.
     *
     * [record와 equals]
     *   PostCreateRequest는 record이므로 값이 같으면 equals가 true입니다.
     *   그래서 given(this.postService.create(expected))는 Jackson이 만든
     *   "값이 같은" 객체와도 매칭됩니다.
     */
    @Test
    void create_returns_201_and_passes_request_values() throws Exception {
        // Arrange: 서비스에 전달될 것으로 기대하는 요청 값
        PostCreateRequest expected = new PostCreateRequest("plate", "menu", 7L);

        // 서비스가 반환할 생성 결과(DB가 id=42를 부여했다고 가정)
        PostResponse created = new PostResponse(42L, "plate", "writer07",
                                                Instant.parse("2026-08-21T00:00:00Z"));
        given(this.postService.create(expected)).willReturn(created);

        // Act + Assert
        this.mockMvc.perform(post("/api/v1/posts")
                                     .contentType(APPLICATION_JSON)
                                     .content("{\"title\":\"plate\",\"body\":\"menu\",\"memberId\":7}"))
                    .andExpect(status().isCreated())

                    // ResponseEntity.created(location)이 만든 Location 헤더.
                    // header().string(이름, 기대값)은 정확히 일치하는지 봅니다.
                    .andExpect(header().string("Location", "/api/v1/posts/42"))
                    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(42))
                    .andExpect(jsonPath("$.title").value("plate"))
                    .andExpect(jsonPath("$.loginId").value("writer07"))
                    .andExpect(jsonPath("$.createdAt").value("2026-08-21T00:00:00Z"))
                    .andExpect(jsonPath("$.length()").value(4))

                    // doesNotHaveJsonPath: 해당 경로가 "없어야" 통과합니다.
                    // 요청에는 있었지만 응답에는 없어야 하는 내부 필드를 검사합니다.
                    // (응답 DTO에 불필요한 정보를 싣지 않았는지 확인)
                    .andExpect(jsonPath("$.body").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.memberId").doesNotHaveJsonPath());

        // 서비스에 정확히 expected와 같은 값이 전달됐는지 확인합니다.
        // record의 equals 덕분에 필드 값 비교가 됩니다.
        verify(this.postService).create(expected);
    }

    /*
     * ========================================================================
     * 5) 목록 조회 - 데이터가 1건 있는 경우
     *    GET /api/v1/posts  ->  200 + JSON 배열
     * ========================================================================
     *
     * [검증하는 계약]
     *   - 응답이 객체가 아니라 배열($)이다.
     *   - 배열의 0번째 요소가 올바른 4개 필드를 가진다.
     *   - 배열 길이가 1이다.
     *
     * [jsonPath 표기]
     *   $        : 배열 전체
     *   $[0]     : 첫 번째 요소
     *   $[0].id  : 첫 번째 요소의 id
     *   $.length(): 배열이면 요소 개수, 객체면 필드 개수
     */
    @Test
    void list_returns_200_json_array() throws Exception {
        // ArrayList에 응답 1건을 담아 서비스 반환값으로 지정합니다.
        List<PostResponse> rows = new ArrayList<>();
        rows.add(new PostResponse(1L, "closed", "kdt01",
                                  Instant.parse("2026-08-21T00:00:00Z")));
        given(this.postService.listResponse()).willReturn(rows);

        this.mockMvc.perform(get("/api/v1/posts"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))

                    // root가 배열인지 확인합니다.
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].title").value("closed"))
                    .andExpect(jsonPath("$[0].loginId").value("kdt01"))
                    .andExpect(jsonPath("$[0].createdAt").value("2026-08-21T00:00:00Z"))

                    // 첫 번째 요소의 필드 개수는 4개
                    .andExpect(jsonPath("$[0].length()").value(4))

                    // 배열 전체 길이는 1
                    .andExpect(jsonPath("$.length()").value(1));

        verify(this.postService).listResponse();
    }

    /*
     * ========================================================================
     * 6) 목록 조회 - 데이터가 0건인 경우
     *    GET /api/v1/posts  ->  200 + []
     * ========================================================================
     *
     * [검증하는 계약]
     *   결과가 없어도 404가 아니라 200과 빈 배열을 돌려준다.
     *   "목록 조회는 성공했고, 그 결과가 비어 있다"는 의미이기 때문입니다.
     *
     * [content().json("[]")]
     *   문자열을 그대로 비교하는 string()과 달리, JSON으로 파싱해서
     *   "의미상 같은지" 비교합니다. 공백/줄바꿈/필드 순서 차이는 무시됩니다.
     */
    @Test
    void empty_list_returns_200_and_empty_array() throws Exception {
        given(this.postService.listResponse()).willReturn(new ArrayList<>());

        this.mockMvc.perform(get("/api/v1/posts"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                    .andExpect(content().json("[]"));

        verify(this.postService).listResponse();
    }

    /*
     * ========================================================================
     * 7) 제목별 개수 조회
     *    GET /api/v1/posts/count-by-title?title=closed  ->  200 + 숫자 2
     * ========================================================================
     *
     * [검증하는 계약]
     *   - 쿼리 파라미터 title이 서비스까지 전달된다.
     *   - 응답이 JSON 숫자(따옴표 없는 2)다.
     *
     * [.param("title", "closed")]
     *   URL 뒤에 ?title=closed를 붙이는 것과 같습니다.
     *
     * [content().string("2")]
     *   primitive long은 Jackson이 문자열 "2"가 아니라 숫자 2로 직렬화합니다.
     *   그래서 본문은 정확히 `2`입니다. jsonPath의 isNumber()와 함께 확인하면
     *   "숫자 타입으로 나갔다"는 점까지 검증됩니다.
     */
    @Test
    void title_query_returns_json_number() throws Exception {
        given(this.postService.countByTitle("closed")).willReturn(2L);

        this.mockMvc.perform(get("/api/v1/posts/count-by-title")
                                     .param("title", "closed"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))

                    // root 값 자체가 숫자인지 확인
                    .andExpect(jsonPath("$").isNumber())

                    // 본문 텍스트가 정확히 "2"인지 확인 (따옴표가 없어야 함)
                    .andExpect(content().string("2"));

        verify(this.postService).countByTitle("closed");
    }

    /*
     * ========================================================================
     * 8) 제목별 개수 조회 - 필수 파라미터 누락
     *    GET /api/v1/posts/count-by-title  ->  400, 서비스 미호출
     * ========================================================================
     *
     * [검증하는 계약]
     *   @RequestParam("title")은 기본값 required=true입니다.
     *   파라미터가 없으면 스프링이 컨트롤러를 호출하기 전에 400으로 거절합니다.
     */
    @Test
    void missing_title_query_returns_400_before_service() throws Exception {
        this.mockMvc.perform(get("/api/v1/posts/count-by-title"))
                    .andExpect(status().isBadRequest());

        verifyNoInteractions(this.postService);
    }

    /*
     * ========================================================================
     * 9) 경로 변수 타입 오류
     *    GET /api/v1/posts/abc  ->  400, 서비스 미호출
     * ========================================================================
     *
     * [검증하는 계약]
     *   컨트롤러의 @PathVariable Long id는 숫자여야 합니다.
     *   "abc"는 Long으로 변환할 수 없으므로 스프링이 400으로 거절합니다.
     *
     * [왜 서비스 미호출을 확인하나?]
     *   타입 변환 단계에서 막혔다는 것을 증명합니다. 잘못된 타입이 서비스까지
     *   내려가면 예상치 못한 예외나 불필요한 DB 조회가 발생할 수 있습니다.
     */
    @Test
    void non_numeric_id_returns_400_before_service() throws Exception {
        this.mockMvc.perform(get("/api/v1/posts/abc"))
                    .andExpect(status().isBadRequest());

        verifyNoInteractions(this.postService);
    }

    /*
     * ========================================================================
     * 10) 수정 성공
     *     PUT /api/v1/posts/1  ->  200 + 수정된 JSON
     * ========================================================================
     *
     * [검증하는 계약]
     *   - PUT 요청 본문이 PostUpdateRequest로 변환된다.
     *   - @Valid 검증을 통과하면 서비스가 호출된다.
     *   - 서비스가 돌려준 수정 결과가 그대로 JSON으로 나간다.
     *
     * [ArgumentMatchers]
     *   given(this.postService.update(eq(1L), any(PostUpdateRequest.class)))
     *   - eq(1L): 값이 1L인 인자와 매칭
     *   - any(PostUpdateRequest.class): 어떤 PostUpdateRequest든 매칭
     *   규칙: 한 메서드 호출에서 인자 하나라도 matcher를 쓰면, 나머지 인자도
     *   모두 matcher(eq/any 등)로 써야 합니다. 섞으면 Mockito가 예외를 던집니다.
     *
     * [verify에서의 record 비교]
     *   new PostUpdateRequest("notice", "edited")는 record이므로 equals가
     *   필드 값으로 비교됩니다. 그래서 값이 정확히 같은지 검증할 수 있습니다.
     */
    @Test
    void update_returns_200_json() throws Exception {
        PostResponse edited = new PostResponse(1L, "notice", "kdt01",
                                               Instant.parse("2026-08-21T00:00:00Z"));
        given(this.postService.update(eq(1L), any(PostUpdateRequest.class)))
                .willReturn(edited);

        this.mockMvc.perform(put("/api/v1/posts/{id}", 1L)
                                     .contentType(APPLICATION_JSON)
                                     .content("{\"title\":\"notice\",\"body\":\"edited\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                    .andExpect(jsonPath("$.title").value("notice"))
                    .andExpect(jsonPath("$.loginId").value("kdt01"))
                    .andExpect(jsonPath("$.createdAt").value("2026-08-21T00:00:00Z"))
                    .andExpect(jsonPath("$.length()").value(4));

        verify(this.postService).update(1L, new PostUpdateRequest("notice", "edited"));
    }

    /*
     * ========================================================================
     * 11) 수정 실패 - 없는 게시글
     *     PUT /api/v1/posts/999999  ->  404 + 에러 JSON
     * ========================================================================
     *
     * 조회 때와 마찬가지로 서비스가 NotFoundException을 던지면
     * ApiExceptionAdvice가 404 JSON으로 변환합니다.
     */
    @Test
    void missing_post_put_returns_404_json() throws Exception {
        given(this.postService.update(eq(999999L), any(PostUpdateRequest.class)))
                .willThrow(new NotFoundException("post missing"));

        this.mockMvc.perform(put("/api/v1/posts/{id}", 999999L)
                                     .contentType(APPLICATION_JSON)
                                     .content("{\"title\":\"notice\",\"body\":\"edited\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("post missing"));

        verify(this.postService).update(999999L, new PostUpdateRequest("notice", "edited"));
    }

    /*
     * ========================================================================
     * 12) 수정 실패 - 공백뿐인 제목
     *     PUT /api/v1/posts/1  ->  400, 서비스 미호출
     * ========================================================================
     *
     * [@NotBlank의 의미]
     *   null, "", "   "처럼 공백만 있는 문자열을 모두 거절합니다.
     *   (단순히 길이만 보는 @NotEmpty/@Size와 다른 점입니다.)
     */
    @Test
    void blank_title_put_returns_400_before_service() throws Exception {
        this.mockMvc.perform(put("/api/v1/posts/{id}", 1L)
                                     .contentType(APPLICATION_JSON)
                                     .content("{\"title\":\"   \",\"body\":\"edited\"}"))
                    .andExpect(status().isBadRequest());

        verifyNoInteractions(this.postService);
    }

    /*
     * ========================================================================
     * 13) 삭제 성공
     *     DELETE /api/v1/posts/1  ->  204 No Content
     * ========================================================================
     *
     * [검증하는 계약]
     *   - 상태 코드 204 (성공했지만 돌려줄 본문이 없음)
     *   - 본문이 비어 있음
     *   - 서비스의 delete(1L)가 호출됨
     *
     * [스텁이 없는 이유]
     *   PostService.delete는 void 메서드이고, mock의 void 메서드는 기본적으로
     *   아무 일도 하지 않습니다. 그래서 반환값을 정할 필요가 없습니다.
     */
    @Test
    void delete_ok_returns_204() throws Exception {
        this.mockMvc.perform(delete("/api/v1/posts/{id}", 1L))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

        verify(this.postService).delete(1L);
    }

    /*
     * ========================================================================
     * 14) 삭제 실패 - 없는 게시글
     *     DELETE /api/v1/posts/999999  ->  404 + 에러 JSON
     * ========================================================================
     *
     * [void 메서드에 예외 스텁을 거는 BDD 문법]
     *   반환값이 있는 메서드: given(mock.method(...)).willThrow(예외)
     *   반환값이 없는 메서드: willThrow(예외).given(mock).method(...)
     *
     *   void 메서드는 given(...)에 넣을 "반환값"이 없기 때문에 문법이 다릅니다.
     *   이 테스트에서는 delete(999999L)가 호출되면 NotFoundException을 던집니다.
     */
    @Test
    void missing_post_delete_returns_404_json() throws Exception {
        willThrow(new NotFoundException("post missing"))
                .given(this.postService).delete(999999L);

        this.mockMvc.perform(delete("/api/v1/posts/{id}", 999999L))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("post missing"));

        verify(this.postService).delete(999999L);
    }
}
