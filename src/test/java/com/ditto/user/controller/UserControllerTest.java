package com.ditto.user.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.community.service.PostBookmarkService;
import com.ditto.global.common.response.PageResponse;
import com.ditto.security.AuthUser;
import com.ditto.user.dto.response.UserBookmarkResponse;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostBookmarkService postBookmarkService;

    @Test
    @DisplayName("내 북마크 목록 조회 성공 시 ApiResponse와 PageResponse 형태로 반환한다")
    void getMyBookmarksSuccess() throws Exception {
        AuthUser principal = new AuthUser(1L, "yuki@example.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        UserBookmarkResponse item = UserBookmarkResponse.builder()
                .postId(23L)
                .courseId(23L)
                .title("K-POP 팝업스토어 & 한식 맛집 코스")
                .likeCount(77L)
                .bookmarkCount(34L)
                .bookmarkedAt(LocalDateTime.of(2026, 8, 18, 14, 20, 0))
                .build();
        PageResponse<UserBookmarkResponse> pageResponse = new PageResponse<>(List.of(item), 0, 1L);

        given(postBookmarkService.getMyBookmarks(1L, 0, 10)).willReturn(pageResponse);

        mockMvc.perform(get("/api/v1/users/me/bookmarks")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.data.content[0].postId").value(23L))
                .andExpect(jsonPath("$.data.content[0].title").value("K-POP 팝업스토어 & 한식 맛집 코스"))
                .andExpect(jsonPath("$.data.content[0].likeCount").value(77L))
                .andExpect(jsonPath("$.data.content[0].bookmarkCount").value(34L))
                .andExpect(jsonPath("$.data.totalElements").value(1L));

        verify(postBookmarkService).getMyBookmarks(1L, 0, 10);
    }
}
