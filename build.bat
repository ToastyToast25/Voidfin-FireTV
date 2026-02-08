@echo off
setlocal enabledelayedexpansion

REM Create build-logs directory if it doesn't exist
if not exist "build-logs\" mkdir "build-logs\"

REM Generate timestamped log filename
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set DATE=%%c-%%a-%%b)
for /f "tokens=1-2 delims=/: " %%a in ('time /t') do (set TIME=%%a-%%b)
set LOGFILE=build-logs\build-%DATE%_%TIME%.log

REM Initialize log file
echo VoidStream Build Tool - Session started at %date% %time% > "%LOGFILE%"
echo. >> "%LOGFILE%"

REM ============================================
REM   VoidStream APK Build Tool v1.0
REM ============================================
echo.
echo Log file: %LOGFILE%
echo.

REM Check for ANDROID_HOME
if not defined ANDROID_HOME (
    echo.
    echo ERROR: ANDROID_HOME environment variable is not set
    echo Please set ANDROID_HOME to your Android SDK location
    echo Example: C:\Users\YourName\AppData\Local\Android\Sdk
    echo.
    pause
    exit /b 1
)

REM Set ADB path
set ADB=%ANDROID_HOME%\platform-tools\adb.exe
if not exist "%ADB%" (
    echo.
    echo ERROR: ADB not found at %ADB%
    echo Please ensure Android SDK platform-tools are installed
    echo.
    pause
    exit /b 1
)

:MAIN_MENU
cls
echo ========================================
echo   VoidStream APK Build Tool v1.0
echo ========================================
echo.
echo Select build flavors:
echo   [1] GitHub (sideload)
echo   [2] Amazon Appstore
echo   [3] Google Play Store
echo   [4] All flavors
echo.
set /p FLAVOR_CHOICE="Choice: "

REM Validate input and set flavors
if "%FLAVOR_CHOICE%"=="1" (
    set FLAVORS=Github
    set FLAVOR_NAMES=github
)
if "%FLAVOR_CHOICE%"=="2" (
    set FLAVORS=Amazon
    set FLAVOR_NAMES=amazon
)
if "%FLAVOR_CHOICE%"=="3" (
    set FLAVORS=Googleplay
    set FLAVOR_NAMES=googleplay
)
if "%FLAVOR_CHOICE%"=="4" (
    set FLAVORS=Github Amazon Googleplay
    set FLAVOR_NAMES=github amazon googleplay
)

if not defined FLAVORS (
    echo.
    echo Invalid choice. Press any key to try again...
    pause >nul
    goto MAIN_MENU
)

:BUILD_TYPE_MENU
cls
echo.
echo Select build type:
echo   [1] Debug (fast, debuggable)
echo   [2] Release (optimized, signed)
echo.
set /p BUILD_TYPE_CHOICE="Choice: "

if "%BUILD_TYPE_CHOICE%"=="1" (
    set BUILD_TYPE=Debug
    set BUILD_TYPE_LOWER=debug
)
if "%BUILD_TYPE_CHOICE%"=="2" (
    set BUILD_TYPE=Release
    set BUILD_TYPE_LOWER=release
)

if not defined BUILD_TYPE (
    echo.
    echo Invalid choice. Press any key to try again...
    pause >nul
    goto BUILD_TYPE_MENU
)

REM Check for keystore if release build
if "%BUILD_TYPE%"=="Release" (
    if not exist "keystore.properties" (
        echo.
        echo ERROR: Release build requires keystore.properties
        echo.
        echo Copy keystore.properties.template and fill in your values:
        echo   1. Copy keystore.properties.template to keystore.properties
        echo   2. Edit keystore.properties with your keystore details
        echo.
        pause
        exit /b 1
    )
)

:OUTPUT_DIR_MENU
cls
echo.
echo Output directory:
echo   [1] Default (app\build\outputs\apk\)
echo   [2] Custom path
echo.
set /p OUTPUT_CHOICE="Choice: "

if "%OUTPUT_CHOICE%"=="1" (
    set OUTPUT_DIR=app\build\outputs\apk
)
if "%OUTPUT_CHOICE%"=="2" (
    echo.
    set /p OUTPUT_DIR="Enter full path (e.g., C:\Builds\VoidStream): "
)

if not defined OUTPUT_DIR (
    echo.
    echo Invalid choice. Press any key to try again...
    pause >nul
    goto OUTPUT_DIR_MENU
)

REM Create output directory if it doesn't exist
if not exist "%OUTPUT_DIR%" (
    echo.
    echo Creating output directory: %OUTPUT_DIR%
    mkdir "%OUTPUT_DIR%" 2>nul
    if errorlevel 1 (
        echo ERROR: Failed to create output directory
        pause
        exit /b 1
    )
)

