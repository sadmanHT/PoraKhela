"""
Authentication Service for OTP-based Phone Authentication

Handles:
- OTP generation and validation
- Redis storage with TTL
- Applink OTP client integration
- User creation and management
- JWT token generation
"""
import random
import string
from datetime import datetime, timedelta
from typing import Optional, Dict, Tuple
import logging

from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.cache import cache
from django.utils import timezone
from rest_framework_simplejwt.tokens import RefreshToken

from applink_client.otp import OTPClient, OTPPurpose
from applink_client.base import ApplinkConfig

User = get_user_model()
logger = logging.getLogger(__name__)


class OTPService:
    """
    Service class for handling OTP operations with Redis and Applink integration.
    """
    
    # Redis key patterns
    OTP_KEY_PATTERN = "otp:{phone_number}"
    OTP_ATTEMPTS_KEY_PATTERN = "otp_attempts:{phone_number}"
    
    # Constants
    OTP_LENGTH = 6
    OTP_TTL_SECONDS = 180  # 3 minutes
    MAX_OTP_ATTEMPTS = 5
    RATE_LIMIT_WINDOW_SECONDS = 300  # 5 minutes
    
    def __init__(self):
        """Initialize OTP service with Applink client."""
        self.applink_config = ApplinkConfig(mock_mode=getattr(settings, 'APPLINK_MOCK_MODE', True))
        self.otp_client = OTPClient(config=self.applink_config)
    
    def generate_otp(self) -> str:
        """Generate a 6-digit OTP code."""
        return ''.join(random.choices(string.digits, k=self.OTP_LENGTH))
    
    def _get_otp_cache_key(self, phone_number: str) -> str:
        """Get Redis cache key for OTP storage."""
        return self.OTP_KEY_PATTERN.format(phone_number=phone_number)
    
    def _get_attempts_cache_key(self, phone_number: str) -> str:
        """Get Redis cache key for OTP attempts tracking."""
        return self.OTP_ATTEMPTS_KEY_PATTERN.format(phone_number=phone_number)
    
    def _normalize_phone_number(self, phone_number: str) -> str:
        """Normalize phone number to consistent format."""
        # Remove +88 or 88 prefix if present
        if phone_number.startswith('+88'):
            phone_number = phone_number[3:]
        elif phone_number.startswith('88'):
            phone_number = phone_number[2:]
        return phone_number
    
    def check_rate_limit(self, phone_number: str) -> Tuple[bool, int]:
        """
        Check if phone number has exceeded OTP request rate limit.
        
        Returns:
            Tuple[bool, int]: (is_allowed, remaining_attempts)
        """
        phone_number = self._normalize_phone_number(phone_number)
        attempts_key = self._get_attempts_cache_key(phone_number)
        
        current_attempts = cache.get(attempts_key, 0)
        
        if current_attempts >= self.MAX_OTP_ATTEMPTS:
            return False, 0
        
        return True, self.MAX_OTP_ATTEMPTS - current_attempts
    
    def increment_attempts(self, phone_number: str) -> int:
        """
        Increment OTP request attempts for rate limiting.
        
        Returns:
            Current attempt count
        """
        phone_number = self._normalize_phone_number(phone_number)
        attempts_key = self._get_attempts_cache_key(phone_number)
        
        current_attempts = cache.get(attempts_key, 0)
        new_attempts = current_attempts + 1
        
        cache.set(attempts_key, new_attempts, self.RATE_LIMIT_WINDOW_SECONDS)
        
        return new_attempts
    
    def request_otp(self, phone_number: str) -> Dict[str, any]:
        """
        Request OTP for phone number.
        
        Steps:
        1. Check rate limiting
        2. Generate OTP
        3. Store in Redis with TTL
        4. Send via Applink OTP client
        5. Create user if doesn't exist
        
        Args:
            phone_number: Normalized phone number
            
        Returns:
            Dict with success status and message
        """
        phone_number = self._normalize_phone_number(phone_number)
        
        # Check rate limiting
        is_allowed, remaining = self.check_rate_limit(phone_number)
        if not is_allowed:
            logger.warning(f"OTP request rate limit exceeded for {phone_number}")
            return {
                'success': False,
                'message': f'Too many OTP requests. Please try again in {self.RATE_LIMIT_WINDOW_SECONDS // 60} minutes.',
                'code': 'RATE_LIMIT_EXCEEDED'
            }
        
        # Generate OTP
        otp_code = self.generate_otp()
        otp_key = self._get_otp_cache_key(phone_number)
        
        # Store OTP in Redis with TTL
        otp_data = {
            'code': otp_code,
            'phone_number': phone_number,
            'created_at': timezone.now().isoformat(),
            'attempts': 0
        }
        
        cache.set(otp_key, otp_data, self.OTP_TTL_SECONDS)
        
        logger.info(f"OTP generated for {phone_number}, expires in {self.OTP_TTL_SECONDS}s")
        
        # Send OTP via Applink
        try:
            sms_response = self.otp_client.send_otp(
                phone_number=phone_number,
                purpose=OTPPurpose.ACCOUNT_VERIFICATION,
                expiry_minutes=self.OTP_TTL_SECONDS // 60
            )
            
            logger.info(f"Applink OTP response: {sms_response.status} - {sms_response.message}")
            
            # For development/testing - include mock OTP in response
            mock_otp_code = getattr(sms_response, 'mock_otp_code', None)
            
        except Exception as e:
            logger.error(f"Failed to send OTP via Applink: {str(e)}")
            # Continue with success response even if SMS fails (for development)
            mock_otp_code = None
        
        # Create user if doesn't exist
        try:
            user, created = User.objects.get_or_create(
                phone_number=phone_number,
                defaults={
                    'is_parent': True,
                    'user_type': 'parent',
                    'OTP_verified': False,
                    'username': phone_number  # Set username as phone number
                }
            )
            
            if created:
                logger.info(f"New parent user created for {phone_number}")
            
        except Exception as e:
            logger.error(f"Error creating user for {phone_number}: {str(e)}")
        
        # Increment rate limit counter
        self.increment_attempts(phone_number)
        
        # Prepare response
        response_data = {
            'success': True,
            'message': f'OTP sent to {phone_number}. Valid for {self.OTP_TTL_SECONDS // 60} minutes.',
            'ttl_seconds': self.OTP_TTL_SECONDS,
            'remaining_attempts': remaining - 1
        }
        
        # Include mock OTP for development/testing
        if mock_otp_code:
            response_data['mock_otp_code'] = mock_otp_code
            logger.info(f"Mock OTP code for testing: {mock_otp_code}")
        
        # In development, also include actual OTP for testing
        from django.conf import settings
        if settings.DEBUG:
            response_data['debug_otp_code'] = otp_code
            logger.warning(f"DEBUG MODE: Generated OTP {otp_code} for {phone_number}")
        
        return response_data
    
    def verify_otp(self, phone_number: str, otp_code: str) -> Dict[str, any]:
        """
        Verify OTP code for phone number.
        
        Steps:
        1. Retrieve OTP from Redis
        2. Validate code and expiry
        3. Mark user as OTP verified
        4. Generate JWT tokens
        5. Clean up Redis
        
        Args:
            phone_number: Normalized phone number
            otp_code: 6-digit OTP code
            
        Returns:
            Dict with tokens and user info or error
        """
        phone_number = self._normalize_phone_number(phone_number)
        otp_key = self._get_otp_cache_key(phone_number)
        
        # Get OTP data from Redis
        otp_data = cache.get(otp_key)
        
        if not otp_data:
            logger.warning(f"OTP verification failed for {phone_number}: No active OTP")
            return {
                'success': False,
                'message': 'No active OTP found. Please request a new OTP.',
                'code': 'OTP_NOT_FOUND'
            }
        
        # Check OTP attempts
        if otp_data.get('attempts', 0) >= 3:
            logger.warning(f"OTP verification failed for {phone_number}: Too many attempts")
            cache.delete(otp_key)
            return {
                'success': False,
                'message': 'Too many OTP verification attempts. Please request a new OTP.',
                'code': 'TOO_MANY_ATTEMPTS'
            }
        
        # Verify OTP code
        if otp_data['code'] != otp_code:
            # Increment attempts
            otp_data['attempts'] = otp_data.get('attempts', 0) + 1
            cache.set(otp_key, otp_data, self.OTP_TTL_SECONDS)
            
            logger.warning(f"OTP verification failed for {phone_number}: Invalid code")
            return {
                'success': False,
                'message': f'Invalid OTP code. {3 - otp_data["attempts"]} attempts remaining.',
                'code': 'INVALID_OTP'
            }
        
        # Get or create user
        try:
            user = User.objects.get(phone_number=phone_number)
        except User.DoesNotExist:
            logger.error(f"User not found for {phone_number} during OTP verification")
            return {
                'success': False,
                'message': 'User account not found. Please request OTP again.',
                'code': 'USER_NOT_FOUND'
            }
        
        # Mark user as OTP verified
        user.OTP_verified = True
        user.last_login = timezone.now()
        user.save(update_fields=['OTP_verified', 'last_login'])
        
        # Generate JWT tokens
        refresh = RefreshToken.for_user(user)
        access_token = refresh.access_token
        
        # Clean up Redis
        cache.delete(otp_key)
        cache.delete(self._get_attempts_cache_key(phone_number))
        
        # Get user info
        children_count = user.children.count() if user.is_parent else 0
        
        logger.info(f"OTP verification successful for {phone_number}")
        
        return {
            'success': True,
            'message': 'Phone number verified successfully.',
            'tokens': {
                'access': str(access_token),
                'refresh': str(refresh)
            },
            'user': {
                'id': str(user.id),
                'phone_number': user.phone_number,
                'is_parent': user.is_parent,
                'user_type': user.user_type,
                'has_children': children_count > 0,
                'children_count': children_count,
                'first_name': user.first_name,
                'last_name': user.last_name
            }
        }


# Global service instance
otp_service = OTPService()