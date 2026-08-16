package com.ditto.news.inbound.scheduler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ditto.news.domain.GeneratedNewsFeed;
import com.ditto.news.application.service.NewsFeedPipelineService;
import com.ditto.news.config.NewsFeedGenerationProperties;

@ExtendWith(MockitoExtension.class)
class NewsFeedSchedulerTest {

    @Mock
    private NewsFeedPipelineService pipelineService;

    private NewsFeedGenerationProperties properties;
    private NewsFeedScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new NewsFeedGenerationProperties();
        properties.setTopics(List.of("K-POP"));
        scheduler = new NewsFeedScheduler(pipelineService, properties);
    }

    @Test
    @DisplayName("스케줄러 트리거 시 NewsFeedPipelineService의 executeAllTopics를 호출한다")
    void triggersPipelineServiceOnSchedule() {
        GeneratedNewsFeed feed = GeneratedNewsFeed.builder().title("Title").build();
        given(pipelineService.executeAllTopics()).willReturn(List.of(feed));

        scheduler.runNewsFeedGenerationSchedule();

        verify(pipelineService).executeAllTopics();
    }
}
