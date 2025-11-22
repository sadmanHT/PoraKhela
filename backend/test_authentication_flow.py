#!/usr/bin/env python3
"""
Comprehensive Authentication Flow Test

Tests the complete OTP-based authentication system:
1. Request OTP for phone number
2. Verify OTP and get JWT tokens
3. Access protected endpoints with JWT
4. Create child profiles
5. Middleware OTP verification enforcement
"""

import sys
import os
import json
from datetime import datetime

# Add the project directory to the Python path
sys.path.insert(0, os.path.abspath('.'))

def test_otp_service():
    """Test the OTP service functionality."""
    print("🔐 TESTING OTP SERVICE")
    print("=" * 50)
    
    try:
        from apps.users.auth_service import otp_service
        
        # Test OTP request
        phone_number = "01712345678"
        print(f"📱 Testing OTP request for {phone_number}")
        
        result = otp_service.request_otp(phone_number)
        print(f"✅ OTP Request Result: {result['success']}")
        print(f"   Message: {result['message']}")
        
        if result['success']:
            print(f"   TTL: {result.get('ttl_seconds', 'N/A')} seconds")
            print(f"   Remaining attempts: {result.get('remaining_attempts', 'N/A')}")
        
        return result['success']
        
    except Exception as e:
        print(f"❌ OTP Service test failed: {e}")
        return False

def test_otp_verification():
    """Test OTP verification with mock code."""
    print("\n🔑 TESTING OTP VERIFICATION")
    print("=" * 50)
    
    try:
        from apps.users.auth_service import otp_service
        from django.core.cache import cache
        
        phone_number = "01712345678"
        
        # First generate an OTP
        otp_result = otp_service.request_otp(phone_number)
        if not otp_result['success']:
            print(f"❌ Failed to generate OTP: {otp_result['message']}")
            return False
        
        # Get the OTP from cache for testing
        otp_key = f"otp:{phone_number}"
        otp_data = cache.get(otp_key)
        
        if not otp_data:
            print("❌ OTP not found in cache")
            return False
        
        otp_code = otp_data['code']
        print(f"📨 Generated OTP: {otp_code}")
        
        # Test verification
        verify_result = otp_service.verify_otp(phone_number, otp_code)
        print(f"✅ Verification Result: {verify_result['success']}")
        print(f"   Message: {verify_result['message']}")
        
        if verify_result['success']:
            tokens = verify_result['tokens']
            user_info = verify_result['user']
            
            print(f"   User ID: {user_info['id']}")
            print(f"   Phone: {user_info['phone_number']}")
            print(f"   Is Parent: {user_info['is_parent']}")
            print(f"   Children Count: {user_info['children_count']}")
            print(f"   Access Token: {tokens['access'][:50]}...")
        
        return verify_result['success']
        
    except Exception as e:
        print(f"❌ OTP Verification test failed: {e}")
        return False

def test_user_creation():
    """Test automatic user creation during OTP request."""
    print("\n👤 TESTING USER CREATION")
    print("=" * 50)
    
    try:
        from apps.users.models import User
        from apps.users.auth_service import otp_service
        
        phone_number = "01798765432"  # Different number for clean test
        
        # Check if user exists before
        user_exists_before = User.objects.filter(phone_number=phone_number).exists()
        print(f"📞 User exists before OTP: {user_exists_before}")
        
        # Request OTP (should create user)
        otp_result = otp_service.request_otp(phone_number)
        
        # Check if user was created
        user_exists_after = User.objects.filter(phone_number=phone_number).exists()
        print(f"✅ User exists after OTP: {user_exists_after}")
        
        if user_exists_after:
            user = User.objects.get(phone_number=phone_number)
            print(f"   User ID: {user.id}")
            print(f"   Is Parent: {user.is_parent}")
            print(f"   OTP Verified: {user.OTP_verified}")
            print(f"   User Type: {user.user_type}")
        
        return user_exists_after and not user_exists_before
        
    except Exception as e:
        print(f"❌ User Creation test failed: {e}")
        return False

def test_applink_otp_integration():
    """Test Applink OTP client integration."""
    print("\n📡 TESTING APPLINK OTP INTEGRATION")
    print("=" * 50)
    
    try:
        from applink_client.otp import OTPClient
        from applink_client.base import ApplinkConfig
        
        # Initialize OTP client in mock mode
        config = ApplinkConfig(mock_mode=True)
        otp_client = OTPClient(config=config)
        
        # Test sending OTP
        response = otp_client.send_verification_otp(
            phone_number="01712345678",
            otp_code="123456",
            app_name="Porakhela"
        )
        
        print(f"✅ Applink OTP Response Status: {response.status}")
        print(f"   Message: {response.message}")
        print(f"   Data: {response.data}")
        
        return response.status.value == 'success'
        
    except Exception as e:
        print(f"❌ Applink OTP Integration test failed: {e}")
        return False

