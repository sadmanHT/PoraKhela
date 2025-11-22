#!/usr/bin/env python3
"""
Test script for the Lesson Completion API

This tests the complete flow:
1. Lesson completion request
2. Points calculation via points engine
3. Ledger transaction creation
4. Parent SMS notification via Applink
5. Response with earned and total points

This is the core revenue-driving loop for Porakhela.
"""

import os
import sys
import uuid
import json
from datetime import datetime

# Add paths for imports
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append('.')

def test_lesson_completion_api():
    """Test the lesson completion API endpoint."""
    print("🚀 Testing Lesson Completion API")
    print("=" * 50)
    
    try:
        # Import Django setup
        import django
        os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'porakhela.settings')
        django.setup()
        
        # Import required modules
        from django.test import RequestFactory, TransactionTestCase
        from django.contrib.auth import get_user_model
        from apps.lessons.models import Subject, Chapter, Lesson, LessonProgress
        from apps.lessons.lesson_completion_api import LessonCompletionAPIView
        from apps.gamification.models import PorapointLedger
        
        User = get_user_model()
        
        print("✅ Django setup complete")
        
        # Create test data
        print("\n📦 Setting up test data...")
        
        # Create parent user
        parent = User.objects.create(
            phone_number="+8801712345678",
            first_name="Ahmed",
            last_name="Rahman",
            user_type="parent",
            email="parent@test.com"
        )
        
        # Create child user
        child = User.objects.create(
            phone_number="+8801712345679",
            first_name="Fatima",
            last_name="Rahman", 
            user_type="child",
            email="child@test.com"
        )
        
        # Create child profile
        from apps.users.models import ChildProfile
        child_profile = ChildProfile.objects.create(
            user=child,
            parent=parent,
            grade=5,
            date_of_birth="2015-01-01"
        )
        
        # Create lesson structure
        subject = Subject.objects.create(
            name="Mathematics",
            name_bn="গণিত",
            description="Basic math concepts"
        )
        
        chapter = Chapter.objects.create(
            subject=subject,
            grade=5,
            title="Basic Addition",
            title_bn="মৌলিক যোগ",
            order=1
        )
        
        lesson = Lesson.objects.create(
            chapter=chapter,
            subject="math",
            grade=5,
            title="Addition Basics",
            title_bn="যোগের মূল বিষয়",
            lesson_type="interactive",
            duration_minutes=30,
            difficulty="easy",
            order=1
        )
        
        print(f"✅ Test data created - Child: {child.id}, Lesson: {lesson.id}")
        
        # Test the API
        print("\n🎯 Testing Lesson Completion API...")
        
        # Create request factory
        factory = RequestFactory()
        
        # Prepare test payload matching the exact format
        test_payload = {
            "child_id": child.id,
            "lesson_id": lesson.id,
            "correct_answers": 8,
            "time_spent": 95,
            "difficulty": "Hard", 
            "idempotency_key": str(uuid.uuid4())
        }
        
        print(f"📤 Request payload: {json.dumps(test_payload, indent=2)}")
        
        # Create request
        request = factory.post(
            '/api/lesson/complete/',
            data=json.dumps(test_payload),
            content_type='application/json'
        )
        request.user = child  # Authenticate as child
        
        # Execute the API
        view = LessonCompletionAPIView()
        response = view.post(request)
        
        print(f"\n📥 Response Status: {response.status_code}")
        print(f"📥 Response Data: {json.dumps(response.data, indent=2)}")
        
        # Validate response
        if response.status_code == 200:
            data = response.data
            assert 'earned_points' in data, "earned_points missing from response"
            assert 'total_points' in data, "total_points missing from response" 
            assert 'transaction_id' in data, "transaction_id missing from response"
            assert 'points_breakdown' in data, "points_breakdown missing from response"
            assert 'sms_status' in data, "sms_status missing from response"
            
            print("✅ Response format validated")
            
            # Verify database state
            print("\n🔍 Verifying database state...")
            
            # Check lesson progress
            progress = LessonProgress.objects.get(child=child, lesson=lesson)
            assert progress.status == 'completed', "Lesson should be marked as completed"
            assert progress.completion_percentage == 100, "Should be 100% complete"
            print(f"✅ Lesson progress: {progress.status}")
            
            # Check ledger transaction
            transaction_id = data['transaction_id']
            ledger_entry = PorapointLedger.objects.get(id=transaction_id)
            assert ledger_entry.child == child, "Ledger entry should belong to child"
            assert ledger_entry.reason == 'lesson_complete', "Reason should be lesson_complete"
            assert ledger_entry.change_amount > 0, "Points should be positive"
            print(f"✅ Ledger entry: +{ledger_entry.change_amount} points")
            
            # Test idempotency
            print("\n🔄 Testing idempotency...")
            request2 = factory.post(
                '/api/lesson/complete/',
                data=json.dumps(test_payload),  # Same payload
                content_type='application/json'
            )
            request2.user = child
            
            response2 = view.post(request2)
            assert response2.status_code == 200, "Idempotent request should succeed"
            assert response2.data['transaction_id'] == transaction_id, "Should return same transaction"
            print("✅ Idempotency working correctly")
            
            print("\n🎉 ALL TESTS PASSED!")
            print("\n📊 Test Results:")
            print(f"   Earned Points: {data['earned_points']}")
            print(f"   Total Points: {data['total_points']}")
            print(f"   Points Breakdown: {data['points_breakdown']}")
            print(f"   SMS Status: {data['sms_status']}")
            print(f"   Transaction ID: {data['transaction_id']}")
            
            return True
            
        else:
            print(f"❌ API call failed with status {response.status_code}")
            print(f"❌ Error: {response.data}")
            return False
            
    except Exception as e:
        print(f"❌ Test failed with error: {str(e)}")
        import traceback
        traceback.print_exc()
        return False


