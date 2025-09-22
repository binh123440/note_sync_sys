@echo off
echo Starting Note Sync Client...

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

:: Get client name from command line argument or ask user
set CLIENT_NAME=%1
if "%CLIENT_NAME%"=="" (
    set /p CLIENT_NAME="Enter client name: "
)

if "%CLIENT_NAME%"=="" (
    set CLIENT_NAME=DefaultClient
)

echo Starting client: %CLIENT_NAME%

:: Change to build directory and run client
cd build
java client.NoteSyncClient "%CLIENT_NAME%"

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

@echo off
cd build
for %%f in (h2*.jar) do set H2_JAR=%%f
java -cp ".;%H2_JAR%" server.NoteSyncServer
pause