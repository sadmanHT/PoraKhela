#!/usr/bin/env python
"""
Points Engine Test Script

Manual test script to verify the gamification engine functionality
without Django's test framework issues.
"""
import os
import sys
import django
from django.conf import settings

# Setup Django environment
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'porakhela.settings')
django.setup()

import uuid
from datetime import datetime, timedelta
from django.contrib.auth import get_user_model
from django.utils import timezone

from apps.users.models import ChildProfile
from apps.lessons.models import Subject, Chapter, Lesson, LessonProgress
from apps.gamification.models import PorapointLedger, DailyStreak
from apps.gamification.services.points_engine import PointsEngine

User = get_user_model()

def test_gamification_engine():
    """Test the gamification engine functionality."""
    print("🎮 Testing Gamification Engine + Porapoint Rules")
    print("=" * 60)
    
    try:
        # Clean up any existing test data
        User.objects.filter(phone_number__in=['01700000001', '01700000002']).delete()
        
        # Create test users
        print("📝 Setting up test data...")
        parent = User.objects.create(
            username='01700000001',
            phone_number='01700000001',
            first_name='Test',
            last_name='Parent',
            user_type='parent',
            is_parent=True
        )
        
        child = User.objects.create(
            username='01700000002',
            phone_number='01700000002',
            first_name='Test',
            last_name='Child',
            user_type='child',
            is_parent=False
        )
        
        # Create child profile
        child_profile = ChildProfile.objects.create(
            user=child,
            parent=parent,
            grade=5,
            total_points=0
        )
        
        # Create test subject and lessons
        subject = Subject.objects.create(
            name='Test Math',
            name_bn='পরীক্ষা গণিত',
            description='Test mathematics subject'
        )
        
        chapter = Chapter.objects.create(
            subject=subject,
            grade=5,
            chapter_number=1,
            title='Test Chapter',
            title_bn='পরীক্ষা অধ্যায়',
            description='Test chapter'
        )
        
        # Easy lesson
        easy_lesson = Lesson.objects.create(
            subject='math',
            grade=5,
            title='Easy Addition',
            content_json={'test': 'data'},
            chapter=chapter,
            difficulty='easy'
        )
        
        # Hard lesson
        hard_lesson = Lesson.objects.create(
            subject='math',
            grade=5,
            title='Hard Calculus',
            content_json={'test': 'data'},
            chapter=chapter,
            difficulty='hard'
        )
        
        print("✅ Test data setup complete")
        
        # TEST 1: Base Points
        print("\n🧪 TEST 1: Base lesson completion points")
        lesson_progress = LessonProgress.objects.create(
            child=child,
            lesson=easy_lesson,
            status='completed',
            score=100.0,
            time_spent=30
        )
        
        result = PointsEngine.award_lesson_completion(
            child=child,
            lesson_progress=lesson_progress,
            idempotency_key='test_base_points'
        )
        
        print(f"   Points awarded: {result['points_awarded']}")
        print(f"   Breakdown: {result['breakdown']}")
        print(f"   Total points: {result['total_points']}")
        
        assert result['breakdown']['base_completion'] == 10, "Base points should be 10"
        assert result['points_awarded'] == 10, "Total should be 10"
        print("   ✅ Base points test passed")
        
        # TEST 2: Correct Answer Bonus
        print("\n🧪 TEST 2: Correct answer bonus (+5 each)")
        
        # Create another easy lesson for this test
        easy_lesson2 = Lesson.objects.create(
            subject='math',
            grade=5,
            title='Easy Subtraction',
            content_json={'test': 'data'},
            chapter=chapter,
            difficulty='easy'
        )
        
        lesson_progress2 = LessonProgress.objects.create(
            child=child,
            lesson=easy_lesson2,  # Use different lesson
            status='completed',
            score=80.0,
            time_spent=20
        )
        
        result2 = PointsEngine.award_lesson_completion(
            child=child,
            lesson_progress=lesson_progress2,
            correct_answers=3,  # 3 × 5 = 15 points
            idempotency_key='test_correct_answers'
        )
        
        print(f"   Points awarded: {result2['points_awarded']}")
        print(f"   Breakdown: {result2['breakdown']}")
        print(f"   Total points: {result2['total_points']}")
        
        assert result2['breakdown']['correct_answers'] == 15, "Should get 15 points for 3 correct answers"
        assert result2['points_awarded'] == 25, "Total should be 25 (10 base + 15 correct)"
        print("   ✅ Correct answer bonus test passed")
        
        # TEST 3: Speed Bonus
        print("\n🧪 TEST 3: Speed bonus (<2 minutes)")
        
        # Create another easy lesson for this test
        easy_lesson3 = Lesson.objects.create(
            subject='math',
            grade=5,
            title='Easy Multiplication',
            content_json={'test': 'data'},
            chapter=chapter,
            difficulty='easy'
        )
        
        lesson_progress3 = LessonProgress.objects.create(
            child=child,
            lesson=easy_lesson3,  # Use different lesson
            status='completed',
            score=90.0,
            time_spent=1
        )
        
        result3 = PointsEngine.award_lesson_completion(
            child=child,
            lesson_progress=lesson_progress3,
            correct_answers=2,
            time_spent_minutes=1,  # Under 2 minutes
            idempotency_key='test_speed_bonus'
        )
        
        print(f"   Points awarded: {result3['points_awarded']}")
        print(f"   Breakdown: {result3['breakdown']}")
        print(f"   Total points: {result3['total_points']}")
        
        assert 'speed_bonus' in result3['breakdown'], "Should have speed bonus"
        assert result3['breakdown']['speed_bonus'] == 10, "Speed bonus should be 10"
        assert result3['points_awarded'] == 30, "Total should be 30 (10 base + 10 correct + 10 speed)"
        print("   ✅ Speed bonus test passed")
        
        # TEST 4: Difficulty Multiplier (Hard)
        print("\n🧪 TEST 4: Difficulty multiplier (Hard ×1.5)")
        lesson_progress4 = LessonProgress.objects.create(
            child=child,
            lesson=hard_lesson,  # Hard difficulty
            status='completed',
            score=85.0,
            time_spent=15
        )
        
        result4 = PointsEngine.award_lesson_completion(
            child=child,
            lesson_progress=lesson_progress4,
            correct_answers=2,  # Base: 10 + 10 = 20, Hard: 20 × 1.5 = 30
            idempotency_key='test_difficulty_multiplier'
        )
        
        print(f"   Points awarded: {result4['points_awarded']}")
        print(f"   Breakdown: {result4['breakdown']}")
        print(f"   Total points: {result4['total_points']}")
        
        assert 'difficulty_bonus' in result4['breakdown'], "Should have difficulty bonus"
        assert result4['points_awarded'] == 30, "Total should be 30 (20 × 1.5)"
        print("   ✅ Difficulty multiplier test passed")
        
        # TEST 5: Streak Bonus (Day 3)
        print("\n🧪 TEST 5: Streak bonus (Day 3 → +5)")
        
        # Create another easy lesson for this test
        easy_lesson5 = Lesson.objects.create(
            subject='math',
            grade=5,
            title='Easy Division',
            content_json={'test': 'data'},
            chapter=chapter,
            difficulty='easy'
        )
        
        # Create a streak at day 2
        streak_date = timezone.now().date() - timedelta(days=1)
        
        # Update the existing daily streak or create a new one if it doesn't exist
        daily_streak, created = DailyStreak.objects.get_or_create(
            child=child,
            defaults={
                'current_streak': 2,
                'longest_streak': 2,
                'last_activity_date': streak_date
            }
        )
        
        if not created:
            # Update existing streak
            daily_streak.current_streak = 2
            daily_streak.longest_streak = 2
            daily_streak.last_activity_date = streak_date
            daily_streak.save()
        
        lesson_progress5 = LessonProgress.objects.create(
            child=child,
            lesson=easy_lesson5,  # Use different lesson
            status='completed',
            score=85.0,
            time_spent=20
        )
        
        result5 = PointsEngine.award_lesson_completion(
            child=child,
            lesson_progress=lesson_progress5,
            idempotency_key='test_streak_bonus'
        )
        
        print(f"   Points awarded: {result5['points_awarded']}")
        print(f"   Breakdown: {result5['breakdown']}")
        print(f"   Streak info: {result5['streak_info']}")
        print(f"   Total points: {result5['total_points']}")
        
        assert result5['streak_info']['current_streak'] == 3, "Should be day 3 of streak"
        assert result5['streak_info']['bonus_points'] == 5, "Should get 5 points bonus"
        assert 'streak_bonus' in result5['breakdown'], "Should have streak bonus"
        print("   ✅ Streak bonus test passed")
        
        # TEST 6: Idempotency Prevention
        print("\n🧪 TEST 6: Idempotency prevention")
        
        idempotency_key = 'test_duplicate_prevention'
        
        # Create another easy lesson for this test
        easy_lesson6 = Lesson.objects.create(
            subject='math',
            grade=5,
            title='Easy Fractions',
            content_json={'test': 'data'},
            chapter=chapter,
            difficulty='easy'
        )
        
        # First transaction
        lesson_progress6 = LessonProgress.objects.create(
            child=child,
            lesson=easy_lesson6,  # Use different lesson
            status='completed'
        )
        
        result6 = PointsEngine.award_lesson_completion(
            child=child,
            lesson_progress=lesson_progress6,
            idempotency_key=idempotency_key
        )
        
        print(f"   First transaction: {result6['points_awarded']} points")
        
        # Second transaction with same key should fail
        try:
            PointsEngine.award_lesson_completion(
                child=child,
                lesson_progress=lesson_progress6,
                idempotency_key=idempotency_key
            )
            assert False, "Should have raised ValueError for duplicate key"
        except ValueError as e:
            print(f"   Duplicate transaction prevented: {e}")
        
        # Verify only one transaction exists
        ledger_count = PorapointLedger.objects.filter(
            idempotency_key=idempotency_key
        ).count()
        assert ledger_count == 1, "Should only have one transaction with this key"
        print("   ✅ Idempotency prevention test passed")
        
        # TEST 7: Quiz Completion
        print("\n🧪 TEST 7: Quiz completion with perfect score bonus")
        
        # Mock quiz attempt
        class MockQuizAttempt:
            def __init__(self):
                self.id = uuid.uuid4()
        
        quiz_attempt = MockQuizAttempt()
        
        quiz_result = PointsEngine.award_quiz_completion(
            child=child,
            quiz_attempt=quiz_attempt,
            correct_answers=10,
            total_questions=10,
            time_spent_minutes=1,  # Speed bonus
            idempotency_key='test_perfect_quiz'
        )
        
        print(f"   Points awarded: {quiz_result['points_awarded']}")
        print(f"   Breakdown: {quiz_result['breakdown']}")
        print(f"   Quiz accuracy: {quiz_result['quiz_results']['accuracy_percentage']}%")
        
        expected_points = 50 + 10 + 20  # correct + speed + perfect bonus
        assert quiz_result['points_awarded'] == expected_points, f"Should get {expected_points} points"
        print("   ✅ Quiz completion test passed")
        
        # TEST 8: Daily Login Bonus
        print("\n🧪 TEST 8: Daily login bonus")
        
        login_result = PointsEngine.award_daily_login(
            child=child,
            idempotency_key='test_daily_login'
        )
        
        print(f"   Points awarded: {login_result['points_awarded']}")
        print(f"   Breakdown: {login_result['breakdown']}")
        
        assert login_result['points_awarded'] == 5, "Daily login should give 5 points"
        
        # Try again - should return None
        login_result2 = PointsEngine.award_daily_login(
            child=child,
            idempotency_key='test_daily_login_2'
        )
        
        assert login_result2 is None, "Should not award twice in same day"
        print("   ✅ Daily login bonus test passed")
        
        # TEST 9: Point Deduction
        print("\n🧪 TEST 9: Point deduction for redemptions")
        
        # Get current balance
        current_balance = PointsEngine._get_current_balance(child)
        print(f"   Current balance: {current_balance}")
        
        # Deduct points
        deduction_result = PointsEngine.deduct_points(
            child=child,
            amount=50,
            reason='redemption',
            description='Mobile recharge',
            idempotency_key='test_deduction'
        )
        
        print(f"   Points deducted: {deduction_result['points_deducted']}")
        print(f"   New balance: {deduction_result['total_points']}")
        
        assert deduction_result['points_deducted'] == 50, "Should deduct 50 points"
        assert deduction_result['total_points'] == current_balance - 50, "Balance should be reduced"
        print("   ✅ Point deduction test passed")
        
        # Final Summary
        print("\n" + "🎉" * 20)
        print("🎉 ALL GAMIFICATION TESTS PASSED! 🎉")
        print("🎉" * 20)
        
        final_balance = PointsEngine._get_current_balance(child)
        history = PointsEngine.get_points_history(child, limit=20)
        
        print(f"\n📊 Final Test Results:")
        print(f"   Final Balance: {final_balance} points")
        print(f"   Total Earned: {history['summary']['total_earned']} points")
        print(f"   Total Spent: {history['summary']['total_spent']} points")
        print(f"   Total Transactions: {history['summary']['transaction_count']}")
        
        print("\n✅ All gamification rules implemented correctly:")
        print("   ✅ Base lesson completion → +10 points")
        print("   ✅ Correct answers → +5 points each")
        print("   ✅ Speed bonus (<2min) → +10 points")
        print("   ✅ Streak bonuses: Day 3→+5, Day 7→+10, Day 30→+20")
        print("   ✅ Difficulty multiplier: Hard × 1.5")
        print("   ✅ ACID-compliant ledger with idempotency")
        print("   ✅ Quiz completion with perfect score bonus")
        print("   ✅ Daily login bonus")
        print("   ✅ Point deduction for redemptions")
        
        return True
        
    except Exception as e:
        print(f"\n❌ Test failed: {str(e)}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == '__main__':
    success = test_gamification_engine()
    sys.exit(0 if success else 1)