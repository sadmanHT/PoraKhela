#!/usr/bin/env python
"""
Test script for database models to verify schema works correctly
"""
import os
import sys
import django
from django.conf import settings
from django.db import models

# Setup Django environment
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'porakhela.settings')
django.setup()

from django.contrib.auth import get_user_model
from apps.users.models import ChildProfile, ParentalPIN
from apps.lessons.models import Subject, Chapter, Lesson, LessonProgress
from apps.gamification.models import (
    PorapointLedger, Achievement, UserAchievement, 
    DailyStreak, Leaderboard, RewardCatalog, RewardRedemption
)
from django.utils import timezone
from decimal import Decimal
import uuid

User = get_user_model()

def test_database_schema():
    """Test database schema with sample data"""
    print("🧪 Testing database schema...")
    
    # Cleanup any existing test data
    print("🧹 Cleaning up existing test data...")
    User.objects.filter(phone_number__in=['01711111111', '01722222222']).delete()
    
    try:
        # Test 1: Create parent user
        print("\n1. Creating parent user...")
        parent_user = User.objects.create_user(
            username='01711111111',
            phone_number='01711111111',
            first_name='Ahmed',
            last_name='Rahman',
            user_type='parent',
            password='testpass123'
        )
        print(f"✅ Parent user created: {parent_user}")
        
        # Test 2: Create child user and profile
        print("\n2. Creating child user and profile...")
        child_user = User.objects.create_user(
            username='01722222222',
            phone_number='01722222222',
            first_name='Fatima',
            last_name='Rahman', 
            user_type='child',
            password='childpass123'
        )
        
        child_profile = ChildProfile.objects.create(
            user=child_user,
            parent=parent_user,
            grade=5,
            date_of_birth='2015-01-15',
            daily_screen_time_limit=120
        )
        print(f"✅ Child profile created: {child_profile}")
        
        # Test 3: Create subject and lesson structure
        print("\n3. Creating lesson structure...")
        subject = Subject.objects.create(
            name='Mathematics',
            name_bn='গণিত',
            description='Mathematics subject for primary education'
        )
        
        chapter = Chapter.objects.create(
            subject=subject,
            grade=5,
            chapter_number=1,
            title='Numbers and Operations',
            title_bn='সংখ্যা ও ক্রিয়াকলাপ',
            description='Basic mathematical operations with numbers'
        )
        
        lesson = Lesson.objects.create(
            chapter=chapter,
            subject='math',
            grade=5,
            title='Basic Addition',
            title_bn='মৌলিক যোগ',
            lesson_type='interactive',
            duration_minutes=30
        )
        print(f"✅ Lesson structure created: {lesson}")
        
        # Test 4: Create lesson progress
        print("\n4. Creating lesson progress...")
        lesson_progress = LessonProgress.objects.create(
            child=child_user,
            lesson=lesson,
            status='completed',
            score=85,
            time_spent=25,
            completion_percentage=100
        )
        print(f"✅ Lesson progress created: {lesson_progress}")
        
        # Test 5: Create porapoint ledger entries
        print("\n5. Creating porapoint transactions...")
        ledger_entry = PorapointLedger.objects.create(
            child=child_user,
            change_amount=50,
            reason='lesson_complete',
            balance_after=50,
            description='Completed Basic Addition lesson',
            idempotency_key=str(uuid.uuid4())
        )
        print(f"✅ Porapoint ledger entry created: {ledger_entry}")
        
        # Test 6: Create achievement and award it
        print("\n6. Creating achievements...")
        achievement = Achievement.objects.create(
            name='First Lesson Master',
            name_bn='প্রথম পাঠের মাস্টার',
            description='Complete your first lesson',
            description_bn='আপনার প্রথম পাঠ সম্পূর্ণ করুন',
            achievement_type='lesson_streak',
            reward_points=25,
            requirements={'lessons_completed': 1}
        )
        
        user_achievement = UserAchievement.objects.create(
            child=child_user,
            achievement=achievement,
            progress=100,
            is_completed=True,
            completed_at=timezone.now(),
            points_awarded=25
        )
        print(f"✅ Achievement created and awarded: {user_achievement}")
        
        # Test 7: Create daily streak
        print("\n7. Creating daily streak...")
        daily_streak = DailyStreak.objects.create(
            child=child_user,
            current_streak=3,
            longest_streak=5,
            last_activity_date=timezone.now().date(),
            weekly_goals_met=2,
            monthly_goals_met=8
        )
        print(f"✅ Daily streak created: {daily_streak}")
        
        # Test 8: Create reward and redemption
        print("\n8. Creating reward system...")
        reward = RewardCatalog.objects.create(
            name='Banglalink 10 TK Recharge',
            name_bn='বাংলালিংক ১০ টাকা রিচার্জ',
            description='10 TK mobile recharge for Banglalink',
            reward_type='mobile_recharge',
            cost_points=100,
            value_amount=Decimal('10.00'),
            value_unit='BDT',
            stock_quantity=1000,
            is_active=True
        )
        
        redemption = RewardRedemption.objects.create(
            child=child_user,
            reward=reward,
            quantity=1,
            total_points_cost=100,
            delivery_phone='01722222222',
            status='pending'
        )
        print(f"✅ Reward redemption created: {redemption}")
        
        # Test 9: Test model relationships
        print("\n9. Testing model relationships...")
        print(f"   Parent's children: {parent_user.children.count()}")
        print(f"   Child's lessons completed: {child_user.lesson_progress.filter(status='completed').count()}")
        print(f"   Child's total points: {child_user.porapoint_transactions.aggregate(total=models.Sum('change_amount'))}")
        print(f"   Child's achievements: {child_user.achievements.count()}")
        
        # Test 10: Test model string representations
        print("\n10. Testing model string representations...")
        print(f"   User: {parent_user}")
        print(f"   ChildProfile: {child_profile}")
        print(f"   Subject: {subject}")
        print(f"   Lesson: {lesson}")
        print(f"   LessonProgress: {lesson_progress}")
        print(f"   PorapointLedger: {ledger_entry}")
        print(f"   Achievement: {achievement}")
        print(f"   UserAchievement: {user_achievement}")
        print(f"   DailyStreak: {daily_streak}")
        print(f"   RewardCatalog: {reward}")
        print(f"   RewardRedemption: {redemption}")
        
        print("\n🎉 All database schema tests passed successfully!")
        return True
        
    except Exception as e:
        print(f"\n❌ Database schema test failed: {str(e)}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == '__main__':
    success = test_database_schema()
    sys.exit(0 if success else 1)