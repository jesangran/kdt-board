package com.kdt.board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/*
 * [영속화] Service = 비즈니스 + Repository 호출 (3단계)
 *
 * Controller 는 Service만 알고, DB 영속화는 Repository에 위임한다.
 *
 * save()        : id null → INSERT / id 있음 → UPDATE (Post는 setMember 필수)
 * findAll()     : Post 목록 (Member LAZY)
 * findAllWithMember() : join fetch 로 N+1 해결 → /posts/joined
 * countPosts() vs countLoadedPosts() : COUNT(가벼움) vs findAll+size(무거움) — 메서드 주석 참고
 * countByTitle(title) : 조건 COUNT + :title 바인딩 → /posts/title-count/closed
 * findById()    : Optional<Post>.orElse(null)
 *
 * [08-JSON·DTO] listResponse() / toResponse()
 * - Entity(Post) → 응답 DTO(PostResponse) 로 변환
 * - GET /api/v1/posts (PostApiController) 에서 사용
 * - 평문 /posts 와 달리 JSON 배열로 내려갈 데이터 형태를 여기서 정한다
 */
@Service
public class PostService {

    private final PostRepository postRepository;

    @Autowired // 생성자 주입: Repository Bean을 연결
    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    // 영속화 진입점: 비영속 Post → DB 저장 → 영속 Post 반환
    public Post save(Post post) {
        return this.postRepository.save(post);
    }

    // DB에 영속화된 Post 전체 조회
    public List<Post> findAll() {
        return this.postRepository.findAll();
    }

    // Optional<Post> → Post로 꺼내기 위해 orElse 사용 (없으면 null)
    public Post findById(Long id) {
        return this.postRepository.findById(id).orElse(null);
    }

    public void deletePost(Long id) {
        Post post = this.postRepository.findById(id).orElse(null);
        if (post == null) {
            System.out.println("error");
        }
        this.postRepository.delete(post);
    }

    /*
     * N+1 해결용 조회
     * - join fetch 로 Post+Member 를 한 트랜잭션·한 쿼리에서 로드
     * - @Transactional = AOP 표시 → 프록시 Advice 가 읽기 트랜잭션·영속 유지
     * - readOnly=true : 조회만 → 더티 체킹 부담 감소
     *
     * [비용] 주로 DB 왕복(1+N → ≈1)을 줄임
     */
    @Transactional(readOnly = true)
    public List<Post> findAllWithMember() {
        return this.postRepository.findAllWithMember();
    }

    /*
     * [countPosts] DB 집계만 — "몇 건인가?" 만 물음
     *
     * - Repository: select count(p) from Post p
     * - SQL 예: SELECT COUNT(*) FROM board_post
     * - 엔티티(Post 객체)를 만들지 않음 → 메모리·네트워크 부담 작음
     * - 개수만 필요할 때 올바른 방법
     *
     * countLoadedPosts() 와 숫자 결과는 같지만,
     * 여기는 "숫자를 DB 가 세어 줌" / 저기는 "앱이 전부 가져온 뒤 셈"
     *
     * [비용]
     * - 메모리       : long 하나 (Post N개 안 만듦) ← 핵심
     * - DB/네트워크 : COUNT 결과만 전송
     * - 시간         : 데이터·객체 생성 최소
     */
    @Transactional(readOnly = true)
    public long countPosts() {
        return this.postRepository.countPosts();
    }

    /*
     * [countLoadedPosts] 전부 로드 후 size — 비효율적인 개수 세기 (비교·학습용)
     *
     * - findAll() → board_post 행을 Post 객체로 전부 영속 로드
     * - posts.size() → 자바 List 길이 = 건수
     * - 글 10만 개면 10만 객체를 만든 뒤에야 "10만" 을 알게 됨
     *
     * countPosts() 와의 차이)
     *   countPosts()       : COUNT 쿼리 1번 → long 숫자만          (가벼움)
     *   countLoadedPosts() : SELECT * 전부 → List 로드 → size()   (무거움)
     *
     * [비용] ← 오늘 "메모리 비용"을 가장 はっきり 보는 예시
     * - 메모리       : Post 객체 N개 + 영속 컨텍스트 관리 비용
     * - DB/네트워크 : 행 전체 SELECT (컬럼·데이터량 ↑)
     * - 시간         : 로드·매핑 후야 개수를 앎 → 느림
     *
     * ※ 실무에서 "개수만" 필요하면 countPosts() / count() 를 쓰고
     *    이 메서드는 "로드해서 세는 것"의 비용을 보여 주는 용도
     */
    @Transactional(readOnly = true)
    public int countLoadedPosts() {
        List<Post> posts = this.postRepository.findAll();

        return posts.size();
    }

    /*
     * [countByTitle] 조건 있는 COUNT + 파라미터 바인딩
     * - title 값이 Repository @Query 의 :title 에 바인딩됨
     * - 문자열을 JPQL 에 붙이지 않고 구멍(:title)에 값만 전달
     * - 호출: PostController /posts/title-count/closed → "closed"
     * - 바인딩 값은 logging.level.org.hibernate.orm.jdbc.bind: TRACE 로 확인
     */
    @Transactional(readOnly = true)
    public long countByTitle(String title) {
        return this.postRepository.countByTitle(title);
    }

    /*
     * [listResponse] 게시글 목록 → List<PostResponse> (JSON API용)
     *
     * 흐름)
     *   1) findAll() 로 Post 엔티티 목록 조회
     *   2) 각 Post 를 toResponse() 로 DTO 변환
     *   3) List<PostResponse> 반환 → PostApiController → JSON 배열
     *
     * @Transactional(readOnly = true)
     * - toResponse 에서 getMember().getLoginId() (LAZY) 를 쓰므로
     *   트랜잭션 안에서 Member 를 읽을 수 있게 유지한다
     * - 조회만 하므로 readOnly=true
     *
     * /posts(문자열) 와의 차이)
     *   /posts           : Controller 가 Post 필드·문자열을 직접 이어 붙임
     *   /api/v1/posts    : Service 가 DTO 목록을 만들고, 직렬화는 Spring/Jackson
     */
    @Transactional(readOnly = true)
    public List<PostResponse> listResponse() {
        List<PostResponse> result = new ArrayList<>();
        for (Post post : this.postRepository.findAll()) {
            PostResponse pr = toResponse(post);
            result.add(pr);
        }
        return result;
    }

    /*
     * [toResponse] Post(Entity) → PostResponse(DTO) 한 건 매핑
     *
     * - id, title, createdAt : Post 에서 그대로
     * - loginId              : post.getMember().getLoginId() (작성자만 노출)
     * - body / updatedAt / Member 객체 전체는 응답에 넣지 않음
     *
     * "엔티티 ≠ API 응답 모양" → 여기서 API 계약을 고정한다
     */
    private PostResponse toResponse(Post post) {
        String loginId = post.getMember().getLoginId();
        return new PostResponse(post.getId(), post.getTitle(), loginId, post.getCreatedAt());
    }

}
