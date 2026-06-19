@echo off
echo ========================================
echo    Sxcution - Build APK Script
echo ========================================
echo.

echo [1/3] Cleaning previous build...
call gradlew clean
if %errorlevel% neq 0 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)

echo.
echo [2/3] Building debug APK...
call gradlew assembleDebug
if %errorlevel% neq 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo.
echo [3/3] APK built successfully!
echo.
echo APK Location: app\build\outputs\apk\debug\app-debug.apk
echo.
echo ========================================
echo    Build completed successfully!
echo ========================================
echo.
echo To install APK:
echo adb install app\build\outputs\apk\debug\app-debug.apk
echo.
pause