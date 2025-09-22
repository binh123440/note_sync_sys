@echo off
echo Starting H2 Database Server...

for %%f in (lib\h2*.jar) do set H2_JAR=%%f
if not defined H2_JAR (
    echo H2 jar file not found in 'lib' directory.
    pause
    exit /b 1
)

echo Found H2 Jar: %H2_JAR%

:: Chạy H2 Server và cho phép kết nối từ bên ngoài
:: -baseDir . nghĩa là database sẽ được tạo/tìm trong thư mục hiện tại
java -jar "%H2_JAR%" -tcp -tcpAllowOthers -web -webAllowOthers -baseDir .

pause