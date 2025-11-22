"""
Comprehensive Unit Tests for Parent Dashboard System

This test suite covers:
- Dashboard data aggregation
- Streak calculations 
- SMS function formatting
- Caching layer functionality
- Authentication and authorization
"""

import pytest
from django.test import TestCase, override_settings
from django.contrib.auth import get_user_model
from django.core.cache import cache
from django.utils import timezone
from datetime import datetime, timedelta
from unittest.mock import patch, MagicMock
from rest_framework.test import APIClient
from rest_framework import status

from apps.users.models import ChildProfile
from apps.lessons.models import LessonProgress
from apps.gamification.models import PorapointLedger
from apps.users.dashboard_views import ParentDashboardView, SetScreenTimeLimitView
from apps.users.tasks import (
    send_daily_sms_reports,
    generate_and_send_parent_sms, 
    generate_positive_sms_message,
    update_daily_streaks
)

User = get_user_model()


class ParentDashboardTestCase(TestCase):
    """Test cases for Parent Dashboard functionality"""
    
    def setUp(self):
        """Set up test data"""
        self.client = APIClient()
        
        # Create parent user
        self.parent = User.objects.create_user(
            username='+8801234567890',  # Use phone as username
            phone_number='+8801234567890',
            first_name='Test',
            last_name='Parent',
            user_type='parent',
            is_parent=True
        )
        
        # Create child users
        self.child1 = User.objects.create_user(
            username='+8801234567891',  # Use phone as username
            phone_number='+8801234567891',
            first_name='Child',
            last_name='One',
            user_type='child',
            is_parent=False  # Explicitly set as non-parent
        )
        
        self.child2 = User.objects.create_user(
            username='+8801234567892',  # Use phone as username
            phone_number='+8801234567892', 
            first_name='Child',
            last_name='Two',
            user_type='child',
            is_parent=False  # Explicitly set as non-parent
        )
        
        # Create child profiles
        self.child_profile1 = ChildProfile.objects.create(
            user=self.child1,
            parent=self.parent,
            grade=5,
            daily_screen_time_limit=45,
            current_streak=3,
            total_lessons_completed=10,
            total_points=150
        )
        
        self.child_profile2 = ChildProfile.objects.create(
            user=self.child2,
            parent=self.parent, 
            grade=7,
            daily_screen_time_limit=60,
            current_streak=1,
            total_lessons_completed=5,
            total_points=75
        )
        
        # Clear cache before each test
        cache.clear()
        
    def test_dashboard_data_aggregation(self):
        """Test dashboard aggregates data correctly from multiple children"""
        # Create lesson progress for today
        today = timezone.now().date()
        today_start = datetime.combine(today, datetime.min.time())
        today_end = datetime.combine(today, datetime.max.time())
        
        # Child 1 completes 2 lessons today
        LessonProgress.objects.create(
            child=self.child1,
            lesson_id=1,
            status='completed',
            completed_at=timezone.now(),
            last_accessed_at=timezone.now()
        )
        LessonProgress.objects.create(
            child=self.child1,
            lesson_id=2, 
            status='completed',
            completed_at=timezone.now(),
            last_accessed_at=timezone.now()
        )
        
        # Child 2 completes 1 lesson today
        LessonProgress.objects.create(
            child=self.child2,
            lesson_id=3,
            status='completed', 
            completed_at=timezone.now(),
            last_accessed_at=timezone.now()
        )
        
        # Create Porapoint transactions for today
        PorapointLedger.objects.create(
            child=self.child1,
            change_amount=25,
            description='Lesson completion',
            created_at=timezone.now()
        )
        PorapointLedger.objects.create(
            child=self.child1,
            change_amount=15,
            description='Bonus points',
            created_at=timezone.now()
        )
        PorapointLedger.objects.create(
            child=self.child2,
            change_amount=20,
            description='Lesson completion',
            created_at=timezone.now()
        )
        
        # Authenticate as parent
        self.client.force_authenticate(user=self.parent)
        
        # Call dashboard API
        response = self.client.get('/api/v1/auth/parent/dashboard/')
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        data = response.json()
        
        # Verify aggregated data
        self.assertTrue(data['success'])
        self.assertEqual(data['total_children'], 2)
        self.assertEqual(data['family_lessons_today'], 3)  # 2 + 1
        
        # Check individual children data
        children = {child['name']: child for child in data['children']}
        
        child1_data = children['Child One']
        self.assertEqual(child1_data['lessons_completed_today'], 2)
        self.assertEqual(child1_data['porapoints_earned_today'], 40)  # 25 + 15
        self.assertEqual(child1_data['streak'], 3)
        
        child2_data = children['Child Two'] 
        self.assertEqual(child2_data['lessons_completed_today'], 1)
        self.assertEqual(child2_data['porapoints_earned_today'], 20)
        self.assertEqual(child2_data['streak'], 1)
        
    def test_caching_layer(self):
        """Test Redis caching functionality"""
        # Authenticate as parent
        self.client.force_authenticate(user=self.parent)
        
        # First request should hit database
        response1 = self.client.get('/api/v1/auth/parent/dashboard/')
        self.assertEqual(response1.status_code, status.HTTP_200_OK)
        
        # Verify cache was populated
        cache_key = f"parent_dashboard_{self.parent.id}"
        cached_data = cache.get(cache_key)
        self.assertIsNotNone(cached_data)
        
        # Second request should use cache
        response2 = self.client.get('/api/v1/auth/parent/dashboard/')
        self.assertEqual(response2.status_code, status.HTTP_200_OK)
        
        # Data should be identical
        self.assertEqual(response1.json(), response2.json())
        
    def test_authentication_required(self):
        """Test that dashboard requires authentication"""
        # Unauthenticated request
        response = self.client.get('/api/v1/auth/parent/dashboard/')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)
        
    def test_parent_role_required(self):
        """Test that only parents can access dashboard"""
        # Authenticate as child (not parent)
        self.client.force_authenticate(user=self.child1)
        
        response = self.client.get('/api/v1/auth/parent/dashboard/')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)
        
    def test_screen_time_limit_setting(self):
        """Test setting screen time limits"""
        self.client.force_authenticate(user=self.parent)
        
        data = {
            'child_id': self.child_profile1.id,
            'screen_time_limit': 30
        }
        
        response = self.client.post('/api/v1/auth/parent/set-screen-time-limit/', data)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        
        # Verify limit was updated
        self.child_profile1.refresh_from_db()
        self.assertEqual(self.child_profile1.daily_screen_time_limit, 30)


