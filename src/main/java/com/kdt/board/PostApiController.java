package com.kdt.board;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/*
 * =========================================================
 * [08·09] PostApiController — /api/v1/posts JSON REST API
 * =========================================================
 * 평문 PostController(/posts) 는 유지. 여기는 JSON API.
 *
 * [엔드포인트]
 *   GET    /api/v1/posts                 목록 → 200 + List<PostResponse>
 *   GET    /api/v1/posts/{id}            단건 → 200 / 없으면 404
 *   GET    /api/v1/posts/count-by-title  집계 → 200 + long  (?title=)
 *   POST   /api/v1/posts                 생성 → 201 + Location + PostResponse
 *   PUT    /api/v1/posts/{id}            수정 → 200 + PostResponse
 *   DELETE /api/v1/posts/{id}            삭제 → 204 No Content
 *
 * [자원 중심 vs 행위 중심] (노션)
 *   자원 중심: URL=명사(/posts), 행위=HTTP 메서드(GET/POST/PUT/DELETE)
 *   행위 중심: URL에 동사(/getList, /createPost)
 *   → 이 API는 자원 중심 (count-by-title 만 집계용 하위 경로)
 *
 * [값을 받는 위치] (노션)
 *   @PathVariable  경로   /posts/{id}
 *   @RequestParam  쿼리   ?title=closed
 *   @RequestBody   JSON   POST/PUT 본문 → PostCreateRequest / PostUpdateRequest
 *
 * [HTTP 상태 코드] (노션)
 *   200  조회·수정 성공(+본문)   201  생성(+Location)
 *   204  삭제 성공(본문 없음)    400  @Valid 실패
 *   404  자원 없음 → NotFoundException → ApiExceptionAdvice → ApiError
 *   ※ 컨트롤러는 404를 직접 안 만듦
 *
 * [매핑 순서] /count-by-title 을 /{id} 보다 위 → id 로 오인 방지
 */
@RestController
@RequestMapping("/api/v1/posts")
public class PostApiController {

    private final PostService postService;

    @Autowired
    public PostApiController(PostService postService) {
        this.postService = postService;
    }

    /** GET /api/v1/posts → 200 + JSON 배열 */
    @GetMapping()
    public List<PostResponse> list() {
        return this.postService.listResponse();
    }

    /** GET .../count-by-title?title= → @RequestParam (평문 /posts/title-count/closed 와 동일 Service) */
    @GetMapping("/count-by-title")
    public long countByTitle(@RequestParam("title") String title) {
        return this.postService.countByTitle(title);
    }

    /** GET /{id} → 200 + PostResponse | 없으면 NotFoundException → 404 */
    @GetMapping("/{id}")
    public PostResponse one(@PathVariable("id") Long id) {
        return this.postService.findResponse(id);
    }

    /*
     * POST /api/v1/posts → 201 Created
     * - @Valid @RequestBody : JSON → PostCreateRequest 검증 (실패 시 400)
     * - ResponseEntity.created(Location).body(...) : 201 + Location + PostResponse
     *   Location = /api/v1/posts/{새id} (방금 만든 자원 URI)
     */
    @PostMapping()
    public ResponseEntity<PostResponse> create(@Valid @RequestBody PostCreateRequest request) {
        PostResponse response = this.postService.create(request);
        URI location = URI.create("/api/v1/posts/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    /*
     * PUT /{id} → 200 + PostResponse
     * PathVariable=대상 id, Body=PostUpdateRequest(@Valid)
     */
    @PutMapping("/{id}")
    public PostResponse update(
            @PathVariable("id") Long id,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        return this.postService.update(id, request);
    }

    /*
     * DELETE /{id} → 204 No Content
     * ResponseEntity.noContent() = 성공·본문 없음 (201/200 과 대비)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        this.postService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
