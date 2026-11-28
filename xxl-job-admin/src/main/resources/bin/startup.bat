@echo off
setlocal enabledelayedexpansion
chcp 65001 > nul
title run jar

if exist "%JAVA_HOME%\bin\java.exe" (
    echo Java env has found.
) else (
    :: from jdk get jre
	:: bin\jlink --module-path jmods --add-modules java.base,java.sql,java.desktop,java.xml,jdk.compiler,java.instrument,java.logging,java.prefs,java.naming,java.management,java.security.jgss,java.scripting,jdk.dynalink,jdk.unsupported,java.sql.rowset --bind-services --output jre --strip-debug --no-man-pages --no-header-files --compress=2
	set JAVA_HOME=jre
	echo Java env use embed jre.
)

set CLASSPATH=.;%JAVA_HOME%\lib;%JAVA_HOME%\lib\dt.jar;%JAVA_HOME%\lib\tools.jar
set PATH=%JAVA_HOME%\bin;%PATH%

set MAX_JAR=

set JVM_OPTS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dlogback.app.env=test
SET OPENS_JVM_OPTS=
set JAVA_APP=java.exe

:: try got java version
for /f "tokens=3" %%v in ('%JAVA_APP% -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VER=%%~v"
    goto :found_version
)
echo not java found.
exit /b 1

:found_version
echo detect java version: %JAVA_VER%

for /f "tokens=1 delims=." %%m in ("%JAVA_VER%") do set "MAJOR_VER=%%m"
echo java major version: %MAJOR_VER%

mkdir lib-unload

if %MAJOR_VER% GTR 1 (
	SET OPENS_JVM_OPTS=--add-opens java.base/java.lang.invoke=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED
	echo jdk9+ add modules reflect jvm args.
	move lib-unload\nashorn*.jar lib
) else (
	move lib\nashorn*.jar lib-unload
)

:: search the max jar file ...
for /f "tokens=*" %%a in ('dir /b /o-s /a-d *.jar') do (
    set "MAX_JAR=%%a"
    goto :done
)

:: not found, exit
echo "not found any jar in current dir, will exit!"
pause
exit

:: found jar file, run it
:done
title %MAX_JAR%

@echo on
%JAVA_APP%  %JVM_OPTS% %OPENS_JVM_OPTS%  -jar %MAX_JAR%