:CONFIRM_BUILD
cls
echo.
echo ========================================
echo   Build Configuration
echo ========================================
echo.
echo Flavors:     %FLAVOR_NAMES%
echo Build Type:  %BUILD_TYPE%
echo Output Dir:  %OUTPUT_DIR%
echo.
echo Press any key to start build, or Ctrl+C to cancel...
pause >nul

:BUILD_APKS
cls
echo.
echo ========================================
echo   Building APKs...
echo ========================================
echo.
echo This may take several minutes...
echo.

REM Track build success
set BUILD_SUCCESS=1

REM Build each flavor
for %%f in (%FLAVORS%) do (
    echo.
    echo ----------------------------------------
    echo Building %%f %BUILD_TYPE%...
    echo ----------------------------------------
    echo.

    REM Run Gradle build with logging
    call :LogMsg "Executing: gradlew.bat assemble%%f%BUILD_TYPE% --stacktrace"
    call gradlew.bat assemble%%f%BUILD_TYPE% --stacktrace >> "%LOGFILE%" 2>&1

    if errorlevel 1 (
        echo.
        echo ========================================
        echo   ERROR: Build failed for %%f
        echo ========================================
        echo.
        call :LogMsg "ERROR: Build failed for %%f"
        call :LogMsg "Check %LOGFILE% for full details"
        echo Check the Gradle output above for details
        echo Common issues:
        echo   - Missing keystore.properties (for Release builds)
        echo   - Build cache corruption (try: gradlew clean)
        echo   - Out of memory (close other applications)
        echo.
        set BUILD_SUCCESS=0
        pause
        exit /b 1
    )

    REM Copy APK to output directory if different from default
    if not "%OUTPUT_CHOICE%"=="1" (
        echo.
        echo Copying APK to %OUTPUT_DIR%...

        REM Find the flavor name in lowercase for the path
        set FLAVOR_LOWER=%%f
        call :ToLower FLAVOR_LOWER

        REM Copy all APKs from the flavor directory
        for %%a in ("app\build\outputs\apk\!FLAVOR_LOWER!\%BUILD_TYPE_LOWER%\*.apk") do (
            copy /Y "%%a" "%OUTPUT_DIR%\" >nul
            if errorlevel 1 (
                echo WARNING: Failed to copy %%a
            ) else (
                echo   Copied: %%~nxa
            )
        )
    )

    echo.
    echo ✓ %%f %BUILD_TYPE% build complete
)

if %BUILD_SUCCESS%==1 (
    echo.
    echo ========================================
    echo   Build Complete!
    echo ========================================
    echo.
    echo APKs are located in:
    if "%OUTPUT_CHOICE%"=="1" (
        echo   app\build\outputs\apk\[flavor]\%BUILD_TYPE_LOWER%\
    ) else (
        echo   %OUTPUT_DIR%\
    )
    echo.
)

:EMULATOR_MENU
echo.
echo ========================================
echo   Emulator Deployment
echo ========================================
echo.
echo Options:
echo   [1] Install to running emulator
echo   [2] Uninstall old + Install new
echo   [3] List running emulators/devices
echo   [4] Skip deployment
echo.
set /p EMULATOR_CHOICE="Choice: "

if "%EMULATOR_CHOICE%"=="1" goto INSTALL_APK
if "%EMULATOR_CHOICE%"=="2" goto UNINSTALL_AND_INSTALL
if "%EMULATOR_CHOICE%"=="3" goto LIST_EMULATORS
if "%EMULATOR_CHOICE%"=="4" goto END

REM Invalid choice
echo.
echo Invalid choice. Press any key to try again...
pause >nul
goto EMULATOR_MENU

:LIST_EMULATORS
cls
echo.
echo ========================================
echo   Connected Devices
echo ========================================
echo.
"%ADB%" devices -l
echo.
echo Press any key to continue...
pause >nul
goto EMULATOR_MENU

:UNINSTALL_AND_INSTALL
echo.
echo ----------------------------------------
echo   Uninstalling old version...
echo ----------------------------------------
echo.
"%ADB%" uninstall org.voidstream.androidtv 2>nul
if errorlevel 1 (
    echo App was not installed or uninstall failed
) else (
    echo ✓ Old version uninstalled successfully
)
echo.
goto INSTALL_APK

:INSTALL_APK
echo.
echo ----------------------------------------
echo   Checking for connected devices...
echo ----------------------------------------
echo.

REM Check if any device is connected
"%ADB%" devices | findstr /C:"device" | findstr /V "List" >nul
if errorlevel 1 (
    echo.
    echo ERROR: No emulator or device detected
    echo.
    echo Please:
    echo   1. Start an Android emulator, or
    echo   2. Connect a physical device via USB/WiFi
    echo   3. Enable USB debugging on the device
    echo.
    echo Then run this script again or press any key to retry...
    pause >nul
    goto EMULATOR_MENU
)

REM Count connected devices
set DEVICE_COUNT=0
for /f "tokens=1" %%d in ('"%ADB%" devices ^| findstr /C:"device" ^| findstr /V "List"') do (
    set /a DEVICE_COUNT+=1
    set DEVICE_ID=%%d
)

