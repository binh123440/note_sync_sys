@echo off
echo Downloading Portable Java...

echo This script will help you download a portable version of Java
echo that doesn't require system installation.
echo.
echo Please visit: https://adoptium.net/temurin/releases/
echo.
echo 1. Select "JDK"
echo 2. Select your Windows version (x64)
echo 3. Select "Archive" (zip file)
echo 4. Download and extract to a folder
echo 5. Run this script again and provide the path
echo.

set /p JAVA_PATH="Enter the path to your Java installation (e.g., C:\java\jdk-17): "

if not exist "%JAVA_PATH%\bin\javac.exe" (
    echo Error: javac.exe not found in %JAVA_PATH%\bin\
    echo Please check the path and try again.
    pause
    exit /b 1
)

echo.
echo Setting up Java environment...
set "JAVA_HOME=%JAVA_PATH%"
set "PATH=%JAVA_PATH%\bin;%PATH%"

echo Testing Java installation...
javac -version
java -version

echo.
echo Java setup complete! You can now run:
echo   compile.bat
echo   run-server.bat
echo   run-client.bat
echo.
pause