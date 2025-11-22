#!/bin/bash

# Porakhela Android App - Comprehensive Test Suite Runner
# This script runs all test types: Unit, UI, Device, End-to-End, Performance, and Accessibility tests

echo "🧪 Starting Porakhela Comprehensive Test Suite"
echo "=============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test results tracking
UNIT_TEST_RESULT=0
UI_TEST_RESULT=0
DEVICE_TEST_RESULT=0
E2E_TEST_RESULT=0
PERFORMANCE_TEST_RESULT=0
ACCESSIBILITY_TEST_RESULT=0

# Function to print colored output
print_status() {
    local status=$1
    local message=$2
    if [ "$status" = "SUCCESS" ]; then
        echo -e "${GREEN}✅ $message${NC}"
    elif [ "$status" = "FAIL" ]; then
        echo -e "${RED}❌ $message${NC}"
    elif [ "$status" = "INFO" ]; then
        echo -e "${BLUE}ℹ️  $message${NC}"
    elif [ "$status" = "WARNING" ]; then
        echo -e "${YELLOW}⚠️  $message${NC}"
    fi
}

# Function to run tests with error handling
run_test_suite() {
    local test_name=$1
    local test_command=$2
    local test_package=$3
    
    echo ""
    print_status "INFO" "Running $test_name..."
    echo "Command: $test_command"
    echo "Package: $test_package"
    echo "----------------------------------------"
    
    if eval $test_command; then
        print_status "SUCCESS" "$test_name completed successfully"
        return 0
    else
        print_status "FAIL" "$test_name failed"
        return 1
    fi
}

# Check if Android device/emulator is connected
print_status "INFO" "Checking for connected Android devices..."
adb devices -l

if [ $(adb devices | grep -c device) -lt 2 ]; then
    print_status "FAIL" "No Android devices found. Please connect a device or start an emulator."
    exit 1
fi

print_status "SUCCESS" "Android device detected"

# Build the app first
print_status "INFO" "Building the application..."
if ./gradlew assembleDebug assembleDebugAndroidTest; then
    print_status "SUCCESS" "Application build completed"
else
    print_status "FAIL" "Application build failed"
    exit 1
fi

# 1. Unit Tests
print_status "INFO" "===== PHASE 1: UNIT TESTS ====="
if run_test_suite "Unit Tests" "./gradlew test" "com.porakhela.*"; then
    UNIT_TEST_RESULT=1
fi

# 2. API Mock Tests
print_status "INFO" "===== PHASE 2: API MOCK TESTS ====="
if run_test_suite "API Mock Tests" "./gradlew testDebugUnitTest --tests com.porakhela.data.api.*" "com.porakhela.data.api.*"; then
    API_TEST_RESULT=1
fi

# 3. UI Tests (Espresso)
print_status "INFO" "===== PHASE 3: UI TESTS (ESPRESSO) ====="
if run_test_suite "Login/OTP UI Tests" "./gradlew connectedAndroidTest --tests com.porakhela.ui.auth.*" "com.porakhela.ui.auth.*"; then
    LOGIN_UI_RESULT=1
fi

if run_test_suite "Profile Creation UI Tests" "./gradlew connectedAndroidTest --tests com.porakhela.ui.profile.*" "com.porakhela.ui.profile.*"; then
    PROFILE_UI_RESULT=1
fi

if run_test_suite "Dashboard UI Tests" "./gradlew connectedAndroidTest --tests com.porakhela.ui.dashboard.*" "com.porakhela.ui.dashboard.*"; then
    DASHBOARD_UI_RESULT=1
fi

# Calculate UI test success
if [ $LOGIN_UI_RESULT -eq 1 ] && [ $PROFILE_UI_RESULT -eq 1 ] && [ $DASHBOARD_UI_RESULT -eq 1 ]; then
    UI_TEST_RESULT=1
fi

# 4. Device Compatibility Tests
print_status "INFO" "===== PHASE 4: DEVICE COMPATIBILITY TESTS ====="
if run_test_suite "Device Compatibility Tests" "./gradlew connectedAndroidTest --tests com.porakhela.device.*" "com.porakhela.device.*"; then
    DEVICE_TEST_RESULT=1
fi

# 5. End-to-End Tests
print_status "INFO" "===== PHASE 5: END-TO-END TESTS ====="
if run_test_suite "Complete User Journey Test" "./gradlew connectedAndroidTest --tests com.porakhela.e2e.*" "com.porakhela.e2e.*"; then
    E2E_TEST_RESULT=1
fi

# 6. Performance Tests
print_status "INFO" "===== PHASE 6: PERFORMANCE TESTS ====="
if run_test_suite "Performance Benchmark Tests" "./gradlew connectedAndroidTest --tests com.porakhela.performance.*" "com.porakhela.performance.*"; then
    PERFORMANCE_TEST_RESULT=1
fi

