-- 개발표준정의서 온톨로지 초기 데이터
-- 문서 구조에 맞는 개념, 관계, 규칙 정의

-- =====================================================
-- 1. 주요 개념(Concept) 등록
-- =====================================================

-- 문서 섹션 개념
INSERT INTO ontology_concepts (name, name_en, concept_type, definition) VALUES
-- 1장: 개요
('개발표준정의서', 'Development Standard', 'POLICY', '소프트웨어 개발 시 준수해야 할 표준과 가이드라인을 정의한 문서'),

-- 2장: 개발 환경
('개발환경', 'Development Environment', 'TERM', '소프트웨어 개발에 필요한 하드웨어, 소프트웨어, 도구 등의 구성'),

-- 3장: 형상관리
('형상관리', 'Configuration Management', 'PROCESS', '소프트웨어 개발 과정에서 발생하는 모든 산출물의 변경 이력을 체계적으로 관리하는 활동'),
('Git', 'Git', 'SYSTEM', '분산 버전 관리 시스템'),
('Repository', 'Repository', 'TERM', '소스코드와 변경 이력을 저장하는 저장소'),
('Branch', 'Branch', 'TERM', '독립적인 개발 라인을 나타내는 Git의 기능'),
('Commit', 'Commit', 'TERM', '변경사항을 저장소에 기록하는 행위'),

-- 4장: 프로그램 개발 가이드
('명명규칙', 'Naming Convention', 'TERM', '변수, 클래스, 메서드 등의 이름을 짓는 규칙'),
('시스템명명규칙', 'System Naming Convention', 'TERM', '시스템 구성요소의 명명 규칙'),
('JAVA프로그램', 'Java Program', 'TERM', 'Java 언어로 작성하는 프로그램의 코딩 표준'),
('Batch프로그램', 'Batch Program', 'TERM', '일괄 처리 프로그램의 개발 표준'),

-- 5장: 웹접근성
('웹접근성', 'Web Accessibility', 'TERM', '장애인, 고령자 등이 웹사이트를 이용할 수 있도록 보장하는 것'),
('웹접근성지침', 'WCAG', 'REGULATION', '웹 콘텐츠 접근성 지침'),

-- 6장: Code Inspection
('코드인스펙션', 'Code Inspection', 'PROCESS', '소스코드의 품질을 검사하는 활동'),
('PMD', 'PMD', 'SYSTEM', 'Java 소스코드 정적 분석 도구'),
('전자정부표준Inspection', 'eGov Standard Inspection', 'TERM', '전자정부 프레임워크의 표준 코드 검사 규칙'),

-- 7장: SQL 개발 표준
('SQL개발표준', 'SQL Development Standard', 'TERM', 'SQL 작성 시 준수해야 할 표준'),
('DDL', 'DDL', 'TERM', 'Data Definition Language - 테이블, 인덱스 등 데이터 구조 정의'),
('DML', 'DML', 'TERM', 'Data Manipulation Language - 데이터 조회, 입력, 수정, 삭제'),

-- 8장: DB 명명 규칙
('DB명명규칙', 'DB Naming Convention', 'TERM', '데이터베이스 객체(테이블, 컬럼 등)의 명명 규칙'),

-- 9장: 소프트웨어 보안
('소프트웨어보안', 'Software Security', 'TERM', '소프트웨어 개발 시 보안 취약점을 예방하는 활동'),
('보안약점', 'Security Weakness', 'TERM', '소프트웨어의 보안 취약점을 유발할 수 있는 코딩 오류'),
('시큐어코딩', 'Secure Coding', 'TERM', '보안 취약점을 사전에 예방하는 안전한 코딩 기법')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 2. 동의어(Synonym) 등록
-- =====================================================

-- 형상관리 동의어
INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '버전관리' FROM ontology_concepts WHERE name = '형상관리'
ON CONFLICT DO NOTHING;

INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, 'SCM' FROM ontology_concepts WHERE name = '형상관리'
ON CONFLICT DO NOTHING;

INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, 'CM' FROM ontology_concepts WHERE name = '형상관리'
ON CONFLICT DO NOTHING;

-- Git 동의어
INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '깃' FROM ontology_concepts WHERE name = 'Git'
ON CONFLICT DO NOTHING;

-- 명명규칙 동의어
INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '네이밍규칙' FROM ontology_concepts WHERE name = '명명규칙'
ON CONFLICT DO NOTHING;

INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '네이밍컨벤션' FROM ontology_concepts WHERE name = '명명규칙'
ON CONFLICT DO NOTHING;

INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '이름규칙' FROM ontology_concepts WHERE name = '명명규칙'
ON CONFLICT DO NOTHING;

-- 웹접근성 동의어
INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '접근성' FROM ontology_concepts WHERE name = '웹접근성'
ON CONFLICT DO NOTHING;

INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, 'a11y' FROM ontology_concepts WHERE name = '웹접근성'
ON CONFLICT DO NOTHING;

