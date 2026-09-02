@echo off
set MAVEN_HOME=%~dp0..\tools\apache-maven-3.9.16
"%MAVEN_HOME%\bin\mvn.cmd" %*
