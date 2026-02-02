@echo off
chcp 65001 > nul
echo ========================================
echo   Policy QnA RAG System 시작
echo ========================================
echo.

REM 1. Docker 실행 확인
echo [1/4] Docker 상태 확인 중...
docker info > nul 2>&1
if errorlevel 1 (
    echo [오류] Docker가 실행되지 않았습니다.
    echo        Docker Desktop을 먼저 실행해주세요.
    pause
    exit /b 1
)
echo       Docker 정상 실행 중

REM 2. Docker Compose로 PostgreSQL, Ollama 시작
echo.
echo [2/4] PostgreSQL + Ollama 시작 중...
docker-compose up -d
if errorlevel 1 (
    echo [오류] Docker Compose 실행 실패
    pause
    exit /b 1
)

REM 3. Ollama 모델 다운로드 (처음 한 번만)
echo.
echo [3/4] AI 모델 다운로드 중... (처음 실행시 5-10분 소요)
echo       - llama3.2:3b (대화용, 약 2GB)
echo       - nomic-embed-text (검색용, 약 300MB)
timeout /t 10 /nobreak > nul
docker exec policy-qna-ollama ollama pull llama3.2:3b
docker exec policy-qna-ollama ollama pull nomic-embed-text

REM 4. Spring Boot 실행
echo.
echo [4/4] 웹 서버 시작 중...
echo       http://localhost:8080 에서 접속 가능
echo.
echo ========================================
echo   Ctrl+C 로 종료
echo ========================================
call mvnw spring-boot:run

pause