-- 코드인스펙션 동의어
INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '코드검사' FROM ontology_concepts WHERE name = '코드인스펙션'
ON CONFLICT DO NOTHING;

INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '코드리뷰' FROM ontology_concepts WHERE name = '코드인스펙션'
ON CONFLICT DO NOTHING;

INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '정적분석' FROM ontology_concepts WHERE name = '코드인스펙션'
ON CONFLICT DO NOTHING;

-- 시큐어코딩 동의어
INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '보안코딩' FROM ontology_concepts WHERE name = '시큐어코딩'
ON CONFLICT DO NOTHING;

INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '안전한코딩' FROM ontology_concepts WHERE name = '시큐어코딩'
ON CONFLICT DO NOTHING;

-- Branch 동의어
INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '브랜치' FROM ontology_concepts WHERE name = 'Branch'
ON CONFLICT DO NOTHING;

INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '분기' FROM ontology_concepts WHERE name = 'Branch'
ON CONFLICT DO NOTHING;

-- Commit 동의어
INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '커밋' FROM ontology_concepts WHERE name = 'Commit'
ON CONFLICT DO NOTHING;

-- Repository 동의어
INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '레포지토리' FROM ontology_concepts WHERE name = 'Repository'
ON CONFLICT DO NOTHING;

INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '저장소' FROM ontology_concepts WHERE name = 'Repository'
ON CONFLICT DO NOTHING;

INSERT INTO concept_synonyms (concept_id, synonym)
SELECT id, '리포지토리' FROM ontology_concepts WHERE name = 'Repository'
ON CONFLICT DO NOTHING;

-- =====================================================
-- 3. 검색 리다이렉트 규칙
-- =====================================================

INSERT INTO ontology_rules (name, rule_type, condition, consequence, description, priority) VALUES
-- 형상관리 관련
('Git 브랜치 전략', 'REDIRECT', '브랜치 전략', '개발표준정의서 3.4 Branches 전략', 'Git 브랜치 전략 질문시 3.4절로 안내', 100),
('커밋 규칙', 'REDIRECT', '커밋 규칙', '개발표준정의서 3.5 Commit 작성 정책', '커밋 메시지 작성 규칙 질문시 3.5절로 안내', 100),
('커밋 메시지', 'REDIRECT', '커밋 메시지', '개발표준정의서 3.5 Commit 작성 정책', '커밋 메시지 관련 질문시 3.5절로 안내', 100),

-- 명명규칙 관련
('클래스 명명', 'REDIRECT', '클래스 이름', '개발표준정의서 4.2 JAVA 프로그램', 'Java 클래스 명명 규칙 질문시 4.2절로 안내', 100),
('메서드 명명', 'REDIRECT', '메서드 이름', '개발표준정의서 4.2 JAVA 프로그램', 'Java 메서드 명명 규칙 질문시 4.2절로 안내', 100),
('변수 명명', 'REDIRECT', '변수 이름', '개발표준정의서 4.2 JAVA 프로그램', 'Java 변수 명명 규칙 질문시 4.2절로 안내', 100),
('패키지 명명', 'REDIRECT', '패키지 이름', '개발표준정의서 4.1 시스템 명명규칙', '패키지 명명 규칙 질문시 4.1절로 안내', 100),

-- SQL 관련
('테이블 명명', 'REDIRECT', '테이블 이름', '개발표준정의서 8. 개발 DB 명명 규칙', '테이블 명명 규칙 질문시 8장으로 안내', 100),
('컬럼 명명', 'REDIRECT', '컬럼 이름', '개발표준정의서 8. 개발 DB 명명 규칙', '컬럼 명명 규칙 질문시 8장으로 안내', 100),
('SQL 작성', 'REDIRECT', 'SQL 작성', '개발표준정의서 7. SQL 개발 표준', 'SQL 작성 방법 질문시 7장으로 안내', 100),
('SELECT 문', 'REDIRECT', 'SELECT', '개발표준정의서 7.3 DML 작성 표준', 'SELECT 쿼리 관련 질문시 7.3절로 안내', 90),
('INSERT 문', 'REDIRECT', 'INSERT', '개발표준정의서 7.3 DML 작성 표준', 'INSERT 쿼리 관련 질문시 7.3절로 안내', 90),

-- 보안 관련
('SQL Injection', 'REDIRECT', 'SQL 인젝션', '개발표준정의서 9.2 소프트웨어 보안약점 구현단계 주요 항목', 'SQL Injection 관련 질문시 9.2절로 안내', 100),
('XSS', 'REDIRECT', 'XSS', '개발표준정의서 9.2 소프트웨어 보안약점 구현단계 주요 항목', 'XSS 관련 질문시 9.2절로 안내', 100),
('크로스사이트스크립팅', 'REDIRECT', '크로스사이트', '개발표준정의서 9.2 소프트웨어 보안약점 구현단계 주요 항목', 'XSS 관련 질문시 9.2절로 안내', 100),
('보안 취약점', 'REDIRECT', '보안 취약점', '개발표준정의서 9. 소프트웨어 개발보안 가이드', '보안 취약점 관련 질문시 9장으로 안내', 100),

