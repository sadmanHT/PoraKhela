"""
Gamification URLs for Porakhela API
"""
from django.urls import path, include
from rest_framework.routers import DefaultRouter
from apps.gamification.views import (
    PorapointViewSet, AchievementViewSet, LeaderboardViewSet,
    RewardViewSet, DailyStreakViewSet
)

router = DefaultRouter()
router.register(r'points', PorapointViewSet, basename='points')
router.register(r'achievements', AchievementViewSet, basename='achievements')
router.register(r'leaderboard', LeaderboardViewSet, basename='leaderboard')
router.register(r'rewards', RewardViewSet, basename='rewards')
router.register(r'streaks', DailyStreakViewSet, basename='streaks')

urlpatterns = [
    path('', include(router.urls)),
]