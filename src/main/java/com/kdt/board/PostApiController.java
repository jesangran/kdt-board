package com.kdt.board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 * =========================================================
 * [08-JSON·DTO] PostApiController — JSON 목록 API
 * =========================================================
 *
 * 기존 PostController(/posts) 는 String(평문)을 반환한다.
 * 이 컨트롤러는 같은 게시글 데이터를 List<PostResponse> 로 반환한다.
 *
 * 왜 컨트롤러를 나눴나?
 * - /posts          → 학습용 문자열 응답 (기존 유지)
 * - /api/v1/posts   → 클라이언트가 쓰기 좋은 JSON 응답 (이번 차시)
 *
 * -------------------------------------------------
 * [자원 중심 API vs 행위 중심 API]
 * -------------------------------------------------
 * 자원 중심 (Resource-oriented, REST에 가깝다)
 *   - URL = 명사(자원). 예: /api/v1/posts  ← "게시글" 자원
 *   - 행위 = HTTP 메서드가 담당
 *       GET    /api/v1/posts     → 목록 조회  (지금 여기)
 *       GET    /api/v1/posts/1   → 단건 조회  (이후 차시)
 *       POST   /api/v1/posts     → 생성
 *       PUT    /api/v1/posts/1   → 수정
 *       DELETE /api/v1/posts/1   → 삭제
 *   - 같은 자원 URL에 메서드만 바꿔 행위를 구분한다
 *
 * 행위 중심 (Action-oriented)
 *   - URL에 동사·기능 이름이 들어감
 *   - 예: /posts/getList, /posts/createPost, /doPublish
 *   - 학습용 /posts/with-member, /posts/title-count/closed 도
 *     "무엇을 할지"가 경로에 드러난 쪽에 가깝다
 *
 * → 이 클래스는 자원 중심: 경로에 posts(자원)만, 조회는 GET 이 표현
 *
 * -------------------------------------------------
 * [HTTP 흐름]
 * -------------------------------------------------
 *   GET /api/v1/posts
 *     → list()
 *     → postService.listResponse()   // Entity → DTO 변환은 Service
 *     → List<PostResponse>
 *     → Spring + Jackson 이 JSON 배열로 직렬화
 *
 *   응답 예)
 *   [
 *     { "id": 1, "title": "closed", "loginId": "ran", "createdAt": "..." },
 *     ...
 *   ]
 *
 * @RestController = @Controller + @ResponseBody
 *   → 반환 객체를 뷰 이름이 아니라 HTTP 본문으로 씀
 *
 * @RequestMapping("/api/v1/posts")
 *   → 클래스의 공통 URL prefix (자원 경로)
 *   → 메서드 @GetMapping() 과 합쳐져 최종 경로 = GET /api/v1/posts
 *   → /api/v1 은 API 버전 구분용 prefix (나중에 v2 를 옆에 둘 수 있음)
 *
 * ※ 기존 /posts, /posts/joined 등 주소는 그대로 둔다 (노션 조건).
 */
@RestController
@RequestMapping("/api/v1/posts")
public class PostApiController {

    private final PostService postService;

    @Autowired
    public PostApiController(PostService postService) {
        this.postService = postService;
    }

    /*
     * GET /api/v1/posts  — 자원(posts)에 대한 "조회" 행위
     *
     * - HTTP GET = 읽기(조회). URL에 getList 같은 동사를 쓰지 않음 (자원 중심)
     * - 반환 타입 List<PostResponse> → Jackson 이 JSON 배열로 직렬화
     * - 컨트롤러는 "어떤 URL·메서드로 무엇을 줄지"만 담당
     * - Entity→DTO 변환·DB 조회는 Service(listResponse)에 위임
     *
     * 평문 /posts 와의 차이)
     *   PostController /posts     → String (브라우저에 글자만)
     *   여기 /api/v1/posts        → JSON (프론트·다른 서버가 파싱하기 좋음)
     */
    @GetMapping()
    public List<PostResponse> list() {
        return this.postService.listResponse();
    }

}
