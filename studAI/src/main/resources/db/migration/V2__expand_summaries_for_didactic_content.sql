ALTER TABLE summaries
    ADD COLUMN IF NOT EXISTS introduction TEXT,
    ADD COLUMN IF NOT EXISTS formulas_used TEXT,
    ADD COLUMN IF NOT EXISTS exam_questions TEXT,
    ADD COLUMN IF NOT EXISTS study_tips TEXT,
    ADD COLUMN IF NOT EXISTS final_summary TEXT,
    ADD COLUMN IF NOT EXISTS page_analysis_json TEXT,
    ADD COLUMN IF NOT EXISTS quality_score INT,
    ADD COLUMN IF NOT EXISTS word_count INT,
    ADD COLUMN IF NOT EXISTS model_used VARCHAR(100),
    ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(50);
