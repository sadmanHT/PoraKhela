"""
Gamification Serializers for Porakhela API
"""
from rest_framework import serializers
from apps.gamification.models import (
    PorapointLedger, Achievement, UserAchievement, 
    DailyStreak, Leaderboard, RewardCatalog, RewardRedemption
)


class PorapointLedgerSerializer(serializers.ModelSerializer):
    """Porapoint transaction serializer"""
    child_name = serializers.CharField(source='child.get_full_name', read_only=True)
    
    class Meta:
        model = PorapointLedger
        fields = ['id', 'child_name', 'transaction_type', 'source_type', 
                 'amount', 'balance_after', 'description', 'created_at']
        read_only_fields = ['id', 'balance_after', 'created_at']


class AchievementSerializer(serializers.ModelSerializer):
    """Achievement serializer"""
    
    class Meta:
        model = Achievement
        fields = ['id', 'name', 'name_bn', 'description', 'description_bn',
                 'achievement_type', 'icon_url', 'badge_color', 'reward_points',
                 'is_hidden']


class UserAchievementSerializer(serializers.ModelSerializer):
    """User achievement progress serializer"""
    achievement = AchievementSerializer(read_only=True)
    child_name = serializers.CharField(source='child.get_full_name', read_only=True)
    
    class Meta:
        model = UserAchievement
        fields = ['id', 'child_name', 'achievement', 'progress', 'is_completed',
                 'completed_at', 'points_awarded', 'created_at']
        read_only_fields = ['id', 'points_awarded', 'created_at', 'completed_at']


class DailyStreakSerializer(serializers.ModelSerializer):
    """Daily streak serializer"""
    child_name = serializers.CharField(source='child.get_full_name', read_only=True)
    
    class Meta:
        model = DailyStreak
        fields = ['id', 'child_name', 'current_streak', 'longest_streak',
                 'last_activity_date', 'weekly_goals_met', 'monthly_goals_met']
        read_only_fields = ['id', 'updated_at']


class LeaderboardSerializer(serializers.ModelSerializer):
    """Leaderboard serializer"""
    child_name = serializers.CharField(source='child.get_full_name', read_only=True)
    
    class Meta:
        model = Leaderboard
        fields = ['id', 'child_name', 'leaderboard_type', 'period', 'rank',
                 'score', 'period_start', 'period_end']
        read_only_fields = ['id', 'created_at']


class RewardCatalogSerializer(serializers.ModelSerializer):
    """Reward catalog serializer"""
    
    class Meta:
        model = RewardCatalog
        fields = ['id', 'name', 'name_bn', 'description', 'reward_type',
                 'cost_points', 'value_amount', 'value_unit', 'image_url',
                 'is_active', 'stock_quantity', 'daily_limit_per_user']


class RewardRedemptionSerializer(serializers.ModelSerializer):
    """Reward redemption serializer"""
    reward = RewardCatalogSerializer(read_only=True)
    child_name = serializers.CharField(source='child.get_full_name', read_only=True)
    
    class Meta:
        model = RewardRedemption
        fields = ['id', 'child_name', 'reward', 'quantity', 'total_points_cost',
                 'status', 'delivery_phone', 'created_at', 'completed_at']
        read_only_fields = ['id', 'total_points_cost', 'created_at', 'completed_at']


class RedeemRewardSerializer(serializers.Serializer):
    """Redeem reward serializer"""
    reward_id = serializers.UUIDField()
    quantity = serializers.IntegerField(min_value=1, default=1)
    delivery_phone = serializers.CharField(max_length=15)
    
    def validate_delivery_phone(self, value):
        from utils.helpers import format_phone_number
        return format_phone_number(value)


class PointsBalanceSerializer(serializers.Serializer):
    """Points balance response serializer"""
    current_balance = serializers.IntegerField()
    total_earned = serializers.IntegerField()
    total_redeemed = serializers.IntegerField()
    pending_redemptions = serializers.IntegerField()