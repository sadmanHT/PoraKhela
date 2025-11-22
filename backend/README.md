# Porakhela Backend - Django REST API

A gamified, NCTB-based Learn-to-Earn EdTech platform backend that integrates with Applink APIs to reward kids with real Porapoints redeemable for Banglalink data/talktime.

## 🏗️ Architecture

### Technology Stack
- **Framework**: Django 4.2 + Django REST Framework
- **Database**: PostgreSQL
- **Cache/Queue**: Redis + Celery
- **Authentication**: JWT (Simple JWT)
- **Documentation**: DRF Spectacular (OpenAPI)

### Project Structure
```
backend/
├── porakhela/              # Django project settings
├── apps/                   # Django apps
│   ├── users/             # User management & authentication
│   ├── lessons/           # NCTB curriculum & lesson content
│   └── gamification/      # Points, achievements, rewards
├── services/              # Business logic
│   ├── applink_client.py  # Applink API integration
│   └── gamification_engine.py  # Points & achievements logic
├── utils/                 # Helper functions
└── config/               # Configuration files
```

## 🚀 Quick Start

### Prerequisites
- Python 3.11+
- PostgreSQL 13+
- Redis 6+

### Local Development

1. **Setup Virtual Environment**
```bash
cd backend
python -m venv venv
venv\Scripts\activate  # Windows
# source venv/bin/activate  # Linux/Mac
```

2. **Install Dependencies**
```bash
pip install -r requirements.txt
```

3. **Environment Configuration**
```bash
cp .env.example .env
# Edit .env with your database credentials
```

4. **Database Setup**
```bash
python manage.py migrate
python manage.py createsuperuser
```

5. **Load Sample Data** (Optional)
```bash
python manage.py loaddata fixtures/initial_data.json
```

6. **Run Development Server**
```bash
python manage.py runserver
```

### Docker Development

```bash
# Start all services
docker-compose up -d

# Check logs
docker-compose logs -f backend

# Run migrations
docker-compose exec backend python manage.py migrate

# Create superuser
docker-compose exec backend python manage.py createsuperuser
```

## 📡 API Endpoints

### Authentication
- `POST /api/v1/auth/register/` - User registration
- `POST /api/v1/auth/login/` - User login
- `POST /api/v1/auth/request_otp/` - Request OTP
- `POST /api/v1/auth/verify_otp/` - Verify OTP

### Users & Profiles
- `GET /api/v1/users/me/` - Current user profile
- `GET /api/v1/users/children/` - Parent's children (parents only)
- `POST /api/v1/children/` - Create child profile (parents only)
- `POST /api/v1/children/{id}/verify_pin/` - Verify parental PIN

### Lessons & Content
- `GET /api/v1/lessons/subjects/` - List subjects
- `GET /api/v1/lessons/chapters/` - List chapters by subject/grade
- `GET /api/v1/lessons/lessons/` - List lessons by chapter
- `POST /api/v1/lessons/lessons/{id}/start_lesson/` - Start lesson
- `POST /api/v1/lessons/lessons/{id}/update_progress/` - Update progress
- `POST /api/v1/lessons/lessons/{id}/submit_quiz/` - Submit quiz answers

### Gamification
- `GET /api/v1/gamification/points/balance/` - Current points balance
- `GET /api/v1/gamification/achievements/` - Available achievements
- `GET /api/v1/gamification/achievements/my_progress/` - User achievements
- `GET /api/v1/gamification/leaderboard/` - Leaderboard rankings
- `POST /api/v1/gamification/rewards/{id}/redeem/` - Redeem rewards

### API Documentation
- `GET /api/docs/` - Swagger UI
- `GET /api/redoc/` - ReDoc
- `GET /api/schema/` - OpenAPI schema

## 🎮 Gamification System

### Points System
- **Base Points**: 10 points per lesson completion
- **Quiz Bonus**: Up to 50% bonus for high scores (90%+)
- **Streak Bonus**: 1.5x multiplier for 7+ day streaks
- **First Attempt Bonus**: 20% bonus for completing on first try
- **Daily Login**: 5 points per day

