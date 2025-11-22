"""
User Models for Porakhela
"""
from django.contrib.auth.models import AbstractUser
from django.db import models
from django.core.validators import RegexValidator
import uuid


class User(AbstractUser):
    """
    Custom User model extending Django's AbstractUser
    Core requirements: phone_number (unique), is_parent, OTP_verified
    """
    USER_TYPE_CHOICES = [
        ('parent', 'Parent'),
        ('child', 'Child'),
    ]
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    phone_number = models.CharField(
        max_length=15, 
        unique=True,
        validators=[RegexValidator(regex=r'^(\+88)?01[3-9]\d{8}$', message='Enter a valid Bangladeshi phone number')]
    )
    is_parent = models.BooleanField(default=True, help_text="True if this is a parent account, False if child account")
    user_type = models.CharField(max_length=10, choices=USER_TYPE_CHOICES, default='parent', help_text="Type of user account")
    OTP_verified = models.BooleanField(default=False, help_text="Phone number verification status")
    banglalink_msisdn = models.CharField(max_length=15, blank=True, null=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    # Override username field to use phone_number
    USERNAME_FIELD = 'phone_number'
    REQUIRED_FIELDS = ['first_name', 'user_type']

    class Meta:
        db_table = 'users'
        verbose_name = 'User'
        verbose_name_plural = 'Users'

    def __str__(self):
        return f"{self.get_full_name()} ({self.phone_number})"


class ChildProfile(models.Model):
    """
    Child profile with core requirements: user (FK), grade, avatar, total_points
    """
    GRADE_CHOICES = [
        (1, 'Class 1'),
        (2, 'Class 2'),
        (3, 'Class 3'),
        (4, 'Class 4'),
        (5, 'Class 5'),
        (6, 'Class 6'),
        (7, 'Class 7'),
        (8, 'Class 8'),
        (9, 'Class 9'),
        (10, 'Class 10'),
    ]
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name='child_profile')
    grade = models.IntegerField(choices=GRADE_CHOICES, help_text="Student's current grade level")
    avatar = models.URLField(blank=True, null=True, help_text="Child's avatar image URL")
    total_points = models.IntegerField(default=0, help_text="Total porapoints accumulated by the child")
    
    # Additional tracking fields for comprehensive functionality
    parent = models.ForeignKey(User, on_delete=models.CASCADE, related_name='children', limit_choices_to={'is_parent': True})
    date_of_birth = models.DateField(null=True, blank=True)
    daily_screen_time_limit = models.IntegerField(default=60, help_text="Daily screen time limit in minutes")
    allowed_subjects = models.JSONField(default=list, help_text="List of subject IDs child is allowed to access")
    total_lessons_completed = models.IntegerField(default=0)
    current_streak = models.IntegerField(default=0)
    longest_streak = models.IntegerField(default=0)
    last_activity_date = models.DateField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'child_profiles'
        verbose_name = 'Child Profile'
        verbose_name_plural = 'Child Profiles'

    def __str__(self):
        return f"{self.user.get_full_name()} - Grade {self.grade}"


class ParentalPIN(models.Model):
    """
    PIN for parental authentication and controls
    """
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    parent = models.OneToOneField(User, on_delete=models.CASCADE, related_name='parental_pin')
    pin_hash = models.CharField(max_length=128)  # Hashed PIN
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'parental_pins'
        verbose_name = 'Parental PIN'
        verbose_name_plural = 'Parental PINs'

    def __str__(self):
        return f"PIN for {self.parent.get_full_name()}"


class OTPVerification(models.Model):
    """
    OTP verification for phone number validation
    """
    OTP_TYPE_CHOICES = [
        ('registration', 'Registration'),
        ('login', 'Login'),
        ('reset_password', 'Reset Password'),
    ]
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    phone_number = models.CharField(max_length=15)
    otp_code = models.CharField(max_length=6)
    otp_type = models.CharField(max_length=20, choices=OTP_TYPE_CHOICES)
    is_verified = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)
    expires_at = models.DateTimeField()

    class Meta:
        db_table = 'otp_verifications'
        verbose_name = 'OTP Verification'
        verbose_name_plural = 'OTP Verifications'
        indexes = [
            models.Index(fields=['phone_number', 'otp_type']),
            models.Index(fields=['expires_at']),
        ]

    def __str__(self):
        return f"OTP for {self.phone_number} - {self.otp_type}"