@echo off
setlocal enabledelayedexpansion

set "work_dir=E:\ITU\S4\Dynamique_web\Projet_Sprint\Projet_Sprint3\sprint3-framework"
set "src=%work_dir%\src"
set "lib=%work_dir%\lib"
set "bin=%work_dir%\classes"
set "jar_name=sprint3"
set "jar_path=%work_dir%\%jar_name%.jar"
set "test_lib_dir=..\sprint3-test\lib"

:: Clean the bin directory if it exists
if exist "%bin%" (
    rd /s /q "%bin%"
)

:: Create the bin directory
mkdir "%bin%"

:: Java files compilation
echo Compiling Java files...
dir /s /B "%src%\*.java" > sources.txt
javac -d "%bin%" -cp "%lib%\*" @sources.txt
del sources.txt

:: Jar packaging
echo Packaging %jar_name%.jar...
jar cf "%jar_path%" -C "%bin%" .

:: Check if the JAR was created successfully
if exist "%jar_path%" (
    echo JAR file created successfully.
    
    :: Move the JAR to the test lib directory
    if not exist "%test_lib_dir%" (
        mkdir "%test_lib_dir%"
    )
    move "%jar_path%" "%test_lib_dir%"

    :: Verify the JAR was moved successfully
    if exist "%test_lib_dir%\%jar_name%.jar" (
        echo JAR file moved to test lib directory successfully.
    ) else (
        echo Failed to move JAR file to test lib directory.
    )
) else (
    echo Failed to create JAR file.
)

pause
