"""
Lesson Completion API Serializers

Serializers for the lesson completion API endpoint that handles
the complete flow: points calculation, ledger transactions, and SMS notifications.
"""

from rest_framework import serializers
from django.db import transaction
from apps.lessons.models import LessonProgress, Lesson
from apps.users.models import User


class LessonCompletionRequestSerializer(serializers.Serializer):
    """
    Serializer for lesson completion request payload.
    
    Expected payload format:
    {
      "child_id": 1,
      "lesson_id": 5,
      "correct_answers": 8,
      "time_spent": 95,
      "difficulty": "Hard",
      "idempotency_key": "uuid"
    }
    """
    
    child_id = serializers.IntegerField(
        help_text="ID of the child completing the lesson"
    )
    lesson_id = serializers.IntegerField(
        help_text="ID of the lesson being completed"
    )
    correct_answers = serializers.IntegerField(
        min_value=0,
        help_text="Number of correct answers in the lesson/quiz"
    )
    time_spent = serializers.IntegerField(
        min_value=1,
        help_text="Time spent in minutes"
    )
    difficulty = serializers.ChoiceField(
        choices=['Easy', 'Medium', 'Hard'],
        help_text="Difficulty level of the lesson"
    )
    idempotency_key = serializers.CharField(
        max_length=255,
        help_text="UUID for idempotency to prevent duplicate processing"
    )
    
    def validate_child_id(self, value):
        """Validate that child exists and is a child user type."""
        try:
            child = User.objects.get(id=value, user_type='child')
            return value
        except User.DoesNotExist:
            raise serializers.ValidationError("Child not found or invalid user type")
    
    def validate_lesson_id(self, value):
        """Validate that lesson exists and is active."""
        try:
            lesson = Lesson.objects.get(id=value, is_active=True)
            return value
        except Lesson.DoesNotExist:
            raise serializers.ValidationError("Lesson not found or inactive")
    
    def validate(self, attrs):
        """Cross-field validation."""
        child_id = attrs['child_id']
        lesson_id = attrs['lesson_id']
        
        # Check if child and lesson are compatible (grade level, etc.)
        child = User.objects.get(id=child_id)
        lesson = Lesson.objects.get(id=lesson_id)
        
        # Validate grade level compatibility
        if hasattr(child, 'child_profile'):
            child_grade = child.child_profile.grade
            if child_grade != lesson.grade:
                raise serializers.ValidationError(
                    f"Lesson grade ({lesson.grade}) doesn't match child's grade ({child_grade})"
                )
        
        return attrs


class LessonCompletionResponseSerializer(serializers.Serializer):
    """
    Serializer for lesson completion response.
    
    Expected response format:
    {
      "earned_points": 45,
      "total_points": 230
    }
    """
    
    earned_points = serializers.IntegerField(
        help_text="Points earned from this lesson completion"
    )
    total_points = serializers.IntegerField(
        help_text="Total points the child now has"
    )
    transaction_id = serializers.CharField(
        help_text="Ledger transaction ID for tracking"
    )
    points_breakdown = serializers.DictField(
        help_text="Breakdown of how points were calculated"
    )
    sms_status = serializers.CharField(
        help_text="Status of SMS notification sent to parent"
    )
    streak_info = serializers.DictField(
        required=False,
        help_text="Information about current learning streak"
    )


class PointsBreakdownSerializer(serializers.Serializer):
    """Detailed breakdown of points calculation."""
    
    base_completion = serializers.IntegerField(default=0)
    correct_answer_bonus = serializers.IntegerField(default=0)
    speed_bonus = serializers.IntegerField(default=0)
    difficulty_bonus = serializers.IntegerField(default=0)
    streak_bonus = serializers.IntegerField(default=0)
    first_attempt_bonus = serializers.IntegerField(default=0)