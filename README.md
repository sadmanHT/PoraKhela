# 🇧🇩 Porakhela Educational Platform

> **Transforming education in Bangladesh through offline-first mobile learning**

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Backend](https://img.shields.io/badge/Backend-Django-blue.svg)](https://djangoproject.com)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Bangladesh](https://img.shields.io/badge/Optimized%20for-Bangladesh-red.svg)](https://bangladesh.gov.bd)

## 🌟 Overview

Porakhela is a revolutionary educational platform specifically designed for Bangladesh's unique challenges. Our offline-first approach ensures children can learn even in areas with poor internet connectivity, while our gamification system keeps them engaged and motivated.

**Built for Banglalink's Learn-to-Earn initiative** - Transforming education through rewards and accessibility.

## ✨ Key Features

### 🎓 **Student Experience**
- **Offline-First Learning**: Download lessons once, play anywhere
- **Gamification**: Points, achievements, streaks, and celebrations
- **Bengali Language Support**: Native language interface with proper fonts
- **NCTB Curriculum**: Aligned with Bangladesh's national curriculum
- **Real Rewards**: Porapoints redeemable for Banglalink data/talktime
- **Low-End Device Optimization**: Smooth performance on budget Android phones

### 👨‍👩‍👧‍👦 **Parent Dashboard**
- **Real-time Progress Tracking**: Monitor child's learning journey
- **Daily SMS Reports**: Automated progress updates via SMS
- **Achievement Notifications**: Celebrate milestones together
- **Parental Controls**: Manage screen time and content access
- **Multi-Child Support**: Manage multiple children from one account

### 🏫 **Educational Features**
- **Comprehensive Curriculum**: Math, Science, English, Bengali
- **Interactive Exercises**: Engaging questions with immediate feedback
- **Progress Analytics**: Detailed learning insights
- **Adaptive Difficulty**: Personalized learning paths
- **Offline Sync**: Seamless data synchronization when online

### 📱 **Technology Highlights**
- **Offline-First Architecture**: Works without internet connection
- **USSD Integration**: Support for feature phone users
- **SMS Integration**: Applink Bangladesh partnership
- **Performance Optimized**: <400ms lesson load times
- **Robust Sync System**: Flawless offline→online transitions

## 🛠️ Tech Stack

### **Mobile Application**
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM + Clean Architecture
- **Database**: Room (SQLite)
- **Networking**: Retrofit + OkHttp
- **Background Tasks**: WorkManager
- **Dependency Injection**: Hilt
- **Testing**: JUnit, Espresso, Compose Testing

### **Backend API**
- **Framework**: Django REST Framework
- **Database**: PostgreSQL
- **Task Queue**: Celery + Redis
- **SMS Service**: Applink Bangladesh
- **Authentication**: JWT with PIN system
- **Deployment**: Docker + Docker Compose

## 🏗️ Architecture

```
porakhela/
├── backend/              # Django REST API
│   ├── apps/            # Modular Django apps
│   ├── services/        # Business logic
│   └── porakhela/       # Project settings
├── mobile/              # Android Kotlin app
│   ├── app/src/main/    # Main application code
│   ├── app/src/test/    # Unit tests
│   └── app/src/androidTest/ # Integration tests
├── docker/              # Container configurations
└── docs/                # Documentation
```

## 🚀 Quick Start

### **Prerequisites**
- Python 3.11+
- Android Studio
- Docker & Docker Compose
- PostgreSQL (or use Docker)

### **Backend Setup**
```bash
cd backend
python -m venv venv
venv\Scripts\activate  # Windows
# source venv/bin/activate  # Linux/Mac
pip install -r requirements.txt
python manage.py migrate
python manage.py runserver
```

### **Mobile Setup**
1. Open `mobile/` in Android Studio
2. Sync project with Gradle files
3. Run on emulator or device

### **Docker Development**
```bash
docker-compose up -d
```

## 🧪 Testing

### **Comprehensive Test Suite**
- **Unit Tests**: 95%+ coverage for core functionality
- **Integration Tests**: Airplane mode simulation with sync verification
- **E2E Tests**: Complete parent-child workflows
- **Performance Tests**: <400ms load time validation
- **Issue Detection**: Automated sync worker and idempotency testing

### **Run Tests**
```bash
# Backend tests
cd backend
python manage.py test

# Mobile tests
cd mobile
./test_runner.ps1  # Windows
./test_runner.sh   # Linux/Mac

# Complete test suite with issue detection
./mobile/test_runner.ps1 -GenerateFixes
```

## 📊 Performance Metrics

- **Lesson Load Time**: <400ms
- **Sync Completion**: <5 seconds offline→online
- **App Size**: <50MB optimized
- **Memory Usage**: <100MB on low-end devices
- **Offline Capability**: 100% lesson functionality
- **Test Coverage**: 95%+ comprehensive testing

## 🌍 Bangladesh Optimization

### **Network Conditions**
- **Offline-First**: Complete functionality without internet
- **Low Bandwidth**: Optimized for 2G/3G networks
- **Intermittent Connectivity**: Robust sync with exponential backoff
- **Data Conservation**: Minimal data usage with smart caching

### **Device Compatibility**
- **Low-End Devices**: Android 7.0+ (API 24+)
- **RAM Optimization**: Smooth performance with 2GB+ RAM
- **Storage Efficient**: Local lesson caching
- **Performance**: <400ms lesson loads on budget phones

### **Localization**
- **Bengali Language**: Full native support with proper fonts
- **NCTB Alignment**: National curriculum compliance
- **Cultural Context**: Bangladesh-specific educational content
- **Applink Integration**: SMS, USSD, and rewards partnership

## 🎮 Gamification System

- **Porapoints**: Earn points for correct answers and lesson completion
- **Achievements**: Unlock badges for learning milestones
- **Streaks**: Daily learning habit reinforcement
- **Celebrations**: Animated rewards with "+10 Porapoints! 🎉"
- **Real Rewards**: Redeem points for Banglalink data/talktime
- **Local Sync**: All gamification works offline

## 🔗 Applink Integration

- **Subscription API**: Manage learning subscriptions
- **SMS API**: Automated parent notifications
- **USSD API**: Feature phone support
- **Rewards API**: Real reward redemption
- **OTP API**: Secure authentication

## 📱 Mobile Features

- **Child-friendly UI** with intuitive navigation
- **Offline lesson player** with questions and timers
- **Progress tracking** with local storage and sync
- **Parental PIN** system for account security
- **Accessibility features** for inclusive learning
- **Bengali font support** with Kalpurush and Noto Sans

## 🤝 Contributing

We welcome contributions! Please see our development guidelines:

1. Fork the repository
2. Create a feature branch
3. Add comprehensive tests
4. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Applink Bangladesh** - SMS, USSD, and rewards integration
- **Banglalink** - Learn-to-Earn initiative support
- **Bangladesh Education Ministry** - NCTB curriculum guidance
- **Open Source Community** - Amazing tools and frameworks

## 📞 Support & Contact

- **Issues**: [GitHub Issues](https://github.com/yourusername/porakhela-platform/issues)
- **Email**: dev@porakhela.com
- **Documentation**: Check `/backend/README.md` and `/mobile/README.md`

## 🗺️ Roadmap

### **Phase 1** ✅ (Completed)
- [x] Offline-first lesson system with local storage
- [x] Parent dashboard with real-time progress tracking
- [x] Comprehensive gamification with celebrations
- [x] SMS integration with daily reports
- [x] Robust sync worker with issue detection
- [x] Complete testing framework with 95%+ coverage

### **Phase 2** 📋 (Planned)
- [ ] Advanced analytics and learning insights
- [ ] Teacher portal for content management
- [ ] Video lesson support with offline caching
- [ ] Peer-to-peer learning features
- [ ] Multi-language curriculum support

---

**Built with ❤️ for Bangladesh's educational future** 🇧🇩

*Empowering every child to learn and earn, regardless of connectivity*