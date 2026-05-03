@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF)
@REM Maven Wrapper startup script for Windows
@REM ----------------------------------------------------------------------------
@echo off
setlocal

if not defined JAVA_HOME (
    echo Error: JAVA_HOME is not set. >&2
    exit /b 1
)

set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties

for /f "tokens=2 delims==" %%a in ('findstr "distributionUrl" "%MAVEN_WRAPPER_PROPERTIES%"') do set DISTRIBUTION_URL=%%a

if not defined MAVEN_USER_HOME set MAVEN_USER_HOME=%USERPROFILE%\.m2
set MAVEN_WRAPPER_HOME=%MAVEN_USER_HOME%\wrapper

for %%a in ("%DISTRIBUTION_URL%") do set DISTRIBUTION_NAME=%%~na
set MAVEN_HOME=%MAVEN_WRAPPER_HOME%\%DISTRIBUTION_NAME%

if not exist "%MAVEN_HOME%" (
    mkdir "%MAVEN_WRAPPER_HOME%"
    echo Downloading Maven from %DISTRIBUTION_URL%
    powershell -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%MAVEN_WRAPPER_HOME%\%DISTRIBUTION_NAME%.zip'"
    powershell -Command "Expand-Archive -Path '%MAVEN_WRAPPER_HOME%\%DISTRIBUTION_NAME%.zip' -DestinationPath '%MAVEN_WRAPPER_HOME%'"
    del "%MAVEN_WRAPPER_HOME%\%DISTRIBUTION_NAME%.zip"
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
endlocal
