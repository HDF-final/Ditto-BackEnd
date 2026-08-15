package com.ditto.news.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = NewsConfig.class)
class NewsFeedGenerationPropertiesTest {

    @Autowired
    private NewsFeedGenerationProperties properties;

    @Test
    @DisplayName("application.yml의 news-feed.generation.topics 설정이 정상 바인딩된다")
    void bindsTopicsFromApplicationYml() {
        assertThat(properties).isNotNull();
        assertThat(properties.getTopics())
                .isNotNull()
                .hasSize(5)
                .containsExactly(
                        "K-POP",
                        "K-뷰티",
                        "K-패션",
                        "한국 팝업스토어",
                        "서울 핫플"
                );
    }

    @Test
    @DisplayName("topics 목록은 정확히 5개의 토픽을 순서대로 유지한다")
    void preservesExactFiveTopicsOrder() {
        List<String> topics = properties.getTopics();

        assertThat(topics).isNotNull();
        assertThat(topics.size()).isEqualTo(5);
        assertThat(topics.get(0)).isEqualTo("K-POP");
        assertThat(topics.get(1)).isEqualTo("K-뷰티");
        assertThat(topics.get(2)).isEqualTo("K-패션");
        assertThat(topics.get(3)).isEqualTo("한국 팝업스토어");
        assertThat(topics.get(4)).isEqualTo("서울 핫플");
    }
}