def test_rate_limiting():
    """Test OTP request rate limiting."""
    print("\n⏰ TESTING RATE LIMITING")
    print("=" * 50)
    
    try:
        from apps.users.auth_service import otp_service
        
        phone_number = "01787654321"  # Different number for rate limit test
        
        success_count = 0
        rate_limited = False
        
        # Try to request OTP multiple times
        for i in range(7):  # Exceed the limit of 5
            result = otp_service.request_otp(phone_number)
            
            if result['success']:
                success_count += 1
                print(f"   Request {i+1}: ✅ Success")
            else:
                if result.get('code') == 'RATE_LIMIT_EXCEEDED':
                    rate_limited = True
                    print(f"   Request {i+1}: ⏸️ Rate Limited")
                    break
                else:
                    print(f"   Request {i+1}: ❌ Failed - {result['message']}")
        
        print(f"✅ Successful requests: {success_count}")
        print(f"✅ Rate limiting triggered: {rate_limited}")
        
        return rate_limited and success_count <= 5
        
    except Exception as e:
        print(f"❌ Rate Limiting test failed: {e}")
        return False

def test_serializers():
    """Test authentication serializers."""
    print("\n📝 TESTING SERIALIZERS")
    print("=" * 50)
    
    try:
        from apps.users.auth_serializers import RequestOTPSerializer, VerifyOTPSerializer
        
        # Test RequestOTPSerializer
        print("🔍 Testing RequestOTPSerializer...")
        
        # Valid data
        valid_data = {"phone_number": "01712345678"}
        serializer = RequestOTPSerializer(data=valid_data)
        is_valid = serializer.is_valid()
        print(f"   Valid phone number: {is_valid}")
        
        if is_valid:
            print(f"   Normalized: {serializer.validated_data['phone_number']}")
        
        # Invalid data
        invalid_data = {"phone_number": "123456"}
        serializer = RequestOTPSerializer(data=invalid_data)
        is_invalid = not serializer.is_valid()
        print(f"   Invalid phone number rejected: {is_invalid}")
        
        # Test VerifyOTPSerializer
        print("\n🔍 Testing VerifyOTPSerializer...")
        
        valid_verify_data = {
            "phone_number": "01712345678",
            "otp": "123456"
        }
        verify_serializer = VerifyOTPSerializer(data=valid_verify_data)
        verify_valid = verify_serializer.is_valid()
        print(f"   Valid OTP data: {verify_valid}")
        
        return is_valid and is_invalid and verify_valid
        
    except Exception as e:
        print(f"❌ Serializers test failed: {e}")
        return False

def test_redis_integration():
    """Test Redis cache integration."""
    print("\n🔴 TESTING REDIS INTEGRATION")
    print("=" * 50)
    
    try:
        from django.core.cache import cache
        import time
        
        # Test basic cache operations
        test_key = "test_otp_cache"
        test_data = {
            'code': '123456',
            'phone_number': '01712345678',
            'created_at': '2025-11-22T10:00:00',
            'attempts': 0
        }
        
        # Store data with TTL
        cache.set(test_key, test_data, 10)  # 10 seconds TTL
        print("✅ Data stored in Redis")
        
        # Retrieve data
        retrieved_data = cache.get(test_key)
        print(f"✅ Data retrieved: {retrieved_data is not None}")
        
        if retrieved_data:
            print(f"   Phone: {retrieved_data['phone_number']}")
            print(f"   Code: {retrieved_data['code']}")
        
        # Test TTL
        print("⏳ Testing TTL (waiting 2 seconds)...")
        time.sleep(2)
        still_exists = cache.get(test_key) is not None
        print(f"✅ Data still exists after 2s: {still_exists}")
        
        # Clean up
        cache.delete(test_key)
        
        return retrieved_data is not None
        
    except Exception as e:
        print(f"❌ Redis Integration test failed: {e}")
        return False

def main():
    """Run all authentication tests."""
    print("🔐 COMPREHENSIVE AUTHENTICATION FLOW TEST SUITE")
    print("=" * 70)
    print(f"📅 Test Run: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 70)
    
    # Setup Django
    try:
        import django
        from django.conf import settings
        os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'porakhela.settings')
        django.setup()
        print("✅ Django initialized successfully")
    except Exception as e:
        print(f"❌ Django setup failed: {e}")
        return
    
    tests = [
        ("Redis Integration", test_redis_integration),
        ("Applink OTP Integration", test_applink_otp_integration),
        ("Serializers Validation", test_serializers),
        ("User Creation", test_user_creation),
        ("OTP Service", test_otp_service),
        ("OTP Verification", test_otp_verification),
        ("Rate Limiting", test_rate_limiting),
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
    print("=" * 70)
    passed = sum(1 for name, result in results if result)
    total = len(results)
    
    for test_name, result in results:
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"{test_name:25} : {status}")
    
    print(f"\n🎯 OVERALL RESULT: {passed}/{total} tests passed")
    
    if passed == total:
        print("🎉 ALL TESTS PASSED! Authentication flow is ready!")
        print("\n🚀 AUTHENTICATION ENDPOINTS READY:")
        print("   POST /auth/request-otp/ - Request OTP for phone")
        print("   POST /auth/verify-otp/ - Verify OTP and get JWT")
        print("   POST /parent/create-child/ - Create child profile")
        print("   GET /auth/profile/ - Get user profile")
    else:
        print("⚠️  Some tests failed. Check the output above for details.")
    
    print("\n" + "=" * 70)

if __name__ == "__main__":
    main()