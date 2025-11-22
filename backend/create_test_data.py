"""
Create Test Data for SMS and Dashboard Testing

This script creates sample parent-child relationships, lesson progress,
and point transactions to test the SMS and dashboard functionality.
"""

import os
import django

# Setup Django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'porakhela.settings')
django.setup()

from django.contrib.auth import get_user_model
from apps.users.models import ChildProfile
from apps.lessons.models import LessonProgress, Lesson, Subject, Chapter
from apps.gamification.models import PorapointLedger
from django.utils import timezone
from datetime import timedelta
from django.db import models
import uuid

User = get_user_model()

def create_test_lessons():
    """Create test lessons if they don't exist"""
    subject, created = Subject.objects.get_or_create(
        name='Mathematics',
        defaults={
            'name_bn': 'গণিত',
            'description': 'Basic mathematics for primary education'
        }
    )
    
    chapter, created = Chapter.objects.get_or_create(
        subject=subject,
        grade=5,
        chapter_number=1,
        defaults={
            'title': 'Basic Arithmetic',
            'title_bn': 'মৌলিক পাটিগণিত',
            'description': 'Addition and subtraction basics'
        }
    )
    
    lessons = []
    lesson_titles = [
        'Addition Basics',
        'Subtraction Basics', 
        'Multiplication Tables',
        'Division Basics',
        'Word Problems'
    ]
    
    for i, title in enumerate(lesson_titles, 1):
        lesson, created = Lesson.objects.get_or_create(
            chapter=chapter,
            order=i,
            title=title,
            defaults={
                'subject': 'math',
                'grade': 5,
                'title_bn': title,  # Simplified for test
                'description': f'Learn about {title.lower()}',
                'content_json': {'type': 'basic_lesson'},
                'duration_minutes': 15
            }
        )
        lessons.append(lesson)
    
    return lessons

