-- PostgreSQL + pgvector 초기화 스크립트

-- pgvector 확장 설치 (필요시)
CREATE EXTENSION IF NOT EXISTS vector;

-- 문서 테이블
CREATE TABLE IF NOT EXISTS policy_documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    document_code VARCHAR(50) UNIQUE,
    document_type VARCHAR(50),
    file_path VARCHAR(1000),
    file_name VARCHAR(500),
    mime_type VARCHAR(100),
    description TEXT,
    version VARCHAR(20),
    effective_date TIMESTAMP,
    department VARCHAR(200),
    metadata JSONB,
    indexed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 문서 청크 테이블
CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES policy_documents(id) ON DELETE CASCADE,
    chunk_index INTEGER,
    content TEXT NOT NULL,
    section_title VARCHAR(500),
    article_number VARCHAR(100),
    page_number INTEGER,
    start_offset INTEGER,
    end_offset INTEGER,
    metadata JSONB,
    vector_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chunks_document ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_chunks_article ON document_chunks(article_number);

-- 온톨로지 개념 테이블
CREATE TABLE IF NOT EXISTS ontology_concepts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    name_en VARCHAR(200),
    concept_type VARCHAR(50) NOT NULL,
    definition TEXT,
    properties JSONB,
    source_document_id BIGINT,
    source_reference VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_concept_name ON ontology_concepts(name);
CREATE INDEX IF NOT EXISTS idx_concept_type ON ontology_concepts(concept_type);

-- 동의어 테이블
CREATE TABLE IF NOT EXISTS concept_synonyms (
    concept_id UUID NOT NULL REFERENCES ontology_concepts(id) ON DELETE CASCADE,
    synonym VARCHAR(200) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_synonyms ON concept_synonyms(synonym);

-- 약어 테이블
CREATE TABLE IF NOT EXISTS concept_abbreviations (
    concept_id UUID NOT NULL REFERENCES ontology_concepts(id) ON DELETE CASCADE,
    abbreviation VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_abbreviations ON concept_abbreviations(abbreviation);

-- 온톨로지 관계 테이블
CREATE TABLE IF NOT EXISTS ontology_relations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_concept_id UUID NOT NULL REFERENCES ontology_concepts(id) ON DELETE CASCADE,
    target_concept_id UUID NOT NULL REFERENCES ontology_concepts(id) ON DELETE CASCADE,
    relation_type VARCHAR(50) NOT NULL,
    description TEXT,
    weight DOUBLE PRECISION DEFAULT 1.0,
    properties JSONB,
    source_document_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_relation_type ON ontology_relations(relation_type);
CREATE INDEX IF NOT EXISTS idx_source_concept ON ontology_relations(source_concept_id);
CREATE INDEX IF NOT EXISTS idx_target_concept ON ontology_relations(target_concept_id);

-- 온톨로지 규칙 테이블
CREATE TABLE IF NOT EXISTS ontology_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    condition TEXT NOT NULL,
    consequence TEXT,
    description TEXT,
    priority INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    parameters JSONB,
    source_document_id BIGINT,
    source_reference VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rule_type ON ontology_rules(rule_type);
CREATE INDEX IF NOT EXISTS idx_rule_active ON ontology_rules(is_active);

-- 샘플 온톨로지 데이터 (연차휴가 예시)
INSERT INTO ontology_concepts (name, concept_type, definition) VALUES
    ('연차휴가', 'TERM', '근로기준법에 따라 1년간 80% 이상 출근한 근로자에게 부여되는 유급휴가')
ON CONFLICT DO NOTHING;

INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '연차' FROM ontology_concepts WHERE name = '연차휴가'
ON CONFLICT DO NOTHING;

INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '유급휴가' FROM ontology_concepts WHERE name = '연차휴가'
ON CONFLICT DO NOTHING;

INSERT INTO ontology_rules (name, rule_type, condition, consequence, description, priority) VALUES
    ('연차휴가 리다이렉트', 'REDIRECT', '연차', '인사규정 제15조', '연차휴가 관련 질문시 인사규정 제15조로 안내', 100),
    ('결재 동의어', 'SYNONYM', '결재', '승인', '결재와 승인을 동의어로 처리', 50)
ON CONFLICT DO NOTHING;
