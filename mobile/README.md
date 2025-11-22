# Porakhela Mobile - Android App

A child-friendly, gamified NCTB-based educational Android app with offline-first architecture. Part of the Porakhela Learn-to-Earn platform.

## 🎯 Overview

### Features
- **Child-Friendly UI**: Colorful, intuitive design for kids
- **Offline-First**: Download lessons for learning without internet
- **Gamification**: Points, achievements, and real rewards
- **Parental Controls**: PIN protection and progress monitoring
- **NCTB Curriculum**: Aligned with Bangladesh educational standards

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (SQLite)
- **Networking**: Retrofit + OkHttp
- **Dependency Injection**: Hilt
- **Image Loading**: Coil
- **Video Player**: ExoPlayer

## 🏗️ Architecture

### Project Structure
```
app/src/main/java/com/porakhela/
├── ui/                     # UI Layer
│   ├── screens/           # Screen composables
│   ├── components/        # Reusable UI components
│   ├── theme/            # Material Design theme
│   ├── navigation/       # Navigation graph
│   └── viewmodel/        # ViewModels
├── data/                  # Data Layer
│   ├── local/            # Room database (offline storage)
│   ├── remote/           # API service (online data)
│   └── repository/       # Repository implementations
├── domain/               # Domain Layer
│   ├── model/           # Domain models
│   ├── repository/      # Repository interfaces
│   └── usecase/         # Business logic use cases
└── utils/               # Utilities & helpers
```

### MVVM Architecture
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│     UI      │◄──►│  ViewModel  │◄──►│ Repository  │
│ (Composable)│    │   (Logic)   │    │   (Data)    │
└─────────────┘    └─────────────┘    └─────────────┘
                                              │
                                   ┌─────────────┐
                                   │  Data Layer │
                                   │ Room + API  │
                                   └─────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Android Studio Flamingo (2022.2.1) or newer
- Android SDK 21+ (Android 5.0+)
- Kotlin 1.9.10+

### Setup

1. **Clone Repository**
```bash
git clone <repository-url>
cd porakhela/mobile
```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to `mobile/` folder

3. **Backend Configuration**
   - Update `BuildConfig.API_BASE_URL` in `app/build.gradle`
   - For emulator: `http://10.0.2.2:8000/api/v1/`
   - For device: `http://YOUR_IP:8000/api/v1/`

4. **Build & Run**
   - Sync project with Gradle files
   - Run on emulator or device

## 📱 Features

### Authentication Flow
1. **Phone Registration**: OTP-based verification
2. **User Type Selection**: Parent or Child account
3. **Profile Setup**: Grade, subjects, avatar
4. **Parental Controls**: PIN setup for child safety

### Child Dashboard
- **Learning Progress**: Visual progress tracking
- **Subject Cards**: NCTB subjects with progress
- **Achievement Badges**: Unlocked accomplishments
- **Points Balance**: Current Porapoints
- **Daily Streak**: Consecutive learning days

### Lesson Player
- **Video Lessons**: Offline-capable video player
- **Interactive Quizzes**: Child-friendly quiz interface
- **Progress Tracking**: Real-time completion tracking
- **Points Rewards**: Immediate feedback on points earned

### Parental Dashboard
- **Child Progress**: Detailed learning analytics
- **Screen Time Controls**: Daily time limits
- **Subject Management**: Enable/disable subjects
- **Reward Monitoring**: Track point redemptions

### Offline Functionality
- **Lesson Downloads**: Download content for offline use
- **Progress Sync**: Automatic sync when online
- **Local Database**: Room database for offline storage
- **Smart Caching**: Optimized content caching

## 🎮 Gamification Features

### Points System
- **Lesson Completion**: Base points per lesson
- **Quiz Performance**: Bonus points for high scores
- **Streak Bonuses**: Extra points for daily streaks
- **Achievement Unlocks**: Milestone rewards

### Visual Feedback
- **Animated Rewards**: Lottie animations for achievements
- **Progress Bars**: Visual completion indicators
- **Celebration Effects**: Confetti for milestones
- **Character Avatars**: Personalized child avatars

### Rewards Integration
- **Applink Integration**: Real mobile data/talktime rewards
- **Redemption History**: Track redeemed rewards
- **Balance Display**: Current points and pending redemptions

## 🔧 Development

### Key Dependencies

```kotlin
// Core Android
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.activity:activity-compose:1.8.2'

// Compose UI
implementation platform('androidx.compose:compose-bom:2023.10.01')
implementation 'androidx.compose.ui:ui'
implementation 'androidx.compose.material3:material3'

// Navigation
implementation 'androidx.navigation:navigation-compose:2.7.5'

// Database
implementation 'androidx.room:room-runtime:2.6.1'
implementation 'androidx.room:room-ktx:2.6.1'
kapt 'androidx.room:room-compiler:2.6.1'

// Dependency Injection
implementation 'com.google.dagger:hilt-android:2.48.1'
kapt 'com.google.dagger:hilt-compiler:2.48.1'

// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

// Media
implementation 'androidx.media3:media3-exoplayer:1.2.0'
implementation 'io.coil-kt:coil-compose:2.5.0'

// Animations
implementation 'com.airbnb.android:lottie-compose:6.2.0'
```

### Room Database Schema

