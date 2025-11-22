#!/usr/bin/env python3
"""
Comprehensive Authentication Unit Tests
3-Layer Testing: Unit -> Integration -> End-to-End
"""

import pytest
import uuid
from datetime import datetime, timedelta
from django.test import TestCase, TransactionTestCase
from django.contrib.auth import get_user_model
from django.core.cache import cache
from django.conf import settings
from rest_framework.test import APITestCase, APIClient
from rest_framework import status
from rest_framework_simplejwt.tokens import RefreshToken
from unittest.mock import Mock, patch
import json

from apps.users.models import User, ChildProfile
from apps.users.auth_service import OTPService
from apps.users.auth_serializers import (
    RequestOTPSerializer, 
    VerifyOTPSerializer,
    CreateChildProfileSerializer
)
from applink_client.otp import OTPClient, OTPPurpose


class UnitTestOTPService(TestCase):
    """Unit tests for OTP service functionality."""
    
    def setUp(self):
        """Set up test data."""
        self.otp_service = OTPService()
        self.phone_number = "01712345678"
        self.test_user = None
        # Clear cache before each test
        cache.clear()
    
    def tearDown(self):
        """Clean up after each test."""
        cache.clear()
        if self.test_user:
            self.test_user.delete()

    def test_normalize_phone_number(self):
        """Test phone number normalization."""
        test_cases = [
            ("01712345678", "01712345678"),
            ("+8801712345678", "01712345678"), 
            ("8801712345678", "01712345678"),
        ]
        
        for input_phone, expected in test_cases:
            result = self.otp_service._normalize_phone_number(input_phone)
            self.assertEqual(result, expected, f"Failed for input: {input_phone}")

    def test_generate_otp(self):
        """Test OTP generation."""
        otp = self.otp_service.generate_otp()
        
        # Check OTP format
        self.assertEqual(len(otp), 6, "OTP should be 6 digits")
        self.assertTrue(otp.isdigit(), "OTP should contain only digits")
        self.assertTrue(100000 <= int(otp) <= 999999, "OTP should be between 100000-999999")

    def test_cache_key_generation(self):
        """Test OTP cache key generation."""
        key = self.otp_service._get_otp_cache_key(self.phone_number)
        expected = f"otp:{self.phone_number}"
        self.assertEqual(key, expected)

    @patch('apps.users.auth_service.OTPClient')
    def test_request_otp_success(self, mock_otp_client):
        """Test successful OTP request."""
        # Mock Applink OTP client
        mock_client = Mock()
        mock_response = Mock()
        mock_response.status = "success"
        mock_response.message = "OTP sent"
        mock_client.send_otp.return_value = mock_response
        mock_otp_client.return_value = mock_client
        
        result = self.otp_service.request_otp(self.phone_number)
        
        # Verify response structure
        self.assertTrue(result['success'])
        self.assertIn('OTP sent to', result['message'])
        self.assertEqual(result['ttl_seconds'], 180)
        self.assertIn('remaining_attempts', result)
        
        # Verify OTP is stored in cache
        cache_key = self.otp_service._get_otp_cache_key(self.phone_number)
        cached_data = cache.get(cache_key)
        self.assertIsNotNone(cached_data)
        self.assertIn('code', cached_data)
        self.assertEqual(cached_data['phone_number'], self.phone_number)

    def test_rate_limiting(self):
        """Test OTP request rate limiting."""
        # Make maximum allowed requests
        for i in range(self.otp_service.MAX_OTP_ATTEMPTS):
            self.otp_service.increment_attempts(self.phone_number)
        
        # Check rate limiting
        is_allowed, remaining = self.otp_service.check_rate_limit(self.phone_number)
        self.assertFalse(is_allowed, "Should be rate limited after max attempts")
        self.assertEqual(remaining, 0)

    @patch('apps.users.auth_service.OTPClient')
    def test_verify_otp_success(self, mock_otp_client):
        """Test successful OTP verification."""
        # Mock Applink client
        mock_client = Mock()
        mock_response = Mock()
        mock_response.status = "success"
        mock_response.message = "OTP sent"
        mock_client.send_otp.return_value = mock_response
        mock_otp_client.return_value = mock_client
        
        # First request OTP
        self.otp_service.request_otp(self.phone_number)
        
        # Get the generated OTP from cache
        cache_key = self.otp_service._get_otp_cache_key(self.phone_number)
        cached_data = cache.get(cache_key)
        otp_code = cached_data['code']
        
        # Verify the OTP
        result = self.otp_service.verify_otp(self.phone_number, otp_code)
        
        # Check success response
        self.assertTrue(result['success'])
        self.assertIn('tokens', result)
        self.assertIn('access', result['tokens'])
        self.assertIn('refresh', result['tokens'])
        
        # Check user was created and verified
        user = User.objects.get(phone_number=self.phone_number)
        self.test_user = user  # For cleanup
        self.assertTrue(user.OTP_verified)
        self.assertTrue(user.is_parent)

    @patch('apps.users.auth_service.OTPClient')
    def test_verify_wrong_otp(self, mock_otp_client):
        """Test OTP verification with wrong code."""
        # Mock Applink client
        mock_client = Mock()
        mock_response = Mock()
        mock_response.status = "success"
        mock_response.message = "OTP sent"
        mock_client.send_otp.return_value = mock_response
        mock_otp_client.return_value = mock_client
        
        # First request OTP
        self.otp_service.request_otp(self.phone_number)
        
        # Try wrong OTP
        result = self.otp_service.verify_otp(self.phone_number, "000000")
        
        # Check failure response
        self.assertFalse(result['success'])
        self.assertIn('Invalid OTP', result['message'])
        self.assertIn('attempts remaining', result['message'])

    def test_verify_expired_otp(self):
        """Test OTP verification after expiration."""
        # Manually set expired OTP in cache
        cache_key = self.otp_service._get_otp_cache_key(self.phone_number)
        expired_time = datetime.now() - timedelta(minutes=5)
        cache.set(cache_key, {
            'code': '123456',
            'phone_number': self.phone_number,
            'created_at': expired_time.isoformat(),
            'attempts': 0
        }, timeout=1)  # Very short timeout
        
        # Wait for expiration
        import time
        time.sleep(2)
        
        # Try to verify expired OTP
        result = self.otp_service.verify_otp(self.phone_number, "123456")
        
        # Check failure response
        self.assertFalse(result['success'])
        self.assertEqual(result['code'], 'OTP_NOT_FOUND')