def create_test_data():
    print("🏗️ Creating test data for SMS and Dashboard testing...")
    
    # First create test lessons
    print("📚 Creating test lessons...")
    test_lessons = create_test_lessons()
    print(f"✅ Created/found {len(test_lessons)} test lessons")
    
    # Create parent user
    parent, created = User.objects.get_or_create(
        phone_number='+8801712345678',
        defaults={
            'username': '+8801712345678',
            'first_name': 'Ahmed',
            'last_name': 'Rahman',
            'user_type': 'parent',
            'is_parent': True,
            'OTP_verified': True
        }
    )
    
    if created:
        print(f"✅ Created parent: {parent.get_full_name()}")
    else:
        print(f"📋 Using existing parent: {parent.get_full_name()}")
    
    # Create child users
    children_data = [
        {
            'phone': '+8801712345679',
            'first_name': 'Rafi',
            'last_name': 'Rahman',
            'grade': 5
        },
        {
            'phone': '+8801712345680', 
            'first_name': 'Sadia',
            'last_name': 'Rahman',
            'grade': 3
        }
    ]
    
    created_children = []
    
    for child_data in children_data:
        child, created = User.objects.get_or_create(
            phone_number=child_data['phone'],
            defaults={
                'username': child_data['phone'],
                'first_name': child_data['first_name'],
                'last_name': child_data['last_name'],
                'user_type': 'child',
                'is_parent': False,
                'OTP_verified': True
            }
        )
        
        if created:
            print(f"✅ Created child: {child.get_full_name()}")
        else:
            print(f"📋 Using existing child: {child.get_full_name()}")
        
        # Create child profile
        child_profile, profile_created = ChildProfile.objects.get_or_create(
            user=child,
            defaults={
                'parent': parent,
                'grade': child_data['grade'],
                'daily_screen_time_limit': 45,
                'current_streak': 3,
                'total_lessons_completed': 10,
                'total_points': 150
            }
        )
        
        if profile_created:
            print(f"✅ Created profile for {child.get_full_name()}")
        
        created_children.append(child)
    
    # Create lesson progress for today
    print("\n📚 Creating lesson progress data...")
    today = timezone.now()
    
    lessons_data = [
        # Rafi's lessons (using actual lesson objects)
        {'child': created_children[0], 'lesson': test_lessons[0], 'points': 25},
        {'child': created_children[0], 'lesson': test_lessons[1], 'points': 30},
        {'child': created_children[0], 'lesson': test_lessons[2], 'points': 15},
        
        # Sadia's lessons  
        {'child': created_children[1], 'lesson': test_lessons[3], 'points': 20},
        {'child': created_children[1], 'lesson': test_lessons[4], 'points': 25},
    ]
    
    for lesson_data in lessons_data:
        lesson_progress, created = LessonProgress.objects.get_or_create(
            child=lesson_data['child'],
            lesson=lesson_data['lesson'],
            defaults={
                'status': 'completed',
                'completed_at': today,
                'last_accessed_at': today,
                'completion_percentage': 100,
                'score': 85.0,
                'time_spent': 15,
                'porapoints_earned': lesson_data['points']
            }
        )
        
        if created:
            print(f"✅ Added lesson {lesson_data['lesson'].title} for {lesson_data['child'].first_name}")
            
            # Get current balance for child
            latest_transaction = PorapointLedger.objects.filter(
                child=lesson_data['child']
            ).order_by('-created_at').first()
            
            current_balance = latest_transaction.balance_after if latest_transaction else 0
            new_balance = current_balance + lesson_data['points']
            
            # Add Porapoints for the lesson
            PorapointLedger.objects.create(
                child=lesson_data['child'],
                change_amount=lesson_data['points'],
                balance_after=new_balance,
                reason='lesson_complete',
                idempotency_key=f"lesson_{lesson_data['lesson'].id}_{lesson_data['child'].id}_{today.strftime('%Y%m%d')}",
                description=f"Completed lesson: {lesson_data['lesson'].title}",
                created_at=today
            )
    
    # Create some historical data for streaks
    print("\n🔥 Creating historical data for streaks...")
    for i in range(1, 4):  # Last 3 days
        past_date = today - timedelta(days=i)
        
        for j, child in enumerate(created_children):
            # Use different lessons for historical data
            historical_lesson = test_lessons[i % len(test_lessons)]
            
            lesson_progress, created = LessonProgress.objects.get_or_create(
                child=child,
                lesson=historical_lesson,
                defaults={
                    'status': 'completed',
                    'completed_at': past_date,
                    'last_accessed_at': past_date,
                    'completion_percentage': 100,
                    'score': 80.0,
                    'time_spent': 10,
                    'porapoints_earned': 15
                }
            )
            
            if created:
                # Get current balance for child
                latest_transaction = PorapointLedger.objects.filter(
                    child=child
                ).order_by('-created_at').first()
                
                current_balance = latest_transaction.balance_after if latest_transaction else 0
                new_balance = current_balance + 15
                
                PorapointLedger.objects.create(
                    child=child,
                    change_amount=15,
                    balance_after=new_balance,
                    reason='lesson_complete',
                    idempotency_key=f"historical_{historical_lesson.id}_{child.id}_{past_date.strftime('%Y%m%d')}",
                    description=f"Historical lesson: {historical_lesson.title}",
                    created_at=past_date
                )
    
    print("\n📊 Test Data Summary:")
    print(f"👨‍👩‍👧‍👦 Parents: {User.objects.filter(is_parent=True).count()}")
    print(f"🧒 Children: {User.objects.filter(is_parent=False).count()}")
    print(f"👶 Child Profiles: {ChildProfile.objects.count()}")
    print(f"📚 Lesson Progress: {LessonProgress.objects.count()}")
    print(f"🏆 Porapoint Transactions: {PorapointLedger.objects.count()}")
    
    # Calculate today's stats
    today_lessons = LessonProgress.objects.filter(
        completed_at__date=today.date(),
        status='completed'
    ).count()
    
    today_points = PorapointLedger.objects.filter(
        created_at__date=today.date(),
        change_amount__gt=0
    ).aggregate(total=models.Sum('change_amount'))['total'] or 0
    
    print(f"📅 Today's lessons: {today_lessons}")
    print(f"💰 Today's points: {today_points}")
    
    print("\n✅ Test data creation completed!")
    return parent, created_children

if __name__ == '__main__':
    # Fix import for models.Sum
    from django.db import models
    create_test_data()