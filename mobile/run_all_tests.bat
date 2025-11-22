@echo off
REM Porakhela Android App - Windows Test Suite Runner
REM Comprehensive testing for child-friendly onboarding experience

echo 🧪 Starting Porakhela Comprehensive Test Suite
echo ==============================================

REM Test results tracking
set UNIT_TEST_RESULT=0
set UI_TEST_RESULT=0
set DEVICE_TEST_RESULT=0
set E2E_TEST_RESULT=0
set PERFORMANCE_TEST_RESULT=0
set ACCESSIBILITY_TEST_RESULT=0

echo.
echo ℹ️  Checking for connected Android devices...
adb devices -l

REM Count devices (excluding header)
for /f %%i in ('adb devices ^| find /c "device"') do set DEVICE_COUNT=%%i
if %DEVICE_COUNT% LSS 2 (
    echo ❌ No Android devices found. Please connect a device or start an emulator.
    pause
    exit /b 1
)

echo ✅ Android device detected

echo.
echo ℹ️  Building the application...
call gradlew assembleDebug assembleDebugAndroidTest
if errorlevel 1 (
    echo ❌ Application build failed
    pause
    exit /b 1
)
echo ✅ Application build completed

echo.
echo ===== PHASE 1: UNIT TESTS =====
echo ℹ️  Running Unit Tests...
call gradlew test
if errorlevel 1 (
    echo ❌ Unit Tests failed
) else (
    echo ✅ Unit Tests completed successfully
    set UNIT_TEST_RESULT=1
)

echo.
echo ===== PHASE 2: API MOCK TESTS =====
echo ℹ️  Running API Mock Tests...
call gradlew testDebugUnitTest --tests com.porakhela.data.api.*
if errorlevel 1 (
    echo ❌ API Mock Tests failed
) else (
    echo ✅ API Mock Tests completed successfully
    set API_TEST_RESULT=1
)

echo.
echo ===== PHASE 3: UI TESTS (ESPRESSO) =====
echo ℹ️  Running Login/OTP UI Tests...
call gradlew connectedAndroidTest --tests com.porakhela.ui.auth.*
if errorlevel 1 (
    echo ❌ Login/OTP UI Tests failed
    set LOGIN_UI_RESULT=0
) else (
    echo ✅ Login/OTP UI Tests completed successfully
    set LOGIN_UI_RESULT=1
)

echo ℹ️  Running Profile Creation UI Tests...
call gradlew connectedAndroidTest --tests com.porakhela.ui.profile.*
if errorlevel 1 (
    echo ❌ Profile Creation UI Tests failed
    set PROFILE_UI_RESULT=0
) else (
    echo ✅ Profile Creation UI Tests completed successfully
    set PROFILE_UI_RESULT=1
)

echo ℹ️  Running Dashboard UI Tests...
call gradlew connectedAndroidTest --tests com.porakhela.ui.dashboard.*
if errorlevel 1 (
    echo ❌ Dashboard UI Tests failed
    set DASHBOARD_UI_RESULT=0
) else (
    echo ✅ Dashboard UI Tests completed successfully
    set DASHBOARD_UI_RESULT=1
)

REM Calculate UI test success
if %LOGIN_UI_RESULT%==1 if %PROFILE_UI_RESULT%==1 if %DASHBOARD_UI_RESULT%==1 (
    set UI_TEST_RESULT=1
)

echo.
echo ===== PHASE 4: DEVICE COMPATIBILITY TESTS =====
echo ℹ️  Running Device Compatibility Tests...
call gradlew connectedAndroidTest --tests com.porakhela.device.*
if errorlevel 1 (
    echo ❌ Device Compatibility Tests failed
) else (
    echo ✅ Device Compatibility Tests completed successfully
    set DEVICE_TEST_RESULT=1
)

echo.
echo ===== PHASE 5: END-TO-END TESTS =====
echo ℹ️  Running Complete User Journey Test...
call gradlew connectedAndroidTest --tests com.porakhela.e2e.*
if errorlevel 1 (
    echo ❌ End-to-End Tests failed
) else (
    echo ✅ End-to-End Tests completed successfully
    set E2E_TEST_RESULT=1
)

echo.
echo ===== PHASE 6: PERFORMANCE TESTS =====
echo ℹ️  Running Performance Benchmark Tests...
call gradlew connectedAndroidTest --tests com.porakhela.performance.*
if errorlevel 1 (
    echo ❌ Performance Tests failed
) else (
    echo ✅ Performance Tests completed successfully
    set PERFORMANCE_TEST_RESULT=1
)

