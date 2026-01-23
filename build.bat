@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21
cd /d d:\Mohamed\ca-bnc-ciam-autotests
"C:\Program Files\apache-maven-3.9.6\bin\mvn.cmd" clean compile > build_output.txt 2>&1
