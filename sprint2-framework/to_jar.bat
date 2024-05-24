@echo off
setlocal enabledelayedexpansion

set "work_dir=E:\ITU\S4\Dynamique_web\Projet_Sprint\Projet_Sprint2\sprint2_framework"
set "src=%work_dir%\src"
set "lib=%work_dir%\lib"
set "bin=%work_dir%\classes"
set "jar_name=sprint2"
set "jar_path=%work_dir%\%jar_name%.jar"

if exist "%bin%" (
    rd /s /q "%bin%"
)

:: Java files compilation
dir /s /B "%src%\*.java" > sources.txt
javac -d "%bin%" -cp "%lib%\*" @sources.txt
del sources.txt

:: Jar packaging
echo Packaging %jar_name%.jar...
jar cf "%jar_path%" -C "%bin%" .

echo fichier JAR  a ete cree
pause
