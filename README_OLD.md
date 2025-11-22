# Porakhela - Gamified Learn-to-Earn EdTech Platform

Porakhela is a child-friendly, NCTB-based educational app that rewards learning with real Porapoints redeemable for Banglalink data/talktime through Applink APIs.

## 🎯 Project Overview

- **Backend**: Django REST API with PostgreSQL
- **Mobile**: Native Android app with offline-first architecture
- **Gamification**: Points system integrated with Applink Rewards API
- **Target**: Kids learning NCTB curriculum with parental controls

## 🏗️ Architecture

```
porakhela/
├── backend/          # Django REST API
├── mobile/           # Android Kotlin app
├── docker/           # Container configurations
└── docs/             # API and setup documentation
```

## 🚀 Quick Start

### Prerequisites
- Python 3.11+
- Android Studio
- Docker & Docker Compose
- PostgreSQL (or use Docker)

### Backend Setup
```bash
cd backend
python -m venv venv
venv\Scripts\activate  # Windows
pip install -r requirements.txt
python manage.py migrate
python manage.py runserver
```

### Mobile Setup
1. Open `mobile/` in Android Studio
2. Sync project with Gradle files
3. Run on emulator or device

### Docker Development
```bash
docker-compose up -d
```

## 📱 Features

- **Child-friendly UI** with gamified learning
- **Offline-first** lesson content
- **Parental controls** and progress tracking
- **Real rewards** via Applink integration
- **NCTB curriculum** alignment

## 🔧 Development

- Backend: Django REST Framework
- Mobile: MVVM with Room DB
- APIs: Applink (Subscription, SMS, USSD, Rewards, OTP)
- Database: PostgreSQL with offline sync

---
*Built for Banglalink's Learn-to-Earn initiative*