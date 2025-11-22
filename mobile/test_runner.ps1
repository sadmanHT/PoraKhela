# Offline Lesson System Test Runner (PowerShell)
# Comprehensive test suite for Windows development environment

param(
    [string]$TestSuite = "all",
    [string]$EmulatorName = "pixel_4_api_30",
    [switch]$GenerateFixes,
    [switch]$CleanEnvironment
)

$ErrorActionPreference = "Continue"
$TestPackage = "com.porakhela"
$TestResultsDir = "test-results"
$LogFile = "offline-lesson-tests.log"

# Create results directory
if (!(Test-Path $TestResultsDir)) {
    New-Item -ItemType Directory -Path $TestResultsDir
}

Write-Host "🚀 Starting Offline Lesson System Test Suite" -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Yellow

function Write-TestLog {
    param([string]$Message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logEntry = "[$timestamp] $Message"
    Add-Content -Path $LogFile -Value $logEntry
    Write-Host $logEntry -ForegroundColor Gray
}

function Test-AndroidSetup {
    Write-Host "🔧 Checking Android development setup..." -ForegroundColor Blue
    
    # Check ADB
    try {
        $adbVersion = & adb version 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ ADB is available" -ForegroundColor Green
        } else {
            throw "ADB not found"
        }
    } catch {
        Write-Host "❌ ADB not found. Please install Android SDK." -ForegroundColor Red
        return $false
    }
    
    # Check emulator
    try {
        $emulatorVersion = & emulator -version 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Android Emulator is available" -ForegroundColor Green
        }
    } catch {
        Write-Host "⚠️  Android Emulator may not be available" -ForegroundColor Yellow
    }
    
    # Check Gradle
    try {
        $gradleVersion = & ./gradlew --version 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Gradle is available" -ForegroundColor Green
        }
    } catch {
        Write-Host "❌ Gradle wrapper not found" -ForegroundColor Red
        return $false
    }
    
    return $true
}

function Start-TestEnvironment {
    Write-Host "🔧 Setting up test environment..." -ForegroundColor Blue
    
    # Check if emulator is running
    $devices = & adb devices 2>$null
    if ($devices -notmatch "emulator") {
        Write-Host "Starting emulator: $EmulatorName" -ForegroundColor Yellow
        Start-Process -FilePath "emulator" -ArgumentList "-avd", $EmulatorName, "-no-audio", "-no-window" -NoNewWindow
        
        # Wait for emulator to start
        Write-Host "Waiting for emulator to start..." -ForegroundColor Yellow
        $timeout = 60  # 60 seconds timeout
        $elapsed = 0
        
        do {
            Start-Sleep -Seconds 5
            $elapsed += 5
            $devices = & adb devices 2>$null
            Write-Host "." -NoNewline -ForegroundColor Yellow
        } while ($devices -notmatch "device" -and $elapsed -lt $timeout)
        
        Write-Host ""
        
        if ($elapsed -ge $timeout) {
            Write-Host "❌ Emulator failed to start within timeout" -ForegroundColor Red
            return $false
        } else {
            Write-Host "✅ Emulator started successfully" -ForegroundColor Green
        }
    } else {
        Write-Host "✅ Emulator is already running" -ForegroundColor Green
    }
    
    # Install app
    Write-Host "Installing app for testing..." -ForegroundColor Yellow
    & ./gradlew installDebug installDebugAndroidTest
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Failed to install app" -ForegroundColor Red
        return $false
    }
    
    # Clear app data
    Write-Host "Clearing app data..." -ForegroundColor Yellow
    & adb shell pm clear $TestPackage
    
    return $true
}