### Achievements
- **Lesson Streak**: Complete lessons for consecutive days
- **Subject Mastery**: Complete all lessons in a subject
- **Quiz Perfectionist**: Score 100% on multiple quizzes
- **Early Bird**: Learn before 8 AM
- **Weekend Warrior**: Learn on weekends

### Rewards (Applink Integration)
- **Data Bundles**: Internet packages (500MB, 1GB, 2GB)
- **Talktime**: Mobile credit (10 BDT, 25 BDT, 50 BDT)
- **SMS Bundles**: Text message packages

## 🔧 Development

### Database Models

#### User Management
- `User` - Custom user model (phone-based auth)
- `ChildProfile` - Child-specific profile with parental controls
- `ParentalPIN` - PIN for parental authentication
- `OTPVerification` - Phone number verification

#### Educational Content
- `Subject` - NCTB subjects (Bengali, English, Math, etc.)
- `Chapter` - Chapters within subjects per grade
- `Lesson` - Individual lessons with content
- `LessonProgress` - Track student progress
- `QuizQuestion` - Quiz questions for lessons
- `QuizAttempt` - Student quiz attempts

#### Gamification
- `PorapointLedger` - All point transactions
- `Achievement` - Available achievements
- `UserAchievement` - User achievement progress
- `DailyStreak` - Daily learning streaks
- `Leaderboard` - Rankings across categories
- `RewardCatalog` - Available rewards
- `RewardRedemption` - Redemption history

### Business Logic Services

#### Gamification Engine (`services/gamification_engine.py`)
- Points calculation with bonuses
- Achievement progress tracking
- Leaderboard updates
- Streak management

#### Applink Client (`services/applink_client.py`)
- SMS OTP sending
- Subscription verification
- Data bundle redemption
- Talktime credit
- USSD balance checks

### Testing

```bash
# Run all tests
python manage.py test

# Run specific app tests
python manage.py test apps.users
python manage.py test apps.lessons
python manage.py test apps.gamification

# Coverage report
coverage run --source='.' manage.py test
coverage report
```

### Code Quality

```bash
# Code formatting
black .
isort .

# Linting
flake8
mypy .

# Security check
bandit -r .
```

## 🚀 Deployment

### Production Setup

1. **Environment Variables**
```bash
cp .env.prod .env
# Fill in production values
```

2. **Docker Production**
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

3. **Database Migration**
```bash
docker-compose exec backend python manage.py migrate
docker-compose exec backend python manage.py collectstatic --noinput
```

### Monitoring & Logging

- **Application Logs**: `/app/logs/django.log`
- **Database Logs**: PostgreSQL container logs
- **Celery Logs**: Worker and beat container logs

### Performance Optimization

- **Database Indexing**: All foreign keys and query fields indexed
- **Caching**: Redis for session and query caching
- **Background Tasks**: Celery for heavy operations
- **Static Files**: WhiteNoise for production static serving

## 🔐 Security

### Authentication & Authorization
- JWT-based authentication
- Phone number verification via OTP
- Parental controls with PIN protection
- Role-based permissions (parent/child)

### Data Protection
- Input validation and sanitization
- SQL injection prevention (Django ORM)
- XSS protection (DRF serializers)
- CSRF protection enabled

### API Security
- Rate limiting (future implementation)
- CORS configuration
- HTTPS enforcement in production
- API key authentication for Applink

## 📊 Monitoring

### Health Checks
- `GET /health/` - Basic health check
- Database connectivity
- Redis connectivity
- Celery worker status

### Metrics & Analytics
- User engagement tracking
- Lesson completion rates
- Point redemption analytics
- Performance monitoring

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

### Code Standards
- Follow PEP 8 style guide
- Write comprehensive docstrings
- Include unit tests for new features
- Update API documentation

## 📝 License

This project is proprietary software developed for Banglalink's Learn-to-Earn initiative.

---

For questions or support, contact the development team.