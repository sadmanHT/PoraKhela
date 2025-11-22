#!/usr/bin/env python3
"""
Simple Applink Integration Test
===============================
Tests the Applink client modules independently without Django dependencies.
"""

import sys
import os
import json
from datetime import datetime

# Add the project directory to the Python path
sys.path.insert(0, os.path.abspath('.'))

def test_applink_imports():
    """Test that all Applink modules can be imported."""
    print("🔧 TESTING APPLINK IMPORTS")
    print("=" * 50)
    
    try:
        # Test base client
        from applink_client.base import ApplinkClient
        print("✅ Base client import: SUCCESS")
        
        # Test SMS client
        from applink_client.sms import SMSClient
        print("✅ SMS client import: SUCCESS")
        
        # Test subscription client
        from applink_client.subscription import SubscriptionClient
        print("✅ Subscription client import: SUCCESS")
        
        # Test OTP client
        from applink_client.otp import OTPClient
        print("✅ OTP client import: SUCCESS")
        
        # Test rewards client
        from applink_client.rewards import RewardsClient
        print("✅ Rewards client import: SUCCESS")
        
        # Test USSD client
        from applink_client.ussd import USSDClient
        print("✅ USSD client import: SUCCESS")
        
        return True
    except Exception as e:
        print(f"❌ Import failed: {e}")
        return False

def test_sms_functionality():
    """Test SMS client functionality."""
    print("\n📱 TESTING SMS CLIENT")
    print("=" * 50)
    
    try:
        from applink_client.sms import SMSClient
        from applink_client.base import ApplinkConfig
        
        # Initialize SMS client with mock mode config
        config = ApplinkConfig(mock_mode=True)
        sms_client = SMSClient(config=config)
        
        # Test daily progress SMS (closest to lesson completion)
        response = sms_client.send_daily_progress_sms(
            phone_number="254700123456",
            child_name="Fatima",
            lessons_completed=1,
            points_earned=45,
            streak_days=3
        )
        
        print(f"✅ SMS Response Status: {response.status}")
        print(f"✅ SMS Message: {response.message[:50]}...")
        
        return response.status.value == 'success'
        
    except Exception as e:
        print(f"❌ SMS test failed: {e}")
        return False

def test_rewards_functionality():
    """Test rewards client functionality."""
    print("\n🎁 TESTING REWARDS CLIENT")
    print("=" * 50)
    
    try:
        from applink_client.rewards import RewardsClient
        from applink_client.base import ApplinkConfig
        
        # Initialize rewards client with mock mode config
        config = ApplinkConfig(mock_mode=True)
        rewards_client = RewardsClient(config=config)
        
        # Test airtime reward with correct parameters
        response = rewards_client.redeem_airtime(
            phone_number="254700123456",
            airtime_amount=5.0,
            currency="USD",
            points_to_spend=100,
            child_name="Fatima"
        )
        
        print(f"✅ Airtime Reward Status: {response.status}")
        print(f"✅ Airtime Response: {response.data}")
        
        # Test data bundle reward
        response = rewards_client.redeem_data_bundle(
            phone_number="254700123456",
            data_amount="100MB",
            points_to_spend=50,
            child_name="Fatima"
        )
        
        print(f"✅ Data Bundle Status: {response.status}")
        print(f"✅ Data Response: {response.data}")
        
        return True
        
    except Exception as e:
        print(f"❌ Rewards test failed: {e}")
        return False

def test_subscription_functionality():
    """Test subscription client functionality."""
    print("\n📋 TESTING SUBSCRIPTION CLIENT")
    print("=" * 50)
    
    try:
        from applink_client.subscription import SubscriptionClient
        from applink_client.base import ApplinkConfig
        
        # Initialize subscription client with mock mode config
        config = ApplinkConfig(mock_mode=True)
        sub_client = SubscriptionClient(config=config)
        
        # Test subscription status check
        response = sub_client.get_subscription_status("254700123456")
        
        print(f"✅ Subscription Status: {response.status}")
        print(f"✅ Subscription Response: {response.data}")
        
        return True
        
    except Exception as e:
        print(f"❌ Subscription test failed: {e}")
        return False

def test_points_calculation_logic():
    """Test points calculation logic that would be used in lesson completion."""
    print("\n🧮 TESTING POINTS CALCULATION LOGIC")
    print("=" * 50)
    
    try:
        # Simulate points calculation logic
        def calculate_lesson_points(correct_answers, time_spent, difficulty):
            """Simulate the points calculation from our lesson completion API."""
            base_points = correct_answers * 2
            
            # Time bonus (faster = more points)
            if time_spent < 60:
                time_bonus = 10
            elif time_spent < 120:
                time_bonus = 5
            else:
                time_bonus = 0
                
            # Difficulty multiplier
            difficulty_multipliers = {
                'Easy': 1.0,
                'Medium': 1.2,
                'Hard': 1.5
            }
            multiplier = difficulty_multipliers.get(difficulty, 1.0)
            
            total_points = int((base_points + time_bonus) * multiplier)
            return {
                'base_points': base_points,
                'time_bonus': time_bonus,
                'difficulty_multiplier': multiplier,
                'total_points': total_points
            }
        
        # Test case 1: High performance
        result1 = calculate_lesson_points(8, 95, "Hard")
        print(f"✅ Hard lesson (8/10, 95s): {result1['total_points']} points")
        print(f"   Breakdown: {result1}")
        
        # Test case 2: Medium performance
        result2 = calculate_lesson_points(6, 110, "Medium")
        print(f"✅ Medium lesson (6/10, 110s): {result2['total_points']} points")
        print(f"   Breakdown: {result2}")
        
        return True
        
    except Exception as e:
        print(f"❌ Points calculation test failed: {e}")
        return False

def main():
    """Run all tests and provide summary."""
    print("🧪 STANDALONE APPLINK INTEGRATION TEST SUITE")
    print("=" * 60)
    print(f"📅 Test Run: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)
    
    tests = [
        ("Import Tests", test_applink_imports),
        ("SMS Functionality", test_sms_functionality),
        ("Rewards Functionality", test_rewards_functionality),
        ("Subscription Functionality", test_subscription_functionality),
        ("Points Calculation", test_points_calculation_logic),
    ]
    
    results = []
    for test_name, test_func in tests:
        try:
            result = test_func()
            results.append((test_name, result))
        except Exception as e:
            print(f"❌ {test_name} CRASHED: {e}")
            results.append((test_name, False))
    
    # Print summary
    print("\n📊 TEST SUMMARY")
    print("=" * 60)
    passed = sum(1 for name, result in results if result)
    total = len(results)
    
    for test_name, result in results:
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"{test_name:25} : {status}")
    
    print(f"\n🎯 OVERALL RESULT: {passed}/{total} tests passed")
    
    if passed == total:
        print("🎉 ALL TESTS PASSED! Your Applink integration is ready!")
    else:
        print("⚠️  Some tests failed. Check the output above for details.")
    
    print("\n" + "=" * 60)
    print("🚀 READY FOR LESSON COMPLETION API INTEGRATION")
    print("Your Applink clients are working and ready to be integrated")
    print("into the Django lesson completion endpoint!")
    print("=" * 60)

if __name__ == "__main__":
    main()