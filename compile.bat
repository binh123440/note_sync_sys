@echo off
echo Compiling Note Sync System...

:: Find and set Java path
call :find_java
if errorlevel 1 (
    echo Java JDK not found!
    echo Please install Java JDK and run setup-java.bat
    pause
    exit /b 1
)

:: Create build directory
if not exist build mkdir build

:: Compile common models
echo Compiling models...
javac -d build src\common\models\*.java
if errorlevel 1 goto error

:: Compile common network
echo Compiling network classes...
javac -d build -cp build src\common\network\*.java
if errorlevel 1 goto error

:: Compile common utils
echo Compiling utilities...
javac -d build -cp build src\common\utils\*.java
if errorlevel 1 goto error

:: Compile server
echo Compiling server...
javac -d build -cp build src\server\*.java
if errorlevel 1 goto error

:: Compile client
echo Compiling client...
javac -d build -cp build src\client\*.java
if errorlevel 1 goto error

echo.
echo Compilation successful!
echo.
echo To run:
echo   Server: run-server.bat
echo   Client: run-client.bat [client_name]
goto end

:error
echo.
echo Compilation failed!
pause

:find_java
:: Check if javac is already in PATH
javac -version >nul 2>&1
if not errorlevel 1 goto :java_found

:: Search for Java in common locations
set "JAVA_PATHS=C:\Program Files\Java C:\Program Files (x86)\Java C:\Program Files\Eclipse Adoptium"

for %%P in (%JAVA_PATHS%) do (
    if exist "%%P" (
        for /d %%D in ("%%P\*jdk*") do (
            if exist "%%D\bin\javac.exe" (
                set "PATH=%%D\bin;%PATH%"
                goto :java_found
            )
        )
    )
)

:: Java not found
exit /b 1

:java_found
echo Java found and configured.
exit /b 0

:end