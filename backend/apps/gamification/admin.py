"""
Django Admin Configuration for Gamification Models
"""
from django.contrib import admin
from apps.gamification.models import (
    PorapointLedger, Achievement, UserAchievement, 
    DailyStreak, Leaderboard, RewardCatalog, RewardRedemption
)


@admin.register(PorapointLedger)
class PorapointLedgerAdmin(admin.ModelAdmin):
    """Admin for PorapointLedger model"""
    list_display = ['child', 'change_amount', 'reason', 'balance_after', 'created_at']
    list_filter = ['reason', 'created_at']
    search_fields = ['child__user__first_name', 'child__user__last_name', 'idempotency_key', 'reference_id']
    readonly_fields = ['created_at']
    ordering = ['-created_at']
    
    fieldsets = (
        ('Transaction', {'fields': ('child', 'change_amount', 'reason', 'balance_after')}),
        ('Details', {'fields': ('reference_id', 'description', 'metadata')}),
        ('ACID Compliance', {'fields': ('idempotency_key',)}),
        ('Applink', {'fields': ('applink_transaction_id', 'applink_status')}),
        ('Timestamp', {'fields': ('created_at',)}),
    )
    
    def get_queryset(self, request):
        return super().get_queryset(request).select_related('child__user')


@admin.register(Achievement)
class AchievementAdmin(admin.ModelAdmin):
    """Admin for Achievement model"""
    list_display = ['name', 'achievement_type', 'reward_points', 'is_hidden', 'is_active', 'created_at']
    list_filter = ['achievement_type', 'is_hidden', 'is_active', 'created_at']
    search_fields = ['name', 'name_bn', 'description']
    readonly_fields = ['created_at']


@admin.register(UserAchievement)
class UserAchievementAdmin(admin.ModelAdmin):
    """Admin for UserAchievement model"""
    list_display = ['child', 'achievement', 'progress', 'is_completed', 'points_awarded', 'completed_at']
    list_filter = ['is_completed', 'achievement__achievement_type', 'completed_at', 'created_at']
    search_fields = ['child__user__first_name', 'child__user__last_name', 'achievement__name']
    readonly_fields = ['created_at', 'updated_at']
    
    def get_queryset(self, request):
        return super().get_queryset(request).select_related('child__user', 'achievement')


@admin.register(DailyStreak)
class DailyStreakAdmin(admin.ModelAdmin):
    """Admin for DailyStreak model"""
    list_display = ['child', 'current_streak', 'longest_streak', 'last_activity_date', 'weekly_goals_met']
    list_filter = ['last_activity_date', 'created_at']
    search_fields = ['child__user__first_name', 'child__user__last_name']
    readonly_fields = ['created_at', 'updated_at']
    
    def get_queryset(self, request):
        return super().get_queryset(request).select_related('child__user')


@admin.register(Leaderboard)
class LeaderboardAdmin(admin.ModelAdmin):
    """Admin for Leaderboard model"""
    list_display = ['child', 'leaderboard_type', 'period', 'rank', 'score', 'period_start', 'period_end']
    list_filter = ['leaderboard_type', 'period', 'period_start', 'created_at']
    search_fields = ['child__user__first_name', 'child__user__last_name']
    readonly_fields = ['created_at']
    ordering = ['leaderboard_type', 'period', 'rank']
    
    def get_queryset(self, request):
        return super().get_queryset(request).select_related('child__user')


@admin.register(RewardCatalog)
class RewardCatalogAdmin(admin.ModelAdmin):
    """Admin for RewardCatalog model"""
    list_display = ['name', 'reward_type', 'cost_points', 'value_amount', 'value_unit', 'is_active', 'stock_quantity']
    list_filter = ['reward_type', 'is_active', 'created_at']
    search_fields = ['name', 'name_bn', 'description']
    readonly_fields = ['created_at', 'updated_at']
    
    fieldsets = (
        ('Basic Info', {'fields': ('name', 'name_bn', 'description', 'reward_type')}),
        ('Pricing', {'fields': ('cost_points', 'value_amount', 'value_unit')}),
        ('Media', {'fields': ('image_url',)}),
        ('Availability', {'fields': ('is_active', 'stock_quantity', 'daily_limit_per_user')}),
        ('Applink', {'fields': ('applink_product_id',)}),
        ('Timestamps', {'fields': ('created_at', 'updated_at'), 'classes': ('collapse',)}),
    )


@admin.register(RewardRedemption)
class RewardRedemptionAdmin(admin.ModelAdmin):
    """Admin for RewardRedemption model"""
    list_display = ['child', 'reward', 'quantity', 'total_points_cost', 'status', 'delivery_phone', 'created_at']
    list_filter = ['status', 'reward__reward_type', 'created_at', 'processed_at', 'completed_at']
    search_fields = ['child__user__first_name', 'child__user__last_name', 'reward__name', 'delivery_phone']
    readonly_fields = ['created_at']
    
    fieldsets = (
        ('Redemption', {'fields': ('child', 'reward', 'quantity', 'total_points_cost', 'status')}),
        ('Delivery', {'fields': ('delivery_phone', 'delivery_details')}),
        ('Applink', {'fields': ('applink_transaction_id', 'applink_response')}),
        ('Timestamps', {'fields': ('created_at', 'processed_at', 'completed_at')}),
    )
    
    def get_queryset(self, request):
        return super().get_queryset(request).select_related('child__user', 'reward')