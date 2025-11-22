"""
Django Admin Configuration for User Models
"""
from django.contrib import admin
from django.contrib.auth.admin import UserAdmin
from apps.users.models import User, ChildProfile, ParentalPIN, OTPVerification


@admin.register(User)
class CustomUserAdmin(UserAdmin):
    """Custom admin for User model"""
    list_display = ['phone_number', 'first_name', 'last_name', 'user_type', 'is_parent', 'OTP_verified', 'created_at']
    list_filter = ['user_type', 'is_parent', 'OTP_verified', 'is_staff', 'is_superuser', 'created_at']
    search_fields = ['phone_number', 'first_name', 'last_name', 'username']
    ordering = ['-created_at']
    
    fieldsets = (
        (None, {'fields': ('phone_number', 'password')}),
        ('Personal info', {'fields': ('first_name', 'last_name', 'email', 'user_type')}),
        ('Verification', {'fields': ('OTP_verified', 'banglalink_msisdn')}),
        ('Permissions', {
            'fields': ('is_active', 'is_staff', 'is_superuser', 'groups', 'user_permissions'),
            'classes': ('collapse',)
        }),
        ('Important dates', {'fields': ('last_login', 'date_joined')}),
    )
    
    add_fieldsets = (
        (None, {
            'classes': ('wide',),
            'fields': ('phone_number', 'password1', 'password2', 'user_type'),
        }),
    )


@admin.register(ChildProfile)
class ChildProfileAdmin(admin.ModelAdmin):
    """Admin for ChildProfile model"""
    list_display = ['user', 'parent', 'grade', 'total_points', 'current_streak', 'total_lessons_completed', 'last_activity_date']
    list_filter = ['grade', 'parent', 'created_at']
    search_fields = ['user__first_name', 'user__last_name', 'user__phone_number', 'parent__first_name', 'parent__last_name']
    readonly_fields = ['total_lessons_completed', 'created_at', 'updated_at']
    
    fieldsets = (
        ('Basic Info', {'fields': ('user', 'parent', 'grade', 'date_of_birth')}),
        ('Profile', {'fields': ('avatar', 'total_points')}),
        ('Learning', {'fields': ('allowed_subjects', 'total_lessons_completed')}),
        ('Streaks', {'fields': ('current_streak', 'longest_streak', 'last_activity_date')}),
        ('Controls', {'fields': ('daily_screen_time_limit',)}),
        ('Timestamps', {'fields': ('created_at', 'updated_at'), 'classes': ('collapse',)}),
    )


@admin.register(ParentalPIN)
class ParentalPINAdmin(admin.ModelAdmin):
    """Admin for ParentalPIN model"""
    list_display = ['parent', 'created_at', 'updated_at']
    list_filter = ['created_at']
    search_fields = ['parent__first_name', 'parent__last_name', 'parent__phone_number']
    readonly_fields = ['pin_hash', 'created_at', 'updated_at']


@admin.register(OTPVerification)
class OTPVerificationAdmin(admin.ModelAdmin):
    """Admin for OTPVerification model"""
    list_display = ['phone_number', 'otp_type', 'is_verified', 'created_at', 'expires_at']
    list_filter = ['otp_type', 'is_verified', 'created_at', 'expires_at']
    search_fields = ['phone_number']
    readonly_fields = ['created_at']
    
    def get_queryset(self, request):
        return super().get_queryset(request).order_by('-created_at')