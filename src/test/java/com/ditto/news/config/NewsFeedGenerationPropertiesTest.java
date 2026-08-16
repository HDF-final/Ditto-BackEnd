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
                .hasSize(1)
                .containsExactly("K-POP");
    }

    @Test
    @DisplayName("topics 목록은 K-POP 단일 토픽을 유지한다")
    void preservesKpopTopic() {
        List<String> topics = properties.getTopics();

        assertThat(topics).isNotNull();
        assertThat(topics).containsExactly("K-POP");
    }
}
