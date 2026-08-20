CREATE TABLE content_translation (
    source_type       VARCHAR2(40 CHAR)   NOT NULL,
    source_key        VARCHAR2(128 CHAR)  NOT NULL,
    source_field      VARCHAR2(80 CHAR)   NOT NULL,
    target_language   VARCHAR2(10 CHAR)   NOT NULL,
    source_hash       CHAR(64 CHAR)       NOT NULL,
    translated_text   CLOB,
    status            VARCHAR2(16 CHAR)   NOT NULL,
    failure_count     NUMBER(10)          DEFAULT 0 NOT NULL,
    retry_after       TIMESTAMP,
    lease_until       TIMESTAMP,
    last_error        VARCHAR2(500 CHAR),
    created_at        TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at        TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_content_translation PRIMARY KEY (
        source_type,
        source_key,
        source_field,
        target_language
    ),
    CONSTRAINT ck_content_translation_status
        CHECK (status IN ('IDLE', 'PENDING', 'SUCCESS', 'FAILED')),
    CONSTRAINT ck_content_translation_lang
        CHECK (target_language IN ('zh', 'ja', 'en')),
    CONSTRAINT ck_content_translation_failures
        CHECK (failure_count >= 0)
);

CREATE INDEX ix_content_translation_retry
    ON content_translation (status, retry_after, lease_until);

CREATE TABLE translation_monthly_usage (
    usage_month       VARCHAR2(7 CHAR) NOT NULL,
    used_characters   NUMBER(19)       DEFAULT 0 NOT NULL,
    created_at        TIMESTAMP        DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at        TIMESTAMP        DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_translation_monthly_usage PRIMARY KEY (usage_month),
    CONSTRAINT ck_translation_usage_chars CHECK (used_characters >= 0)
);

COMMENT ON TABLE content_translation IS
    'Amazon Translate 결과 캐시. 원문 해시 변경 시 같은 키의 번역을 갱신한다.';
COMMENT ON TABLE translation_monthly_usage IS
    '월별 Amazon Translate 요청 문자 예약량. YYYY-MM 단위로 집계한다.';
