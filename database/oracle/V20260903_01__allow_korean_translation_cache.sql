ALTER TABLE content_translation
    DROP CONSTRAINT ck_content_translation_lang;

ALTER TABLE content_translation
    ADD CONSTRAINT ck_content_translation_lang
    CHECK (target_language IN ('ko', 'zh', 'ja', 'en'));

COMMENT ON COLUMN content_translation.target_language IS
    '번역 결과 언어. 커뮤니티 다국어 원문을 위해 ko, zh, ja, en을 지원한다.';
