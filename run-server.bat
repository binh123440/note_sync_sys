@echo off
echo Starting Note Sync Server...

:: Find and set Java path
call :find_java
if errorlevel 1 (
    echo Java not found! Please install Java JDK and run setup.bat
    pause
    exit /b 1
)

:: Check if build directory exists
if not exist build (
    echo Build directory not found. Please run setup.bat first.
    pause
    exit /b 1
)

:: Copy config file to build directory
copy config.properties build\ >nul 2>&1

:: Change to build directory and run server
cd build
java server.NoteSyncServer

pause

:: Function to find Java
:find_java
javac -version >nul 2>&1
if not errorlevel 1 goto :eof

set "JAVA_PATHS=C:\Program Files\Java C:\Program Files (x86)\Java C:\Program Files\Eclipse Adoptium"
for %%P in (%JAVA_PATHS%) do (
    if exist "%%P" (
        for /d %%D in ("%%P\*jdk*") do (
            if exist "%%D\bin\javac.exe" (
                set "PATH=%%D\bin;%PATH%"
                goto :eof
            )
        )
    )
)
exit /b 1