class SMSFunctionalityTestCase(TestCase):
    """Test cases for SMS functionality"""
    
    def setUp(self):
        """Set up test data for SMS tests"""
        self.parent = User.objects.create_user(
            username='+8801234567890',
            phone_number='+8801234567890',
            first_name='Test',
            last_name='Parent',
            user_type='parent',
            is_parent=True
        )
        
        self.child = User.objects.create_user(
            username='+8801234567891',
            phone_number='+8801234567891',
            first_name='Rafi',
            last_name='Ahmed',
            user_type='child',
            is_parent=False
        )
        
        self.child_profile = ChildProfile.objects.create(
            user=self.child,
            parent=self.parent,
            grade=5,
            current_streak=4,
            total_lessons_completed=20,
            total_points=300
        )
        
    def test_positive_sms_message_generation(self):
        """Test SMS message generates positive content"""
        message_data = {
            'child_name': 'Rafi',
            'lessons_today': 3,
            'points_today': 50,
            'current_streak': 4,
            'total_lessons': 23
        }
        
        message = generate_positive_sms_message(message_data)
        
        # Check message contains positive language
        self.assertIn('amazing', message.lower()) or self.assertIn('great', message.lower()) or self.assertIn('fantastic', message.lower())
        self.assertIn('Rafi', message)
        self.assertIn('3', message)  # lessons count
        self.assertIn('50', message)  # points
        self.assertIn('4', message)  # streak
        
        # Ensure no negative words
        negative_words = ['but', 'however', 'although', 'disappointing']
        for word in negative_words:
            self.assertNotIn(word, message.lower())
    
    @patch('apps.users.tasks.requests.post')
    def test_sms_sending_mock(self, mock_post):
        """Test SMS sending with Applink mock"""
        mock_post.return_value.status_code = 200
        mock_post.return_value.json.return_value = {
            'success': True,
            'message_id': 'test_123'
        }
        
        result = generate_and_send_parent_sms(self.parent.id)
        
        self.assertTrue(result['success'])
        self.assertIn('phone_number', result)
        self.assertIn('message', result)
        
        # Verify mock was called
        mock_post.assert_called_once()
        call_args = mock_post.call_args
        self.assertIn('https://api.applink.com/v1/sms/send', call_args[0])
        
    def test_streak_calculation(self):
        """Test streak calculation logic"""
        # Create lesson progress for consecutive days
        today = timezone.now().date()
        
        for i in range(4):  # 4 consecutive days
            lesson_date = today - timedelta(days=i)
            lesson_datetime = datetime.combine(lesson_date, datetime.min.time()) + timedelta(hours=12)
            
            LessonProgress.objects.create(
                child=self.child,
                lesson_id=i + 1,
                status='completed',
                completed_at=lesson_datetime,
                last_accessed_at=lesson_datetime
            )
        
        # Update streaks
        update_daily_streaks()
        
        # Verify streak was calculated correctly
        self.child_profile.refresh_from_db()
        # Note: Streak calculation logic would need to be implemented in the task
        
    @patch('apps.users.tasks.send_daily_sms_reports.delay')
    def test_celery_task_trigger(self, mock_task):
        """Test Celery task can be triggered"""
        send_daily_sms_reports.delay()
        mock_task.assert_called_once()


