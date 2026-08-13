package com.ditto.aicourse.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ditto.aicourse.client.AiEngineChatResponse;
import com.ditto.aicourse.client.AiEngineClient;
import com.ditto.aicourse.client.AiEnginePlace;
import com.ditto.aicourse.dto.request.CourseChatRequest;
import com.ditto.aicourse.dto.response.CourseChatResponse;
import com.ditto.aicourse.dto.response.RecommendedPlaceResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 코스 추천 대화.
 *
 * <p>추천 로직은 외부 AI 엔진에 있고 여기서는 호출과 변환만 한다.
 * DB 를 쓰지 않으므로 {@code @Transactional} 을 붙이지 않는다 —
 * LLM 응답을 기다리는 수십 초 동안 커넥션을 잡고 있을 이유가 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCourseRecommendationService {

    private final AiEngineClient aiEngineClient;

    public CourseChatResponse chat(Long userId, CourseChatRequest request) {
        AiEngineChatResponse engineResponse =
                aiEngineClient.chat(request.getSessionId(), request.getMessage());

        // 대화 내용은 남기지 않는다. 추적에 필요한 것은 누가 몇 번째 턴을 돌았는지다.
        log.debug("AI 코스 추천 완료. userId={}, turn={}, places={}",
                userId, engineResponse.getTurn(), sizeOf(engineResponse.getPlaces()));

        return CourseChatResponse.builder()
                .sessionId(engineResponse.getSession())
                .reply(engineResponse.getReply())
                .turn(engineResponse.getTurn())
                .places(toPlaceResponses(engineResponse.getPlaces()))
                .build();
    }

    private List<RecommendedPlaceResponse> toPlaceResponses(List<AiEnginePlace> places) {
        if (places == null) {
            return Collections.emptyList();
        }
        return places.stream()
                .map(place -> RecommendedPlaceResponse.builder()
                        .navigationKey(place.getNavigationKey())
                        .placeName(place.getPlaceName())
                        .reason(place.getReason())
                        .build())
                .toList();
    }

    private int sizeOf(List<AiEnginePlace> places) {
        return places == null ? 0 : places.size();
    }
}
