@echo off
echo Compiling Note Sync System with H2 Database...

:: Check H2 jar
if not exist "lib\h2*.jar" (
    echo H2 jar not found! Download from h2database.com
    pause & exit /b 1
)

:: Find H2 jar
for %%f in (lib\h2*.jar) do set H2_JAR=%%f
set CLASSPATH=.;%H2_JAR%

:: Create directories
if not exist build mkdir build
if not exist data mkdir data

:: Compile with H2 classpath
echo Compiling models...
javac -d build -cp "%CLASSPATH%" src\common\models\*.java
if errorlevel 1 goto error

echo Compiling network classes...
javac -d build -cp "build;%CLASSPATH%" src\common\network\*.java
if errorlevel 1 goto error

echo Compiling utilities...
javac -d build -cp "build;%CLASSPATH%" src\common\utils\*.java
if errorlevel 1 goto error

echo Compiling server...
javac -d build -cp "build;%CLASSPATH%" src\server\*.java
if errorlevel 1 goto error

echo Compiling client...
javac -d build -cp "build;%CLASSPATH%" src\client\*.java
if errorlevel 1 goto error

copy "%H2_JAR%" build\
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