# Contributing to Porakhela Educational Platform

Thank you for your interest in contributing to Porakhela! This document provides guidelines and information for contributors.

## 🌟 Getting Started

### Prerequisites
- Python 3.11+
- Android Studio
- Git
- Docker & Docker Compose
- Basic knowledge of Django and Kotlin

### Development Environment Setup

1. **Fork the repository**
   ```bash
   git clone https://github.com/yourusername/porakhela-platform.git
   cd porakhela-platform
   ```

2. **Backend Setup**
   ```bash
   cd backend
   python -m venv venv
   venv\Scripts\activate  # Windows
   pip install -r requirements.txt
   python manage.py migrate
   python manage.py runserver
   ```

3. **Mobile Setup**
   - Open `mobile/` in Android Studio
   - Sync project with Gradle files
   - Run tests to ensure setup is correct

## 🚀 Development Workflow

### Branch Naming Convention
- `feature/description` - New features
- `fix/description` - Bug fixes
- `docs/description` - Documentation updates
- `test/description` - Test improvements
- `refactor/description` - Code refactoring

### Commit Message Format
```
type(scope): description

- feat: new feature
- fix: bug fix
- docs: documentation
- style: formatting
- refactor: code restructuring
- test: adding tests
- chore: maintenance
```

Example:
```bash
git commit -m "feat(mobile): add offline lesson player with gamification"
git commit -m "fix(backend): resolve SMS notification delivery issue"
```

## 🧪 Testing Requirements

### Backend Testing
```bash
cd backend
python manage.py test
coverage run --source='.' manage.py test
coverage report
```

### Mobile Testing
```bash
cd mobile
./gradlew testDebugUnitTest
./gradlew connectedAndroidTest
./test_runner.ps1 -TestSuite "all"
```

### Testing Standards
- **Minimum 90% test coverage** for new code
- **Unit tests** for all business logic
- **Integration tests** for API endpoints
- **E2E tests** for critical user flows
- **Performance tests** for mobile components

## 📱 Mobile Development Guidelines

