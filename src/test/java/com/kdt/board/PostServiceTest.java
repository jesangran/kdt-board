package com.kdt.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/*
 * ============================================================================
 * [10] PostServiceTest — PostService 생성·수정·삭제 (순수 단위 테스트)
 * ============================================================================
 * Spring/@WebMvcTest 없이 new PostService(모의 Repository...) 로 실제 서비스만 검증.
 * Repository 는 mock → SQL·DB 없음. 통과해도 "실제 DB 저장"을 증명한 것은 아님.
 *
 * [도구]
 *   given(...).willReturn / willThrow — 스텁(준비)
 *   verify / never / verifyNoInteractions — 호출 여부(검증)
 *   ArgumentCaptor — 서비스가 내부 new 한 Post 를 save 인자로 꺼내 검사
 *   assertThrows — HTTP 404가 아니라 Java 예외(NotFoundException) 자체 확인
 *   assertSame — 같은 인스턴스 참조 (equals와 다름)
 * ============================================================================
 */
class PostServiceTest {
    private PostRepository postRepository;
    private MemberRepository memberRepository;
    private PostService postService;

    /** 각 테스트마다 새 mock + 실제 PostService (테스트 간 호출 기록 격리) */
    @BeforeEach
    void setUp() {
        this.postRepository = mock(PostRepository.class);
        this.memberRepository = mock(MemberRepository.class);
        this.postService = new PostService(this.postRepository, this.memberRepository);
    }

    @Test
    void create_saves_request_values_and_uses_saved_result() {
        Member member = new Member("writer07", "kimbap");
        given(this.memberRepository.findById(7L)).willReturn(Optional.of(member));
        Instant createdAt = Instant.parse("2026-08-21T00:00:00Z");
        Post saved = mock(Post.class);
        given(saved.getId()).willReturn(42L);
        given(saved.getTitle()).willReturn("plate");
        given(saved.getMember()).willReturn(member);
        given(saved.getCreatedAt()).willReturn(createdAt);
        given(this.postRepository.save(any(Post.class))).willReturn(saved);

        PostResponse response = this.postService.create(
                new PostCreateRequest("plate", "menu", 7L));

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(this.postRepository).save(captor.capture());
        Post submitted = captor.getValue();
        assertEquals("plate", submitted.getTitle());
        assertEquals("menu", submitted.getBody());
        assertSame(member, submitted.getMember());
        assertEquals(new PostResponse(42L, "plate", "writer07", createdAt), response);
        verify(this.memberRepository).findById(7L);
    }

    @Test
    void create_rejects_missing_member_without_saving() {
        given(this.memberRepository.findById(7L)).willReturn(Optional.empty());

        NotFoundException error = assertThrows(NotFoundException.class,
                () -> this.postService.create(new PostCreateRequest("plate", "menu", 7L)));

        assertEquals("member missing", error.getMessage());
        verifyNoInteractions(this.postRepository);
        verify(this.memberRepository).findById(7L);
    }

    @Test
    void update_changes_existing_object_without_another_save() {
        Member member = new Member("writer07", "kimbap");
        Post post = new Post();
        post.setTitle("before");
        post.setBody("old body");
        post.setMember(member);
        given(this.postRepository.findById(42L)).willReturn(Optional.of(post));

        PostResponse response = this.postService.update(
                42L, new PostUpdateRequest("notice", "edited"));

        assertEquals("notice", post.getTitle());
        assertEquals("edited", post.getBody());
        assertSame(member, post.getMember());
        assertEquals("notice", response.title());
        assertEquals("writer07", response.loginId());
        verify(this.postRepository, never()).save(any(Post.class));
        verifyNoInteractions(this.memberRepository);
        verify(this.postRepository).findById(42L);
    }

    @Test
    void update_rejects_missing_post() {
        given(this.postRepository.findById(42L)).willReturn(Optional.empty());

        NotFoundException error = assertThrows(NotFoundException.class,
                () -> this.postService.update(42L, new PostUpdateRequest("notice", "edited")));

        assertEquals("post missing", error.getMessage());
        verify(this.postRepository, never()).save(any(Post.class));
        verifyNoInteractions(this.memberRepository);
        verify(this.postRepository).findById(42L);
    }

    @Test
    void delete_passes_found_object_to_repository() {
        Post post = new Post();
        given(this.postRepository.findById(42L)).willReturn(Optional.of(post));

        this.postService.delete(42L);

        verify(this.postRepository).delete(post);
        verifyNoInteractions(this.memberRepository);
        verify(this.postRepository).findById(42L);
    }

    @Test
    void delete_rejects_missing_post_without_deleting() {
        given(this.postRepository.findById(42L)).willReturn(Optional.empty());

        NotFoundException error = assertThrows(NotFoundException.class,
                () -> this.postService.delete(42L));

        assertEquals("post missing", error.getMessage());
        verify(this.postRepository, never()).delete(any(Post.class));
        verifyNoInteractions(this.memberRepository);
        verify(this.postRepository).findById(42L);
    }
}