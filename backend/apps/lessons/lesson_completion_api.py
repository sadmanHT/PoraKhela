"""
Lesson Completion API View

This is the core revenue-driving endpoint for Porakhela:
lesson → calculate points → update ledger → send SMS → respond

POST /api/lesson/complete/

Flow:
1. Validate idempotency
2. Run points engine
3. Insert ledger transaction
4. Update child.total_points
5. Trigger SMS through Applink mock
6. Return points earned and total
"""

import logging
from decimal import Decimal
from django.db import transaction
from django.utils import timezone
from rest_framework import status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

# Import models
from apps.users.models import User
from apps.lessons.models import Lesson, LessonProgress
from apps.gamification.models import PorapointLedger

# Import services
from apps.gamification.services.points_engine import PointsEngine
from apps.lessons.lesson_completion_serializers import (
    LessonCompletionRequestSerializer,
    LessonCompletionResponseSerializer
)

# Import Applink client
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

try:
    from applink_client.sms import SMSClient
    from applink_client.base import ApplinkConfig
    APPLINK_AVAILABLE = True
except ImportError:
    APPLINK_AVAILABLE = False

logger = logging.getLogger(__name__)


class LessonCompletionAPIView(APIView):
    """
    Core lesson completion API endpoint.
    
    This endpoint handles the complete flow when a child completes a lesson:
    - Points calculation through the points engine
    - ACID-compliant ledger transactions
    - Parent SMS notifications via Applink
    - Response with earned and total points
    """
    
    permission_classes = [IsAuthenticated]
    
    def post(self, request):
        """
        Handle lesson completion.
        
        Expected payload:
        {
          "child_id": 1,
          "lesson_id": 5,
          "correct_answers": 8,
          "time_spent": 95,
          "difficulty": "Hard",
          "idempotency_key": "uuid"
        }
        
        Returns:
        {
          "earned_points": 45,
          "total_points": 230,
          "transaction_id": "txn_123",
          "points_breakdown": {...},
          "sms_status": "sent"
        }
        """
        
        # Validate request data
        serializer = LessonCompletionRequestSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(
                {"error": "Invalid request data", "details": serializer.errors},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        validated_data = serializer.validated_data
        
        try:
            # Execute the complete lesson completion flow
            result = self._process_lesson_completion(validated_data)
            
            # Return success response
            response_serializer = LessonCompletionResponseSerializer(data=result)
            if response_serializer.is_valid():
                return Response(response_serializer.validated_data, status=status.HTTP_200_OK)
            else:
                logger.error(f"Response serialization failed: {response_serializer.errors}")
                return Response(result, status=status.HTTP_200_OK)
                
        except Exception as e:
            logger.error(f"Lesson completion failed: {str(e)}", exc_info=True)
            return Response(
                {"error": "Lesson completion failed", "details": str(e)},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
    
    def _process_lesson_completion(self, validated_data):
        """
        Process the complete lesson completion flow in a database transaction.
        
        This ensures ACID compliance for all operations.
        """
        
        with transaction.atomic():
            # Extract data
            child_id = validated_data['child_id']
            lesson_id = validated_data['lesson_id']
            correct_answers = validated_data['correct_answers']
            time_spent = validated_data['time_spent']
            difficulty = validated_data['difficulty'].lower()
            idempotency_key = validated_data['idempotency_key']
            
            # Get child and lesson objects
            child = User.objects.select_for_update().get(id=child_id, user_type='child')
            lesson = Lesson.objects.get(id=lesson_id, is_active=True)
            
            # Step 1: Validate idempotency
            existing_transaction = PorapointLedger.objects.filter(
                idempotency_key=idempotency_key
            ).first()
            
            if existing_transaction:
                logger.info(f"Idempotent request detected: {idempotency_key}")
                return self._get_existing_transaction_response(existing_transaction)
            
            # Step 2: Create or update lesson progress
            lesson_progress, created = LessonProgress.objects.get_or_create(
                child=child,
                lesson=lesson,
                defaults={
                    'status': 'completed',
                    'completion_percentage': 100,
                    'time_spent_minutes': time_spent,
                    'completed_at': timezone.now(),
                    'score': self._calculate_score_percentage(correct_answers, lesson)
                }
            )
            
            if not created:
                # Update existing progress
                lesson_progress.status = 'completed'
                lesson_progress.completion_percentage = 100
                lesson_progress.time_spent_minutes = time_spent
                lesson_progress.completed_at = timezone.now()
                lesson_progress.score = self._calculate_score_percentage(correct_answers, lesson)
                lesson_progress.save()
            
            # Step 3: Run points engine
            points_result = PointsEngine.award_lesson_completion(
                child=child,
                lesson_progress=lesson_progress,
                correct_answers=correct_answers,
                time_spent_minutes=time_spent,
                idempotency_key=idempotency_key
            )
            
            # Step 4: Get updated total points
            total_points = self._get_child_total_points(child)
            
            # Step 5: Send SMS notification to parent
            sms_status = self._send_parent_notification(
                child, lesson, points_result, total_points
            )
            
            # Step 6: Prepare response
            return {
                'earned_points': points_result['points_awarded'],
                'total_points': total_points,
                'transaction_id': points_result['transaction_id'],
                'points_breakdown': points_result['breakdown'],
                'sms_status': sms_status,
                'streak_info': points_result.get('streak_info', {})
            }
    
    def _calculate_score_percentage(self, correct_answers, lesson):
        """Calculate score percentage based on correct answers."""
        # For now, assume 10 questions per lesson
        # This should be updated based on actual lesson structure
        total_questions = 10
        return (correct_answers / total_questions) * 100 if total_questions > 0 else 0
    
    def _get_child_total_points(self, child):
        """Get child's current total points from ledger."""
        latest_entry = PorapointLedger.objects.filter(child=child).order_by('-created_at').first()
        return latest_entry.balance_after if latest_entry else 0
    
    def _send_parent_notification(self, child, lesson, points_result, total_points):
        """
        Send SMS notification to parent via Applink client.
        
        Format: "Your child completed 3 lessons today and earned 50 Porapoints!"
        """
        
        if not APPLINK_AVAILABLE:
            logger.warning("Applink client not available - SMS notification skipped")
            return "skipped_no_applink"
        
        try:
            # Get parent phone number
            parent = self._get_parent_for_child(child)
            if not parent or not parent.phone_number:
                logger.warning(f"No parent or phone number found for child {child.id}")
                return "skipped_no_parent"
            
            # Configure Applink SMS client
            config = ApplinkConfig(
                base_url="https://api.applink.com",
                api_key="production_key_here",  # Should come from settings
                mock_mode=True  # Set to False in production
            )
            
            sms_client = SMSClient(config)
            
            # Send notification using exact format requested
            response = sms_client.send_daily_progress_sms(
                phone_number=parent.phone_number,
                child_name=child.first_name or "Your child",
                lessons_completed=1,  # This lesson
                points_earned=points_result['points_awarded'],
                streak_days=points_result.get('streak_info', {}).get('current_streak', 1)
            )
            
            logger.info(f"SMS sent to parent {parent.phone_number}: {response.status.value}")
            return response.status.value
            
        except Exception as e:
            logger.error(f"Failed to send SMS notification: {str(e)}")
            return "failed"
    
    def _get_parent_for_child(self, child):
        """Get the parent user for this child."""
        try:
            if hasattr(child, 'child_profile') and child.child_profile.parent:
                return child.child_profile.parent
            return None
        except Exception:
            return None
    
    def _get_existing_transaction_response(self, existing_transaction):
        """Return response for idempotent requests."""
        return {
            'earned_points': existing_transaction.change_amount,
            'total_points': existing_transaction.balance_after,
            'transaction_id': existing_transaction.id,
            'points_breakdown': existing_transaction.metadata.get('points_breakdown', {}),
            'sms_status': 'previously_sent',
            'streak_info': existing_transaction.metadata.get('streak_info', {})
        }


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def lesson_complete_endpoint(request):
    """
    Function-based view for lesson completion.
    
    This can be used as an alternative to the class-based view above.
    """
    view = LessonCompletionAPIView()
    return view.post(request)