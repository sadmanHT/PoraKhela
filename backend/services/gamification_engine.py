"""
Gamification Rules Engine for Porakhela
Handles point calculations, achievement tracking, and reward logic
"""
from typing import Dict, List, Optional
from datetime import datetime, timedelta, date
from django.contrib.auth import get_user_model
from django.db import models
from apps.lessons.models import LessonProgress, QuizAttempt
from apps.gamification.models import (
    PorapointLedger, Achievement, UserAchievement, 
    DailyStreak, Leaderboard
)
from django.conf import settings
import logging

User = get_user_model()
logger = logging.getLogger(__name__)


class GamificationEngine:
    """
    Central engine for all gamification logic
    """
    
    def __init__(self):
        self.base_points_per_lesson = settings.PORAPOINTS_PER_LESSON
        self.streak_multiplier = settings.STREAK_BONUS_MULTIPLIER
        self.daily_login_bonus = settings.DAILY_LOGIN_BONUS
    
    def calculate_lesson_points(self, child: User, lesson_progress: LessonProgress) -> int:
        """
        Calculate points earned for completing a lesson
        Factors in quiz performance, streak bonuses, etc.
        """
        base_points = lesson_progress.lesson.porapoints_reward or self.base_points_per_lesson
        multiplier = 1.0
        
        # Quiz performance bonus
        if lesson_progress.quiz_score:
            if lesson_progress.quiz_score >= 90:
                multiplier += 0.5  # 50% bonus for 90%+ score
            elif lesson_progress.quiz_score >= 80:
                multiplier += 0.3  # 30% bonus for 80%+ score
            elif lesson_progress.quiz_score >= 70:
                multiplier += 0.1  # 10% bonus for 70%+ score
        
        # Streak bonus
        streak = self.get_current_streak(child)
        if streak >= 7:
            multiplier *= self.streak_multiplier
        
        # First attempt bonus
        if lesson_progress.attempts == 1:
            multiplier += 0.2  # 20% bonus for first attempt completion
        
        final_points = int(base_points * multiplier)
        logger.info(f"Lesson points for {child}: {base_points} × {multiplier:.2f} = {final_points}")
        
        return final_points
    
    def award_points(self, child: User, amount: int, source_type: str, 
                     reference_id: str = "", description: str = "", 
                     metadata: Dict = None) -> PorapointLedger:
        """
        Award points to a child and update their balance
        """
        if metadata is None:
            metadata = {}
        
        # Get current balance
        last_transaction = PorapointLedger.objects.filter(child=child).first()
        current_balance = last_transaction.balance_after if last_transaction else 0
        new_balance = current_balance + amount
        
        # Create transaction record
        transaction = PorapointLedger.objects.create(
            child=child,
            transaction_type='earned',
            source_type=source_type,
            amount=amount,
            balance_after=new_balance,
            reference_id=reference_id,
            description=description,
            metadata=metadata
        )
        
        logger.info(f"Awarded {amount} points to {child} for {source_type}")
        return transaction
    
    def deduct_points(self, child: User, amount: int, source_type: str,
                      reference_id: str = "", description: str = "",
                      metadata: Dict = None) -> Optional[PorapointLedger]:
        """
        Deduct points from a child's balance (for redemptions)
        Returns None if insufficient balance
        """
        if metadata is None:
            metadata = {}
        
        # Get current balance
        last_transaction = PorapointLedger.objects.filter(child=child).first()
        current_balance = last_transaction.balance_after if last_transaction else 0
        
        if current_balance < amount:
            logger.warning(f"Insufficient balance for {child}: {current_balance} < {amount}")
            return None
        
        new_balance = current_balance - amount
        
        # Create transaction record
        transaction = PorapointLedger.objects.create(
            child=child,
            transaction_type='redeemed',
            source_type=source_type,
            amount=-amount,  # Negative for deduction
            balance_after=new_balance,
            reference_id=reference_id,
            description=description,
            metadata=metadata
        )
        
        logger.info(f"Deducted {amount} points from {child} for {source_type}")
        return transaction
    
    def get_current_balance(self, child: User) -> int:
        """Get current Porapoint balance for a child"""
        last_transaction = PorapointLedger.objects.filter(child=child).first()
        return last_transaction.balance_after if last_transaction else 0
    
    def update_daily_streak(self, child: User) -> DailyStreak:
        """
        Update daily learning streak for a child
        """
        today = date.today()
        streak_obj, created = DailyStreak.objects.get_or_create(child=child)
        
        if created or not streak_obj.last_activity_date:
            # First activity
            streak_obj.current_streak = 1
            streak_obj.longest_streak = 1
            streak_obj.last_activity_date = today
        else:
            last_date = streak_obj.last_activity_date
            days_diff = (today - last_date).days
            
            if days_diff == 1:
                # Consecutive day
                streak_obj.current_streak += 1
                if streak_obj.current_streak > streak_obj.longest_streak:
                    streak_obj.longest_streak = streak_obj.current_streak
            elif days_diff == 0:
                # Same day - no change
                pass
            else:
                # Streak broken
                streak_obj.current_streak = 1
            
            streak_obj.last_activity_date = today
        
        streak_obj.save()
        
        # Award daily login bonus if it's a new day
        if not created and (today - (streak_obj.last_activity_date or today)).days >= 1:
            self.award_points(
                child=child,
                amount=self.daily_login_bonus,
                source_type='daily_login',
                description=f"Daily login bonus - Day {streak_obj.current_streak}"
            )
        
        return streak_obj
    
    def get_current_streak(self, child: User) -> int:
        """Get current streak count for a child"""
        try:
            streak_obj = DailyStreak.objects.get(child=child)
            return streak_obj.current_streak
        except DailyStreak.DoesNotExist:
            return 0
    
    def check_achievements(self, child: User) -> List[UserAchievement]:
        """
        Check and unlock new achievements for a child
        Returns list of newly unlocked achievements
        """
        newly_unlocked = []
        achievements = Achievement.objects.filter(is_active=True)
        
        for achievement in achievements:
            user_achievement, created = UserAchievement.objects.get_or_create(
                child=child,
                achievement=achievement,
                defaults={'progress': 0}
            )
            
            if user_achievement.is_completed:
                continue  # Already unlocked
            
            # Calculate progress based on achievement type
            progress = self._calculate_achievement_progress(child, achievement)
            user_achievement.progress = min(progress, 100.0)
            
            # Check if completed
            if progress >= 100 and not user_achievement.is_completed:
                user_achievement.is_completed = True
                user_achievement.completed_at = datetime.now()
                user_achievement.points_awarded = achievement.reward_points
                
                # Award points
                self.award_points(
                    child=child,
                    amount=achievement.reward_points,
                    source_type='achievement',
                    reference_id=str(achievement.id),
                    description=f"Achievement unlocked: {achievement.name}"
                )
                
                newly_unlocked.append(user_achievement)
                logger.info(f"Achievement unlocked: {child} - {achievement.name}")
            
            user_achievement.save()
        
        return newly_unlocked
    
    def _calculate_achievement_progress(self, child: User, achievement: Achievement) -> float:
        """
        Calculate progress percentage for a specific achievement
        """
        req = achievement.requirements
        
        if achievement.achievement_type == 'lesson_streak':
            target_days = req.get('days', 7)
            current_streak = self.get_current_streak(child)
            return min((current_streak / target_days) * 100, 100)
        
        elif achievement.achievement_type == 'subject_mastery':
            subject_id = req.get('subject_id')
            target_lessons = req.get('lessons_count', 10)
            completed = LessonProgress.objects.filter(
                child=child,
                lesson__chapter__subject_id=subject_id,
                status='completed'
            ).count()
            return min((completed / target_lessons) * 100, 100)
        
        elif achievement.achievement_type == 'quiz_perfectionist':
            target_perfect = req.get('perfect_scores', 5)
            perfect_scores = QuizAttempt.objects.filter(
                child=child,
                score=100
            ).count()
            return min((perfect_scores / target_perfect) * 100, 100)
        
        elif achievement.achievement_type == 'early_bird':
            # Learning before 8 AM
            target_days = req.get('days', 5)
            # This would need timezone-aware checking in real implementation
            return 0  # Placeholder
        
        # Add more achievement types as needed
        return 0
    
    def update_leaderboards(self, child: User):
        """
        Update leaderboard positions for the child
        """
        today = date.today()
        
        # Weekly leaderboard
        week_start = today - timedelta(days=today.weekday())
        week_points = PorapointLedger.objects.filter(
            child=child,
            transaction_type='earned',
            created_at__date__gte=week_start
        ).aggregate(total=models.Sum('amount'))['total'] or 0
        
        # Update or create weekly entry
        Leaderboard.objects.update_or_create(
            child=child,
            leaderboard_type='weekly_points',
            period='weekly',
            period_start=week_start,
            defaults={'score': week_points}
        )
        
        # Monthly leaderboard
        month_start = today.replace(day=1)
        month_points = PorapointLedger.objects.filter(
            child=child,
            transaction_type='earned',
            created_at__date__gte=month_start
        ).aggregate(total=models.Sum('amount'))['total'] or 0
        
        Leaderboard.objects.update_or_create(
            child=child,
            leaderboard_type='monthly_points',
            period='monthly',
            period_start=month_start,
            defaults={'score': month_points}
        )


# Singleton instance
gamification_engine = GamificationEngine()