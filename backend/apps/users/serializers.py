"""
User Serializers for Porakhela API
"""
from rest_framework import serializers
from django.contrib.auth import get_user_model, authenticate
from django.contrib.auth.hashers import make_password
from apps.users.models import ChildProfile, OTPVerification
from utils.helpers import generate_otp, hash_pin, format_phone_number

User = get_user_model()


class UserSerializer(serializers.ModelSerializer):
    """Base user serializer"""
    
    class Meta:
        model = User
        fields = ['id', 'phone_number', 'first_name', 'last_name', 'user_type', 
                 'OTP_verified', 'banglalink_msisdn', 'created_at']
        read_only_fields = ['id', 'OTP_verified', 'created_at']


class RegisterSerializer(serializers.Serializer):
    """User registration serializer"""
    phone_number = serializers.CharField(max_length=15)
    first_name = serializers.CharField(max_length=30)
    last_name = serializers.CharField(max_length=30)
    user_type = serializers.ChoiceField(choices=User.USER_TYPE_CHOICES)
    password = serializers.CharField(write_only=True, min_length=6)
    banglalink_msisdn = serializers.CharField(max_length=15, required=False)
    
    def validate_phone_number(self, value):
        """Validate and format phone number"""
        formatted_phone = format_phone_number(value)
        if User.objects.filter(phone_number=formatted_phone).exists():
            raise serializers.ValidationError("User with this phone number already exists.")
        return formatted_phone
    
    def create(self, validated_data):
        """Create new user"""
        password = validated_data.pop('password')
        user = User.objects.create(**validated_data)
        user.set_password(password)
        user.save()
        return user


class LoginSerializer(serializers.Serializer):
    """User login serializer"""
    phone_number = serializers.CharField()
    password = serializers.CharField()
    
    def validate(self, attrs):
        phone_number = format_phone_number(attrs['phone_number'])
        password = attrs['password']
        
        user = authenticate(username=phone_number, password=password)
        if not user:
            raise serializers.ValidationError("Invalid credentials.")
        
        if not user.OTP_verified:
            raise serializers.ValidationError("Phone number not verified.")
        
        attrs['user'] = user
        return attrs


class OTPRequestSerializer(serializers.Serializer):
    """OTP request serializer"""
    phone_number = serializers.CharField(max_length=15)
    otp_type = serializers.ChoiceField(choices=OTPVerification.OTP_TYPE_CHOICES)
    
    def validate_phone_number(self, value):
        return format_phone_number(value)


class OTPVerifySerializer(serializers.Serializer):
    """OTP verification serializer"""
    phone_number = serializers.CharField(max_length=15)
    otp_code = serializers.CharField(max_length=6)
    otp_type = serializers.ChoiceField(choices=OTPVerification.OTP_TYPE_CHOICES)


class ChildProfileSerializer(serializers.ModelSerializer):
    """Child profile serializer"""
    user = UserSerializer(read_only=True)
    parent_name = serializers.CharField(source='parent.get_full_name', read_only=True)
    
    class Meta:
        model = ChildProfile
        fields = ['id', 'user', 'parent_name', 'grade', 'date_of_birth', 
                 'avatar', 'daily_screen_time_limit', 'allowed_subjects',
                 'total_lessons_completed', 'total_points', 
                 'current_streak', 'longest_streak', 'last_activity_date']
        read_only_fields = ['id', 'total_lessons_completed', 'total_points',
                           'current_streak', 'longest_streak', 'last_activity_date']


class CreateChildProfileSerializer(serializers.ModelSerializer):
    """Create child profile with user"""
    first_name = serializers.CharField(max_length=30)
    last_name = serializers.CharField(max_length=30)
    phone_number = serializers.CharField(max_length=15)
    password = serializers.CharField(write_only=True, min_length=6)
    parental_pin = serializers.CharField(write_only=True, min_length=4, max_length=6)
    
    class Meta:
        model = ChildProfile
        fields = ['first_name', 'last_name', 'phone_number', 'password',
                 'grade', 'date_of_birth', 'avatar', 'daily_screen_time_limit',
                 'allowed_subjects', 'parental_pin']
    
    def validate_phone_number(self, value):
        formatted_phone = format_phone_number(value)
        if User.objects.filter(phone_number=formatted_phone).exists():
            raise serializers.ValidationError("User with this phone number already exists.")
        return formatted_phone
    
    def create(self, validated_data):
        # Extract user data
        user_data = {
            'phone_number': validated_data.pop('phone_number'),
            'first_name': validated_data.pop('first_name'),
            'last_name': validated_data.pop('last_name'),
            'user_type': 'child'
        }
        password = validated_data.pop('password')
        parental_pin = validated_data.pop('parental_pin')
        
        # Create child user
        child_user = User.objects.create(**user_data)
        child_user.set_password(password)
        child_user.save()
        
        # Create child profile
        parent = self.context['request'].user
        child_profile = ChildProfile.objects.create(
            user=child_user,
            parent=parent,
            **validated_data
        )
        
        # Create parental PIN if not exists
        from apps.users.models import ParentalPIN
        ParentalPIN.objects.get_or_create(
            parent=parent,
            defaults={'pin_hash': hash_pin(parental_pin)}
        )
        
        return child_profile


class ParentalPINSerializer(serializers.Serializer):
    """Parental PIN verification"""
    pin = serializers.CharField(min_length=4, max_length=6)
    
    def validate_pin(self, value):
        user = self.context['request'].user
        try:
            parental_pin = user.parental_pin
            from utils.helpers import verify_pin
            if not verify_pin(value, parental_pin.pin_hash):
                raise serializers.ValidationError("Invalid PIN.")
        except:
            raise serializers.ValidationError("Parental PIN not set.")
        return value