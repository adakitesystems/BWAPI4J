set DEV_CMD_BAT="..\\VsDevCmd.bat"
set TARGET_BUILD_DIR=build_vanilla_windows
set TARGET_BUILD_SOLUTION=BWAPI4JBridge.sln

IF NOT EXIST %TARGET_BUILD_DIR% (
mkdir %TARGET_BUILD_DIR%
)

cd %TARGET_BUILD_DIR%
cmake ..

IF EXIST %TARGET_BUILD_SOLUTION% (
%DEV_CMD_BAT%
MSBuild.exe %TARGET_BUILD_SOLUTION% /p:Configuration=Release
)