### Code Style
- Follow [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- Use **Jetpack Compose** for UI components
- Implement **MVVM architecture** with Repository pattern
- Use **Hilt** for dependency injection

### Key Principles
- **Offline-First**: All features must work without internet
- **Performance**: Load times < 400ms
- **Accessibility**: Support for children with disabilities
- **Bengali Support**: Proper font rendering and localization

### Testing Mobile Code
```kotlin
@Test
fun `test lesson loading performance`() {
    // Arrange
    val lessonId = "test_lesson"
    
    // Act
    val startTime = System.currentTimeMillis()
    val lesson = repository.loadLesson(lessonId)
    val loadTime = System.currentTimeMillis() - startTime
    
    // Assert
    assertNotNull(lesson)
    assertTrue("Lesson should load in <400ms", loadTime < 400)
}
```

## 🐍 Backend Development Guidelines

### Code Style
- Follow [PEP 8](https://www.python.org/dev/peps/pep-0008/)
- Use **Django REST Framework** conventions
- Implement proper **serializers** and **viewsets**
- Use **Celery** for background tasks

### API Design
- **RESTful** endpoints with proper HTTP methods
- **Consistent** response formats
- **Proper** error handling with meaningful messages
- **API versioning** for backward compatibility

### Database Guidelines
- Use **migrations** for all schema changes
- Implement **proper indexing** for performance
- Follow **normalization** principles
- Use **transactions** for data integrity

### Testing Backend Code
```python
class LessonAPITestCase(TestCase):
    def test_lesson_completion_api(self):
        """Test lesson completion tracking"""
        # Arrange
        lesson = Lesson.objects.create(title="Test Lesson")
        user = User.objects.create_user(phone="01712345678")
        
        # Act
        response = self.client.post('/api/lessons/complete/', {
            'lesson_id': lesson.id,
            'score': 85,
            'time_taken': 120
        })
        
        # Assert
        self.assertEqual(response.status_code, 200)
        self.assertTrue(LessonProgress.objects.filter(
            lesson=lesson, 
            user=user
        ).exists())
```

## 🔧 Code Review Process

### Before Submitting PR
1. **Run all tests** and ensure they pass
2. **Update documentation** if needed
3. **Check code coverage** meets requirements
4. **Test manually** on both backend and mobile
5. **Lint your code** and fix any issues

### PR Requirements
- **Clear description** of changes
- **Link to issue** being addressed
- **Screenshots** for UI changes
- **Performance impact** assessment
- **Backward compatibility** considerations

### Review Checklist
- [ ] Code follows project conventions
- [ ] Tests are comprehensive and passing
- [ ] Documentation is updated
- [ ] Performance requirements met
- [ ] Accessibility compliance
- [ ] Bengali language support intact

## 🌍 Bangladesh-Specific Considerations

### Network Optimization
- **Minimal API calls** in mobile app
- **Efficient data structures** for offline storage
- **Compression** for data transfer
- **Retry mechanisms** with exponential backoff

### Device Compatibility
- **Android 7.0+** minimum support
- **2GB RAM** minimum requirements
- **ARM and x86** architecture support
- **Various screen sizes** and densities

### Cultural Sensitivity
- **Age-appropriate** content and interactions
- **Local educational standards** compliance
- **Bengali language** accuracy
- **Cultural context** in examples and illustrations

## 📊 Performance Standards

### Mobile Performance
- **Lesson load time**: < 400ms
- **App startup time**: < 3 seconds
- **Memory usage**: < 100MB on low-end devices
- **Battery optimization**: Background sync efficiency

### Backend Performance
- **API response time**: < 200ms for 95% of requests
- **Database query optimization**: < 100ms for complex queries
- **Concurrent user support**: 1000+ simultaneous users
- **SMS delivery**: < 30 seconds for notifications

## 🔒 Security Guidelines

### Data Protection
- **GDPR compliance** for user data
- **Child privacy** protection measures
- **Encryption** for sensitive data
- **Secure API** authentication

### Code Security
- **Input validation** for all user inputs
- **SQL injection** prevention
- **XSS protection** in frontend
- **Dependency vulnerability** scanning

## 📚 Documentation Standards

### Code Documentation
- **Comprehensive docstrings** for all functions
- **Inline comments** for complex logic
- **README updates** for new features
- **API documentation** using OpenAPI/Swagger

### Example Documentation
```python
def calculate_lesson_score(answers: List[Answer], lesson: Lesson) -> int:
    """
    Calculate the final score for a completed lesson.
    
    Args:
        answers: List of user's answers to lesson questions
        lesson: The lesson being scored
        
    Returns:
        int: Final score as percentage (0-100)
        
    Raises:
        ValueError: If answers don't match lesson questions
    """
    # Implementation here
```

## 🚨 Issue Reporting

### Bug Reports
Include:
- **Clear description** of the issue
- **Steps to reproduce** the problem
- **Expected vs actual** behavior
- **Environment details** (device, OS version, app version)
- **Screenshots or logs** if applicable

### Feature Requests
Include:
- **Clear use case** and user story
- **Proposed solution** with alternatives considered
- **Impact assessment** on existing features
- **Implementation complexity** estimate

## 🎯 Areas for Contribution

### High Priority
- **Offline sync optimization** improvements
- **Performance testing** and optimization
- **Accessibility features** enhancement
- **Bengali language** accuracy improvements

### Medium Priority
- **UI/UX improvements** for child-friendly design
- **Additional test coverage** for edge cases
- **Documentation** updates and translations
- **Code refactoring** for maintainability

### Low Priority
- **Code comments** and documentation
- **Minor bug fixes** and improvements
- **Development tooling** enhancements
- **CI/CD pipeline** optimizations

## 📞 Getting Help

### Communication Channels
- **GitHub Issues**: For bugs and feature requests
- **GitHub Discussions**: For general questions
- **Email**: dev@porakhela.com for private inquiries
- **Documentation**: Check existing docs before asking

### Mentorship
New contributors can request mentorship for:
- **Code review guidance** 
- **Architecture understanding**
- **Bangladesh context** education
- **Best practices** learning

## 🏆 Recognition

### Contributor Levels
- **First-time contributors**: Welcome package and guidance
- **Regular contributors**: Recognition in release notes
- **Core contributors**: Maintainer access and decision-making input
- **Champions**: Special recognition for exceptional contributions

Thank you for contributing to Porakhela! Together, we're transforming education in Bangladesh! 🇧🇩📱📚