@echo off
setlocal
set "GRADLE_VERSION=8.11.1"
set "BASE=%USERPROFILE%\.gradle\chronodeck-bootstrap"
set "GRADLE_HOME=%BASE%\gradle-%GRADLE_VERSION%"
set "WRAPPER_JAR=%~dp0gradle\wrapper\gradle-wrapper.jar"
if exist "%WRAPPER_JAR%" (
  java %JAVA_OPTS% %GRADLE_OPTS% -classpath "%WRAPPER_JAR%" -Dorg.gradle.appname=gradlew org.gradle.wrapper.GradleWrapperMain %*
  exit /b %ERRORLEVEL%
)
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%BASE%" mkdir "%BASE%"
  set "ZIP=%BASE%\gradle-%GRADLE_VERSION%-bin.zip"
  if not exist "%ZIP%" powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%BASE%\gradle-%GRADLE_VERSION%-bin.zip'"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%BASE%\gradle-%GRADLE_VERSION%-bin.zip' '%BASE%'"
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
