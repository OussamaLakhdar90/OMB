@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d d:\Mohamed\ca-bnc-ciam-autotests
mvn test -Dgroups=unit -DfailIfNoTests=false