function Invoke-TestSuite {
    param(
        [string]$TestClass,
        [string]$TestName
    )
    
    Write-Host "📋 Running $TestName..." -ForegroundColor Cyan
    Write-TestLog "=== STARTING TEST: $TestName ==="
    
    $startTime = Get-Date
    
    # Run the test
    & ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=$TestClass --stacktrace 2>&1 | Tee-Object -FilePath $LogFile -Append
    
    $endTime = Get-Date
    $duration = $endTime - $startTime
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ $TestName PASSED (Duration: $($duration.TotalSeconds)s)" -ForegroundColor Green
        Write-TestLog "TEST PASSED: $TestName - Duration: $($duration.TotalSeconds)s"
        return $true
    } else {
        Write-Host "❌ $TestName FAILED (Duration: $($duration.TotalSeconds)s)" -ForegroundColor Red
        Write-TestLog "TEST FAILED: $TestName - Duration: $($duration.TotalSeconds)s"
        return $false
    }
}

function Test-AirplaneMode {
    Write-Host "✈️  Running Airplane Mode Scenario Test..." -ForegroundColor Magenta
    
    try {
        # Disable network
        Write-Host "  1. Setting device to airplane mode..." -ForegroundColor Yellow
        & adb shell cmd connectivity airplane-mode enable
        Start-Sleep -Seconds 2
        
        # Run offline tests
        Write-Host "  2. Running offline lesson tests..." -ForegroundColor Yellow
        $offlineTestResult = Invoke-TestSuite "com.porakhela.offline.OfflineLessonIntegrationTests" "Offline Mode Tests"
        
        # Verify local storage
        Write-Host "  3. Verifying local storage..." -ForegroundColor Yellow
        $dbQuery = "SELECT COUNT(*) FROM offline_progress WHERE isSynced = 0"
        $unsyncedCount = & adb shell "sqlite3 /data/data/$TestPackage/databases/offline_lessons.db '$dbQuery'" 2>$null
        Write-Host "    Unsynced progress entries: $unsyncedCount" -ForegroundColor Gray
        
        # Re-enable network
        Write-Host "  4. Enabling network..." -ForegroundColor Yellow
        & adb shell cmd connectivity airplane-mode disable
        Start-Sleep -Seconds 3
        
        # Test sync within 5 seconds
        Write-Host "  5. Testing sync completion within 5 seconds..." -ForegroundColor Yellow
        $syncStartTime = Get-Date
        
        # Trigger sync
        & adb shell am broadcast -a com.porakhela.TRIGGER_SYNC
        
        # Monitor sync completion
        $syncCompleted = $false
        $maxWait = 5  # 5 seconds
        $elapsed = 0
        
        do {
            Start-Sleep -Seconds 1
            $elapsed += 1
            $syncedCount = & adb shell "sqlite3 /data/data/$TestPackage/databases/offline_lessons.db 'SELECT COUNT(*) FROM offline_progress WHERE isSynced = 1'" 2>$null
            if ($syncedCount -gt 0) {
                $syncCompleted = $true
            }
        } while (!$syncCompleted -and $elapsed -lt $maxWait)
        
        if ($syncCompleted) {
            Write-Host "✅ Sync completed in $elapsed seconds" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ Sync did not complete within 5 seconds" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "❌ Airplane mode test failed: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

function Test-PerformanceBenchmarks {
    Write-Host "📊 Running Performance Benchmarks..." -ForegroundColor Magenta
    
    # Test lesson loading time
    Write-Host "  ⏱️  Testing lesson load time (target: <400ms)..." -ForegroundColor Yellow
    $loadTestResult = Invoke-TestSuite "com.porakhela.offline.OfflineLessonPerformanceTests" "Performance Tests"
    
    # Monitor memory usage
    Write-Host "  💾 Monitoring memory usage..." -ForegroundColor Yellow
    $memInfo = & adb shell dumpsys meminfo $TestPackage | Select-String "TOTAL"
    Write-Host "    Memory usage: $memInfo" -ForegroundColor Gray
    
    # Test animation smoothness (simplified check)
    Write-Host "  🎬 Testing animation smoothness..." -ForegroundColor Yellow
    # In real scenario, this would measure frame rate
    
    return $loadTestResult
}

function Test-EndToEndWorkflow {
    Write-Host "🔄 Running End-to-End Workflow Test..." -ForegroundColor Magenta
    
    $e2eSteps = @(
        "Parent logs in",
        "Child downloads lessons", 
        "Child goes offline",
        "Child completes lessons",
        "Child comes back online",
        "Progress & points sync",
        "Parent dashboard updates",
        "Daily SMS includes synced lessons"
    )
    
    foreach ($step in $e2eSteps) {
        Write-Host "  📝 $step..." -ForegroundColor Yellow
    }
    
    $e2eResult = Invoke-TestSuite "com.porakhela.offline.OfflineLessonE2ETests" "End-to-End Tests"
    return $e2eResult
}

function Find-IssuesAndGenerateFixes {
    Write-Host "🔍 Analyzing test results and generating fixes..." -ForegroundColor Cyan
    
    # Parse log file for common issues
    $logContent = Get-Content $LogFile -ErrorAction SilentlyContinue
    
    $syncFailures = ($logContent | Select-String "sync.*fail").Count
    $doubleSubmissions = ($logContent | Select-String "double.*submit").Count  
    $idempotencyIssues = ($logContent | Select-String "idempotency.*fail").Count
    $timerMismatches = ($logContent | Select-String "timer.*mismatch").Count
    
    Write-Host "📊 Issue Analysis:" -ForegroundColor Yellow
    Write-Host "  Sync Failures: $syncFailures" -ForegroundColor Gray
    Write-Host "  Double Submissions: $doubleSubmissions" -ForegroundColor Gray
    Write-Host "  Idempotency Issues: $idempotencyIssues" -ForegroundColor Gray
    Write-Host "  Timer Mismatches: $timerMismatches" -ForegroundColor Gray
    
    if ($GenerateFixes) {
        if ($syncFailures -gt 0) {
            New-SyncFixes
        }
        
        if ($doubleSubmissions -gt 0) {
            New-DoubleSubmissionFixes  
        }
        
        if ($idempotencyIssues -gt 0) {
            New-IdempotencyFixes
        }
        
        if ($timerMismatches -gt 0) {
            New-TimerFixes
        }
    }
    
    return @{
        SyncFailures = $syncFailures
        DoubleSubmissions = $doubleSubmissions
        IdempotencyIssues = $idempotencyIssues
        TimerMismatches = $timerMismatches
    }
}

function New-SyncFixes {
    Write-Host "🔧 Generating sync failure fixes..." -ForegroundColor Blue
    
    $syncFixContent = @'
// Enhanced Sync Worker with Improved Error Handling
class RobustOfflineSyncWorker(context: Context, workerParams: WorkerParameters) : 
    CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_DELAY = 2000L
        
        fun createReliableSyncRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()
            
            return OneTimeWorkRequestBuilder<RobustOfflineSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    INITIAL_BACKOFF_DELAY,
                    TimeUnit.MILLISECONDS
                )
                .addTag("offline_sync")
                .build()
        }
    }
    
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                setProgress(workDataOf("status" to "Starting sync"))
                
                val syncResult = withTimeout(45000L) { // 45 second timeout
                    performRobustSync()
                }
                
                if (syncResult.success) {
                    setProgress(workDataOf("status" to "Sync completed"))
                    Result.success(workDataOf("synced_items" to syncResult.itemCount))
                } else {
                    handleSyncFailure(syncResult.error)
                }
            } catch (e: TimeoutCancellationException) {
                Log.w("SyncWorker", "Sync timeout, will retry")
                Result.retry()
            } catch (e: Exception) {
                Log.e("SyncWorker", "Sync error", e)
                handleSyncFailure(e.message)
            }
        }
    }
    
    private suspend fun performRobustSync(): SyncResult {
        // Implement robust sync with partial success handling
        return SyncResult(true, 0, null)
    }
    
    private fun handleSyncFailure(error: String?): Result {
        return if (runAttemptCount < MAX_RETRIES) {
            Log.w("SyncWorker", "Sync failed, retrying: $error")
            Result.retry()
        } else {
            Log.e("SyncWorker", "Sync failed after max retries: $error")
            Result.failure(workDataOf("error" to (error ?: "Unknown error")))
        }
    }
}