class UnitTestSerializers(TestCase):
    """Unit tests for authentication serializers."""
    
    def setUp(self):
        """Set up test data."""
        self.parent_user = User.objects.create(
            username="01712345678",
            phone_number="01712345678",
            first_name="Test",
            last_name="Parent", 
            is_parent=True,
            user_type="parent",
            OTP_verified=True
        )

    def test_request_otp_serializer_valid(self):
        """Test RequestOTPSerializer with valid data."""
        data = {"phone_number": "01712345678"}
        serializer = RequestOTPSerializer(data=data)
        
        self.assertTrue(serializer.is_valid())
        self.assertEqual(serializer.validated_data['phone_number'], "01712345678")

    def test_request_otp_serializer_invalid(self):
        """Test RequestOTPSerializer with invalid phone."""
        test_cases = [
            {"phone_number": "123456"},  # Too short
            {"phone_number": "02712345678"},  # Wrong prefix
            {"phone_number": "+1234567890"},  # Wrong country
        ]
        
        for data in test_cases:
            serializer = RequestOTPSerializer(data=data)
            self.assertFalse(serializer.is_valid(), f"Should fail for: {data}")

    def test_verify_otp_serializer_valid(self):
        """Test VerifyOTPSerializer with valid data."""
        data = {
            "phone_number": "01712345678",
            "otp": "123456"
        }
        serializer = VerifyOTPSerializer(data=data)
        
        self.assertTrue(serializer.is_valid())

    def test_verify_otp_serializer_invalid(self):
        """Test VerifyOTPSerializer with invalid data."""
        test_cases = [
            {"phone_number": "01712345678", "otp": "12345"},  # Too short
            {"phone_number": "01712345678", "otp": "1234567"},  # Too long
            {"phone_number": "01712345678", "otp": "abcdef"},  # Not digits
        ]
        
        for data in test_cases:
            serializer = VerifyOTPSerializer(data=data)
            self.assertFalse(serializer.is_valid(), f"Should fail for: {data}")

    def test_create_child_serializer_valid(self):
        """Test CreateChildProfileSerializer with valid data."""
        data = {
            "name": "Test Child",
            "phone_number": "01611111111",
            "password": "child123",
            "grade": 3,
            "avatar": "lion"
        }
        
        # Mock request context
        request = Mock()
        request.user = self.parent_user
        context = {'request': request}
        
        serializer = CreateChildProfileSerializer(data=data, context=context)
        self.assertTrue(serializer.is_valid())

    def test_create_child_serializer_invalid_grade(self):
        """Test CreateChildProfileSerializer with invalid grade."""
        data = {
            "name": "Test Child",
            "phone_number": "01622222222",
            "password": "child123",
            "grade": 15,  # Invalid grade
            "avatar": "lion"
        }
        
        request = Mock()
        request.user = self.parent_user
        context = {'request': request}
        
        serializer = CreateChildProfileSerializer(data=data, context=context)
        self.assertFalse(serializer.is_valid())
        self.assertIn('grade', serializer.errors)


