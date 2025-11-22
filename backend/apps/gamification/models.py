"""
Gamification Models for Porakhela
"""
from django.db import models
from django.conf import settings
from decimal import Decimal
import uuid


class PorapointLedger(models.Model):
    """
    ACID-compliant ledger for all Porapoint transactions with core requirements:
    child, change_amount, reason, idempotency_key, created_at
    """
    REASON_CHOICES = [
        ('lesson_complete', 'Lesson Completion'),
        ('redemption', 'Points Redemption'),
        ('quiz_score', 'Quiz Performance'),
        ('daily_login', 'Daily Login'),
        ('streak_bonus', 'Streak Bonus'),
        ('achievement', 'Achievement Unlock'),
        ('referral', 'Referral Bonus'),
        ('manual_adjustment', 'Manual Adjustment'),
        ('data_purchase', 'Data Purchase'),
        ('talktime_purchase', 'Talktime Purchase'),
        ('refund', 'Refund'),
        ('penalty', 'Penalty'),
        ('expired', 'Points Expired'),
    ]
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    child = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='porapoint_transactions', help_text="Child account for this transaction")
    change_amount = models.IntegerField(help_text="Amount of points changed (positive for earning, negative for spending)")
    reason = models.CharField(max_length=30, choices=REASON_CHOICES, help_text="Reason for the point change")
    idempotency_key = models.CharField(max_length=255, unique=True, help_text="Unique key to prevent duplicate transactions")
    created_at = models.DateTimeField(auto_now_add=True, help_text="When the transaction was created")
    
    # Additional ACID compliance and tracking fields
    balance_after = models.IntegerField(help_text="Running balance after this transaction")
    reference_id = models.CharField(max_length=100, blank=True, help_text="Reference to related object (lesson_id, achievement_id, etc.)")
    description = models.TextField(blank=True, help_text="Additional transaction details")
    metadata = models.JSONField(default=dict, help_text="Additional context data for the transaction")
    applink_transaction_id = models.CharField(max_length=100, blank=True, null=True)
    applink_status = models.CharField(max_length=20, default='pending')

    class Meta:
        db_table = 'porapoint_ledger'
        verbose_name = 'Porapoint Transaction'
        verbose_name_plural = 'Porapoint Transactions'
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['child', 'reason']),
            models.Index(fields=['created_at']),
            models.Index(fields=['applink_transaction_id']),
        ]

    def __str__(self):
        sign = '+' if self.change_amount > 0 else ''
        return f"{self.child.get_full_name()}: {sign}{self.change_amount} pts ({self.reason})"


class Achievement(models.Model):
    """
    Available achievements for gamification
    """
    ACHIEVEMENT_TYPE_CHOICES = [
        ('lesson_streak', 'Lesson Streak'),
        ('subject_mastery', 'Subject Mastery'),
        ('quiz_perfectionist', 'Quiz Perfect Score'),
        ('early_bird', 'Early Learner'),
        ('weekend_warrior', 'Weekend Learning'),
        ('speedster', 'Fast Learner'),
        ('persistent', 'Never Give Up'),
        ('explorer', 'Subject Explorer'),
    ]
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    
    # Achievement Details
    name = models.CharField(max_length=100)
    name_bn = models.CharField(max_length=100)
    description = models.TextField()
    description_bn = models.TextField()
    achievement_type = models.CharField(max_length=20, choices=ACHIEVEMENT_TYPE_CHOICES)
    
    # Visual
    icon_url = models.URLField(blank=True, null=True)
    badge_color = models.CharField(max_length=7, default='#FFD700')  # Hex color
    
    # Requirements
    requirements = models.JSONField(default=dict)  # Flexible requirement structure
    reward_points = models.IntegerField(default=50)
    
    # Metadata
    is_hidden = models.BooleanField(default=False)  # Hidden until unlocked
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'achievements'
        verbose_name = 'Achievement'
        verbose_name_plural = 'Achievements'

    def __str__(self):
        return self.name


class UserAchievement(models.Model):
    """
    Track achieved accomplishments for each child
    """
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    child = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='achievements')
    achievement = models.ForeignKey(Achievement, on_delete=models.CASCADE)
    
    progress = models.FloatField(default=0)  # 0-100 percentage
    is_completed = models.BooleanField(default=False)
    completed_at = models.DateTimeField(null=True, blank=True)
    points_awarded = models.IntegerField(default=0)
    
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'user_achievements'
        verbose_name = 'User Achievement'
        verbose_name_plural = 'User Achievements'
        unique_together = ['child', 'achievement']
        indexes = [
            models.Index(fields=['child', 'is_completed']),
            models.Index(fields=['completed_at']),
        ]

    def __str__(self):
        status = "✓" if self.is_completed else f"{self.progress}%"
        return f"{self.child.get_full_name()} - {self.achievement.name} ({status})"


