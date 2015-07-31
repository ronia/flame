@ECHO OFF
IF [%1] == [] (ECHO USERNAME parameter is missing && ECHO Usage: XTEAMEngine.bat USERNAME XTEAM_ENGINE_MODE && GOTO LAST)
IF [%2] == [] (ECHO XTEAM_ENGINE_MODE parameter is missing && ECHO Usage: XTEAMEngine.bat USERNAME XTEAM_ENGINE_MODE && GOTO LAST)
CALL ant -Dusername=%1 XTEAMEngine%2
:LAST
EXIT