class UnitTestJWTGeneration(TestCase):
    """Unit tests for JWT token generation."""
    
    def setUp(self):
        """Set up test user."""
        self.user = User.objects.create(
            username="01712345678",
            phone_number="01712345678", 
            first_name="Test",
            last_name="User",
            is_parent=True,
            user_type="parent",
            OTP_verified=True
        )

    def test_jwt_token_generation(self):
        """Test JWT token generation for user."""
        refresh = RefreshToken.for_user(self.user)
        access_token = str(refresh.access_token)
        refresh_token = str(refresh)
        
        # Check tokens are generated
        self.assertIsNotNone(access_token)
        self.assertIsNotNone(refresh_token)
        
        # Check token format (JWT has 3 parts separated by dots)
        self.assertEqual(len(access_token.split('.')), 3)
        self.assertEqual(len(refresh_token.split('.')), 3)

    def test_token_payload_content(self):
        """Test JWT token contains correct user data."""
        refresh = RefreshToken.for_user(self.user)
        
        # Access token payload
        access_payload = refresh.access_token.payload
        self.assertEqual(str(access_payload['user_id']), str(self.user.id))
        self.assertIn('exp', access_payload)  # Expiration
        self.assertIn('iat', access_payload)  # Issued at


class UnitTestChildCreation(TransactionTestCase):
    """Unit tests for child profile creation."""
    
    def setUp(self):
        """Set up test parent."""
        self.parent = User.objects.create(
            username="01712345678",
            phone_number="01712345678",
            first_name="Test",
            last_name="Parent",
            is_parent=True,
            user_type="parent",
            OTP_verified=True
        )

    def test_child_profile_creation(self):
        """Test child profile creation."""
        # Create child profile
        child_user = User.objects.create(
            username="C567801",
            phone_number="C567801",
            first_name="Test",
            last_name="Child",
            is_parent=False,
            user_type="child",
            OTP_verified=True
        )
        
        child_profile = ChildProfile.objects.create(
            user=child_user,
            parent=self.parent,
            grade=3
        )
        
        # Verify child profile
        self.assertEqual(child_profile.parent, self.parent)
        self.assertEqual(child_profile.user.first_name, "Test")
        self.assertEqual(child_profile.grade, 3)
        self.assertFalse(child_profile.user.is_parent)

    def test_parent_children_relationship(self):
        """Test parent-children relationship."""
        # Create multiple children
        for i in range(3):
            child_user = User.objects.create(
                username=f"C5678{i:02d}",
                phone_number=f"C5678{i:02d}",
                first_name=f"Child{i}",
                is_parent=False,
                user_type="child",
                OTP_verified=True
            )
            
            ChildProfile.objects.create(
                user=child_user,
                parent=self.parent,
                grade=i + 1
            )
        
        # Check parent has 3 children
        self.assertEqual(self.parent.children.count(), 3)
        
        # Check each child has correct parent
        for child_profile in self.parent.children.all():
            self.assertEqual(child_profile.parent, self.parent)


if __name__ == '__main__':
    # Run individual test classes
    import subprocess
    import sys
    
    print("🧪 RUNNING UNIT TESTS")
    print("=" * 50)
    
    # Run with pytest for better output
    result = subprocess.run([
        sys.executable, '-m', 'pytest', 
        __file__, '-v', '--tb=short'
    ], capture_output=True, text=True)
    
    print(result.stdout)
    if result.stderr:
        print("STDERR:", result.stderr)
    
    print(f"\n📊 Test Result: {'✅ PASSED' if result.returncode == 0 else '❌ FAILED'}")
    exit(result.returncode)