$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;C:\Program Files\apache-maven-3.9.6\bin;$env:PATH"

Write-Host "Using Java:"
java -version

Write-Host "`nRunning unit tests with coverage..."
Set-Location "d:\Mohamed\ca-bnc-ciam-autotests"
& "C:\Program Files\apache-maven-3.9.6\bin\mvn.cmd" clean test jacoco:report "-DsuiteXmlFile=suites/unit.xml"
