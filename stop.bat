@echo off
chcp 65001 > nul
echo ========================================
echo   Policy QnA RAG System 종료
echo ========================================
echo.
echo Docker 컨테이너 종료 중...
docker-compose down
echo.
echo 종료 완료!
pause
