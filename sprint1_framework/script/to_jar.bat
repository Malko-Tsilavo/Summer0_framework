@echo off
setlocal enabledelayedexpansion

set "work-dir=E:\ITU\S4\dynamique_web\Projet_Sprint1\sprint1_framework"
set "src=%work-dir%\src"
set "bin=%work-dir%\bin"
set "lib=%work-dir%\lib"



echo Demarage Jar
jar cf "%work-dir%\lib\sprint1.jar" -C "%bin%" .
echo Fin Jar