class DailyStreak(models.Model):
    """
    Track daily learning streaks for each child
    """
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    child = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='daily_streaks')    # Streak Information
    current_streak = models.IntegerField(default=0)
    longest_streak = models.IntegerField(default=0)
    last_activity_date = models.DateField()
    
    # Weekly/Monthly tracking
    weekly_goals_met = models.IntegerField(default=0)
    monthly_goals_met = models.IntegerField(default=0)
    
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'daily_streaks'
        verbose_name = 'Daily Streak'
        verbose_name_plural = 'Daily Streaks'
        unique_together = ['child']

    def __str__(self):
        return f"{self.child.get_full_name()} - {self.current_streak} day streak"


class Leaderboard(models.Model):
    """
    Leaderboard rankings for different categories
    """
    LEADERBOARD_TYPE_CHOICES = [
        ('global_points', 'Global Points'),
        ('weekly_points', 'Weekly Points'),
        ('grade_points', 'Grade-wise Points'),
        ('subject_mastery', 'Subject Mastery'),
        ('streak_champion', 'Streak Champion'),
    ]
    
    PERIOD_CHOICES = [
        ('daily', 'Daily'),
        ('weekly', 'Weekly'),
        ('monthly', 'Monthly'),
        ('all_time', 'All Time'),
    ]
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    child = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='leaderboard_entries')
    
    # Leaderboard Details
    leaderboard_type = models.CharField(max_length=20, choices=LEADERBOARD_TYPE_CHOICES)
    period = models.CharField(max_length=10, choices=PERIOD_CHOICES)
    rank = models.IntegerField()
    score = models.IntegerField()  # Points, lessons completed, etc.
    
    # Metadata
    period_start = models.DateField()
    period_end = models.DateField()
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'leaderboards'
        verbose_name = 'Leaderboard Entry'
        verbose_name_plural = 'Leaderboard Entries'
        unique_together = ['child', 'leaderboard_type', 'period', 'period_start']
        indexes = [
            models.Index(fields=['leaderboard_type', 'period', 'rank']),
            models.Index(fields=['period_start', 'period_end']),
        ]

    def __str__(self):
        return f"#{self.rank} {self.child.get_full_name()} - {self.leaderboard_type} ({self.score})"


class RewardCatalog(models.Model):
    """
    Available rewards that can be redeemed with Porapoints
    """
    REWARD_TYPE_CHOICES = [
        ('data_bundle', 'Internet Data Bundle'),
        ('talktime', 'Mobile Talktime'),
        ('sms_bundle', 'SMS Bundle'),
        ('digital_content', 'Digital Content'),
        ('physical_reward', 'Physical Reward'),
    ]
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    
    # Reward Details
    name = models.CharField(max_length=100)
    name_bn = models.CharField(max_length=100)
    description = models.TextField()
    reward_type = models.CharField(max_length=20, choices=REWARD_TYPE_CHOICES)
    
    # Pricing
    cost_points = models.IntegerField()
    value_amount = models.DecimalField(max_digits=10, decimal_places=2)  # Taka value
    value_unit = models.CharField(max_length=20)  # GB, MB, Minutes, etc.
    
    # Visual
    image_url = models.URLField(blank=True, null=True)
    
    # Availability
    is_active = models.BooleanField(default=True)
    stock_quantity = models.IntegerField(null=True, blank=True)  # None for unlimited
    daily_limit_per_user = models.IntegerField(null=True, blank=True)
    
    # Applink Configuration
    applink_product_id = models.CharField(max_length=100, blank=True)
    
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'reward_catalog'
        verbose_name = 'Reward'
        verbose_name_plural = 'Reward Catalog'

    def __str__(self):
        return f"{self.name} - {self.cost_points} points"


class RewardRedemption(models.Model):
    """
    Track reward redemptions by users
    """
    STATUS_CHOICES = [
        ('pending', 'Pending'),
        ('processing', 'Processing'),
        ('completed', 'Completed'),
        ('failed', 'Failed'),
        ('cancelled', 'Cancelled'),
    ]
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    child = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='redemptions')
    reward = models.ForeignKey(RewardCatalog, on_delete=models.CASCADE)
    
    # Redemption Details
    quantity = models.IntegerField(default=1)
    total_points_cost = models.IntegerField()
    status = models.CharField(max_length=15, choices=STATUS_CHOICES, default='pending')
    
    # Delivery Information
    delivery_phone = models.CharField(max_length=15)  # Target phone for data/talktime
    delivery_details = models.JSONField(default=dict)
    
    # Applink Integration
    applink_transaction_id = models.CharField(max_length=100, blank=True, null=True)
    applink_response = models.JSONField(default=dict)
    
    # Timestamps
    created_at = models.DateTimeField(auto_now_add=True)
    processed_at = models.DateTimeField(null=True, blank=True)
    completed_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        db_table = 'reward_redemptions'
        verbose_name = 'Reward Redemption'
        verbose_name_plural = 'Reward Redemptions'
        indexes = [
            models.Index(fields=['child', 'status']),
            models.Index(fields=['created_at']),
            models.Index(fields=['applink_transaction_id']),
        ]

    def __str__(self):
        return f"{self.child.get_full_name()} - {self.reward.name} ({self.status})"