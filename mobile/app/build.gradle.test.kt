// Test configuration for offline lesson system
android {
    // Test configurations
    testOptions {
        unitTests {
            includeAndroidResources = true
            returnDefaultValues = true
        }
        
        animationsDisabled = true
        
        // Test execution options
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        
        // Test results configuration
        unitTests.all {
            testLogging {
                events "passed", "skipped", "failed"
                exceptionFormat "full"
                showCauses true
                showExceptions true
                showStackTraces true
            }
            
            // Performance test timeout
            timeout = Duration.ofMinutes(10)
        }
    }
    
    // Test build types
    buildTypes {
        debug {
            testCoverageEnabled true
            debuggable true
        }
        
        // Special build type for offline testing
        offline {
            debuggable true
            testCoverageEnabled true
            buildConfigField "boolean", "OFFLINE_MODE_TESTING", "true"
            buildConfigField "String", "MOCK_API_BASE_URL", "\"http://localhost:8080\""
        }
    }
    
    // Test source sets
    sourceSets {
        test {
            java.srcDirs += ['src/test/java']
        }
        androidTest {
            java.srcDirs += ['src/androidTest/java']
            assets.srcDirs += ['src/androidTest/assets']
        }
    }
    
    // Test flavors for different scenarios
    flavorDimensions += "testing"
    productFlavors {
        dev {
            dimension "testing"
            buildConfigField "boolean", "ENABLE_TEST_MODE", "true"
        }
        
        performance {
            dimension "testing"
            buildConfigField "boolean", "PERFORMANCE_TESTING", "true"
            buildConfigField "long", "LESSON_LOAD_TIMEOUT_MS", "400L"
        }
    }
}

dependencies {
    // Unit testing dependencies
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:4.8.0'
    testImplementation 'org.mockito:mockito-android:4.8.0'
    testImplementation 'org.mockito.kotlin:mockito-kotlin:4.1.0'
    testImplementation 'io.mockk:mockk:1.13.3'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4'
    testImplementation 'androidx.test:core:1.5.0'
    testImplementation 'androidx.test.ext:junit:1.1.5'
    testImplementation 'androidx.room:room-testing:2.4.3'
    testImplementation 'androidx.work:work-testing:2.8.1'
    
    // Instrumentation testing dependencies
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test:runner:1.5.2'
    androidTestImplementation 'androidx.test:rules:1.5.0'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.test.espresso:espresso-intents:3.5.1'
    androidTestImplementation 'androidx.compose.ui:ui-test-junit4:1.5.4'
    androidTestImplementation 'androidx.compose.ui:ui-test-manifest:1.5.4'
    androidTestImplementation 'androidx.test.uiautomator:uiautomator:2.2.0'
    androidTestImplementation 'androidx.benchmark:benchmark-junit4:1.1.1'
    androidTestImplementation 'androidx.work:work-testing:2.8.1'
    androidTestImplementation 'com.squareup.okhttp3:mockwebserver:4.12.0'
    androidTestImplementation 'androidx.test.espresso:espresso-idling-resource:3.5.1'
    androidTestImplementation 'io.mockk:mockk-android:1.13.3'
    
    // Hilt testing
    testImplementation 'com.google.dagger:hilt-android-testing:2.44'
    kaptTest 'com.google.dagger:hilt-android-compiler:2.44'
    androidTestImplementation 'com.google.dagger:hilt-android-testing:2.44'
    kaptAndroidTest 'com.google.dagger:hilt-android-compiler:2.44'
    
    // Test orchestrator
    androidTestUtil 'androidx.test:orchestrator:1.4.2'
    
    // Performance testing
    androidTestImplementation 'androidx.benchmark:benchmark-junit4:1.1.1'
    androidTestImplementation 'androidx.test.espresso:espresso-idling-resource:3.5.1'
}

// Gradle tasks for comprehensive testing
task runOfflineTests(type: GradleBuild) {
    description = 'Run all offline lesson system tests'
    tasks = [
        'testDebugUnitTest',
        'connectedDebugAndroidTest'
    ]
}

task runPerformanceTests(type: Test) {
    description = 'Run performance-specific tests'
    include '**/OfflineLessonPerformanceTests.class'
    
    testLogging {
        events "passed", "skipped", "failed"
        showStandardStreams = true
    }
    
    // Performance test specific configuration
    systemProperty 'performance.testing', 'true'
    systemProperty 'lesson.load.timeout', '400'
    systemProperty 'animation.fps.threshold', '30'
}

task runIntegrationTests(type: Test) {
    description = 'Run integration tests with airplane mode simulation'
    include '**/OfflineLessonIntegrationTests.class'
    
    // Setup for airplane mode testing
    systemProperty 'network.simulation', 'true'
    systemProperty 'sync.timeout', '5000'
}

task runE2ETests(type: Test) {
    description = 'Run end-to-end workflow tests'
    include '**/OfflineLessonE2ETests.class'
    
    // E2E test configuration
    systemProperty 'e2e.testing', 'true'
    systemProperty 'mock.api.enabled', 'true'
}

