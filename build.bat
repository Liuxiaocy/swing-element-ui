@echo off
setlocal enabledelayedexpansion
where javac >nul 2>nul || (echo ERROR: javac not on PATH & exit /b 1)
if not exist out mkdir out
(for /f "delims=" %%f in ('dir /s /b src\*.java') do set "p=%%f" & echo "!p:\=/!") > .sources.txt
javac -encoding UTF-8 --release 8 -d out @.sources.txt
if errorlevel 1 (
  echo --release 8 not supported, retrying with -source/-target 8
  javac -encoding UTF-8 -source 8 -target 8 -d out @.sources.txt
)
if errorlevel 1 (echo BUILD FAILED & exit /b 1)
echo BUILD OK