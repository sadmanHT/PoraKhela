# Porakhela App Testing Documentation

## Overview

This document outlines the comprehensive testing strategy for the Porakhela educational app, ensuring a smooth and child-friendly onboarding experience.

## Test Structure

The testing suite is organized into 7 main categories:

### 1. Unit Tests 🧪

**Purpose**: Test individual components, business logic, and data operations in isolation.

**Location**: `src/test/java/com/porakhela/`

**Key Test Files**:
- `LoginViewModelTest.kt` - Phone validation, OTP sending logic
- `OTPViewModelTest.kt` - OTP verification, timer, auto-progression
- `OfflineCachingTest.kt` - Room database, download management
- `ApiResponseTest.kt` - API mocking, network responses

**Coverage**:
- ✅ Phone number validation (Bangladesh format: 01XXXXXXXXX)
- ✅ OTP generation and verification flow
- ✅ Offline caching with Room database
- ✅ ViewModel state management with coroutines
- ✅ Network error handling and retry logic

### 2. UI Tests (Espresso) 🖱️

**Purpose**: Test user interface interactions and navigation flows.

**Location**: `src/androidTest/java/com/porakhela/ui/`

**Key Test Files**:
- `LoginOTPFlowTest.kt` - Complete authentication flow
- `CreateChildProfileTest.kt` - Child profile setup validation
- `DashboardLessonTest.kt` - Dashboard interactions, lesson browsing

**Coverage**:
- ✅ Phone number input with automatic formatting
- ✅ OTP screen with 6-digit input and auto-progression
- ✅ Child profile creation with name, grade, avatar selection
- ✅ Subject tabs (Math, Science, Bangla) navigation
- ✅ Lesson list scrolling and selection
- ✅ Search and filtering functionality
- ✅ Offline mode indicators

### 3. Device Compatibility Tests 📱

**Purpose**: Ensure app works across different Android devices and configurations.

**Location**: `src/androidTest/java/com/porakhela/device/`

**Key Test Files**:
- `DeviceCompatibilityTest.kt` - Multi-device testing

**Coverage**:
- ✅ Low-end device simulation (Android Go)
- ✅ Orientation changes preserving state
- ✅ Font scaling for accessibility
- ✅ Performance on mid-range devices
- ✅ Memory usage during heavy operations
- ✅ Network state change handling
- ✅ Background/foreground state preservation

### 4. End-to-End Tests 🎯

**Purpose**: Test complete user journeys from app launch to lesson completion.

**Location**: `src/androidTest/java/com/porakhela/e2e/`

**Key Test Files**:
- `CompleteUserJourneyTest.kt` - Full user flow validation

**Coverage**:
- ✅ App launch and onboarding
- ✅ Phone authentication + OTP verification
- ✅ Child profile creation
- ✅ Dashboard interaction
- ✅ Lesson selection and detail view
- ✅ Lesson content consumption
- ✅ Exercise completion
- ✅ Progress tracking and achievements
- ✅ Parent dashboard verification

### 5. Performance Tests ⚡

**Purpose**: Measure and optimize critical app operations.

**Location**: `src/androidTest/java/com/porakhela/performance/`

**Key Test Files**:
- `PerformanceTest.kt` - Benchmark critical operations

**Coverage**:
- ✅ Database lesson insertion performance
- ✅ Lesson query and filtering speed
- ✅ Download progress update efficiency
- ✅ Large lesson content processing
- ✅ Offline cache operations

### 6. Accessibility Tests ♿

**Purpose**: Ensure app is usable by children with varying abilities.

**Location**: `src/androidTest/java/com/porakhela/accessibility/`

**Key Test Files**:
- `AccessibilityTest.kt` - Child-friendly accessibility validation

**Coverage**:
- ✅ Content descriptions for screen readers
- ✅ Text size scaling support
- ✅ Minimum touch target sizes (44dp+)
- ✅ Color contrast requirements
- ✅ Keyboard navigation support
- ✅ Bangla text display correctly
- ✅ Child-friendly error messages
- ✅ Visual feedback for interactions

### 7. API Mock Tests 🌐

**Purpose**: Test network layer with mocked responses.

**Location**: `src/test/java/com/porakhela/data/api/`

**Key Test Files**:
- `ApiResponseTest.kt` - Complete API response validation

**Coverage**:
- ✅ Send OTP API success/error responses
- ✅ Verify OTP with auth token generation
- ✅ Create child profile API validation
- ✅ Lesson content retrieval
- ✅ Progress sync functionality
- ✅ Network error handling (HTTP 500, etc.)

## Running Tests

### Automated Test Suite

Use the provided scripts to run all tests:

**Windows**:
```bash
run_all_tests.bat
```

