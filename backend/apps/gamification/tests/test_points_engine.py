"""
Test Suite for Points Engine - Gamification System

Tests cover all the core gamification rules:
- Base points for lesson completion
- Streak bonuses
- Difficulty multipliers  
- Speed bonuses
- ACID compliance and idempotency prevention
"""

import uuid
from datetime import datetime, timedelta
from unittest.mock import patch
from django.test import TestCase
from django.contrib.auth import get_user_model
from django.utils import timezone
from django.db import transaction

from apps.users.models import ChildProfile
from apps.lessons.models import Subject, Chapter, Lesson, LessonProgress
from apps.gamification.models import PorapointLedger, DailyStreak
from apps.gamification.services.points_engine import PointsEngine

User = get_user_model()


class PointsEngineTestCase(TestCase):
    """Base test case with common setup for points engine tests."""
    
    def setUp(self):
        """Set up test data."""
        # Create parent user
        self.parent = User.objects.create(
            username='01700000001',
            phone_number='01700000001',
            first_name='Test',
            last_name='Parent',
            user_type='parent',
            is_parent=True
        )
        
        # Create child user
        self.child = User.objects.create(
            username='01700000002',
            phone_number='01700000002',
            first_name='Test',
            last_name='Child',
            user_type='child',
            is_parent=False
        )
        
        # Create child profile
        self.child_profile = ChildProfile.objects.create(
            user=self.child,
            parent=self.parent,
            grade=5,
            total_points=0
        )
        
        # Create test subject and lesson
        self.subject = Subject.objects.create(
            name='Test Math',
            name_bn='পরীক্ষা গণিত',
            description='Test mathematics subject'
        )
        
        self.chapter = Chapter.objects.create(
            subject=self.subject,
            grade=5,
            chapter_number=1,
            title='Test Chapter',
            title_bn='পরীক্ষা অধ্যায়',
            description='Test chapter'
        )
        
        # Easy lesson
        self.easy_lesson = Lesson.objects.create(
            subject='math',
            grade=5,
            title='Easy Addition',
            content_json={'test': 'data'},
            chapter=self.chapter,
            difficulty='easy'
        )
        
        # Medium lesson
        self.medium_lesson = Lesson.objects.create(
            subject='math',
            grade=5,
            title='Medium Multiplication',
            content_json={'test': 'data'},
            chapter=self.chapter,
            difficulty='medium'
        )
        
        # Hard lesson
        self.hard_lesson = Lesson.objects.create(
            subject='math',
            grade=5,
            title='Hard Calculus',
            content_json={'test': 'data'},
            chapter=self.chapter,
            difficulty='hard'
        )


class TestBasePoints(PointsEngineTestCase):
    """Test base point calculations for lesson completion."""
    
    def test_base_lesson_completion_points(self):
        """Test that lesson completion awards base 10 points."""
        # Create lesson progress
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.easy_lesson,
            status='completed',
            score=100.0,
            time_spent=30
        )
        
        # Award points for lesson completion
        result = PointsEngine.award_lesson_completion(
            child=self.child,
            lesson_progress=lesson_progress,
            idempotency_key='test_base_completion'
        )
        
        # Assertions
        self.assertEqual(result['breakdown']['base_completion'], 10)
        self.assertEqual(result['points_awarded'], 10)
        self.assertEqual(result['total_points'], 10)
        
        # Verify ledger entry
        ledger_entry = PorapointLedger.objects.get(id=result['transaction_id'])
        self.assertEqual(ledger_entry.change_amount, 10)
        self.assertEqual(ledger_entry.reason, 'lesson_complete')
        self.assertEqual(ledger_entry.balance_after, 10)
        
        # Verify child profile updated
        self.child_profile.refresh_from_db()
        self.assertEqual(self.child_profile.total_points, 10)
    
    def test_correct_answers_points(self):
        """Test that correct answers award 5 points each."""
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.easy_lesson,
            status='completed',
            score=80.0,
            time_spent=15
        )
        
        # Award points with 3 correct answers
        result = PointsEngine.award_lesson_completion(
            child=self.child,
            lesson_progress=lesson_progress,
            correct_answers=3,
            idempotency_key='test_correct_answers'
        )
        
        # Assertions
        self.assertEqual(result['breakdown']['base_completion'], 10)
        self.assertEqual(result['breakdown']['correct_answers'], 15)  # 3 × 5
        self.assertEqual(result['points_awarded'], 25)  # 10 + 15
    
    def test_speed_bonus(self):
        """Test speed bonus for completing lesson under 2 minutes."""
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.easy_lesson,
            status='completed',
            score=90.0,
            time_spent=1  # Under 2 minutes
        )
        
        # Award points with speed bonus
        result = PointsEngine.award_lesson_completion(
            child=self.child,
            lesson_progress=lesson_progress,
            correct_answers=2,
            time_spent_minutes=1,  # Under threshold
            idempotency_key='test_speed_bonus'
        )
        
        # Assertions
        self.assertEqual(result['breakdown']['base_completion'], 10)
        self.assertEqual(result['breakdown']['correct_answers'], 10)  # 2 × 5
        self.assertEqual(result['breakdown']['speed_bonus'], 10)
        self.assertEqual(result['points_awarded'], 30)  # 10 + 10 + 10
    
    def test_no_speed_bonus_over_threshold(self):
        """Test no speed bonus when time exceeds threshold."""
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.easy_lesson,
            status='completed',
            score=90.0,
            time_spent=5  # Over 2 minutes
        )
        
        result = PointsEngine.award_lesson_completion(
            child=self.child,
            lesson_progress=lesson_progress,
            time_spent_minutes=5,
            idempotency_key='test_no_speed_bonus'
        )
        
        # Should not have speed bonus
        self.assertNotIn('speed_bonus', result['breakdown'])
        self.assertEqual(result['points_awarded'], 10)  # Only base points


