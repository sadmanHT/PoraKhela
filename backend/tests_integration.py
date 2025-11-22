#!/usr/bin/env python3
"""
Integration Tests for Authentication API
Tests the actual HTTP endpoints and API integration
"""

import json
import time
from django.test import TransactionTestCase, Client
from django.urls import reverse
from django.core.cache import cache
from django.contrib.auth import get_user_model
from rest_framework.test import APITestCase, APIClient
from rest_framework import status
from unittest.mock import patch

from apps.users.models import User, ChildProfile

User = get_user_model()


class AuthenticationIntegrationTest(APITestCase):
    """Integration tests for the authentication API endpoints."""
    
    def setUp(self):
        """Set up test client and clear cache."""
        self.client = APIClient()
        self.phone_number = "01712345678"
        cache.clear()
        
        # Ensure DEBUG mode for tests (needed for debug_otp_code)
        from django.conf import settings
        settings.DEBUG = True
        
        # Clean up any existing data for this phone number
        User.objects.filter(phone_number=self.phone_number).delete()
        ChildProfile.objects.filter(parent__phone_number=self.phone_number).delete()
        ChildProfile.objects.filter(user__phone_number=self.phone_number).delete()
    
    def tearDown(self):
        """Clean up after tests."""
        cache.clear()
        # Clean up any data created during testing
        User.objects.filter(phone_number=self.phone_number).delete()
        ChildProfile.objects.filter(parent__phone_number=self.phone_number).delete()
        ChildProfile.objects.filter(user__phone_number=self.phone_number).delete()

    def test_complete_authentication_flow(self):
        """Test the complete authentication flow from OTP request to child creation."""
        
        # Step 1: Request OTP
        print("\n🔐 Step 1: Testing OTP Request")
        otp_request_data = {"phone_number": self.phone_number}
        
        response = self.client.post(
            '/api/v1/auth/request-otp/', 
            otp_request_data, 
            format='json'
        )
        
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['success'])
        self.assertIn('OTP sent to', response.data['message'])
        self.assertEqual(response.data['ttl_seconds'], 180)
        
        # Extract debug OTP if available
        debug_otp = response.data.get('debug_otp_code')
        self.assertIsNotNone(debug_otp, "Debug OTP should be available in test mode")
        print(f"   ✅ OTP Request Success - Debug OTP: {debug_otp}")
        
        # Step 2: Verify OTP
        print("\n🔑 Step 2: Testing OTP Verification")
        verify_request_data = {
            "phone_number": self.phone_number,
            "otp": debug_otp
        }
        
        response = self.client.post(
            '/api/v1/auth/verify-otp/',
            verify_request_data,
            format='json'
        )
        
        self.assertEqual(response.status_code, 200)
        self.assertIn('token', response.data)
        self.assertIn('refresh', response.data)
        
        access_token = response.data['token']
        user_id = response.data['user_id']
        phone = response.data['phone_number']
        
        self.assertEqual(phone, self.phone_number)
        self.assertTrue(response.data['is_parent'])
        print(f"   ✅ OTP Verification Success - User ID: {user_id}")
        
        # Step 3: Test authenticated profile access
        print("\n👤 Step 3: Testing Profile Access")
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {access_token}')
        
        response = self.client.get('/api/v1/auth/profile/')
        self.assertEqual(response.status_code, 200)
        
        profile = response.data['profile']
        self.assertEqual(profile['phone_number'], self.phone_number)
        self.assertTrue(profile['OTP_verified'])
        self.assertTrue(profile['is_parent'])
        print(f"   ✅ Profile Access Success - OTP Verified: {profile['OTP_verified']}")
        
        # Step 4: Test child profile creation
        print("\n👶 Step 4: Testing Child Profile Creation")
        child_data = {
            "name": "Integration Test Child",
            "phone_number": "01633333333",
            "password": "child123",
            "grade": 4,
            "avatar": "elephant"
        }
        
        response = self.client.post(
            '/api/v1/auth/parent/create-child/',
            child_data,
            format='json'
        )
        
        self.assertEqual(response.status_code, 201)
        self.assertTrue(response.data['success'])
        self.assertIn('child', response.data)
        
        child = response.data['child']
        self.assertEqual(child['name'], "Integration Test Child")
        self.assertEqual(child['grade'], 4)
        print(f"   ✅ Child Creation Success - Name: {child['name']}, Grade: {child['grade']}")
        
        # Step 5: Verify user now has children
        print("\n👨‍👩‍👧‍👦 Step 5: Verifying Parent-Child Relationship")
        response = self.client.get('/api/v1/auth/parent/children/')
        self.assertEqual(response.status_code, 200)
        
        children = response.data
        print(f"   🔍 Debug: Response data type: {type(children)}")
        print(f"   🔍 Debug: Response data: {children}")
        
        # Check if it's success response format or direct list
        if isinstance(children, dict) and 'children' in children:
            children_list = children['children']
        else:
            children_list = children
        
        print(f"   🔍 Debug: Found {len(children_list)} children")
        self.assertEqual(len(children_list), 1)
        
        child_info = children_list[0]
        print(f"   ✅ Parent-Child Link Success - Children count: {len(children_list)}")

    def test_invalid_phone_number(self):
        """Test OTP request with invalid phone number."""
        print("\n📱 Testing Invalid Phone Number")
        
        invalid_phones = [
            "123456",  # Too short
            "02712345678",  # Wrong prefix
            "+1234567890",  # Wrong country code
        ]
        
        for phone in invalid_phones:
            response = self.client.post(
                '/api/v1/auth/request-otp/',
                {"phone_number": phone},
                format='json'
            )
            self.assertEqual(response.status_code, 400)
            self.assertFalse(response.data['success'])
            print(f"   ✅ Rejected invalid phone: {phone}")

    def test_invalid_otp_verification(self):
        """Test OTP verification with wrong codes."""
        print("\n🔑 Testing Invalid OTP Verification")
        
        # First request a valid OTP
        response = self.client.post(
            '/api/v1/auth/request-otp/',
            {"phone_number": self.phone_number},
            format='json'
        )
        self.assertEqual(response.status_code, 200)
        
        # Try various invalid OTPs
        invalid_otps = ["000000", "123456", "999999"]
        
        for otp in invalid_otps:
            response = self.client.post(
                '/api/v1/auth/verify-otp/',
                {"phone_number": self.phone_number, "otp": otp},
                format='json'
            )
            self.assertEqual(response.status_code, 400)
            self.assertFalse(response.data['success'])
            self.assertIn('Invalid OTP', response.data['message'])
            print(f"   ✅ Rejected invalid OTP: {otp}")

    def test_protected_endpoints_without_auth(self):
        """Test that protected endpoints require authentication."""
        print("\n🚫 Testing Protected Endpoints Without Auth")
        
        protected_endpoints = [
            ('/api/v1/auth/profile/', 'GET'),
            ('/api/v1/auth/parent/create-child/', 'POST'),
        ]
        
        for endpoint, method in protected_endpoints:
            if method == 'GET':
                response = self.client.get(endpoint)
            elif method == 'POST':
                response = self.client.post(endpoint, {}, format='json')
            
            self.assertEqual(response.status_code, 401)
            print(f"   ✅ {method} {endpoint} requires auth")

    def test_middleware_blocks_unverified_users(self):
        """Test that middleware blocks unverified users."""
        print("\n🛡️ Testing Middleware Protection")
        
        # Check if our middleware is enabled
        from django.conf import settings
        middleware_enabled = any('EnsureOTPVerifiedMiddleware' in mw for mw in settings.MIDDLEWARE)
        
        if not middleware_enabled:
            print("   ⚠️ EnsureOTPVerifiedMiddleware not enabled in settings - skipping test")
            return
        
        # Create an unverified user manually
        unverified_user = User.objects.create(
            username="01987654321",
            phone_number="01987654321",
            first_name="Unverified",
            is_parent=True,
            user_type="parent",
            OTP_verified=False  # Not verified
        )
        
        # Generate token for unverified user
        from rest_framework_simplejwt.tokens import RefreshToken
        refresh = RefreshToken.for_user(unverified_user)
        access_token = str(refresh.access_token)
        
        # Try to access protected endpoint
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {access_token}')
        response = self.client.get('/api/v1/auth/profile/')
        
        # Should be blocked by middleware
        self.assertEqual(response.status_code, 403)
        print("   ✅ Middleware blocks unverified users")
        
        # Clean up
        unverified_user.delete()

    def test_rate_limiting(self):
        """Test OTP request rate limiting."""
        print("\n⏱️ Testing Rate Limiting")
        
        # Make multiple requests quickly
        for i in range(6):  # More than the limit
            response = self.client.post(
                '/api/v1/auth/request-otp/',
                {"phone_number": self.phone_number},
                format='json'
            )
            
            if i < 5:  # First 5 should succeed
                self.assertEqual(response.status_code, 200)
                print(f"   ✅ Request {i+1} allowed")
            else:  # 6th should be rate limited
                self.assertEqual(response.status_code, 429)
                self.assertIn('Too many OTP requests', response.data['message'])
                print(f"   ✅ Request {i+1} rate limited")
                break


def run_integration_tests():
    """Run integration tests and report results."""
    import subprocess
    import sys
    
    print("🔗 RUNNING INTEGRATION TESTS")
    print("=" * 50)
    
    # Run with Django's test runner for better database handling
    result = subprocess.run([
        sys.executable, 'manage.py', 'test', 
        'tests_integration.AuthenticationIntegrationTest',
        '--verbosity=2'
    ], capture_output=True, text=True)
    
    print(result.stdout)
    if result.stderr:
        print("STDERR:", result.stderr)
    
    print(f"\n📊 Integration Test Result: {'✅ PASSED' if result.returncode == 0 else '❌ FAILED'}")
    return result.returncode == 0


if __name__ == '__main__':
    run_integration_tests()