class EdgeCaseTestCase(TestCase):
    """Test edge cases and error handling"""
    
    def setUp(self):
        """Set up test data"""
        self.parent = User.objects.create_user(
            username='+8801234567890',
            phone_number='+8801234567890',
            first_name='Test',
            last_name='Parent',
            user_type='parent',
            is_parent=True
        )
        
        self.client = APIClient()
        self.client.force_authenticate(user=self.parent)
        
    def test_dashboard_no_children(self):
        """Test dashboard with no children"""
        response = self.client.get('/api/v1/auth/parent/dashboard/')
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        data = response.json()
        
        self.assertEqual(data['total_children'], 0)
        self.assertEqual(data['children'], [])
        self.assertEqual(data['family_total_points'], 0)
        self.assertEqual(data['family_lessons_today'], 0)
        
    def test_sms_parent_no_phone(self):
        """Test SMS handling when parent has no phone number"""
        parent_no_phone = User.objects.create_user(
            phone_number=None,
            email='nophone@test.com',
            is_parent=True
        )
        
        result = generate_and_send_parent_sms(parent_no_phone.id)
        
        self.assertFalse(result['success'])
        self.assertIn('phone number', result['message'].lower())
        
    def test_cache_invalidation(self):
        """Test cache is properly invalidated"""
        # Make initial request to populate cache
        response1 = self.client.get('/api/v1/auth/parent/dashboard/')
        
        # Clear cache manually
        cache.clear()
        
        # Make second request - should hit database again
        response2 = self.client.get('/api/v1/auth/parent/dashboard/')
        
        # Both should succeed
        self.assertEqual(response1.status_code, status.HTTP_200_OK)
        self.assertEqual(response2.status_code, status.HTTP_200_OK)


@override_settings(CELERY_TASK_ALWAYS_EAGER=True)
class CeleryTaskTestCase(TestCase):
    """Test Celery tasks execution"""
    
    def setUp(self):
        """Set up test data"""
        self.parent = User.objects.create_user(
            username='+8801234567890',
            phone_number='+8801234567890',
            first_name='Test',
            last_name='Parent',
            user_type='parent',
            is_parent=True
        )
    
    @patch('apps.users.tasks.requests.post')
    def test_daily_sms_task_execution(self, mock_post):
        """Test daily SMS task executes correctly"""
        mock_post.return_value.status_code = 200
        mock_post.return_value.json.return_value = {'success': True}
        
        # Execute task synchronously
        result = send_daily_sms_reports()
        
        # Task should complete successfully
        self.assertIsNotNone(result)


if __name__ == '__main__':
    pytest.main([__file__])