class TestStreakBonus(PointsEngineTestCase):
    """Test streak bonus calculations."""
    
    def test_day_3_streak_bonus(self):
        """Test +5 points bonus for 3-day streak."""
        # Create a daily streak that's already at day 2 as of yesterday
        yesterday = timezone.now().date() - timedelta(days=1) 
        daily_streak, created = DailyStreak.objects.get_or_create(
            child=self.child,
            defaults={
                'current_streak': 2,  # Already at 2 days
                'longest_streak': 2,
                'last_activity_date': yesterday
            }
        )
        if not created:
            # Update existing record
            daily_streak.current_streak = 2
            daily_streak.longest_streak = 2
            daily_streak.last_activity_date = yesterday
            daily_streak.save()

        # Create lesson progress
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.easy_lesson,
            status='completed',
            score=85.0,
            time_spent=20
        )

        # Award points (this should be day 3)
        result = PointsEngine.award_lesson_completion(
            child=self.child,
            lesson_progress=lesson_progress,
            idempotency_key='test_day_3_streak'
        )

        # Assertions for day 3 bonus
        self.assertEqual(result['streak_info']['current_streak'], 3)
        self.assertEqual(result['streak_info']['bonus_points'], 5)
        self.assertEqual(result['breakdown']['streak_bonus'], 5)
        self.assertEqual(result['points_awarded'], 15)  # 10 base + 5 streak    def test_day_7_streak_bonus(self):
        """Test +10 points bonus for 7-day streak."""
        # Create daily streak record - set last activity to yesterday for continuation
        yesterday = timezone.now().date() - timedelta(days=1)
        daily_streak, created = DailyStreak.objects.get_or_create(
            child=self.child,
            defaults={
                'current_streak': 6,  # Already at 6 days
                'longest_streak': 6,
                'last_activity_date': yesterday  # Yesterday so it continues today
            }
        )
        if not created:
            # Update existing record
            daily_streak.current_streak = 6
            daily_streak.longest_streak = 6
            daily_streak.last_activity_date = yesterday
            daily_streak.save()
        
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.easy_lesson,
            status='completed',
            score=85.0,
            time_spent=20
        )
        
        # Award points (this should be day 7)
        result = PointsEngine.award_lesson_completion(
            child=self.child,
            lesson_progress=lesson_progress,
            idempotency_key='test_day_7_streak'
        )
        
        # Assertions for day 7 bonus
        self.assertEqual(result['streak_info']['current_streak'], 7)
        self.assertEqual(result['streak_info']['bonus_points'], 10)
        self.assertEqual(result['breakdown']['streak_bonus'], 10)
        self.assertEqual(result['points_awarded'], 20)  # 10 base + 10 streak
    
    def test_day_30_streak_bonus(self):
        """Test +20 points bonus for 30-day streak."""
        # Create daily streak record at 29 days
        streak_date = timezone.now().date() - timedelta(days=1)
        daily_streak = DailyStreak.objects.create(
            child=self.child,
            current_streak=29,  # Already at 29 days
            longest_streak=29,
            last_activity_date=streak_date
        )
        
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.easy_lesson,
            status='completed',
            score=85.0,
            time_spent=20
        )
        
        # Award points (this should be day 30)
        result = PointsEngine.award_lesson_completion(
            child=self.child,
            lesson_progress=lesson_progress,
            idempotency_key='test_day_30_streak'
        )
        
        # Assertions for day 30 bonus
        self.assertEqual(result['streak_info']['current_streak'], 30)
        self.assertEqual(result['streak_info']['bonus_points'], 20)
        self.assertEqual(result['breakdown']['streak_bonus'], 20)
        self.assertEqual(result['points_awarded'], 30)  # 10 base + 20 streak
    
    def test_streak_broken(self):
        """Test streak reset when broken."""
        # Create streak that will be broken (gap of more than 1 day)
        old_date = timezone.now().date() - timedelta(days=3)  # 3 days ago
        daily_streak = DailyStreak.objects.create(
            child=self.child,
            current_streak=5,
            longest_streak=5,
            last_activity_date=old_date
        )
        
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.easy_lesson,
            status='completed'
        )
        
        result = PointsEngine.award_lesson_completion(
            child=self.child,
            lesson_progress=lesson_progress,
            idempotency_key='test_broken_streak'
        )
        
        # Streak should reset to 1
        self.assertEqual(result['streak_info']['current_streak'], 1)
        self.assertEqual(result['streak_info']['bonus_points'], 0)
        self.assertNotIn('streak_bonus', result['breakdown'])