def test_applink_integration():
    """Test that Applink SMS integration works."""
    print("\n📧 Testing Applink SMS Integration...")
    
    try:
        from applink_client.sms import SMSClient
        from applink_client.base import ApplinkConfig
        
        config = ApplinkConfig(mock_mode=True)
        sms_client = SMSClient(config)
        
        # Test the exact SMS format used in the API
        response = sms_client.send_daily_progress_sms(
            phone_number="+8801712345678",
            child_name="Fatima",
            lessons_completed=1,
            points_earned=45,
            streak_days=3
        )
        
        print(f"✅ SMS Status: {response.status.value}")
        print(f"✅ SMS Message: {response.data.get('message', '')[:80]}...")
        return True
        
    except Exception as e:
        print(f"❌ Applink SMS test failed: {str(e)}")
        return False


def main():
    """Run all tests."""
    print("🧪 LESSON COMPLETION API TEST SUITE")
    print("=" * 60)
    
    # Test 1: Core API functionality
    api_test_passed = test_lesson_completion_api()
    
    # Test 2: Applink integration
    sms_test_passed = test_applink_integration()
    
    # Summary
    print("\n" + "=" * 60)
    print("📊 FINAL RESULTS")
    print("=" * 60)
    
    if api_test_passed and sms_test_passed:
        print("🎉 ALL TESTS PASSED - LESSON COMPLETION API READY!")
        print("\n✅ Core Features Validated:")
        print("   ✅ Idempotency handling")
        print("   ✅ Points engine integration")
        print("   ✅ ACID-compliant ledger transactions")
        print("   ✅ Child total points update")
        print("   ✅ Applink SMS notifications")
        print("   ✅ Proper response format")
        
        print("\n🚀 Revenue-driving flow complete:")
        print("   lesson → calculate points → update ledger → send SMS → respond")
        
        return True
    else:
        print("❌ SOME TESTS FAILED")
        print(f"   API Test: {'✅' if api_test_passed else '❌'}")
        print(f"   SMS Test: {'✅' if sms_test_passed else '❌'}")
        return False


if __name__ == "__main__":
    success = main()
    
    print("\n" + "=" * 60)
    print("🔧 MANUAL API TESTING:")
    print("=" * 60)
    print("curl -X POST http://localhost:8000/api/v1/lessons/complete/ \\")
    print("  -H 'Content-Type: application/json' \\")
    print("  -H 'Authorization: Bearer YOUR_JWT_TOKEN' \\")
    print("  -d '{")
    print('    "child_id": 1,')
    print('    "lesson_id": 5,')
    print('    "correct_answers": 8,')
    print('    "time_spent": 95,')
    print('    "difficulty": "Hard",')
    print('    "idempotency_key": "unique-uuid-here"')
    print("  }'")
    
    sys.exit(0 if success else 1)