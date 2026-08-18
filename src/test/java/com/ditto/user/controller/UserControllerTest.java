package com.ditto.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.community.service.PostBookmarkService;
import com.ditto.global.common.response.PageResponse;
import com.ditto.security.AuthUser;
import com.ditto.user.dto.request.UpdatePersonaRequest;
import com.ditto.user.dto.request.UpdateUserProfileRequest;
import com.ditto.user.dto.response.PersonaResponse;
import com.ditto.user.dto.response.UserBookmarkResponse;
import com.ditto.user.dto.response.UserProfileResponse;
import com.ditto.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostBookmarkService postBookmarkService;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("내 프로필 조회 성공 시 200 OK와 사용자 프로필 정보를 반환한다")
    void getMyProfileSuccess() throws Exception {
        AuthUser principal = new AuthUser(1L, "yuki@example.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(1L)
                .email("yuki@example.com")
                .nickname("사토 유키")
                .countryId(1L)
                .preferredLanguageCode("ko")
                .role("ROLE_CUSTOMER")
                .persona("OPEN_RUN_LOVER")
                .personaDisplayName("오픈런러버")
                .build();

        given(userService.getMyProfile(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.email").value("yuki@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("사토 유키"))
                .andExpect(jsonPath("$.data.persona").value("OPEN_RUN_LOVER"));

        verify(userService).getMyProfile(1L);
    }

    @Test
    @DisplayName("내 프로필 정보(닉네임/비밀번호/페르소나) 수정 성공 시 200 OK와 수정된 정보를 반환한다")
    void updateMyProfileSuccess() throws Exception {
        AuthUser principal = new AuthUser(1L, "yuki@example.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .nickname("새로운닉네임")
                .password("newPassword123!")
                .persona("FLEX_SPENDER")
                .build();

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(1L)
                .email("yuki@example.com")
                .nickname("새로운닉네임")
                .countryId(1L)
                .preferredLanguageCode("ko")
                .role("ROLE_CUSTOMER")
                .persona("FLEX_SPENDER")
                .personaDisplayName("플렉스족")
                .build();

        given(userService.updateProfile(eq(1L), any(UpdateUserProfileRequest.class))).willReturn(response);

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.nickname").value("새로운닉네임"))
                .andExpect(jsonPath("$.data.persona").value("FLEX_SPENDER"));

        verify(userService).updateProfile(eq(1L), any(UpdateUserProfileRequest.class));
    }

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

    @Test
    @DisplayName("쇼핑 페르소나 조회 성공 시 200 OK와 페르소나 정보를 반환한다")
    void getPersonaSuccess() throws Exception {
        AuthUser principal = new AuthUser(1L, "yuki@example.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        PersonaResponse response = PersonaResponse.builder()
                .persona("OPEN_RUN_LOVER")
                .displayName("오픈런러버")
                .englishName("OPEN-RUN LOVER")
                .description("신상·팝업 뜨면 제일 먼저")
                .build();

        given(userService.getPersona(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/users/me/persona"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.persona").value("OPEN_RUN_LOVER"))
                .andExpect(jsonPath("$.data.displayName").value("오픈런러버"))
                .andExpect(jsonPath("$.data.englishName").value("OPEN-RUN LOVER"));

        verify(userService).getPersona(1L);
    }

    @Test
    @DisplayName("쇼핑 페르소나 수정 성공 시 200 OK와 업데이트된 페르소나 정보를 반환한다")
    void updatePersonaSuccess() throws Exception {
        AuthUser principal = new AuthUser(1L, "yuki@example.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        UpdatePersonaRequest request = UpdatePersonaRequest.builder()
                .persona("FLEX_SPENDER")
                .build();

        PersonaResponse response = PersonaResponse.builder()
                .persona("FLEX_SPENDER")
                .displayName("플렉스족")
                .englishName("FLEX SPENDER")
                .description("명품·프리미엄 제대로")
                .build();

        given(userService.updatePersona(eq(1L), any(UpdatePersonaRequest.class))).willReturn(response);

        mockMvc.perform(patch("/api/v1/users/me/persona")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.persona").value("FLEX_SPENDER"))
                .andExpect(jsonPath("$.data.displayName").value("플렉스족"));

        verify(userService).updatePersona(eq(1L), any(UpdatePersonaRequest.class));
    }
}