# 7. Accessibility Tests
print_status "INFO" "===== PHASE 7: ACCESSIBILITY TESTS ====="
if run_test_suite "Accessibility Tests" "./gradlew connectedAndroidTest --tests com.porakhela.accessibility.*" "com.porakhela.accessibility.*"; then
    ACCESSIBILITY_TEST_RESULT=1
fi

# Generate test reports
print_status "INFO" "===== GENERATING TEST REPORTS ====="
./gradlew createDebugCoverageReport

# Summary Report
echo ""
echo "=============================================="
print_status "INFO" "🏁 TEST SUITE EXECUTION COMPLETE"
echo "=============================================="
echo ""

# Individual test results
echo "📊 DETAILED RESULTS:"
echo "-------------------"

[ $UNIT_TEST_RESULT -eq 1 ] && print_status "SUCCESS" "Unit Tests" || print_status "FAIL" "Unit Tests"
[ $API_TEST_RESULT -eq 1 ] && print_status "SUCCESS" "API Mock Tests" || print_status "FAIL" "API Mock Tests"
[ $UI_TEST_RESULT -eq 1 ] && print_status "SUCCESS" "UI Tests (Espresso)" || print_status "FAIL" "UI Tests (Espresso)"
[ $DEVICE_TEST_RESULT -eq 1 ] && print_status "SUCCESS" "Device Compatibility Tests" || print_status "FAIL" "Device Compatibility Tests"
[ $E2E_TEST_RESULT -eq 1 ] && print_status "SUCCESS" "End-to-End Tests" || print_status "FAIL" "End-to-End Tests"
[ $PERFORMANCE_TEST_RESULT -eq 1 ] && print_status "SUCCESS" "Performance Tests" || print_status "FAIL" "Performance Tests"
[ $ACCESSIBILITY_TEST_RESULT -eq 1 ] && print_status "SUCCESS" "Accessibility Tests" || print_status "FAIL" "Accessibility Tests"

echo ""

# Calculate overall success rate
TOTAL_TESTS=7
PASSED_TESTS=$((UNIT_TEST_RESULT + API_TEST_RESULT + UI_TEST_RESULT + DEVICE_TEST_RESULT + E2E_TEST_RESULT + PERFORMANCE_TEST_RESULT + ACCESSIBILITY_TEST_RESULT))
SUCCESS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))

echo "📈 OVERALL RESULTS:"
echo "-------------------"
echo "Tests Passed: $PASSED_TESTS/$TOTAL_TESTS"
echo "Success Rate: $SUCCESS_RATE%"
echo ""

# Final verdict
if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    print_status "SUCCESS" "🎉 ALL TESTS PASSED! The Porakhela app is ready for child-friendly onboarding."
    echo ""
    print_status "INFO" "✨ The app successfully validates:"
    echo "   • Phone authentication & OTP verification"
    echo "   • Child profile creation with proper validation"
    echo "   • Dashboard navigation & lesson browsing"
    echo "   • Offline caching & download functionality"
    echo "   • Device compatibility across different configurations"
    echo "   • Complete user journey from install to lesson completion"
    echo "   • Performance optimization for smooth experience"
    echo "   • Accessibility features for all children"
    
    # Open test reports if available
    if [ -d "app/build/reports/tests/" ]; then
        print_status "INFO" "📋 Test reports available at: app/build/reports/tests/"
    fi
    
    if [ -d "app/build/reports/coverage/" ]; then
        print_status "INFO" "📊 Coverage reports available at: app/build/reports/coverage/"
    fi
    
    exit 0
else
    print_status "FAIL" "❌ SOME TESTS FAILED. Please review the failed tests before deployment."
    echo ""
    print_status "WARNING" "🔍 Failed test categories need attention:"
    
    [ $UNIT_TEST_RESULT -eq 0 ] && echo "   • Unit Tests - Check business logic and ViewModels"
    [ $API_TEST_RESULT -eq 0 ] && echo "   • API Tests - Verify network mocking and responses"
    [ $UI_TEST_RESULT -eq 0 ] && echo "   • UI Tests - Check user interface interactions"
    [ $DEVICE_TEST_RESULT -eq 0 ] && echo "   • Device Tests - Verify compatibility across devices"
    [ $E2E_TEST_RESULT -eq 0 ] && echo "   • E2E Tests - Check complete user journey"
    [ $PERFORMANCE_TEST_RESULT -eq 0 ] && echo "   • Performance Tests - Optimize slow operations"
    [ $ACCESSIBILITY_TEST_RESULT -eq 0 ] && echo "   • Accessibility Tests - Improve child-friendly features"
    
    echo ""
    print_status "INFO" "💡 To debug specific failures, run individual test suites:"
    echo "   ./gradlew test --tests com.porakhela.[package].*"
    echo "   ./gradlew connectedAndroidTest --tests com.porakhela.[package].*"
    
    exit 1
fi