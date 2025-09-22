@echo off
echo Searching for Java installation...

:: Common Java installation paths
set "JAVA_PATHS=C:\Program Files\Java C:\Program Files (x86)\Java C:\Program Files\Eclipse Adoptium C:\Program Files\Eclipse Foundation"

for %%P in (%JAVA_PATHS%) do (
    if exist "%%P" (
        echo Checking: %%P
        for /d %%D in ("%%P\*jdk*") do (
            if exist "%%D\bin\javac.exe" (
                echo Found JDK at: %%D
                set "JAVA_HOME=%%D"
                set "PATH=%%D\bin;%PATH%"
                goto :found
            )
        )
    )
)

echo Java JDK not found in common locations.
echo Please install Java JDK from:
echo   - Oracle: https://www.oracle.com/java/technologies/downloads/
echo   - OpenJDK: https://adoptium.net/
echo.
echo After installation, run this script again.
pause
exit /b 1

:found
echo.
echo Java JDK found and configured!
echo JAVA_HOME: %JAVA_HOME%
echo.
echo Testing Java installation...
javac -version
java -version
echo.
echo You can now run compile.bat
pause