#### User Data
```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val phoneNumber: String,
    val firstName: String,
    val lastName: String,
    val userType: String,
    // ... other fields
)
```

#### Educational Content
```kotlin
@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    val title: String,
    val videoUrl: String?,
    val isDownloaded: Boolean = false,
    // ... other fields
)
```

#### Progress Tracking
```kotlin
@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val lessonId: String,
    val completionPercentage: Int,
    val porapointsEarned: Int,
    // ... other fields
)
```

### API Integration

#### Repository Pattern
```kotlin
@Singleton
class LessonRepository @Inject constructor(
    private val apiService: PorakhelaApiService,
    private val localDatabase: PorakhelaDatabase
) {
    fun getLessons(): Flow<List<Lesson>> = flow {
        // Emit local data first (offline-first)
        emit(localDatabase.lessonDao().getAllLessons())
        
        try {
            // Fetch from API and update local
            val remoteLessons = apiService.getLessons()
            localDatabase.lessonDao().insertLessons(remoteLessons)
            emit(remoteLessons)
        } catch (e: Exception) {
            // Handle network error, keep local data
        }
    }
}
```

### UI Components

#### Child-Friendly Design
- **Large Touch Targets**: Easy for small fingers
- **Bright Colors**: Engaging color palette
- **Simple Navigation**: Intuitive user flow
- **Audio Feedback**: Sound effects for interactions
- **Accessibility**: Screen reader support

#### Custom Components
```kotlin
@Composable
fun PorapointsBadge(
    points: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Points",
                tint = Color.Yellow
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = points.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}
```

## 📱 Screens

### 1. Splash & Onboarding
- **Splash Screen**: App logo with loading animation
- **Welcome**: Introduction to Porakhela
- **Tutorial**: How to earn points and redeem rewards

### 2. Authentication
- **Phone Input**: Bangladesh phone number format
- **OTP Verification**: 6-digit code input
- **User Type**: Parent or Child selection
- **Profile Setup**: Name, grade, avatar selection

### 3. Child Dashboard
- **Progress Overview**: Daily/weekly learning stats
- **Subject Grid**: NCTB subjects with icons
- **Achievement Showcase**: Recent accomplishments
- **Quick Actions**: Continue lesson, view rewards

### 4. Lesson Player
- **Video Player**: ExoPlayer with controls
- **Quiz Interface**: Multiple choice with images
- **Progress Indicator**: Visual completion tracking
- **Points Animation**: Celebration on completion

### 5. Parental Controls
- **PIN Setup**: 4-6 digit PIN creation
- **Child Management**: Add/edit child profiles
- **Progress Monitoring**: Detailed learning analytics
- **Settings**: Screen time, subject controls

### 6. Rewards Center
- **Catalog**: Available data/talktime rewards
- **Redemption**: Point-based purchasing
- **History**: Past redemptions and status
- **Balance**: Current points and transactions

## 🔒 Security & Privacy

### Child Safety
- **Parental PIN**: Required for sensitive actions
- **Content Filtering**: Age-appropriate content only
- **Screen Time Limits**: Configurable daily limits
- **Offline Mode**: Reduced exposure to online risks

### Data Protection
- **Local Encryption**: Sensitive data encrypted in Room
- **API Security**: JWT token authentication
- **Minimal Data**: Only essential data collected
- **Parental Consent**: Required for child accounts

## 🧪 Testing

### Unit Testing
```bash
# Run unit tests
./gradlew testDebugUnitTest

# With coverage
./gradlew testDebugUnitTestCoverage
```

### UI Testing
```bash
# Run instrumented tests
./gradlew connectedDebugAndroidTest
```

### Test Structure
```kotlin
@RunWith(AndroidJUnit4::class)
class LessonRepositoryTest {
    
    @Test
    fun getLessons_returnsLocalDataFirst() {
        // Test offline-first behavior
    }
    
    @Test
    fun completeLesson_updatesProgress() {
        // Test progress tracking
    }
}
```

## 📦 Build & Release

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Code Obfuscation
- ProGuard rules for release builds
- Keep API model classes
- Preserve Room entities

## 🚀 Deployment

### Play Store Release
1. **Version Increment**: Update `versionCode` and `versionName`
2. **Signing**: Configure release signing
3. **Bundle Generation**: `./gradlew bundleRelease`
4. **Upload**: Upload AAB to Play Console

### Beta Testing
- Internal testing track for development team
- Closed testing for parents and educators
- Open testing for wider audience feedback

## 🎨 Design System

### Color Palette
```kotlin
val PorakhelaColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFEF7FF),
    surface = Color(0xFFFEF7FF)
)
```

### Typography
```kotlin
val PorakhelaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    // ... other text styles
)
```

### Child-Friendly Animations
- **Entrance**: Slide and fade animations
- **Success**: Bouncy celebrations
- **Progress**: Smooth progress indicators
- **Navigation**: Gentle transitions

## 🤝 Contributing

### Development Workflow
1. Create feature branch
2. Follow Kotlin coding standards
3. Write comprehensive tests
4. Update documentation
5. Submit pull request

### Code Standards
- **Kotlin Style Guide**: Follow official guidelines
- **Compose Best Practices**: Stateless composables
- **Architecture**: MVVM with clear separation
- **Documentation**: KDoc for public APIs

---

**Porakhela Mobile** - Making learning fun and rewarding for Bangladeshi children! 🎓⭐