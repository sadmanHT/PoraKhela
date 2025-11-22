"""
Utility functions for Porakhela backend
"""
import random
import string
import hashlib
from datetime import datetime, timedelta
from django.core.cache import cache
from django.contrib.auth.hashers import make_password, check_password


def generate_otp(length: int = 6) -> str:
    """Generate a random OTP code"""
    return ''.join(random.choices(string.digits, k=length))


def generate_referral_code(user_id: str) -> str:
    """Generate a unique referral code for a user"""
    timestamp = str(int(datetime.now().timestamp()))
    raw_string = f"{user_id}_{timestamp}"
    hash_object = hashlib.md5(raw_string.encode())
    return hash_object.hexdigest()[:8].upper()


def hash_pin(pin: str) -> str:
    """Hash a parental PIN for secure storage"""
    return make_password(pin)


def verify_pin(pin: str, hashed_pin: str) -> bool:
    """Verify a PIN against its hash"""
    return check_password(pin, hashed_pin)


def cache_user_session(user_id: str, session_data: dict, timeout: int = 3600):
    """Cache user session data"""
    cache_key = f"user_session:{user_id}"
    cache.set(cache_key, session_data, timeout)


def get_cached_user_session(user_id: str) -> dict:
    """Get cached user session data"""
    cache_key = f"user_session:{user_id}"
    return cache.get(cache_key, {})


def clear_user_session(user_id: str):
    """Clear cached user session"""
    cache_key = f"user_session:{user_id}"
    cache.delete(cache_key)


def format_phone_number(phone: str) -> str:
    """Normalize phone number format for Bangladesh"""
    # Remove all non-digits
    digits_only = ''.join(filter(str.isdigit, phone))
    
    # Handle different formats
    if digits_only.startswith('88'):
        return f"+{digits_only}"
    elif digits_only.startswith('01'):
        return f"+88{digits_only}"
    else:
        return phone  # Return as-is if unknown format


def calculate_age_from_dob(date_of_birth) -> int:
    """Calculate age from date of birth"""
    today = datetime.now().date()
    return today.year - date_of_birth.year - ((today.month, today.day) < (date_of_birth.month, date_of_birth.day))


def generate_lesson_download_url(lesson_id: str, user_id: str) -> str:
    """Generate a secure download URL for lesson content"""
    timestamp = int(datetime.now().timestamp())
    expire_time = timestamp + 3600  # 1 hour expiry
    
    # Create signature
    raw_string = f"{lesson_id}_{user_id}_{expire_time}"
    signature = hashlib.sha256(raw_string.encode()).hexdigest()[:16]
    
    return f"/api/v1/lessons/{lesson_id}/download?user={user_id}&expires={expire_time}&sig={signature}"


def validate_lesson_download_url(lesson_id: str, user_id: str, expires: str, signature: str) -> bool:
    """Validate a lesson download URL"""
    try:
        expire_time = int(expires)
        current_time = int(datetime.now().timestamp())
        
        # Check expiry
        if current_time > expire_time:
            return False
        
        # Verify signature
        raw_string = f"{lesson_id}_{user_id}_{expire_time}"
        expected_signature = hashlib.sha256(raw_string.encode()).hexdigest()[:16]
        
        return signature == expected_signature
    except (ValueError, TypeError):
        return False


def time_until_next_reward(last_reward_time: datetime) -> timedelta:
    """Calculate time until next daily reward"""
    next_reward_time = last_reward_time.replace(hour=0, minute=0, second=0, microsecond=0) + timedelta(days=1)
    current_time = datetime.now()
    
    if current_time >= next_reward_time:
        return timedelta(0)
    else:
        return next_reward_time - current_time