-- 코드 검사 관련
('PMD 규칙', 'REDIRECT', 'PMD', '개발표준정의서 6.1 PMD 가이드', 'PMD 관련 질문시 6.1절로 안내', 100),
('코드 검사', 'REDIRECT', '코드 검사', '개발표준정의서 6. Code Inspection', '코드 검사 관련 질문시 6장으로 안내', 100),

-- 웹접근성 관련
('웹접근성 지침', 'REDIRECT', '웹접근성', '개발표준정의서 5. 웹접근성 표준', '웹접근성 관련 질문시 5장으로 안내', 100),

-- 배치 관련
('배치 프로그램', 'REDIRECT', '배치', '개발표준정의서 4.3 Batch JAVA 프로그램', '배치 프로그램 관련 질문시 4.3절로 안내', 100)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 4. 동의어 확장 규칙
-- =====================================================

INSERT INTO ontology_rules (name, rule_type, condition, consequence, description, priority) VALUES
('네이밍 동의어', 'SYNONYM', '네이밍', '명명규칙', '네이밍을 명명규칙으로 확장', 50),
('코딩 규칙 동의어', 'SYNONYM', '코딩규칙', '프로그램 개발 가이드', '코딩규칙을 프로그램 개발 가이드로 확장', 50),
('코딩 표준 동의어', 'SYNONYM', '코딩표준', '프로그램 개발 가이드', '코딩표준을 프로그램 개발 가이드로 확장', 50),
('자바 동의어', 'SYNONYM', '자바', 'JAVA', '자바를 JAVA로 확장', 50),
('데이터베이스 동의어', 'SYNONYM', '데이터베이스', 'DB', '데이터베이스를 DB로 확장', 50)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 5. 개념 간 관계 설정
-- =====================================================

-- 형상관리 하위 개념
INSERT INTO ontology_relations (source_concept_id, target_concept_id, relation_type, description)
SELECT s.id, t.id, 'PART_OF', 'Git은 형상관리의 도구'
FROM ontology_concepts s, ontology_concepts t
WHERE s.name = 'Git' AND t.name = '형상관리'
ON CONFLICT DO NOTHING;

INSERT INTO ontology_relations (source_concept_id, target_concept_id, relation_type, description)
SELECT s.id, t.id, 'PART_OF', 'Repository는 형상관리의 구성요소'
FROM ontology_concepts s, ontology_concepts t
WHERE s.name = 'Repository' AND t.name = '형상관리'
ON CONFLICT DO NOTHING;

INSERT INTO ontology_relations (source_concept_id, target_concept_id, relation_type, description)
SELECT s.id, t.id, 'PART_OF', 'Branch는 Git의 기능'
FROM ontology_concepts s, ontology_concepts t
WHERE s.name = 'Branch' AND t.name = 'Git'
ON CONFLICT DO NOTHING;

INSERT INTO ontology_relations (source_concept_id, target_concept_id, relation_type, description)
SELECT s.id, t.id, 'PART_OF', 'Commit은 Git의 기능'
FROM ontology_concepts s, ontology_concepts t
WHERE s.name = 'Commit' AND t.name = 'Git'
ON CONFLICT DO NOTHING;

-- 코드인스펙션 관련
INSERT INTO ontology_relations (source_concept_id, target_concept_id, relation_type, description)
SELECT s.id, t.id, 'PART_OF', 'PMD는 코드인스펙션 도구'
FROM ontology_concepts s, ontology_concepts t
WHERE s.name = 'PMD' AND t.name = '코드인스펙션'
ON CONFLICT DO NOTHING;

-- SQL 관련
INSERT INTO ontology_relations (source_concept_id, target_concept_id, relation_type, description)
SELECT s.id, t.id, 'PART_OF', 'DDL은 SQL개발표준의 일부'
FROM ontology_concepts s, ontology_concepts t
WHERE s.name = 'DDL' AND t.name = 'SQL개발표준'
ON CONFLICT DO NOTHING;

INSERT INTO ontology_relations (source_concept_id, target_concept_id, relation_type, description)
SELECT s.id, t.id, 'PART_OF', 'DML은 SQL개발표준의 일부'
FROM ontology_concepts s, ontology_concepts t
WHERE s.name = 'DML' AND t.name = 'SQL개발표준'
ON CONFLICT DO NOTHING;

-- 보안 관련
INSERT INTO ontology_relations (source_concept_id, target_concept_id, relation_type, description)
SELECT s.id, t.id, 'RELATED_TO', '시큐어코딩은 보안약점 예방 방법'
FROM ontology_concepts s, ontology_concepts t
WHERE s.name = '시큐어코딩' AND t.name = '보안약점'
ON CONFLICT DO NOTHING;