class TestDifficultyMultiplier(PointsEngineTestCase):
    """Test difficulty multiplier calculations."""
    
    def test_easy_difficulty_no_multiplier(self):
        """Test that easy difficulty has no multiplier (1.0)."""
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.easy_lesson,
            status='completed',
            score=85.0
        )
        
        result = PointsEngine.award_lesson_completion(
            child=self.child,
            lesson_progress=lesson_progress,
            correct_answers=2,  # 10 extra points
            idempotency_key='test_easy_difficulty'
        )
        
        # Should not have difficulty bonus
        self.assertNotIn('difficulty_bonus', result['breakdown'])
        self.assertEqual(result['points_awarded'], 20)  # 10 base + 10 correct
    
    def test_medium_difficulty_no_multiplier(self):
        """Test that medium difficulty has no multiplier (1.0)."""
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.medium_lesson,
            status='completed',
            score=85.0
        )
        
        result = PointsEngine.award_lesson_completion(
            child=self.child,
            lesson_progress=lesson_progress,
            correct_answers=2,
            idempotency_key='test_medium_difficulty'
        )
        
        # Should not have difficulty bonus
        self.assertNotIn('difficulty_bonus', result['breakdown'])
        self.assertEqual(result['points_awarded'], 20)  # 10 base + 10 correct
    
    def test_hard_difficulty_multiplier(self):
        """Test that hard difficulty applies 1.5x multiplier."""
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.hard_lesson,  # Hard difficulty
            status='completed',
            score=85.0
        )
        
        result = PointsEngine.award_lesson_completion(
            child=self.child,
            lesson_progress=lesson_progress,
            correct_answers=2,  # Base: 10 + 10 = 20, Hard: 20 * 1.5 = 30
            idempotency_key='test_hard_difficulty'
        )
        
        # Should have difficulty bonus
        self.assertIn('difficulty_bonus', result['breakdown'])
        self.assertEqual(result['breakdown']['base_completion'], 10)
        self.assertEqual(result['breakdown']['correct_answers'], 10)
        self.assertEqual(result['breakdown']['difficulty_bonus'], 10)  # 30 - 20 = 10
        self.assertEqual(result['points_awarded'], 30)  # 20 * 1.5 = 30
    
    def test_hard_difficulty_with_speed_bonus(self):
        """Test hard difficulty multiplier with speed bonus."""
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.hard_lesson,
            status='completed',
            score=100.0
        )
        
        result = PointsEngine.award_lesson_completion(
            child=self.child,
            lesson_progress=lesson_progress,
            correct_answers=2,    # 10 points
            time_spent_minutes=1, # Speed bonus: 10 points
            idempotency_key='test_hard_with_speed'
        )
        
        # Base: 10 + 10 + 10 = 30, Hard: 30 * 1.5 = 45
        expected_total = 45
        difficulty_bonus = expected_total - 30  # 15
        
        self.assertEqual(result['breakdown']['base_completion'], 10)
        self.assertEqual(result['breakdown']['correct_answers'], 10)
        self.assertEqual(result['breakdown']['speed_bonus'], 10)
        self.assertEqual(result['breakdown']['difficulty_bonus'], difficulty_bonus)
        self.assertEqual(result['points_awarded'], expected_total)


