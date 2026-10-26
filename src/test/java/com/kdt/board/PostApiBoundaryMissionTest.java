package com.kdt.board;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/*
 * ============================================================================
 * [10 미션] PostApiBoundaryMissionTest — @Size 경계·415·member 404
 * ============================================================================
 * 본문 PostApiControllerTest 와 같은 슬라이스. 앱 코드는 수정하지 않음.
 * title max 20 / body max 100 경계값, 깨진 JSON, Content-Type text/plain(415)
 * ============================================================================
 */
@WebMvcTest(PostApiController.class)
@Import(ApiExceptionAdvice.class)
class PostApiBoundaryMissionTest {
    private static final String TITLE20 = "12345678901234567890";
    private static final String BODY100 =
            "1234567890" + "1234567890" + "1234567890" + "1234567890" + "1234567890"
            + "1234567890" + "1234567890" + "1234567890" + "1234567890" + "1234567890";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @Test
    void create_accepts_20_and_100_and_passes_exact_values() throws Exception {
        PostCreateRequest expected = new PostCreateRequest(TITLE20, BODY100, 7L);
        PostResponse response = new PostResponse(42L, TITLE20, "writer07",
                Instant.parse("2026-08-21T00:00:00Z"));
        given(this.postService.create(expected)).willReturn(response);

        this.mockMvc.perform(post("/api/v1/posts")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + TITLE20 + "\",\"body\":\"" + BODY100
                                + "\",\"memberId\":7}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/posts/42"))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.title").value(TITLE20))
                .andExpect(jsonPath("$.loginId").value("writer07"))
                .andExpect(jsonPath("$.createdAt").value("2026-08-21T00:00:00Z"))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.memberId").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.body").doesNotHaveJsonPath());
        verify(this.postService).create(expected);
    }

    @Test
    void create_rejects_21_title_before_service() throws Exception {
        this.mockMvc.perform(post("/api/v1/posts")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + TITLE20 + "x\",\"body\":\"" + BODY100
                                + "\",\"memberId\":7}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(this.postService);
    }

    @Test
    void update_accepts_20_and_100_and_passes_exact_values() throws Exception {
        PostUpdateRequest expected = new PostUpdateRequest(TITLE20, BODY100);
        given(this.postService.update(42L, expected))
                .willReturn(new PostResponse(42L, TITLE20, "writer07",
                        Instant.parse("2026-08-21T00:00:00Z")));

        this.mockMvc.perform(put("/api/v1/posts/{id}", 42L)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + TITLE20 + "\",\"body\":\"" + BODY100 + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value(TITLE20))
                .andExpect(jsonPath("$.loginId").value("writer07"))
                .andExpect(jsonPath("$.createdAt").value("2026-08-21T00:00:00Z"))
                .andExpect(jsonPath("$.length()").value(4));
        verify(this.postService).update(42L, expected);
    }

    @Test
    void update_rejects_101_body_before_service() throws Exception {
        this.mockMvc.perform(put("/api/v1/posts/{id}", 42L)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + TITLE20 + "\",\"body\":\"" + BODY100 + "x\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(this.postService);
    }

    @Test
    void missing_author_uses_404_advice() throws Exception {
        PostCreateRequest expected = new PostCreateRequest("plate", "menu", 7L);
        given(this.postService.create(expected))
                .willThrow(new NotFoundException("member missing"));

        this.mockMvc.perform(post("/api/v1/posts")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"plate\",\"body\":\"menu\",\"memberId\":7}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("member missing"));
        verify(this.postService).create(expected);
    }

    @Test
    void malformed_json_returns_400_before_service() throws Exception {
        this.mockMvc.perform(post("/api/v1/posts")
                        .contentType(APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(this.postService);
    }

    @Test
    void text_plain_returns_415_before_service() throws Exception {
        this.mockMvc.perform(post("/api/v1/posts")
                        .contentType(TEXT_PLAIN)
                        .content("{\"title\":\"plate\",\"body\":\"menu\",\"memberId\":7}"))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(this.postService);
    }
}