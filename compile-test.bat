@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "MAVEN_HOME=C:\Program Files\apache-maven-3.9.6"
set "PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%"

d:
cd \Mohamed\ca-bnc-ciam-autotests
echo Current directory: %cd%

call "%MAVEN_HOME%\bin\mvn.cmd" compile -q
echo Compile exit code: %ERRORLEVEL%

call "%MAVEN_HOME%\bin\mvn.cmd" test -Dgroups=unit
echo Test exit code: %ERRORLEVEL%
