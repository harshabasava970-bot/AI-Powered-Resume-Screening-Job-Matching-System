@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Batch script for Windows
@REM ----------------------------------------------------------------------------
@ECHO OFF
SETLOCAL

SET MAVEN_PROJECTBASEDIR=%~dp0
SET WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar

IF NOT EXIST "%WRAPPER_JAR%" (
    ECHO Downloading Maven wrapper...
    curl -o "%WRAPPER_JAR%" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    IF %ERRORLEVEL% NEQ 0 (
        ECHO Failed to download Maven wrapper.
        EXIT /B 1
    )
)

java ^
  -classpath "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*
