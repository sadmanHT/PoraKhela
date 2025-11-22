"""
Gamification Engine - Porapoint Rules System

This module implements the core gamification logic for awarding points to children
based on lesson completion, streaks, speed, difficulty, and other factors.

Rules:
- Base: lesson completion → +10 points
- Correct answer → +5 points each
- Speed bonus (<2 min) → +10 points
- Streaks: Day 3 → +5, Day 7 → +10, Day 30 → +20
- Difficulty multiplier: Hard × 1.5
- ACID-compliant ledger transactions with idempotency
"""

import uuid
from decimal import Decimal
from typing import Dict, Any, Optional, Tuple
from django.db import transaction
from django.db.models import Sum
from django.contrib.auth import get_user_model
from django.utils import timezone
from datetime import datetime, timedelta

from apps.gamification.models import PorapointLedger, DailyStreak
from apps.lessons.models import LessonProgress

User = get_user_model()


class PointsEngine:
    """
    Core Points Engine for Porakhela Gamification System
    
    Handles all point calculations and ledger transactions with ACID compliance.
    """
    
    # Point values as per requirements
    BASE_LESSON_COMPLETION = 10
    CORRECT_ANSWER = 5
    SPEED_BONUS_THRESHOLD_MINUTES = 2
    SPEED_BONUS = 10
    
    # Streak bonuses
    STREAK_BONUSES = {
        3: 5,    # Day 3 → +5
        7: 10,   # Day 7 → +10
        30: 20   # Day 30 → +20
    }
    
    # Difficulty multipliers
    DIFFICULTY_MULTIPLIERS = {
        'easy': 1.0,
        'medium': 1.0,
        'hard': 1.5  # Hard × 1.5
    }

    @classmethod
    def award_lesson_completion(
        cls,
        child: User,
        lesson_progress: LessonProgress,
        correct_answers: int = 0,
        time_spent_minutes: int = 0,
        idempotency_key: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Award points for lesson completion with all applicable bonuses.
        
        Args:
            child: Child user who completed the lesson
            lesson_progress: LessonProgress instance
            correct_answers: Number of correct answers (default: 0)
            time_spent_minutes: Time spent on lesson in minutes
            idempotency_key: Optional unique key to prevent duplicate transactions
            
        Returns:
            Dict containing:
                - points_awarded: Total points awarded for this action
                - breakdown: Detailed breakdown of point sources
                - total_points: Child's total points after transaction
                - transaction_id: UUID of the ledger transaction
                - streak_info: Current streak information
        """
        if not idempotency_key:
            idempotency_key = f"lesson_completion_{child.id}_{lesson_progress.lesson.id}_{timezone.now().timestamp()}"
        
        # Check for duplicate transaction
        if PorapointLedger.objects.filter(idempotency_key=idempotency_key).exists():
            raise ValueError(f"Transaction with idempotency_key '{idempotency_key}' already exists")
        
        with transaction.atomic():
            # Calculate all points
            points_breakdown = cls._calculate_lesson_points(
                lesson_progress=lesson_progress,
                correct_answers=correct_answers,
                time_spent_minutes=time_spent_minutes
            )
            
            # Calculate streak bonus
            streak_info = cls._update_and_calculate_streak(child)
            if streak_info['bonus_points'] > 0:
                points_breakdown['streak_bonus'] = streak_info['bonus_points']
            
            # Calculate total points
            total_points = sum(points_breakdown.values())
            
            # Get current balance
            current_balance = cls._get_current_balance(child)
            new_balance = current_balance + total_points
            
            # Create ledger transaction
            ledger_entry = PorapointLedger.objects.create(
                child=child,
                change_amount=total_points,
                reason='lesson_complete',
                idempotency_key=idempotency_key,
                balance_after=new_balance,
                reference_id=str(lesson_progress.lesson.id),
                description=f"Completed lesson: {lesson_progress.lesson.title}",
                metadata={
                    'lesson_id': str(lesson_progress.lesson.id),
                    'lesson_title': lesson_progress.lesson.title,
                    'subject': lesson_progress.lesson.subject,
                    'grade': lesson_progress.lesson.grade,
                    'difficulty': lesson_progress.lesson.difficulty,
                    'correct_answers': correct_answers,
                    'time_spent_minutes': time_spent_minutes,
                    'points_breakdown': points_breakdown,
                    'streak_info': {
                        **streak_info,
                        'last_activity_date': streak_info['last_activity_date'].isoformat() if streak_info.get('last_activity_date') else None
                    }
                }
            )
            
            # Update child's total points
            child_profile = getattr(child, 'child_profile', None)
            if child_profile:
                child_profile.total_points = new_balance
                child_profile.save()
            
            return {
                'points_awarded': total_points,
                'breakdown': points_breakdown,
                'total_points': new_balance,
                'transaction_id': str(ledger_entry.id),
                'streak_info': {
                    **streak_info,
                    'last_activity_date': streak_info['last_activity_date'].isoformat() if streak_info.get('last_activity_date') else None
                },
                'ledger_entry': ledger_entry
            }
    
    @classmethod
    def award_quiz_completion(
        cls,
        child: User,
        quiz_attempt,
        correct_answers: int,
        total_questions: int,
        time_spent_minutes: int = 0,
        idempotency_key: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Award points for quiz completion.
        
        Args:
            child: Child user who completed the quiz
            quiz_attempt: QuizAttempt instance
            correct_answers: Number of correct answers
            total_questions: Total number of questions
            time_spent_minutes: Time spent on quiz in minutes
            idempotency_key: Optional unique key to prevent duplicate transactions
            
        Returns:
            Dict with same structure as award_lesson_completion
        """
        if not idempotency_key:
            idempotency_key = f"quiz_completion_{child.id}_{quiz_attempt.id}_{timezone.now().timestamp()}"
        
        # Check for duplicate transaction
        if PorapointLedger.objects.filter(idempotency_key=idempotency_key).exists():
            raise ValueError(f"Transaction with idempotency_key '{idempotency_key}' already exists")
        
        with transaction.atomic():
            # Calculate quiz points
            points_breakdown = {}
            
            # Correct answer points
            if correct_answers > 0:
                points_breakdown['correct_answers'] = correct_answers * cls.CORRECT_ANSWER
            
            # Speed bonus for quizzes
            if time_spent_minutes > 0 and time_spent_minutes < cls.SPEED_BONUS_THRESHOLD_MINUTES:
                points_breakdown['speed_bonus'] = cls.SPEED_BONUS
            
            # Perfect score bonus
            if correct_answers == total_questions and total_questions > 0:
                points_breakdown['perfect_score_bonus'] = 20
            
            # Calculate total points
            total_points = sum(points_breakdown.values())
            
            # Get current balance and create transaction
            current_balance = cls._get_current_balance(child)
            new_balance = current_balance + total_points
            
            # Create ledger transaction
            ledger_entry = PorapointLedger.objects.create(
                child=child,
                change_amount=total_points,
                reason='quiz_score',
                idempotency_key=idempotency_key,
                balance_after=new_balance,
                reference_id=str(quiz_attempt.id),
                description=f"Quiz completed: {correct_answers}/{total_questions} correct",
                metadata={
                    'quiz_attempt_id': str(quiz_attempt.id),
                    'correct_answers': correct_answers,
                    'total_questions': total_questions,
                    'time_spent_minutes': time_spent_minutes,
                    'accuracy_percentage': round((correct_answers / total_questions) * 100, 2) if total_questions > 0 else 0,
                    'points_breakdown': points_breakdown
                }
            )
            
            # Update child's total points
            child_profile = getattr(child, 'child_profile', None)
            if child_profile:
                child_profile.total_points = new_balance
                child_profile.save()
            
            return {
                'points_awarded': total_points,
                'breakdown': points_breakdown,
                'total_points': new_balance,
                'transaction_id': str(ledger_entry.id),
                'quiz_results': {
                    'correct_answers': correct_answers,
                    'total_questions': total_questions,
                    'accuracy_percentage': round((correct_answers / total_questions) * 100, 2) if total_questions > 0 else 0
                },
                'ledger_entry': ledger_entry
            }
    
    @classmethod
    def award_daily_login(
        cls,
        child: User,
        idempotency_key: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Award points for daily login (first login of the day).
        
        Args:
            child: Child user who logged in
            idempotency_key: Optional unique key to prevent duplicate transactions
            
        Returns:
            Dict with transaction details or None if already awarded today
        """
        today = timezone.now().date()
        
        if not idempotency_key:
            idempotency_key = f"daily_login_{child.id}_{today}"
        
        # Check if already awarded today
        if PorapointLedger.objects.filter(
            child=child,
            reason='daily_login',
            created_at__date=today
        ).exists():
            return None  # Already awarded today
        
        # Check for duplicate transaction
        if PorapointLedger.objects.filter(idempotency_key=idempotency_key).exists():
            return None  # Already processed
        
        with transaction.atomic():
            daily_login_points = 5
            current_balance = cls._get_current_balance(child)
            new_balance = current_balance + daily_login_points
            
            # Create ledger transaction
            ledger_entry = PorapointLedger.objects.create(
                child=child,
                change_amount=daily_login_points,
                reason='daily_login',
                idempotency_key=idempotency_key,
                balance_after=new_balance,
                description=f"Daily login bonus for {today}",
                metadata={
                    'login_date': str(today)
                }
            )
            
            # Update child's total points
            child_profile = getattr(child, 'child_profile', None)
            if child_profile:
                child_profile.total_points = new_balance
                child_profile.save()
            
            return {
                'points_awarded': daily_login_points,
                'breakdown': {'daily_login': daily_login_points},
                'total_points': new_balance,
                'transaction_id': str(ledger_entry.id),
                'ledger_entry': ledger_entry
            }
    
    @classmethod
    def deduct_points(
        cls,
        child: User,
        amount: int,
        reason: str,
        description: str = "",
        reference_id: str = "",
        idempotency_key: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Deduct points from child's account (e.g., for redemptions).
        
        Args:
            child: Child user to deduct points from
            amount: Number of points to deduct (positive number)
            reason: Reason for deduction (e.g., 'redemption')
            description: Detailed description
            reference_id: Reference to related object
            idempotency_key: Optional unique key to prevent duplicate transactions
            
        Returns:
            Dict with transaction details
            
        Raises:
            ValueError: If insufficient balance or duplicate transaction
        """
        if amount <= 0:
            raise ValueError("Deduction amount must be positive")
        
        if not idempotency_key:
            idempotency_key = f"deduction_{child.id}_{reason}_{timezone.now().timestamp()}"
        
        # Check for duplicate transaction
        if PorapointLedger.objects.filter(idempotency_key=idempotency_key).exists():
            raise ValueError(f"Transaction with idempotency_key '{idempotency_key}' already exists")
        
        with transaction.atomic():
            current_balance = cls._get_current_balance(child)
            
            if current_balance < amount:
                raise ValueError(f"Insufficient balance. Current: {current_balance}, Required: {amount}")
            
            new_balance = current_balance - amount
            
            # Create ledger transaction (negative amount for deduction)
            ledger_entry = PorapointLedger.objects.create(
                child=child,
                change_amount=-amount,  # Negative for deduction
                reason=reason,
                idempotency_key=idempotency_key,
                balance_after=new_balance,
                reference_id=reference_id,
                description=description,
                metadata={
                    'deduction_amount': amount,
                    'reason': reason
                }
            )
            
            # Update child's total points
            child_profile = getattr(child, 'child_profile', None)
            if child_profile:
                child_profile.total_points = new_balance
                child_profile.save()
            
            return {
                'points_deducted': amount,
                'total_points': new_balance,
                'transaction_id': str(ledger_entry.id),
                'ledger_entry': ledger_entry
            }
    
    @classmethod
    def _calculate_lesson_points(
        cls,
        lesson_progress: LessonProgress,
        correct_answers: int,
        time_spent_minutes: int
    ) -> Dict[str, int]:
        """Calculate points breakdown for lesson completion."""
        breakdown = {}
        
        # Base lesson completion points
        breakdown['base_completion'] = cls.BASE_LESSON_COMPLETION
        
        # Correct answer points
        if correct_answers > 0:
            breakdown['correct_answers'] = correct_answers * cls.CORRECT_ANSWER
        
        # Speed bonus
        if time_spent_minutes > 0 and time_spent_minutes < cls.SPEED_BONUS_THRESHOLD_MINUTES:
            breakdown['speed_bonus'] = cls.SPEED_BONUS
        
        # Apply difficulty multiplier to all points
        lesson_difficulty = lesson_progress.lesson.difficulty
        multiplier = cls.DIFFICULTY_MULTIPLIERS.get(lesson_difficulty, 1.0)
        
        if multiplier != 1.0:
            # Apply multiplier and calculate bonus
            original_total = sum(breakdown.values())
            multiplied_total = int(original_total * multiplier)
            difficulty_bonus = multiplied_total - original_total
            
            if difficulty_bonus > 0:
                breakdown['difficulty_bonus'] = difficulty_bonus
        
        return breakdown
    
    @classmethod
    def _update_and_calculate_streak(cls, child: User) -> Dict[str, Any]:
        """Update daily streak and calculate streak bonus points."""
        today = timezone.now().date()
        yesterday = today - timedelta(days=1)
        
        # Get or create daily streak record
        daily_streak, created = DailyStreak.objects.get_or_create(
            child=child,
            defaults={
                'current_streak': 0,
                'longest_streak': 0,
                'last_activity_date': today  # Set to today instead of None
            }
        )
        
        bonus_points = 0
        streak_milestone = None
        
        # Check if this is a new day of activity
        if daily_streak.last_activity_date != today:
            if daily_streak.last_activity_date == yesterday:
                # Consecutive day - increment streak
                daily_streak.current_streak += 1
            elif daily_streak.last_activity_date < yesterday:
                # Streak broken - reset to 1
                daily_streak.current_streak = 1
            # If created today, current_streak is already set to 0, so we increment to 1
            elif created:
                daily_streak.current_streak = 1
            
            # Update longest streak
            if daily_streak.current_streak > daily_streak.longest_streak:
                daily_streak.longest_streak = daily_streak.current_streak
            
            # Check for streak milestone bonuses
            current_streak = daily_streak.current_streak
            for milestone, points in cls.STREAK_BONUSES.items():
                if current_streak == milestone:
                    bonus_points = points
                    streak_milestone = milestone
                    break
            
            # Update last activity date
            daily_streak.last_activity_date = today
            daily_streak.save()
        
        return {
            'current_streak': daily_streak.current_streak,
            'longest_streak': daily_streak.longest_streak,
            'bonus_points': bonus_points,
            'milestone': streak_milestone,
            'last_activity_date': daily_streak.last_activity_date
        }
    
    @classmethod
    def _get_current_balance(cls, child: User) -> int:
        """Get child's current point balance from ledger."""
        latest_transaction = PorapointLedger.objects.filter(
            child=child
        ).order_by('-created_at').first()
        
        if latest_transaction:
            return latest_transaction.balance_after
        else:
            return 0
    
    @classmethod
    def get_points_history(
        cls,
        child: User,
        limit: int = 50,
        reason_filter: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Get child's points transaction history.
        
        Args:
            child: Child user
            limit: Maximum number of transactions to return
            reason_filter: Optional filter by transaction reason
            
        Returns:
            Dict containing transaction history and summary
        """
        queryset = PorapointLedger.objects.filter(child=child).order_by('-created_at')
        
        if reason_filter:
            queryset = queryset.filter(reason=reason_filter)
        
        transactions = list(queryset[:limit].values(
            'id', 'change_amount', 'reason', 'description',
            'balance_after', 'created_at', 'metadata'
        ))
        
        # Calculate summary stats
        total_earned = PorapointLedger.objects.filter(
            child=child,
            change_amount__gt=0
        ).aggregate(total=Sum('change_amount'))['total'] or 0
        
        total_spent = abs(PorapointLedger.objects.filter(
            child=child,
            change_amount__lt=0
        ).aggregate(total=Sum('change_amount'))['total'] or 0)
        
        current_balance = cls._get_current_balance(child)
        
        return {
            'transactions': transactions,
            'summary': {
                'current_balance': current_balance,
                'total_earned': total_earned,
                'total_spent': total_spent,
                'transaction_count': queryset.count()
            }
        }