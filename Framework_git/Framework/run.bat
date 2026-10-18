@echo off
setlocal

set "PROJECT_DIR=%~dp0"
cd /d "%PROJECT_DIR%"

set "TOMCAT_HOME=D:\WebDynamique\apache-tomcat-10.0.16"
set "SERVLET_JAR=%TOMCAT_HOME%\lib\servlet-api.jar"
set "GSON_JAR=%PROJECT_DIR%gson-2.10.1.jar"
set "TARGET_DIR=%TOMCAT_HOME%\webapps\sprint8"
set "BIN_DIR=%PROJECT_DIR%bin"
set "APP_BIN_DIR=%PROJECT_DIR%App\WEB-INF\classes"
set "FRAMEWORK_JAR=%PROJECT_DIR%framework.jar"

if not exist "%TOMCAT_HOME%\bin\startup.bat" (
    echo ERREUR : Tomcat introuvable dans "%TOMCAT_HOME%"
    exit /b 1
)
if not exist "%SERVLET_JAR%" (
    echo ERREUR : servlet-api.jar introuvable dans "%TOMCAT_HOME%\lib"
    exit /b 1
)
if not exist "%GSON_JAR%" (
    if exist "D:\WebDynamique\SPRINT6-main\framework\lib\gson-2.10.1.jar" (
        set "GSON_JAR=D:\WebDynamique\SPRINT6-main\framework\lib\gson-2.10.1.jar"
    ) else (
        echo ERREUR : gson-2.10.1.jar introuvable.
        echo Placez-le dans "%PROJECT_DIR%" ou adaptez GSON_JAR dans run.bat.
        exit /b 1
    )
)

echo Nettoyage...
if exist "%BIN_DIR%" rmdir /s /q "%BIN_DIR%"
if exist "%APP_BIN_DIR%" rmdir /s /q "%APP_BIN_DIR%"
if exist "%FRAMEWORK_JAR%" del /f /q "%FRAMEWORK_JAR%"
if exist "%TARGET_DIR%" rmdir /s /q "%TARGET_DIR%"

mkdir "%BIN_DIR%"
mkdir "%APP_BIN_DIR%"

echo Compilation du framework...
dir /s /b "%PROJECT_DIR%src\main\java\*.java" > "%TEMP%\sprint8-framework-sources.txt"
javac -parameters -d "%BIN_DIR%" -cp "%SERVLET_JAR%;%GSON_JAR%" @"%TEMP%\sprint8-framework-sources.txt"
if errorlevel 1 (
    echo ERREUR de compilation du framework.
    exit /b 1
)
jar -cf "%FRAMEWORK_JAR%" -C "%BIN_DIR%" .
if errorlevel 1 (
    echo ERREUR de creation de framework.jar.
    exit /b 1
)

echo Compilation de l'application...
dir /s /b "%PROJECT_DIR%App\src\main\java\*.java" > "%TEMP%\sprint8-app-sources.txt"
javac -parameters -d "%APP_BIN_DIR%" -cp "%FRAMEWORK_JAR%;%SERVLET_JAR%" @"%TEMP%\sprint8-app-sources.txt"
if errorlevel 1 (
    echo ERREUR de compilation de l'application.
    exit /b 1
)

echo Deploiement dans Tomcat...
mkdir "%TARGET_DIR%\WEB-INF\classes"
mkdir "%TARGET_DIR%\WEB-INF\lib"
copy /y "%PROJECT_DIR%App\src\main\webapp\WEB-INF\web.xml" "%TARGET_DIR%\WEB-INF\web.xml" >nul
xcopy /e /i /y "%PROJECT_DIR%App\src\main\webapp" "%TARGET_DIR%" >nul
xcopy /e /i /y "%APP_BIN_DIR%" "%TARGET_DIR%\WEB-INF\classes" >nul
copy /y "%FRAMEWORK_JAR%" "%TARGET_DIR%\WEB-INF\lib\framework.jar" >nul
copy /y "%GSON_JAR%" "%TARGET_DIR%\WEB-INF\lib\gson-2.10.1.jar" >nul

echo.
echo Deploiement Sprint 8 termine.
echo Formulaire : http://localhost:8082/sprint8/upload.html
echo.
endlocal