task runIssueDetectionTests(type: Test) {
    description = 'Run tests to detect sync worker and idempotency issues'
    include '**/OfflineLessonIssueDetectionTests.class'
    
    // Issue detection configuration
    systemProperty 'issue.detection', 'true'
    systemProperty 'fix.generation', 'true'
}

task generateTestReport(type: TestReport) {
    description = 'Generate comprehensive test report'
    destinationDir = file("$buildDir/reports/all-tests")
    testResults from tasks.withType(Test)
}

// Custom task for airplane mode testing
task testAirplaneMode(type: Exec) {
    description = 'Test offline functionality with airplane mode simulation'
    
    doFirst {
        println "Setting up airplane mode test environment..."
    }
    
    commandLine 'adb', 'shell', 'cmd', 'connectivity', 'airplane-mode', 'enable'
    
    doLast {
        // Run offline tests
        exec {
            commandLine './gradlew', 'connectedDebugAndroidTest', 
                        '-Pandroid.testInstrumentationRunnerArguments.class=com.porakhela.offline.OfflineLessonIntegrationTests'
        }
        
        // Re-enable network
        exec {
            commandLine 'adb', 'shell', 'cmd', 'connectivity', 'airplane-mode', 'disable'
        }
        
        // Test sync recovery
        exec {
            commandLine './gradlew', 'connectedDebugAndroidTest',
                        '-Pandroid.testInstrumentationRunnerArguments.annotation=androidx.test.filters.LargeTest'
        }
    }
}

// Task to setup low-end emulator
task setupLowEndEmulator(type: Exec) {
    description = 'Setup emulator with low-end device specifications'
    
    commandLine 'avdmanager', 'create', 'avd', 
                '--name', 'offline_test_device',
                '--package', 'system-images;android-30;google_apis;x86',
                '--device', 'Nexus_5'
                
    doLast {
        // Configure emulator with limited resources
        def configFile = new File("${System.getenv('ANDROID_HOME')}/avd/offline_test_device.avd/config.ini")
        configFile.append("\n# Low-end device simulation\n")
        configFile.append("hw.ramSize=1024\n")
        configFile.append("vm.heapSize=256\n")
        configFile.append("hw.cpu.ncore=2\n")
    }
}

// Performance monitoring task
task monitorPerformance(type: Exec) {
    description = 'Monitor app performance during offline tests'
    
    commandLine 'adb', 'shell', 'dumpsys', 'meminfo', 'com.porakhela'
    
    doLast {
        exec {
            commandLine 'adb', 'shell', 'dumpsys', 'cpuinfo'
        }
    }
}

// Task to validate sync worker reliability
task validateSyncWorker(type: Exec) {
    description = 'Validate sync worker reliability and retry mechanisms'
    
    commandLine './gradlew', 'connectedDebugAndroidTest',
                '-Pandroid.testInstrumentationRunnerArguments.class=com.porakhela.offline.OfflineLessonIssueDetectionTests',
                '-Pandroid.testInstrumentationRunnerArguments.method=detect_and_fix_sync_worker_failures'
}

// Task to test idempotency
task validateIdempotency(type: Exec) {
    description = 'Validate idempotency across all operations'
    
    commandLine './gradlew', 'connectedDebugAndroidTest',
                '-Pandroid.testInstrumentationRunnerArguments.class=com.porakhela.offline.OfflineLessonIssueDetectionTests',
                '-Pandroid.testInstrumentationRunnerArguments.method=detect_and_fix_idempotency_issues'
}

// Master test task that runs everything
task testOfflineLessonSystem {
    description = 'Run complete offline lesson system test suite'
    
    dependsOn 'clean'
    dependsOn 'setupLowEndEmulator'
    dependsOn 'runOfflineTests'
    dependsOn 'runPerformanceTests'
    dependsOn 'runIntegrationTests'
    dependsOn 'runE2ETests'
    dependsOn 'runIssueDetectionTests'
    dependsOn 'testAirplaneMode'
    dependsOn 'validateSyncWorker'
    dependsOn 'validateIdempotency'
    dependsOn 'generateTestReport'
    
    doLast {
        println "🎉 Offline lesson system testing completed!"
        println "📋 Check build/reports/all-tests for detailed results"
        println "🔧 Check generated fix files for any identified issues"
    }
}

// Configure test execution order
setupLowEndEmulator.mustRunAfter clean
runOfflineTests.mustRunAfter setupLowEndEmulator
runPerformanceTests.mustRunAfter runOfflineTests
runIntegrationTests.mustRunAfter runPerformanceTests
runE2ETests.mustRunAfter runIntegrationTests
runIssueDetectionTests.mustRunAfter runE2ETests
testAirplaneMode.mustRunAfter runIssueDetectionTests
validateSyncWorker.mustRunAfter testAirplaneMode
validateIdempotency.mustRunAfter validateSyncWorker
generateTestReport.mustRunAfter validateIdempotency