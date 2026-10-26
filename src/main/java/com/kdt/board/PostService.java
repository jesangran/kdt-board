package com.kdt.board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/*
 * [영속화] PostService — Repository 위임 (Controller는 Service만 앎)
 *
 * 평문(06): findAll / findAllWithMember / count* / findById
 * JSON(08·09): listResponse / findResponse / create / update / delete
 *
 * [@Transactional — 초보]
 *   트랜잭션 = 여러 DB 일을 "전부 성공 or 전부 롤백"으로 묶는 상자
 *   실제 시작/커밋은 AOP 프록시가 함 (@Transactional = "붙여 줘" 표시)
 *
 *   readOnly=true  : 조회만 (listResponse, findResponse…)
 *                    · 더티 체킹 부담 ↓
 *                    · 상자가 열려 있어야 toResponse의 LAZY getMember() 가능
 *   readOnly=false : 기본. @Transactional 만 쓰면 이쪽 (update, delete)
 *                    · set 후 커밋 때 UPDATE / delete 반영
 */
@Service
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Autowired
    public PostService(PostRepository postRepository, MemberRepository memberRepository) {
        this.postRepository = postRepository;
        this.memberRepository = memberRepository;
    }

    /** 비영속 Post → 영속. id null이면 INSERT, 있으면 UPDATE */
    public Post save(Post post) {
        return this.postRepository.save(post);
    }

    public List<Post> findAll() {
        return this.postRepository.findAll();
    }

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

    /** join fetch — Member까지 한 쿼리 (/posts/joined, N+1 해결) */
    @Transactional(readOnly = true)
    public List<Post> findAllWithMember() {
        return this.postRepository.findAllWithMember();
    }

    /** COUNT만 — 엔티티 안 만듦 (/posts/count) */
    @Transactional(readOnly = true)
    public long countPosts() {
        return this.postRepository.countPosts();
    }

    /** findAll+size 비교용 — 무거움 (/posts/list-count) */
    @Transactional(readOnly = true)
    public int countLoadedPosts() {
        return this.postRepository.findAll().size();
    }

    /** :title 바인딩 집계 */
    @Transactional(readOnly = true)
    public long countByTitle(String title) {
        return this.postRepository.countByTitle(title);
    }

    /** 목록 DTO. readOnly 트랜잭션 안에서 LAZY loginId 로드 */
    @Transactional(readOnly = true)
    public List<PostResponse> listResponse() {
        List<PostResponse> result = new ArrayList<>();
        for (Post post : this.postRepository.findAll()) {
            result.add(toResponse(post));
        }
        return result;
    }

    /** 단건 DTO. 없으면 NotFoundException → Advice 404 */
    @Transactional(readOnly = true)
    public PostResponse findResponse(Long id) {
        Post post = this.postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("post missing"));
        return toResponse(post);
    }

    /** Entity → 응답 DTO (getMember는 LAZY → 호출부는 @Transactional 필요) */
    private PostResponse toResponse(Post post) {
        String loginId = post.getMember().getLoginId();
        return new PostResponse(post.getId(), post.getTitle(), loginId, post.getCreatedAt());
    }

    /** INSERT: new Post(비영속) → save → 영속 + Auditing 시각 */
    public PostResponse create(PostCreateRequest request) {
        Member member = this.memberRepository.findById(request.memberId())
                .orElseThrow(() -> new NotFoundException("member missing"));

        Post post = new Post();
        post.setTitle(request.title());
        post.setBody(request.body());
        post.setMember(member);

        return toResponse(this.postRepository.save(post));
    }

    /*
     * 수정 = 더티 체킹 실습
     * find로 영속 Post를 가져온 뒤 setTitle/setBody만 함
     * → 메서드 끝 커밋 때 JPA가 변경 감지 → UPDATE (save 재호출 불필요)
     */
    @Transactional
    public PostResponse update(Long id, PostUpdateRequest request) {
        Post post = this.postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("post missing"));
        post.setTitle(request.title());
        post.setBody(request.body());
        return toResponse(post);
    }

    @Transactional
    public void delete(Long id) {
        Post post = this.postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("post missing"));
        this.postRepository.delete(post);
    }
}
