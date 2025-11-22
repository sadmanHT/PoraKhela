"""
USSD Models for Porakhela Parental Controls

Manages USSD sessions and parent interactions for feature phone access.
"""

from django.db import models
from django.contrib.auth import get_user_model
import uuid

User = get_user_model()


class USSDSession(models.Model):
    """
    Track USSD sessions for state management across multiple interactions.
    """
    session_id = models.CharField(max_length=100, unique=True, help_text="Telecom provider session ID")
    phone_number = models.CharField(max_length=20, help_text="Parent's phone number")
    parent = models.ForeignKey(User, on_delete=models.SET_NULL, null=True, blank=True, help_text="Associated parent user")
    current_state = models.CharField(max_length=50, default='main_menu', help_text="Current menu state")
    session_data = models.JSONField(default=dict, help_text="Session-specific data storage")
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    is_active = models.BooleanField(default=True)
    
    class Meta:
        db_table = 'ussd_sessions'
        verbose_name = 'USSD Session'
        verbose_name_plural = 'USSD Sessions'
        ordering = ['-updated_at']
    
    def __str__(self):
        return f"USSD Session {self.session_id} - {self.phone_number}"


class USSDLog(models.Model):
    """
    Log all USSD interactions for debugging and analytics.
    """
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    session_id = models.CharField(max_length=100, help_text="Telecom provider session ID")
    phone_number = models.CharField(max_length=20, help_text="Parent's phone number")
    user_input = models.CharField(max_length=255, blank=True, help_text="User's menu selection")
    menu_state = models.CharField(max_length=50, help_text="Current menu state")
    response_sent = models.TextField(help_text="USSD response sent to user")
    created_at = models.DateTimeField(auto_now_add=True)
    
    class Meta:
        db_table = 'ussd_logs'
        verbose_name = 'USSD Log'
        verbose_name_plural = 'USSD Logs'
        ordering = ['-created_at']
    
    def __str__(self):
        return f"USSD Log {self.session_id} - {self.menu_state}"


class RedemptionRequest(models.Model):
    """
    Track pending redemption requests that need parental approval.
    """
    REDEMPTION_TYPES = [
        ('data', 'Mobile Data'),
        ('talktime', 'Talk Time'),
        ('sms', 'SMS Package'),
        ('reward', 'Physical Reward'),
    ]
    
    STATUS_CHOICES = [
        ('pending', 'Pending Approval'),
        ('approved', 'Approved'),
        ('rejected', 'Rejected'),
        ('redeemed', 'Redeemed'),
    ]
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    child = models.ForeignKey(User, on_delete=models.CASCADE, related_name='redemption_requests', help_text="Child requesting redemption")
    parent = models.ForeignKey(User, on_delete=models.CASCADE, related_name='child_redemption_requests', help_text="Parent for approval")
    redemption_type = models.CharField(max_length=20, choices=REDEMPTION_TYPES, help_text="Type of redemption")
    item_name = models.CharField(max_length=100, help_text="Name of the item being redeemed")
    points_required = models.IntegerField(help_text="Porapoints required for redemption")
    description = models.TextField(blank=True, help_text="Additional redemption details")
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='pending', help_text="Current approval status")
    requested_at = models.DateTimeField(auto_now_add=True)
    approved_at = models.DateTimeField(null=True, blank=True)
    approved_via_ussd = models.BooleanField(default=False, help_text="Whether approved through USSD")
    ussd_session_id = models.CharField(max_length=100, blank=True, help_text="USSD session used for approval")
    
    class Meta:
        db_table = 'redemption_requests'
        verbose_name = 'Redemption Request'
        verbose_name_plural = 'Redemption Requests'
        ordering = ['-requested_at']
    
    def __str__(self):
        return f"{self.child.first_name} - {self.item_name} ({self.points_required} pts)"


class ScreenTimeSetting(models.Model):
    """
    Parent-controlled screen time limits per child.
    """
    LIMIT_TYPES = [
        ('daily', 'Daily Limit'),
        ('weekly', 'Weekly Limit'),
        ('session', 'Per Session Limit'),
    ]
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    child = models.ForeignKey(User, on_delete=models.CASCADE, related_name='screen_time_settings', help_text="Child user")
    parent = models.ForeignKey(User, on_delete=models.CASCADE, related_name='child_screen_time_settings', help_text="Parent setting the limit")
    limit_type = models.CharField(max_length=20, choices=LIMIT_TYPES, default='daily', help_text="Type of time limit")
    limit_minutes = models.IntegerField(help_text="Time limit in minutes")
    is_active = models.BooleanField(default=True, help_text="Whether the limit is currently active")
    set_via_ussd = models.BooleanField(default=False, help_text="Whether set through USSD")
    ussd_session_id = models.CharField(max_length=100, blank=True, help_text="USSD session used for setting")
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        db_table = 'screen_time_settings'
        verbose_name = 'Screen Time Setting'
        verbose_name_plural = 'Screen Time Settings'
        ordering = ['-updated_at']
        unique_together = ['child', 'limit_type', 'is_active']
    
    def __str__(self):
        return f"{self.child.first_name} - {self.limit_minutes}min {self.limit_type}"