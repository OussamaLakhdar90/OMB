@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "MAVEN_HOME=C:\Program Files\apache-maven-3.9.6"
set "PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%"

cd /d d:\Mohamed\ca-bnc-ciam-autotests

echo JAVA_HOME=%JAVA_HOME%
echo MAVEN_HOME=%MAVEN_HOME%
echo.

call mvn test -Dgroups=unit

echo.
echo BUILD EXIT CODE: %ERRORLEVEL%
