@echo off
setlocal

rem Spécifiez les chemins des dossiers bin et src
set bin="../bin"
set src="../src"

rem Compilez les fichiers sources Java dans le dossier src
javac -d %bin% %src%\sprint\controller\*.java

rem Créez le fichier JAR en incluant les fichiers binaires
jar cf sprint0_test.jar -C %bin% .

rem Spécifiez le chemin de destination
set destination="../../sprint0_test/lib"

rem Copiez le fichier JAR dans le dossier de destination
copy sprint0_test.jar %destination%

rem Supprimez le fichier JAR après la copie
del sprint0_test.jar

endlocal
