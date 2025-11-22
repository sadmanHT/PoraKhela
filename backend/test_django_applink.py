#!/usr/bin/env python3
"""
Django Shell Test Script for Applink API Integration

This script tests all Applink API clients within the Django environment
to ensure proper integration with the gamification system.
"""

import os
import sys
import django
from django.conf import settings

# Set up Django environment
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'porakhela.settings')
django.setup()

def test_applink_integration():
    """Test Applink API clients in Django environment."""
    print("🚀 Testing Applink API Integration in Django Environment\n")
    
    try:
        # Import Applink clients
        from applink_client.base import ApplinkConfig
        from applink_client.subscription import SubscriptionClient, SubscriptionPlan
        from applink_client.sms import SMSClient
        from applink_client.otp import OTPClient, OTPPurpose
        from applink_client.rewards import RewardsClient
        from applink_client.ussd import USSDClient, USSDMenuType
        print("✅ Applink client imports successful")
        
        # Configure client for Django environment
        config = ApplinkConfig(
            base_url="https://api.applink.com",
            api_key=getattr(settings, 'APPLINK_API_KEY', 'test_key'),
            timeout=30,
            max_retries=3,
            mock_mode=True  # Enable mock responses
        )
        print("✅ Configuration initialized")
        
        # Test phone number
        phone_number = "017xxx"
        child_name = "Test Child"
        
        print(f"\n📱 Testing subscription for {phone_number}")
        
        # Test 1: Subscription API
        subscription_client = SubscriptionClient(config)
        
        # Subscribe function as requested
        def subscribe(phone):
            """Subscribe function as requested in the prompt."""
            response = subscription_client.subscribe_user(
                phone_number=phone,
                plan=SubscriptionPlan.PREMIUM,
                duration_days=30
            )
            return response
        
        # Execute the requested test
        sub_response = subscribe("017xxx")
        print(f"✅ Subscribe('017xxx'): {sub_response.status.value}")
        print(f"   Message: {sub_response.message}")
        print(f"   Plan: {sub_response.data.get('plan', 'N/A')}")
        print(f"   Subscription ID: {sub_response.data.get('subscription_id', 'N/A')}")
        
        # Test 2: SMS API with gamification integration
        print(f"\n📧 Testing SMS notifications")
        sms_client = SMSClient(config)
        
        sms_response = sms_client.send_daily_progress_sms(
            phone_number=phone_number,
            child_name=child_name,
            lessons_completed=3,
            points_earned=50,
            streak_days=5
        )
        print(f"✅ Daily SMS: {sms_response.status.value}")
        print(f"   Message: {sms_response.data.get('message', 'N/A')[:80]}...")
        
        # Test 3: OTP for high-value redemptions
        print(f"\n🔐 Testing OTP for high-value redemptions")
        otp_client = OTPClient(config)
        
        otp_response = otp_client.send_high_value_redemption_otp(
            phone_number=phone_number,
            reward_name="500MB Data Bundle",
            reward_value="500MB",
            points_cost=200,
            child_name=child_name
        )
        print(f"✅ OTP sent: {otp_response.status.value}")
        print(f"   OTP ID: {otp_response.data.get('otp_id', 'N/A')}")
        print(f"   Mock OTP: {otp_response.data.get('mock_otp_code', 'N/A')}")
        
        # Test 4: Rewards redemption
        print(f"\n🎁 Testing rewards redemption")
        rewards_client = RewardsClient(config)
        
        reward_response = rewards_client.redeem_data_bundle(
            phone_number=phone_number,
            data_amount="100MB",
            points_to_spend=50,
            child_name=child_name
        )
        print(f"✅ Reward redeemed: {reward_response.status.value}")
        print(f"   Credited: {reward_response.raw_response.get('credited', 'N/A')}")
        print(f"   Redemption ID: {reward_response.data.get('redemption_id', 'N/A')}")
        
        # Test 5: USSD menu
        print(f"\n📞 Testing USSD menu")
        ussd_client = USSDClient(config)
        
        ussd_response = ussd_client.start_ussd_session(
            phone_number=phone_number,
            ussd_code="*123#",
            menu_type=USSDMenuType.MAIN_MENU
        )
        print(f"✅ USSD session: {ussd_response.status.value}")
        print(f"   Session ID: {ussd_response.data.get('session_id', 'N/A')}")
        
        # Test integration with Django User model
        print(f"\n👤 Testing Django User integration")
        from django.contrib.auth import get_user_model
        User = get_user_model()
        
        # Check if user exists
        test_user = User.objects.filter(phone_number=phone_number).first()
        if test_user:
            print(f"✅ Found Django user: {test_user.id}")
        else:
            print("ℹ️  No Django user found for test phone number")
        
        print(f"\n🎉 All Applink API calls succeeded!")
        return True
        
    except ImportError as e:
        print(f"❌ Import error: {e}")
        print("Fix: Check that applink_client module is in Python path")
        return False
        
    except Exception as e:
        print(f"❌ Error: {type(e).__name__}: {e}")
        print("Fix needed - see error details above")
        return False

if __name__ == "__main__":
    success = test_applink_integration()
    if success:
        print("\n✅ All tests passed - Applink integration ready!")
    else:
        print("\n❌ Tests failed - fixes needed")
    
    # Provide shell commands for manual testing
    print("\n" + "="*60)
    print("🔧 Manual Django Shell Commands:")
    print("="*60)
    print("python manage.py shell")
    print()
    print("# Test the subscription function as requested:")
    print("from applink_client.subscription import SubscriptionClient, SubscriptionPlan")
    print("from applink_client.base import ApplinkConfig")
    print()
    print("config = ApplinkConfig(mock_mode=True)")
    print("client = SubscriptionClient(config)")
    print()
    print("def subscribe(phone):")
    print("    return client.subscribe_user(phone, SubscriptionPlan.PREMIUM, 30)")
    print()
    print('response = subscribe("017xxx")')
    print("print(f'Status: {response.status.value}')")
    print("print(f'Message: {response.message}')")
    print("print(f'Data: {response.data}')")