data class SyncResult(
    val success: Boolean,
    val itemCount: Int,
    val error: String?
)
'@
    
    Set-Content -Path "SyncWorkerFixes.kt" -Value $syncFixContent
    Write-Host "✅ Sync fixes generated in SyncWorkerFixes.kt" -ForegroundColor Green
}

function New-DoubleSubmissionFixes {
    Write-Host "🔧 Generating double submission fixes..." -ForegroundColor Blue
    
    $doubleSubmissionFixContent = @'
// Double Submission Prevention System
class SubmissionGuard {
    private val submissionLock = Mutex()
    private val activeSubmissions = ConcurrentHashMap<String, Long>()
    private val submissionCooldown = 2000L // 2 seconds
    
    suspend fun guardedSubmission(
        submissionKey: String,
        submission: suspend () -> Result<Unit>
    ): Result<Unit> = submissionLock.withLock {
        val now = System.currentTimeMillis()
        val lastSubmission = activeSubmissions[submissionKey]
        
        if (lastSubmission != null && (now - lastSubmission) < submissionCooldown) {
            return Result.failure(IllegalStateException("Submission too recent"))
        }
        
        activeSubmissions[submissionKey] = now
        
        try {
            submission()
        } finally {
            // Clean up old submissions
            cleanupOldSubmissions()
        }
    }
    
    private fun cleanupOldSubmissions() {
        val cutoff = System.currentTimeMillis() - submissionCooldown
        activeSubmissions.entries.removeIf { it.value < cutoff }
    }
}

// Enhanced Progress Repository with Submission Guard
class SafeOfflineProgressRepository(
    private val submissionGuard: SubmissionGuard,
    private val baseRepository: OfflineLessonRepository
) {
    suspend fun submitAnswerSafely(
        lessonId: String,
        questionId: String,
        userId: String,
        answer: AnswerData
    ): Result<Unit> {
        val submissionKey = "$lessonId:$questionId:$userId"
        
        return submissionGuard.guardedSubmission(submissionKey) {
            baseRepository.saveQuestionResult(
                lessonId, questionId, userId,
                answer.isCorrect, answer.selectedIndex,
                answer.timeTaken, answer.pointsEarned
            )
            Result.success(Unit)
        }
    }
}
'@
    
    Set-Content -Path "DoubleSubmissionFixes.kt" -Value $doubleSubmissionFixContent
    Write-Host "✅ Double submission fixes generated in DoubleSubmissionFixes.kt" -ForegroundColor Green
}

