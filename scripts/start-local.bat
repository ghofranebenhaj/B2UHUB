@echo off
REM Demarrage local B2U-HUB (backend + gateway optionnel)
echo.
echo [1/2] Demarrage du backend sur http://localhost:8080 ...
start "B2U Backend" cmd /k "cd /d %~dp0backend && mvn spring-boot:run"

timeout /t 8 /nobreak >nul

echo [2/2] Demarrage du gateway sur http://localhost:8081 ...
start "B2U Gateway" cmd /k "cd /d %~dp0gateway && mvn spring-boot:run"

echo.
echo Backend  : http://localhost:8080
echo Gateway  : http://localhost:8081
echo Frontend : http://localhost:4200  (npm start dans frontend/)
echo Swagger  : http://localhost:8080/swagger-ui.html
pause
