@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "MAVEN_HOME=C:\Program Files\apache-maven-3.9.6"
set "PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%"
set "LOGFILE=d:\Mohamed\ca-bnc-ciam-autotests\build-output.log"

d:
cd \Mohamed\ca-bnc-ciam-autotests

echo === Build started at %date% %time% === > "%LOGFILE%"
echo Current directory: %cd% >> "%LOGFILE%"
echo. >> "%LOGFILE%"

echo === COMPILE PHASE === >> "%LOGFILE%"
call "%MAVEN_HOME%\bin\mvn.cmd" compile -q 2>&1 >> "%LOGFILE%"
echo Compile exit code: %ERRORLEVEL% >> "%LOGFILE%"
echo. >> "%LOGFILE%"

echo === TEST PHASE === >> "%LOGFILE%"
call "%MAVEN_HOME%\bin\mvn.cmd" test -Dgroups=unit 2>&1 >> "%LOGFILE%"
echo Test exit code: %ERRORLEVEL% >> "%LOGFILE%"

echo === Build completed at %date% %time% === >> "%LOGFILE%"
