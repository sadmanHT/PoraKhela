#!/usr/bin/env python
"""
Test script for Django model object creation
"""
import os
import sys
import django
from django.conf import settings

# Setup Django environment
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'porakhela.settings')
django.setup()

def test_object_creation():
    """Test creating objects as specified"""
    from django.contrib.auth import get_user_model
    from apps.users.models import ChildProfile
    from apps.lessons.models import Subject, Chapter, Lesson, LessonProgress
    from apps.gamification.models import PorapointLedger
    import uuid
    
    User = get_user_model()
    
    print("🧪 Testing Django model object creation...")
    
    try:
        # Test 1: Create User as specified
        print("\n1. Testing User creation with phone_number and is_parent...")
        
        # Clean up any existing test user first
        User.objects.filter(phone_number="01700000001").delete()
        
        u = User.objects.create(
            username="01700000001",  # Since our custom user uses phone as username
            phone_number="01700000001",
            is_parent=True,
            first_name="Test",
            last_name="User",
            user_type="parent"
        )
        print(f"✅ User created successfully: {u}")
        print(f"   - Phone: {u.phone_number}")
        print(f"   - Is Parent: {u.is_parent}")
        print(f"   - User Type: {u.user_type}")
        
        # Test 2: Create Child User
        print("\n2. Testing Child User creation...")
        User.objects.filter(phone_number="01700000002").delete()
        
        child_user = User.objects.create(
            username="01700000002",
            phone_number="01700000002", 
            is_parent=False,
            first_name="Child",
            last_name="User",
            user_type="child"
        )
        print(f"✅ Child User created successfully: {child_user}")
        
        # Test 3: Create ChildProfile
        print("\n3. Testing ChildProfile creation...")
        child_profile = ChildProfile.objects.create(
            user=child_user,
            parent=u,
            grade=5,
            total_points=0
        )
        print(f"✅ ChildProfile created successfully: {child_profile}")
        
        # Test 4: Create Subject and Lesson
        print("\n4. Testing Subject and Lesson creation...")
        Subject.objects.filter(name="Test Math").delete()
        
        subject = Subject.objects.create(
            name="Test Math",
            name_bn="পরীক্ষা গণিত",
            description="Test mathematics subject"
        )
        
        chapter = Chapter.objects.create(
            subject=subject,
            grade=5,
            chapter_number=1,
            title="Test Chapter",
            title_bn="পরীক্ষা অধ্যায়",
            description="Test chapter description"
        )
        
        lesson = Lesson.objects.create(
            subject="math",
            grade=5,
            title="Test Lesson",
            content_json={"test": "data"},
            chapter=chapter
        )
        print(f"✅ Lesson created successfully: {lesson}")
        
        # Test 5: Create LessonProgress
        print("\n5. Testing LessonProgress creation...")
        progress = LessonProgress.objects.create(
            child=child_user,
            lesson=lesson,
            score=85.5,
            time_spent=30,
            status='completed'
        )
        print(f"✅ LessonProgress created successfully: {progress}")
        
        # Test 6: Create PorapointLedger
        print("\n6. Testing PorapointLedger creation...")
        ledger = PorapointLedger.objects.create(
            child=child_user,
            change_amount=50,
            reason='lesson_complete',
            idempotency_key=str(uuid.uuid4()),
            balance_after=50
        )
        print(f"✅ PorapointLedger created successfully: {ledger}")
        
        # Test 7: Verify relationships
        print("\n7. Testing model relationships...")
        print(f"   - User's child profiles: {u.children.count()}")
        print(f"   - Child's lesson progress: {child_user.lesson_progress.count()}")
        print(f"   - Child's point transactions: {child_user.porapoint_transactions.count()}")
        
        print("\n🎉 All object creation tests passed successfully!")
        return True
        
    except Exception as e:
        print(f"\n❌ Object creation test failed: {str(e)}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == '__main__':
    success = test_object_creation()
    sys.exit(0 if success else 1)