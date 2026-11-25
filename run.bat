@echo off
where javac >nul 2>nul
if errorlevel 1 (
  echo Java JDK not found in this terminal. Run this first with YOUR jdk path, then run.bat again:
  echo   set PATH=C:\path\to\jdk\bin;%PATH%
  pause
  exit /b
)
if not exist out mkdir out
javac -d out src\*.java
if errorlevel 1 (pause ^& exit /b)
java -cp out Main