if %DEVICE_COUNT% GTR 1 (
    echo Multiple devices detected. Listing devices:
    echo.
    "%ADB%" devices -l
    echo.
    echo Please disconnect extra devices or specify device ID manually
    echo.
    pause
    goto EMULATOR_MENU
)

echo ✓ Device connected: %DEVICE_ID%
echo.

:SELECT_APK
echo ----------------------------------------
echo   Select APK to install
echo ----------------------------------------
echo.

REM List built flavors
set APK_INDEX=1
for %%f in (%FLAVOR_NAMES%) do (
    echo   [!APK_INDEX!] %%f %BUILD_TYPE_LOWER%
    set FLAVOR_!APK_INDEX!=%%f
    set /a APK_INDEX+=1
)
echo.
set /p APK_CHOICE="Choice: "

REM Validate choice
if not defined FLAVOR_%APK_CHOICE% (
    echo Invalid choice. Press any key to try again...
    pause >nul
    goto SELECT_APK
)

REM Get selected flavor
call set SELECTED_FLAVOR=%%FLAVOR_%APK_CHOICE%%%

REM Find the APK file
set APK_PATH=
if "%OUTPUT_CHOICE%"=="1" (
    REM Default location
    for %%i in ("app\build\outputs\apk\%SELECTED_FLAVOR%\%BUILD_TYPE_LOWER%\*.apk") do (
        set APK_PATH=%%i
    )
) else (
    REM Custom output directory
    for %%i in ("%OUTPUT_DIR%\*%SELECTED_FLAVOR%*%BUILD_TYPE_LOWER%*.apk") do (
        set APK_PATH=%%i
    )
)

if not defined APK_PATH (
    echo.
    echo ERROR: APK not found
    echo.
    echo Expected location:
    if "%OUTPUT_CHOICE%"=="1" (
        echo   app\build\outputs\apk\%SELECTED_FLAVOR%\%BUILD_TYPE_LOWER%\
    ) else (
        echo   %OUTPUT_DIR%\
    )
    echo.
    pause
    goto EMULATOR_MENU
)

echo.
echo ----------------------------------------
echo   Installing APK
echo ----------------------------------------
echo.
echo APK: %APK_PATH%
echo Device: %DEVICE_ID%
echo.

"%ADB%" install -r "%APK_PATH%"

if errorlevel 1 (
    echo.
    echo ========================================
    echo   Installation Failed
    echo ========================================
    echo.
    echo Common issues:
    echo   - Signature conflict (try option 2: Uninstall + Install)
    echo   - Insufficient storage on device
    echo   - Device is locked (unlock and try again)
    echo   - USB debugging not authorized
    echo.
    pause
    goto EMULATOR_MENU
)

echo.
echo ✓ Installation complete!
echo.

REM Ask if user wants to launch the app
echo.
set /p LAUNCH="Launch app now? (Y/N): "
if /i "%LAUNCH%"=="Y" (
    echo.
    echo Launching VoidStream...
    "%ADB%" shell am start -n org.voidstream.androidtv/.ui.startup.StartupActivity
    echo.
    echo ✓ App launched
)

echo.
pause

:END
cls
echo.
echo ========================================
echo   VoidStream Build Tool - Done!
echo ========================================
echo.
echo Build output location:
if "%OUTPUT_CHOICE%"=="1" (
    echo   app\build\outputs\apk\[flavor]\%BUILD_TYPE_LOWER%\
) else (
    echo   %OUTPUT_DIR%\
)
echo.
echo Build log saved to: %LOGFILE%
echo You can review this log at any time.
echo.
echo Thanks for using VoidStream Build Tool!
echo.
pause
exit /b 0

REM ============================================
REM   Helper Functions
REM ============================================

:LogMsg
REM Log message to both console and file
echo %~1
echo %~1 >> "%LOGFILE%"
goto :eof

:ToLower
REM Convert variable to lowercase
set %1=!%1:A=a!
set %1=!%1:B=b!
set %1=!%1:C=c!
set %1=!%1:D=d!
set %1=!%1:E=e!
set %1=!%1:F=f!
set %1=!%1:G=g!
set %1=!%1:H=h!
set %1=!%1:I=i!
set %1=!%1:J=j!
set %1=!%1:K=k!
set %1=!%1:L=l!
set %1=!%1:M=m!
set %1=!%1:N=n!
set %1=!%1:O=o!
set %1=!%1:P=p!
set %1=!%1:Q=q!
set %1=!%1:R=r!
set %1=!%1:S=s!
set %1=!%1:T=t!
set %1=!%1:U=u!
set %1=!%1:V=v!
set %1=!%1:W=w!
set %1=!%1:X=x!
set %1=!%1:Y=y!
set %1=!%1:Z=z!
goto :eof