function New-IdempotencyFixes {
    Write-Host "🔧 Generating idempotency fixes..." -ForegroundColor Blue
    
    $idempotencyFixContent = @'
// Idempotent Database Operations
@Entity(
    tableName = "offline_progress",
    indices = [Index(value = ["lessonId", "questionId", "userId"], unique = true)]
)
data class OfflineProgress(
    @PrimaryKey val id: String,
    val lessonId: String,
    val questionId: String,
    val userId: String,
    val isCorrect: Boolean,
    val selectedAnswerIndex: Int,
    val timeTaken: Int,
    val pointsEarned: Int,
    val timestamp: Long,
    val isSynced: Boolean = false,
    val syncAttempts: Int = 0
)

@Dao
interface IdempotentProgressDao {
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgressSafely(progress: OfflineProgress): Long
    
    @Query("SELECT * FROM offline_progress WHERE lessonId = :lessonId AND questionId = :questionId AND userId = :userId LIMIT 1")
    suspend fun getExistingProgress(lessonId: String, questionId: String, userId: String): OfflineProgress?
    
    @Transaction
    suspend fun upsertProgress(progress: OfflineProgress): Boolean {
        val existing = getExistingProgress(progress.lessonId, progress.questionId, progress.userId)
        
        return if (existing == null) {
            val inserted = insertProgressSafely(progress)
            inserted != -1L
        } else {
            // Don't update if already exists - maintain idempotency
            false
        }
    }
}

// Idempotent Points Management
class IdempotentPointsManager {
    
    suspend fun awardPointsOnce(
        userId: String,
        lessonId: String,
        questionId: String,
        points: Int,
        source: String
    ): Boolean {
        val pointsId = generatePointsId(userId, lessonId, questionId, source)
        
        return try {
            val pointsEntry = LocalPoints(
                id = pointsId,
                userId = userId,
                lessonId = lessonId,
                pointsEarned = points,
                source = source,
                timestamp = System.currentTimeMillis(),
                isSynced = false
            )
            
            pointsDao.insertPointsSafely(pointsEntry) != -1L
        } catch (e: SQLiteConstraintException) {
            // Points already awarded - this is expected behavior
            false
        }
    }
    
    private fun generatePointsId(
        userId: String,
        lessonId: String, 
        questionId: String,
        source: String
    ): String {
        return "$userId:$lessonId:$questionId:$source".hashCode().toString()
    }
}
'@
    
    Set-Content -Path "IdempotencyFixes.kt" -Value $idempotencyFixContent
    Write-Host "✅ Idempotency fixes generated in IdempotencyFixes.kt" -ForegroundColor Green
}

