package com.ditto.aicourse.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.aicourse.dto.request.CourseChatRequest;
import com.ditto.aicourse.dto.response.CourseChatResponse;
import com.ditto.aicourse.service.AiCourseRecommendationService;
import com.ditto.global.common.response.ApiResponse;
import com.ditto.security.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "AI Course Recommendation",
        description = "AI 코스 추천 API — 대화·맞춤 생성·재추천이 이 엔드포인트 하나로 처리된다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/course-recommendations")
public class AiCourseRecommendationController {

    private final AiCourseRecommendationService aiCourseRecommendationService;

    @Operation(
            summary = "AI 코스 추천 대화 (맞춤 생성·재추천 포함)",
            description = """
                    손님의 말을 받아 추천 코스를 돌려준다. 결과물은 장소마다의 \
                    **navigationKey**(실내지도 위치)와 **reason**(추천 이유) 이다.

                    엔드포인트가 하나인 이유 — 아래 세 가지가 모두 같은 호출이다.

                    | 하고 싶은 것 | 보내는 값 |
                    |---|---|
                    | 맞춤 코스 생성 (첫 요청) | `sessionId` 를 비우고 `message` 만 |
                    | 대화로 다듬기 | 받은 `sessionId` + `"좀 더 저렴한 걸로"` |
                    | 재추천 | 받은 `sessionId` + `"다시 짜줘"` |

                    `sessionId` 를 실어 보내면 앞 대화의 조건을 이어받아 코스를 다시 구성한다. \
                    비우고 보내면 새 대화가 시작되고 서버가 새 `sessionId` 를 발급한다.

                    추천 로직은 외부 AI 엔진에 있다. 엔진 장애·타임아웃은 `E001`(502) 로 변환된다. \
                    한 턴에 수십 초가 걸릴 수 있다.
                    """)
    @PostMapping("/chat")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CourseChatResponse> chat(@Valid @RequestBody CourseChatRequest request) {
        return ApiResponse.success("성공",
                aiCourseRecommendationService.chat(SecurityUtils.requireUserId(), request));
    }
}