class TestIdempotencyPrevention(PointsEngineTestCase):
    """Test ACID compliance and idempotency prevention."""
    
    def test_duplicate_idempotency_key_prevention(self):
        """Test that duplicate idempotency keys are prevented."""
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.easy_lesson,
            status='completed'
        )
        
        idempotency_key = 'test_duplicate_key'
        
        # First transaction should succeed
        result1 = PointsEngine.award_lesson_completion(
            child=self.child,
            lesson_progress=lesson_progress,
            idempotency_key=idempotency_key
        )
        self.assertEqual(result1['points_awarded'], 10)
        
        # Second transaction with same key should fail
        with self.assertRaises(ValueError) as context:
            PointsEngine.award_lesson_completion(
                child=self.child,
                lesson_progress=lesson_progress,
                idempotency_key=idempotency_key
            )
        
        self.assertIn('already exists', str(context.exception))
        
        # Verify only one ledger entry exists
        ledger_count = PorapointLedger.objects.filter(
            idempotency_key=idempotency_key
        ).count()
        self.assertEqual(ledger_count, 1)
    
    def test_atomic_transaction_rollback(self):
        """Test that failed transactions roll back completely."""
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.easy_lesson,
            status='completed'
        )
        
        initial_balance = PointsEngine._get_current_balance(self.child)
        
        # Create a scenario that will fail (duplicate key)
        PorapointLedger.objects.create(
            child=self.child,
            change_amount=5,
            reason='manual_adjustment',
            idempotency_key='existing_key',
            balance_after=5
        )
        
        # Try to use the same idempotency key
        with self.assertRaises(ValueError):
            PointsEngine.award_lesson_completion(
                child=self.child,
                lesson_progress=lesson_progress,
                idempotency_key='existing_key'
            )
        
        # Verify balance unchanged (transaction rolled back)
        final_balance = PointsEngine._get_current_balance(self.child)
        self.assertEqual(final_balance, 5)  # Only the initial manual entry
    
    def test_concurrent_transaction_safety(self):
        """Test safety with concurrent transactions."""
        lesson_progress = LessonProgress.objects.create(
            child=self.child,
            lesson=self.easy_lesson,
            status='completed'
        )
        
        # Simulate concurrent transactions with different keys
        results = []
        for i in range(3):
            result = PointsEngine.award_lesson_completion(
                child=self.child,
                lesson_progress=lesson_progress,
                correct_answers=1,
                idempotency_key=f'concurrent_test_{i}'
            )
            results.append(result)
        
        # Verify sequential balance increments
        self.assertEqual(results[0]['total_points'], 15)  # 10 + 5
        self.assertEqual(results[1]['total_points'], 30)  # 15 + 15
        self.assertEqual(results[2]['total_points'], 45)  # 30 + 15
        
        # Verify all transactions recorded
        ledger_count = PorapointLedger.objects.filter(child=self.child).count()
        self.assertEqual(ledger_count, 3)
    
    def test_balance_consistency(self):
        """Test that balance calculations remain consistent."""
        # Create multiple transactions
        transactions = [
            (10, 'lesson_complete'),
            (15, 'quiz_score'),
            (-5, 'penalty'),
            (20, 'achievement')
        ]
        
        expected_balance = 0
        for amount, reason in transactions:
            expected_balance += amount
            
            PorapointLedger.objects.create(
                child=self.child,
                change_amount=amount,
                reason=reason,
                idempotency_key=str(uuid.uuid4()),
                balance_after=expected_balance
            )
        
        # Verify current balance calculation
        current_balance = PointsEngine._get_current_balance(self.child)
        self.assertEqual(current_balance, expected_balance)
        self.assertEqual(current_balance, 40)  # 10 + 15 - 5 + 20