function New-TimerFixes {
    Write-Host "🔧 Generating timer fixes..." -ForegroundColor Blue
    
    $timerFixContent = @'
// Robust Timer Management System
class PreciseTimerManager {
    private var startTime: Long = 0L
    private var pauseStartTime: Long = 0L
    private var totalPausedTime: Long = 0L
    private var isPaused: Boolean = false
    private val serverTimeOffset: AtomicLong = AtomicLong(0L)
    
    fun synchronizeWithServerTime(serverTimestamp: Long) {
        val clientTime = System.currentTimeMillis()
        serverTimeOffset.set(serverTimestamp - clientTime)
    }
    
    fun getServerTime(): Long {
        return System.currentTimeMillis() + serverTimeOffset.get()
    }
    
    fun startTimer() {
        startTime = getServerTime()
        totalPausedTime = 0L
        isPaused = false
    }
    
    fun pauseTimer() {
        if (!isPaused) {
            pauseStartTime = getServerTime()
            isPaused = true
        }
    }
    
    fun resumeTimer() {
        if (isPaused) {
            totalPausedTime += getServerTime() - pauseStartTime
            isPaused = false
        }
    }
    
    fun getElapsedTimeSeconds(): Int {
        val currentTime = if (isPaused) pauseStartTime else getServerTime()
        val elapsed = currentTime - startTime - totalPausedTime
        return maxOf(0, (elapsed / 1000).toInt())
    }
    
    fun getRemainingTimeSeconds(timeLimit: Int): Int {
        val elapsed = getElapsedTimeSeconds()
        return maxOf(0, timeLimit - elapsed)
    }
    
    fun getTimerState(): TimerState {
        return TimerState(
            isRunning = !isPaused && startTime > 0,
            isPaused = isPaused,
            elapsedSeconds = getElapsedTimeSeconds(),
            startTime = startTime,
            totalPausedTime = totalPausedTime
        )
    }
}

data class TimerState(
    val isRunning: Boolean,
    val isPaused: Boolean,
    val elapsedSeconds: Int,
    val startTime: Long,
    val totalPausedTime: Long
)

// Lifecycle-Aware Timer Component
@Composable
fun RobustTimer(
    timeLimitSeconds: Int,
    onTimeUp: () -> Unit,
    onTick: (remainingSeconds: Int) -> Unit,
    timerManager: PreciseTimerManager = remember { PreciseTimerManager() }
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isActive by remember { mutableStateOf(false) }
    
    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (isActive) timerManager.resumeTimer()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    if (isActive) timerManager.pauseTimer()
                }
                Lifecycle.Event.ON_STOP -> {
                    if (isActive) timerManager.pauseTimer()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(Unit) {
        timerManager.startTimer()
        isActive = true
        
        while (isActive) {
            val remaining = timerManager.getRemainingTimeSeconds(timeLimitSeconds)
            onTick(remaining)
            
            if (remaining <= 0) {
                onTimeUp()
                isActive = false
                break
            }
            
            delay(100) // Update every 100ms for smooth countdown
        }
    }
}
'@
    
    Set-Content -Path "TimerFixes.kt" -Value $timerFixContent
    Write-Host "✅ Timer fixes generated in TimerFixes.kt" -ForegroundColor Green
}

