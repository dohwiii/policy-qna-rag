# Policy QnA RAG System

사내 정책/업무 매뉴얼 Q&A 시스템 (무료 로컬 AI 버전)

## 📋 이 프로그램이 하는 일

```
[사용자 질문] "커밋 메시지 어떻게 작성해?"
        ↓
[시스템] 개발표준정의서 PDF에서 관련 내용 검색
        ↓
[답변] "개발표준정의서 3.5절에 따르면..."
      📄 출처: 개발표준정의서 3.5 Commit 작성 정책
```

## 🔧 필요한 것 (무료)

| 항목 | 설명 | 다운로드 |
|-----|------|---------|
| **Docker Desktop** | PostgreSQL, Ollama 실행 | [다운로드](https://www.docker.com/products/docker-desktop/) |
| **Java 17** | 프로그램 실행 | [다운로드](https://adoptium.net/) |

## 🚀 실행 방법

### 1단계: Docker Desktop 설치 및 실행
- 위 링크에서 Docker Desktop 다운로드
- 설치 후 실행 (트레이에 고래 아이콘 확인)

### 2단계: 시스템 시작
```bash
# 프로젝트 폴더로 이동
cd C:\egovframework\eGovFrameDev-4.2.0-64bit\git\mdsp_git\policy-qna-rag

# 시작 (더블클릭 해도 됨)
start.bat
```

**처음 실행시 AI 모델 다운로드로 5-10분 소요**

### 3단계: 문서 업로드
브라우저에서 http://localhost:8080 접속 후:

```bash
# 개발표준정의서 PDF 업로드
curl -X POST http://localhost:8080/api/v1/documents/upload ^
  -F "file=@개발표준정의서.pdf" ^
  -F "title=개발표준정의서" ^
  -F "documentCode=DEV-001" ^
  -F "documentType=GUIDELINE"
```

### 4단계: 질문하기
```bash
curl -X POST http://localhost:8080/api/v1/qna/ask ^
  -H "Content-Type: application/json" ^
  -d "{\"question\": \"커밋 메시지 어떻게 작성해?\"}"
```

## 📁 프로젝트 구조

```
policy-qna-rag/
├── start.bat              ← 더블클릭으로 시작
├── stop.bat               ← 더블클릭으로 종료
├── docker-compose.yml     ← DB + AI 설정
├── pom.xml                ← Java 라이브러리 설정
└── src/
    └── main/
        ├── java/          ← 소스코드
        └── resources/
            ├── application.yml
            └── schema.sql
```

## 🛠 주요 API

| 기능 | 요청 | 설명 |
|-----|------|------|
| 질문하기 | `POST /api/v1/qna/ask` | Q&A |
| 문서 업로드 | `POST /api/v1/documents/upload` | PDF 등록 |
| 문서 목록 | `GET /api/v1/documents` | 등록된 문서 |
| 용어 조회 | `GET /api/v1/ontology/terms/{용어}` | 용어 정의 |

## ❓ 문제 해결

### Docker가 실행 안 됨
→ Docker Desktop 실행 후 다시 시도

### 포트 충돌 (5432, 11434, 8080)
→ 해당 포트 사용 프로그램 종료 후 재시도

### AI 응답이 느림
→ 처음엔 모델 로딩으로 느릴 수 있음. GPU 있으면 docker-compose.yml에서 GPU 설정 활성화

## 📞 종료 방법

```bash
stop.bat
```
또는 Ctrl+C