echo.
echo ===== PHASE 7: ACCESSIBILITY TESTS =====
echo ℹ️  Running Accessibility Tests...
call gradlew connectedAndroidTest --tests com.porakhela.accessibility.*
if errorlevel 1 (
    echo ❌ Accessibility Tests failed
) else (
    echo ✅ Accessibility Tests completed successfully
    set ACCESSIBILITY_TEST_RESULT=1
)

echo.
echo ===== GENERATING TEST REPORTS =====
echo ℹ️  Creating test reports...
call gradlew createDebugCoverageReport

echo.
echo ==============================================
echo ℹ️  🏁 TEST SUITE EXECUTION COMPLETE
echo ==============================================
echo.

echo 📊 DETAILED RESULTS:
echo -------------------
if %UNIT_TEST_RESULT%==1 (echo ✅ Unit Tests) else (echo ❌ Unit Tests)
if %API_TEST_RESULT%==1 (echo ✅ API Mock Tests) else (echo ❌ API Mock Tests)
if %UI_TEST_RESULT%==1 (echo ✅ UI Tests ^(Espresso^)) else (echo ❌ UI Tests ^(Espresso^))
if %DEVICE_TEST_RESULT%==1 (echo ✅ Device Compatibility Tests) else (echo ❌ Device Compatibility Tests)
if %E2E_TEST_RESULT%==1 (echo ✅ End-to-End Tests) else (echo ❌ End-to-End Tests)
if %PERFORMANCE_TEST_RESULT%==1 (echo ✅ Performance Tests) else (echo ❌ Performance Tests)
if %ACCESSIBILITY_TEST_RESULT%==1 (echo ✅ Accessibility Tests) else (echo ❌ Accessibility Tests)

echo.

REM Calculate overall success rate
set /a TOTAL_TESTS=7
set /a PASSED_TESTS=%UNIT_TEST_RESULT% + %API_TEST_RESULT% + %UI_TEST_RESULT% + %DEVICE_TEST_RESULT% + %E2E_TEST_RESULT% + %PERFORMANCE_TEST_RESULT% + %ACCESSIBILITY_TEST_RESULT%
set /a SUCCESS_RATE=%PASSED_TESTS% * 100 / %TOTAL_TESTS%

echo 📈 OVERALL RESULTS:
echo -------------------
echo Tests Passed: %PASSED_TESTS%/%TOTAL_TESTS%
echo Success Rate: %SUCCESS_RATE%%%
echo.

REM Final verdict
if %PASSED_TESTS%==%TOTAL_TESTS% (
    echo ✅ 🎉 ALL TESTS PASSED! The Porakhela app is ready for child-friendly onboarding.
    echo.
    echo ℹ️  ✨ The app successfully validates:
    echo    • Phone authentication ^& OTP verification
    echo    • Child profile creation with proper validation
    echo    • Dashboard navigation ^& lesson browsing
    echo    • Offline caching ^& download functionality
    echo    • Device compatibility across different configurations
    echo    • Complete user journey from install to lesson completion
    echo    • Performance optimization for smooth experience
    echo    • Accessibility features for all children
    
    if exist "app\build\reports\tests\" (
        echo ℹ️  📋 Test reports available at: app\build\reports\tests\
    )
    
    if exist "app\build\reports\coverage\" (
        echo ℹ️  📊 Coverage reports available at: app\build\reports\coverage\
    )
    
) else (
    echo ❌ SOME TESTS FAILED. Please review the failed tests before deployment.
    echo.
    echo ⚠️  🔍 Failed test categories need attention:
    
    if %UNIT_TEST_RESULT%==0 echo    • Unit Tests - Check business logic and ViewModels
    if %API_TEST_RESULT%==0 echo    • API Tests - Verify network mocking and responses
    if %UI_TEST_RESULT%==0 echo    • UI Tests - Check user interface interactions
    if %DEVICE_TEST_RESULT%==0 echo    • Device Tests - Verify compatibility across devices
    if %E2E_TEST_RESULT%==0 echo    • E2E Tests - Check complete user journey
    if %PERFORMANCE_TEST_RESULT%==0 echo    • Performance Tests - Optimize slow operations
    if %ACCESSIBILITY_TEST_RESULT%==0 echo    • Accessibility Tests - Improve child-friendly features
    
    echo.
    echo ℹ️  💡 To debug specific failures, run individual test suites:
    echo    gradlew test --tests com.porakhela.[package].*
    echo    gradlew connectedAndroidTest --tests com.porakhela.[package].*
)

echo.
echo Press any key to exit...
pause > nul