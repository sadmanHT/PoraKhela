"""
Authentication Serializers for OTP-based Phone Authentication
"""
from rest_framework import serializers
from django.core.validators import RegexValidator
from apps.users.models import User, ChildProfile


class RequestOTPSerializer(serializers.Serializer):
    """
    Serializer for OTP request endpoint.
    Validates phone number and triggers OTP generation.
    """
    phone_number = serializers.CharField(
        max_length=15,
        validators=[
            RegexValidator(
                regex=r'^(\+88)?01[3-9]\d{8}$',
                message='Enter a valid Bangladeshi phone number (format: 01XXXXXXXXX or +8801XXXXXXXXX)'
            )
        ],
        help_text="Bangladeshi phone number"
    )

    def validate_phone_number(self, value):
        """Normalize phone number format."""
        # Remove +88 prefix if present
        if value.startswith('+88'):
            value = value[3:]
        
        # Ensure it starts with 01
        if not value.startswith('01'):
            raise serializers.ValidationError("Phone number must start with 01")
        
        return value


class VerifyOTPSerializer(serializers.Serializer):
    """
    Serializer for OTP verification endpoint.
    Validates phone number and OTP code.
    """
    phone_number = serializers.CharField(
        max_length=15,
        validators=[
            RegexValidator(
                regex=r'^(\+88)?01[3-9]\d{8}$',
                message='Enter a valid Bangladeshi phone number'
            )
        ]
    )
    otp = serializers.CharField(
        min_length=6,
        max_length=6,
        validators=[
            RegexValidator(
                regex=r'^\d{6}$',
                message='OTP must be exactly 6 digits'
            )
        ],
        help_text="6-digit OTP code"
    )

    def validate_phone_number(self, value):
        """Normalize phone number format."""
        if value.startswith('+88'):
            value = value[3:]
        if not value.startswith('01'):
            raise serializers.ValidationError("Phone number must start with 01")
        return value


class AuthenticationResponseSerializer(serializers.Serializer):
    """
    Serializer for successful authentication response.
    """
    token = serializers.CharField(help_text="JWT access token")
    refresh = serializers.CharField(help_text="JWT refresh token")
    is_parent = serializers.BooleanField(help_text="Whether user is a parent")
    user_id = serializers.UUIDField(help_text="User's unique identifier")
    phone_number = serializers.CharField(help_text="User's phone number")
    has_children = serializers.BooleanField(help_text="Whether parent has linked children")
    children_count = serializers.IntegerField(help_text="Number of linked children")


class CreateChildProfileSerializer(serializers.ModelSerializer):
    """
    Serializer for creating child profiles.
    Used by parents to create child accounts.
    """
    name = serializers.CharField(max_length=150, help_text="Child's full name")
    first_name = serializers.CharField(max_length=30, required=False)  # For compatibility
    last_name = serializers.CharField(max_length=30, required=False)   # For compatibility
    phone_number = serializers.CharField(max_length=15, help_text="Child's phone number")
    password = serializers.CharField(write_only=True, min_length=6, help_text="Child's password")
    grade = serializers.ChoiceField(
        choices=ChildProfile.GRADE_CHOICES,
        help_text="Child's current grade level (1-10)"
    )
    avatar = serializers.CharField(
        max_length=50,
        required=False,
        help_text="Avatar identifier (e.g., 'lion', 'tiger', 'elephant')"
    )
    date_of_birth = serializers.DateField(
        required=False,
        help_text="Child's date of birth (YYYY-MM-DD)"
    )
    parental_pin = serializers.CharField(write_only=True, min_length=4, max_length=6, required=False)

    class Meta:
        model = ChildProfile
        fields = ['name', 'first_name', 'last_name', 'phone_number', 'password', 'grade', 'avatar', 'date_of_birth', 'parental_pin']

    def validate_name(self, value):
        """Validate child name."""
        if len(value.strip()) < 2:
            raise serializers.ValidationError("Name must be at least 2 characters long")
        return value.strip().title()

    def validate_phone_number(self, value):
        """Validate phone number format and uniqueness."""
        from utils.helpers import format_phone_number
        formatted_phone = format_phone_number(value)
        if User.objects.filter(phone_number=formatted_phone).exists():
            raise serializers.ValidationError("User with this phone number already exists.")
        return formatted_phone

    def create(self, validated_data):
        """
        Create child user account and profile.
        Links the child to the authenticated parent.
        """
        parent = self.context['request'].user
        name = validated_data.pop('name')
        phone_number = validated_data.pop('phone_number')
        password = validated_data.pop('password')
        
        # Handle both 'name' and 'first_name'/'last_name' fields for compatibility
        if 'first_name' in validated_data:
            first_name = validated_data.pop('first_name')
        else:
            first_name = name.split()[0]
            
        if 'last_name' in validated_data:
            last_name = validated_data.pop('last_name') 
        else:
            last_name = ' '.join(name.split()[1:]) if len(name.split()) > 1 else ''
        
        # Remove parental_pin from validated_data if present
        parental_pin = validated_data.pop('parental_pin', None)
        
        # Create child user account with real phone number
        child_user = User.objects.create(
            username=phone_number,  # Use phone number as username
            first_name=first_name,
            last_name=last_name,
            phone_number=phone_number,  # Use real phone number
            is_parent=False,
            user_type='child',
            OTP_verified=True  # Child inherits parent's verification
        )
        child_user.set_password(password)
        child_user.save()
        
        # Create child profile
        child_profile = ChildProfile.objects.create(
            user=child_user,
            parent=parent,
            **validated_data
        )
        
        return child_profile


class ChildProfileResponseSerializer(serializers.ModelSerializer):
    """
    Serializer for child profile response data.
    """
    name = serializers.SerializerMethodField()
    user_id = serializers.UUIDField(source='user.id')

    class Meta:
        model = ChildProfile
        fields = [
            'user_id', 'name', 'grade', 'avatar', 'total_points',
            'total_lessons_completed', 'current_streak', 'longest_streak'
        ]

    def get_name(self, obj):
        return obj.user.get_full_name()