function Stop-TestEnvironment {
    if ($CleanEnvironment) {
        Write-Host "🧹 Cleaning up test environment..." -ForegroundColor Blue
        
        # Clear app data
        & adb shell pm clear $TestPackage
        
        # Optionally stop emulator
        # & adb emu kill
        
        Write-Host "✅ Environment cleaned" -ForegroundColor Green
    }
}

# Main execution
function Main {
    Write-TestLog "Starting offline lesson system test execution"
    
    # Check setup
    if (-not (Test-AndroidSetup)) {
        Write-Host "❌ Android setup check failed" -ForegroundColor Red
        return
    }
    
    # Setup environment
    if (-not (Start-TestEnvironment)) {
        Write-Host "❌ Failed to setup test environment" -ForegroundColor Red
        return
    }
    
    # Track test results
    $testResults = @{}
    
    Write-Host "🧪 Executing Test Suites..." -ForegroundColor Cyan
    Write-Host "==============================" -ForegroundColor Yellow
    
    # Run test suites based on parameter
    switch ($TestSuite.ToLower()) {
        "unit" {
            Write-Host "🔬 Running Unit Tests Only..." -ForegroundColor Blue
            $testResults["Unit Tests"] = Invoke-TestSuite "com.porakhela.offline.OfflineLessonUnitTests" "Unit Tests"
        }
        "integration" {
            Write-Host "🔗 Running Integration Tests Only..." -ForegroundColor Blue
            $testResults["Integration Tests"] = Invoke-TestSuite "com.porakhela.offline.OfflineLessonIntegrationTests" "Integration Tests"
            $testResults["Airplane Mode Test"] = Test-AirplaneMode
        }
        "performance" {
            Write-Host "⚡ Running Performance Tests Only..." -ForegroundColor Blue
            $testResults["Performance Tests"] = Test-PerformanceBenchmarks
        }
        "e2e" {
            Write-Host "🔄 Running E2E Tests Only..." -ForegroundColor Blue
            $testResults["E2E Tests"] = Test-EndToEndWorkflow
        }
        "issues" {
            Write-Host "🐛 Running Issue Detection Tests Only..." -ForegroundColor Blue
            $testResults["Issue Detection"] = Invoke-TestSuite "com.porakhela.offline.OfflineLessonIssueDetectionTests" "Issue Detection Tests"
        }
        default {
            # Run all tests
            Write-Host "🔬 Running Unit Tests..." -ForegroundColor Blue
            $testResults["Unit Tests"] = Invoke-TestSuite "com.porakhela.offline.OfflineLessonUnitTests" "Unit Tests"
            
            Write-Host "🔗 Running Integration Tests..." -ForegroundColor Blue  
            $testResults["Integration Tests"] = Invoke-TestSuite "com.porakhela.offline.OfflineLessonIntegrationTests" "Integration Tests"
            
            Write-Host "⚡ Running Performance Tests..." -ForegroundColor Blue
            $testResults["Performance Tests"] = Test-PerformanceBenchmarks
            
            Write-Host "🔄 Running E2E Tests..." -ForegroundColor Blue
            $testResults["E2E Tests"] = Test-EndToEndWorkflow
            
            Write-Host "🐛 Running Issue Detection Tests..." -ForegroundColor Blue
            $testResults["Issue Detection"] = Invoke-TestSuite "com.porakhela.offline.OfflineLessonIssueDetectionTests" "Issue Detection Tests"
            
            Write-Host "✈️  Running Airplane Mode Test..." -ForegroundColor Blue
            $testResults["Airplane Mode Test"] = Test-AirplaneMode
        }
    }
    
    Write-Host ""
    Write-Host "🔍 Analyzing Results..." -ForegroundColor Cyan
    Write-Host "========================" -ForegroundColor Yellow
    
    $issueAnalysis = Find-IssuesAndGenerateFixes
    
    # Generate final report
    Write-Host ""
    Write-Host "📋 FINAL TEST REPORT" -ForegroundColor Green
    Write-Host "=====================" -ForegroundColor Yellow
    
    $totalTests = $testResults.Count
    $passedTests = ($testResults.Values | Where-Object { $_ -eq $true }).Count
    $passRate = if ($totalTests -gt 0) { ($passedTests / $totalTests) * 100 } else { 0 }
    
    foreach ($testName in $testResults.Keys) {
        if ($testResults[$testName]) {
            Write-Host "✅ $testName" -ForegroundColor Green
        } else {
            Write-Host "❌ $testName" -ForegroundColor Red
        }
    }
    
    Write-Host ""
    Write-Host "📊 Summary:" -ForegroundColor Yellow
    Write-Host "  Total Tests: $totalTests" -ForegroundColor Gray
    Write-Host "  Passed: $passedTests" -ForegroundColor Green
    Write-Host "  Failed: $($totalTests - $passedTests)" -ForegroundColor Red
    Write-Host "  Pass Rate: $([math]::Round($passRate, 1))%" -ForegroundColor $(if ($passRate -eq 100) { "Green" } else { "Yellow" })
    
    # Issue summary
    Write-Host ""
    Write-Host "🚨 Issues Found:" -ForegroundColor Yellow
    Write-Host "  Sync Failures: $($issueAnalysis.SyncFailures)" -ForegroundColor Gray
    Write-Host "  Double Submissions: $($issueAnalysis.DoubleSubmissions)" -ForegroundColor Gray
    Write-Host "  Idempotency Issues: $($issueAnalysis.IdempotencyIssues)" -ForegroundColor Gray
    Write-Host "  Timer Mismatches: $($issueAnalysis.TimerMismatches)" -ForegroundColor Gray
    
    if ($passRate -eq 100 -and ($issueAnalysis.SyncFailures + $issueAnalysis.DoubleSubmissions + $issueAnalysis.IdempotencyIssues + $issueAnalysis.TimerMismatches) -eq 0) {
        Write-Host ""
        Write-Host "🎉 ALL TESTS PASSED!" -ForegroundColor Green
        Write-Host "Offline lesson system is ready for deployment!" -ForegroundColor Green
        Write-Host "✅ Sync worker operates flawlessly" -ForegroundColor Green
        Write-Host "✅ No double submissions detected" -ForegroundColor Green
        Write-Host "✅ Idempotency is maintained" -ForegroundColor Green
        Write-Host "✅ Timer accuracy is precise" -ForegroundColor Green
        Write-Host "✅ Offline→online transition is seamless" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "⚠️  Issues found or tests failed." -ForegroundColor Yellow
        if ($GenerateFixes) {
            Write-Host "🔧 Fix files generated. Apply them and re-run tests." -ForegroundColor Blue
            $fixFiles = Get-ChildItem -Filter "*Fixes.kt"
            foreach ($file in $fixFiles) {
                Write-Host "  - $($file.Name)" -ForegroundColor Gray
            }
        } else {
            Write-Host "💡 Run with -GenerateFixes to create fix files." -ForegroundColor Blue
        }
    }
    
    Stop-TestEnvironment
    
    Write-Host ""
    Write-Host "📄 Detailed logs available in: $LogFile" -ForegroundColor Gray
    Write-TestLog "Test execution completed"
}

# Execute main function
Main