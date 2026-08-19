@echo off
setlocal enabledelayedexpansion
set "JAVAC=javac"
if exist "C:\Program Files\Java\jdk1.8.0_311\bin\javac.exe" set "JAVAC=C:\Program Files\Java\jdk1.8.0_311\bin\javac.exe"
"%JAVAC%" -version >nul 2>nul || (echo ERROR: javac not found & exit /b 1)
if not exist out mkdir out
(for /f "delims=" %%f in ('dir /s /b src\*.java') do set "p=%%f" & echo "!p:\=/!") > .sources.txt
"%JAVAC%" -encoding UTF-8 --release 8 -d out @.sources.txt
if errorlevel 1 (
  echo --release 8 not supported, retrying with -source/-target 8
  "%JAVAC%" -encoding UTF-8 -source 8 -target 8 -d out @.sources.txt
)
if errorlevel 1 (echo BUILD FAILED & exit /b 1)
echo BUILD OK