class TestQuizAndSpecialCases(PointsEngineTestCase):
    """Test quiz completion and special cases."""
    
    def test_quiz_completion_points(self):
        """Test points for quiz completion."""
        # Mock quiz attempt (in real code this would be a QuizAttempt instance)
        class MockQuizAttempt:
            def __init__(self):
                self.id = uuid.uuid4()
        
        quiz_attempt = MockQuizAttempt()
        
        result = PointsEngine.award_quiz_completion(
            child=self.child,
            quiz_attempt=quiz_attempt,
            correct_answers=8,
            total_questions=10,
            time_spent_minutes=3,  # No speed bonus
            idempotency_key='test_quiz'
        )
        
        # Should get 8 * 5 = 40 points for correct answers
        self.assertEqual(result['breakdown']['correct_answers'], 40)
        self.assertEqual(result['points_awarded'], 40)
        self.assertEqual(result['quiz_results']['accuracy_percentage'], 80.0)
    
    def test_perfect_quiz_score_bonus(self):
        """Test perfect score bonus for quiz."""
        class MockQuizAttempt:
            def __init__(self):
                self.id = uuid.uuid4()
        
        quiz_attempt = MockQuizAttempt()
        
        result = PointsEngine.award_quiz_completion(
            child=self.child,
            quiz_attempt=quiz_attempt,
            correct_answers=10,
            total_questions=10,
            time_spent_minutes=1,  # Speed bonus too
            idempotency_key='test_perfect_quiz'
        )
        
        # Should get: 50 (correct) + 10 (speed) + 20 (perfect) = 80 points
        self.assertEqual(result['breakdown']['correct_answers'], 50)
        self.assertEqual(result['breakdown']['speed_bonus'], 10)
        self.assertEqual(result['breakdown']['perfect_score_bonus'], 20)
        self.assertEqual(result['points_awarded'], 80)
        self.assertEqual(result['quiz_results']['accuracy_percentage'], 100.0)
    
    def test_daily_login_bonus(self):
        """Test daily login bonus."""
        result = PointsEngine.award_daily_login(
            child=self.child,
            idempotency_key='test_daily_login'
        )
        
        self.assertEqual(result['points_awarded'], 5)
        self.assertEqual(result['breakdown']['daily_login'], 5)
        
        # Try again same day - should return None
        result2 = PointsEngine.award_daily_login(
            child=self.child,
            idempotency_key='test_daily_login_2'
        )
        
        self.assertIsNone(result2)
    
    def test_points_deduction(self):
        """Test point deduction for redemptions."""
        # First add some points
        PorapointLedger.objects.create(
            child=self.child,
            change_amount=100,
            reason='manual_adjustment',
            idempotency_key='initial_points',
            balance_after=100
        )
        
        # Update child profile
        self.child_profile.total_points = 100
        self.child_profile.save()
        
        # Deduct points
        result = PointsEngine.deduct_points(
            child=self.child,
            amount=30,
            reason='redemption',
            description='Redeemed mobile recharge',
            idempotency_key='test_deduction'
        )
        
        self.assertEqual(result['points_deducted'], 30)
        self.assertEqual(result['total_points'], 70)
        
        # Verify ledger entry
        ledger_entry = result['ledger_entry']
        self.assertEqual(ledger_entry.change_amount, -30)
        self.assertEqual(ledger_entry.balance_after, 70)
    
    def test_insufficient_balance_deduction(self):
        """Test deduction with insufficient balance."""
        with self.assertRaises(ValueError) as context:
            PointsEngine.deduct_points(
                child=self.child,
                amount=50,
                reason='redemption',
                description='Attempted redemption'
            )
        
        self.assertIn('Insufficient balance', str(context.exception))
    
    def test_points_history(self):
        """Test getting points transaction history."""
        # Create some test transactions
        transactions_data = [
            (10, 'lesson_complete', 'Lesson 1'),
            (15, 'quiz_score', 'Quiz 1'),
            (-5, 'redemption', 'Small reward'),
            (20, 'achievement', 'First milestone')
        ]
        
        balance = 0
        for amount, reason, description in transactions_data:
            balance += amount
            PorapointLedger.objects.create(
                child=self.child,
                change_amount=amount,
                reason=reason,
                idempotency_key=str(uuid.uuid4()),
                balance_after=balance,
                description=description
            )
        
        # Get history
        history = PointsEngine.get_points_history(self.child)
        
        self.assertEqual(len(history['transactions']), 4)
        self.assertEqual(history['summary']['current_balance'], 40)
        self.assertEqual(history['summary']['total_earned'], 45)  # 10 + 15 + 20
        self.assertEqual(history['summary']['total_spent'], 5)    # abs(-5)
        self.assertEqual(history['summary']['transaction_count'], 4)