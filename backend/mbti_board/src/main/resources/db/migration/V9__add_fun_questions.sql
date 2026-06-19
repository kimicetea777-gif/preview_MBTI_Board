-- fun_questions: FN 질문 본체 (mbti_questions와 완전 분리)
CREATE TABLE fun_questions (
    id           BIGSERIAL   PRIMARY KEY,
    title        TEXT        NOT NULL,
    test_version VARCHAR(10) NOT NULL DEFAULT 'v1.0'
);

-- fun_question_options: FN 질문 커스텀 선택지
CREATE TABLE fun_question_options (
    id            BIGSERIAL    PRIMARY KEY,
    question_id   BIGINT       NOT NULL,
    option_text   VARCHAR(100) NOT NULL,
    display_order SMALLINT     NOT NULL,
    CONSTRAINT fk_fun_question_options_question
        FOREIGN KEY (question_id) REFERENCES fun_questions(id) ON DELETE CASCADE
);

CREATE INDEX idx_fun_question_options_question_id ON fun_question_options (question_id);

-- fun_response_counts: (option_id, mbti_id) 조합 카운트 사전 집계
CREATE TABLE fun_response_counts (
    option_id  BIGINT      NOT NULL,
    mbti_id    VARCHAR(6)  NOT NULL,
    count      BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (option_id, mbti_id),
    CONSTRAINT fk_fun_response_counts_option
        FOREIGN KEY (option_id) REFERENCES fun_question_options(id) ON DELETE CASCADE,
    CONSTRAINT fk_fun_response_counts_mbti
        FOREIGN KEY (mbti_id) REFERENCES mbti_metadata(id)
);
