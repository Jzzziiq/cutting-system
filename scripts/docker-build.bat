@echo off
cd /d "%~dp0.."
docker-compose up -d --build
echo Done. Visit http://localhost