**Linux/Mac**:
```bash
./run_all_tests.sh
```

### Individual Test Categories

**Unit Tests**:
```bash
./gradlew test
```

**UI Tests**:
```bash
./gradlew connectedAndroidTest --tests com.porakhela.ui.*
```

**Device Tests**:
```bash
./gradlew connectedAndroidTest --tests com.porakhela.device.*
```

**End-to-End Tests**:
```bash
./gradlew connectedAndroidTest --tests com.porakhela.e2e.*
```

**Performance Tests**:
```bash
./gradlew connectedAndroidTest --tests com.porakhela.performance.*
```

**Accessibility Tests**:
```bash
./gradlew connectedAndroidTest --tests com.porakhela.accessibility.*
```

## Test Dependencies

The following dependencies are configured in `build.gradle.kts`:

### Unit Testing
- JUnit 4.13.2
- Mockk 1.13.8 (Kotlin mocking)
- Google Truth 1.1.5 (Assertions)
- Robolectric 4.11.1 (Android simulation)

### UI Testing
- Espresso 3.5.1 (UI automation)
- AndroidX Test Core 1.5.0
- AndroidX Test Rules 1.5.0

### Integration Testing
- Hilt Testing 2.48
- Room Testing 2.6.1
- Coroutines Test 1.7.3

### Device Testing
- UI Automator 2.2.0
- Benchmark 1.2.2

## Test Configuration

### Custom Test Runner

`PorakhelaTestRunner.kt` extends `AndroidJUnitRunner` to support:
- Hilt dependency injection in instrumented tests
- Custom test application configuration
- Proper test environment setup

### Test Application

`HiltTestApplication` provides:
- Isolated testing environment
- Mock dependency injection
- Database in-memory configuration

## Expected Test Results

When all tests pass, the app validates:

✅ **Authentication Flow**
- Bangladesh phone number validation (01XXXXXXXXX)
- OTP sending and verification
- Proper error handling and user feedback

✅ **Child Profile Creation**
- Name validation (Bangla and English)
- Grade selection (Class 1-12)
- Avatar selection with preview
- Subject preference selection

✅ **Dashboard Functionality**
- Subject tab navigation
- Lesson list with scrolling
- Search and filtering
- Progress tracking display

✅ **Offline Capabilities**
- Lesson download management
- Offline content access
- Cache cleanup and management

✅ **Device Compatibility**
- Low-end device performance
- Orientation change handling
- Font scaling support
- Memory efficiency

✅ **Accessibility Features**
- Screen reader support
- Child-friendly interface
- Proper touch targets
- Bangla language support

✅ **Performance Optimization**
- Fast database operations
- Smooth UI interactions
- Efficient download management
- Optimized content loading

## Troubleshooting

### Common Issues

**1. Device Not Found**
```bash
adb devices
# Ensure emulator or physical device is connected
```

**2. Build Failures**
```bash
./gradlew clean assembleDebug
# Clean build and retry
```

**3. Test Timeouts**
- Increase timeout values in device tests
- Ensure stable network connection for API tests
- Check emulator performance settings

**4. Flaky UI Tests**
- Use IdlingResource for asynchronous operations
- Add explicit waits where needed
- Verify test data setup and cleanup

### Test Reports

After running tests, reports are available at:
- **Unit Test Reports**: `app/build/reports/tests/testDebugUnitTest/`
- **Instrumented Test Reports**: `app/build/reports/androidTests/connected/`
- **Coverage Reports**: `app/build/reports/coverage/`

## Continuous Integration

The test suite is designed to be integrated into CI/CD pipelines:

1. **Pre-commit Hooks**: Run unit tests
2. **Pull Request Validation**: Run UI tests
3. **Release Testing**: Run complete test suite
4. **Performance Monitoring**: Track benchmark results

## Quality Gates

For release readiness, ensure:
- ✅ 100% unit test pass rate
- ✅ 95%+ UI test pass rate  
- ✅ All accessibility tests pass
- ✅ Performance benchmarks within targets
- ✅ Device compatibility verified
- ✅ End-to-end user journey successful

## Child-Friendly Validation

The test suite specifically validates:

🧒 **Age-Appropriate Interface**
- Large, easy-to-tap buttons
- Clear visual feedback
- Simple navigation patterns

🇧🇩 **Bangladeshi Context**
- Proper Bangla text rendering
- Local phone number formats
- Cultural sensitivity in content

📚 **Educational Focus**
- Subject-based organization
- Progress tracking accuracy
- Achievement system validation

🔒 **Safety & Privacy**
- Secure authentication flow
- Parent control features
- Data protection compliance

This comprehensive testing strategy ensures that the Porakhela app provides a smooth, engaging, and safe educational experience for Bangladeshi children.