package com.ditto.global.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.InputStream;
import java.util.List;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * 매퍼 XML 을 <b>실제로 파싱한다.</b> DB 는 안 붙는다.
 *
 * <p>XML 은 컴파일이 안 봐 준다. 오타 난 {@code refid}, 닫히지 않은 태그, 없는
 * {@code resultType} 은 전부 앱이 뜰 때 터지고, 그때는 이미 배포된 뒤다.
 *
 * <p><b>특히 교차 네임스페이스 {@code <include>} 가 이 검사의 이유다.</b>
 * {@code CourseMapper} 와 {@code RecommendedCourseMapper} 가 자리 사진을 고르는 조각
 * ({@code slotImageKey})을 나눠 쓴다 — 어드민 화면과 손님 화면이 같은 사진을 보게 하려면
 * 규칙이 한 벌이어야 하는데, 그러자니 네임스페이스를 넘는 참조가 생겼다. 그 참조가
 * 깨지면 앱이 기동조차 못 한다.
 */
class MapperXmlParseTest {

    /** {@code application.yml} 의 {@code mybatis.mapper-locations} 와 같은 값이어야 한다. */
    private static final String MAPPER_LOCATIONS = "classpath:mapper/**/*.xml";

    private Configuration parseAll() throws Exception {
        Configuration configuration = new Configuration();
        Resource[] resources =
                new PathMatchingResourcePatternResolver().getResources(MAPPER_LOCATIONS);
        assertThat(resources).as("매퍼 XML 을 하나도 못 찾았다 — 경로가 바뀌었나").isNotEmpty();

        for (Resource resource : resources) {
            try (InputStream in = resource.getInputStream()) {
                new XMLMapperBuilder(
                        in, configuration, resource.getFilename(), configuration.getSqlFragments())
                        .parse();
            }
        }
        // 참조가 아직 안 풀린 문장은 여기 남는다. MyBatis 는 로드 순서 때문에 한 번
        // 미뤄 뒀다가 다시 푸는데, 끝내 못 풀면 이 목록이 안 빈다.
        configuration.getMappedStatementNames();
        return configuration;
    }

    @Test
    @DisplayName("매퍼 XML 이 전부 파싱되고 참조가 남김없이 풀린다")
    void allMapperXmlParses() {
        assertThatCode(this::parseAll).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("자리 사진 조각을 어드민과 손님 화면이 나눠 쓴다 — 교차 네임스페이스 참조가 풀린다")
    void slotImageKeyFragmentIsSharedAcrossNamespaces() throws Exception {
        Configuration configuration = parseAll();

        assertThat(configuration.getSqlFragments())
                .containsKey("com.ditto.recommendation.repository.RecommendedCourseMapper.slotImageKey");

        // 두 문장 모두 조각이 풀린 SQL 을 들고 있어야 한다. 안 풀렸으면 파싱에서 이미
        // 터지지만, 조각이 **빈 채로** 끼워지는 사고는 안 터지고 조용히 지나간다.
        List<String> statements = List.of(
                "com.ditto.course.repository.CourseMapper.findPlacesByCourseId",
                "com.ditto.recommendation.repository.RecommendedCourseMapper.findPlaces");
        for (String id : statements) {
            assertThat(configuration.hasStatement(id)).as("%s 가 없다", id).isTrue();
            MappedStatement statement = configuration.getMappedStatement(id);
            String sql = statement.getBoundSql(null).getSql();
            assertThat(sql).as("%s 에 POST_IMAGE 조인이 안 끼워졌다", id).contains("post_image");
            assertThat(sql).as("%s 에 매장 사진 폴백이 없다", id).contains("p.image_url");
        }
    }

    @Test
    @DisplayName("대표 사진 조각도 목록·상세가 나눠 쓴다")
    void heroImageKeyFragmentIsShared() throws Exception {
        Configuration configuration = parseAll();

        assertThat(configuration.getSqlFragments())
                .containsKey("com.ditto.recommendation.repository.RecommendedCourseMapper.heroImageKey");

        String sql = configuration
                .getMappedStatement("com.ditto.course.repository.CourseMapper.findHeroImageKey")
                .getBoundSql(null)
                .getSql();
        assertThat(sql).contains("c.main_image");
        assertThat(sql).contains("post_image");
    }
}
