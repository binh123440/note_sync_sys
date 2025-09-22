@echo off
title Note Sync System - Quick Setup

echo ========================================
echo   Note Sync System - Quick Setup
echo ========================================
echo.

:: Check if Java is installed
echo [1/3] Checking Java installation...
javac -version >nul 2>&1
if not errorlevel 1 (
    echo ✓ Java JDK is already installed and configured
    goto :compile
)

:: Search for Java in common locations
echo [1/3] Searching for Java JDK...
set "JAVA_FOUND=0"
set "JAVA_PATHS=C:\Program Files\Java C:\Program Files (x86)\Java C:\Program Files\Eclipse Adoptium C:\Program Files\Eclipse Foundation"

for %%P in (%JAVA_PATHS%) do (
    if exist "%%P" (
        for /d %%D in ("%%P\*jdk*") do (
            if exist "%%D\bin\javac.exe" (
                echo ✓ Found Java JDK at: %%D
                set "JAVA_HOME=%%D"
                set "PATH=%%D\bin;%PATH%"
                set "JAVA_FOUND=1"
                goto :java_configured
            )
        )
    )
)

:java_configured
if "%JAVA_FOUND%"=="0" (
    echo ✗ Java JDK not found!
    echo.
    echo Please download and install Java JDK:
    echo   Option 1: Oracle JDK - https://www.oracle.com/java/technologies/downloads/
    echo   Option 2: OpenJDK - https://adoptium.net/
    echo.
    echo After installation, run this script again.
    echo.
    pause
    exit /b 1
)

:compile
echo.
echo [2/3] Compiling Note Sync System...

:: Create build directory
if not exist build mkdir build

:: Compile step by step
echo   Compiling models...
javac -d build src\common\models\*.java
if errorlevel 1 goto :compile_error

echo   Compiling network classes...
javac -d build -cp build src\common\network\*.java
if errorlevel 1 goto :compile_error

echo   Compiling utilities...
javac -d build -cp build src\common\utils\*.java
if errorlevel 1 goto :compile_error

echo   Compiling server...
javac -d build -cp build src\server\*.java
if errorlevel 1 goto :compile_error

echo   Compiling client...
javac -d build -cp build src\client\*.java
if errorlevel 1 goto :compile_error

echo ✓ Compilation successful!

:setup_complete
echo.
echo [3/3] Setup complete!
echo.
echo ========================================
echo           HOW TO RUN
echo ========================================
echo.
echo 1. Start Server:
echo    run-server.bat
echo.
echo 2. Start Client(s):
echo    run-client.bat [client_name]
echo.
echo Example:
echo    run-client.bat "Alice"
echo    run-client.bat "Bob"
echo.
echo ========================================
echo.
echo Press any key to exit...
pause >nul
exit /b 0

:compile_error
echo ✗ Compilation failed!
echo.
echo Please check the error messages above.
echo Make sure all Java files are syntactically correct.
echo.
pause
exit /b 1