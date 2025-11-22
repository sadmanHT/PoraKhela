"""
Parent Dashboard API Views
"""
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from drf_spectacular.utils import extend_schema, OpenApiExample
from django.utils import timezone
from django.db.models import Count, Sum, Q
from django.core.cache import cache
import datetime
import logging

from apps.users.models import User, ChildProfile
from apps.lessons.models import LessonProgress
from apps.gamification.models import PorapointLedger

logger = logging.getLogger(__name__)


class ParentDashboardView(APIView):
    """
    Parent Dashboard API - Returns aggregated data for all parent's children
    """
    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Get Parent Dashboard Data",
        description="Returns comprehensive dashboard data including children's progress, points, and streaks",
        responses={
            200: {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean"},
                    "children": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "id": {"type": "integer"},
                                "name": {"type": "string"},
                                "grade": {"type": "integer"},
                                "avatar": {"type": "string"},
                                "total_points": {"type": "integer"},
                                "lessons_completed_today": {"type": "integer"},
                                "porapoints_earned_today": {"type": "integer"},
                                "streak": {"type": "integer"},
                                "total_lessons_completed": {"type": "integer"},
                                "last_activity": {"type": "string", "format": "date-time"},
                                "screen_time_limit": {"type": "integer"}
                            }
                        }
                    },
                    "total_children": {"type": "integer"},
                    "family_total_points": {"type": "integer"},
                    "family_lessons_today": {"type": "integer"}
                }
            }
        },
        examples=[
            OpenApiExample(
                name="Parent Dashboard Response",
                value={
                    "success": True,
                    "children": [
                        {
                            "id": 1,
                            "name": "Rafi",
                            "grade": 5,
                            "avatar": "https://example.com/avatar1.jpg",
                            "total_points": 230,
                            "lessons_completed_today": 3,
                            "porapoints_earned_today": 50,
                            "streak": 4,
                            "total_lessons_completed": 45,
                            "last_activity": "2023-11-22T14:30:00Z",
                            "screen_time_limit": 45
                        }
                    ],
                    "total_children": 1,
                    "family_total_points": 230,
                    "family_lessons_today": 3
                }
            )
        ]
    )
    def get(self, request):
        """Get parent dashboard data with caching."""
        try:
            # Check if user is a parent
            if not request.user.is_parent:
                return Response({
                    'success': False,
                    'message': 'Only parents can access dashboard data'
                }, status=status.HTTP_403_FORBIDDEN)

            parent_id = request.user.id
            
            # Try to get cached data first
            cache_key = f"parent_dashboard_{parent_id}"
            cached_data = cache.get(cache_key)
            
            if cached_data:
                logger.info(f"Serving cached dashboard data for parent {parent_id}")
                return Response(cached_data)

            # Get all children for this parent
            children_profiles = ChildProfile.objects.filter(
                parent=request.user
            ).select_related('user').prefetch_related(
                'user__lesson_progress',
                'user__porapoint_transactions'
            )

            children_data = []
            total_family_points = 0
            total_family_lessons_today = 0

            # Get today's date range
            today = timezone.now().date()
            today_start = datetime.datetime.combine(today, datetime.time.min)
            today_end = datetime.datetime.combine(today, datetime.time.max)
            
            for child_profile in children_profiles:
                child_user = child_profile.user
                
                # Get lessons completed today
                lessons_today = LessonProgress.objects.filter(
                    child=child_user,
                    completed_at__gte=today_start,
                    completed_at__lte=today_end,
                    status='completed'
                ).count()

                # Get Porapoints earned today
                points_today = PorapointLedger.objects.filter(
                    child=child_user,
                    created_at__gte=today_start,
                    created_at__lte=today_end,
                    change_amount__gt=0  # Only positive changes (earnings)
                ).aggregate(
                    total=Sum('change_amount')
                )['total'] or 0

                # Get latest activity
                latest_progress = LessonProgress.objects.filter(
                    child=child_user
                ).order_by('-last_accessed_at').first()

                last_activity = None
                if latest_progress:
                    last_activity = latest_progress.last_accessed_at

                # Current total points (sum all transactions)
                total_points = PorapointLedger.objects.filter(
                    child=child_user
                ).aggregate(
                    total=Sum('change_amount')
                )['total'] or 0

                # Update child profile total_points if different
                if child_profile.total_points != total_points:
                    child_profile.total_points = total_points
                    child_profile.save(update_fields=['total_points'])

                child_data = {
                    'id': child_profile.id,
                    'name': child_user.get_full_name(),
                    'grade': child_profile.grade,
                    'avatar': child_profile.avatar,
                    'total_points': total_points,
                    'lessons_completed_today': lessons_today,
                    'porapoints_earned_today': points_today,
                    'streak': child_profile.current_streak,
                    'total_lessons_completed': child_profile.total_lessons_completed,
                    'last_activity': last_activity.isoformat() if last_activity else None,
                    'screen_time_limit': child_profile.daily_screen_time_limit
                }

                children_data.append(child_data)
                total_family_points += total_points
                total_family_lessons_today += lessons_today

            dashboard_data = {
                'success': True,
                'children': children_data,
                'total_children': len(children_data),
                'family_total_points': total_family_points,
                'family_lessons_today': total_family_lessons_today
            }

            # Cache the data for 5 minutes
            cache.set(cache_key, dashboard_data, 300)  # 5 minutes = 300 seconds
            
            logger.info(f"Generated fresh dashboard data for parent {parent_id} with {len(children_data)} children")
            return Response(dashboard_data)

        except Exception as e:
            logger.error(f"Error fetching dashboard data for parent {request.user.id}: {str(e)}")
            return Response({
                'success': False,
                'message': 'Failed to fetch dashboard data',
                'error': str(e)
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class SetScreenTimeLimitView(APIView):
    """
    Set daily screen time limit for a specific child
    """
    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Set Child Screen Time Limit",
        description="Set daily screen time limit in minutes for a specific child",
        request={
            "type": "object",
            "properties": {
                "child_id": {"type": "string", "format": "uuid"},
                "minutes_per_day": {"type": "integer", "minimum": 15, "maximum": 180}
            },
            "required": ["child_id", "minutes_per_day"]
        },
        responses={
            200: {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean"},
                    "message": {"type": "string"},
                    "child_name": {"type": "string"},
                    "new_limit": {"type": "integer"}
                }
            }
        },
        examples=[
            OpenApiExample(
                name="Set Screen Time Request",
                value={
                    "child_id": "550e8400-e29b-41d4-a716-446655440000",
                    "minutes_per_day": 45
                }
            ),
            OpenApiExample(
                name="Set Screen Time Response",
                value={
                    "success": True,
                    "message": "Screen time limit updated successfully",
                    "child_name": "Rafi",
                    "new_limit": 45
                }
            )
        ]
    )
    def post(self, request):
        """Set screen time limit for a child."""
        try:
            # Check if user is a parent
            if not request.user.is_parent:
                return Response({
                    'success': False,
                    'message': 'Only parents can set screen time limits'
                }, status=status.HTTP_403_FORBIDDEN)

            child_id = request.data.get('child_id')
            minutes_per_day = request.data.get('minutes_per_day')

            # Validate input
            if not child_id:
                return Response({
                    'success': False,
                    'message': 'child_id is required'
                }, status=status.HTTP_400_BAD_REQUEST)

            if not minutes_per_day or not isinstance(minutes_per_day, int):
                return Response({
                    'success': False,
                    'message': 'minutes_per_day must be a valid integer'
                }, status=status.HTTP_400_BAD_REQUEST)

            if minutes_per_day < 15 or minutes_per_day > 180:
                return Response({
                    'success': False,
                    'message': 'Screen time limit must be between 15 and 180 minutes'
                }, status=status.HTTP_400_BAD_REQUEST)

            # Get the child profile and verify parent ownership
            try:
                child_profile = ChildProfile.objects.select_related('user').get(
                    id=child_id,
                    parent=request.user
                )
            except ChildProfile.DoesNotExist:
                return Response({
                    'success': False,
                    'message': 'Child not found or not authorized to modify'
                }, status=status.HTTP_404_NOT_FOUND)

            # Update screen time limit
            old_limit = child_profile.daily_screen_time_limit
            child_profile.daily_screen_time_limit = minutes_per_day
            child_profile.save(update_fields=['daily_screen_time_limit'])

            # Clear parent's dashboard cache since data has changed
            cache_key = f"parent_dashboard_{request.user.id}"
            cache.delete(cache_key)

            logger.info(f"Parent {request.user.phone_number} updated screen time for child {child_profile.user.get_full_name()} from {old_limit} to {minutes_per_day} minutes")

            return Response({
                'success': True,
                'message': 'Screen time limit updated successfully',
                'child_name': child_profile.user.get_full_name(),
                'old_limit': old_limit,
                'new_limit': minutes_per_day
            })

        except Exception as e:
            logger.error(f"Error setting screen time limit: {str(e)}")
            return Response({
                'success': False,
                'message': 'Failed to update screen time limit',
                'error': str(e)
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class ChildrenListView(APIView):
    """
    Get list of parent's children for quick access
    """
    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Get Parent's Children List",
        description="Get a simplified list of all children under this parent",
        responses={
            200: {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean"},
                    "children": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "user_id": {"type": "string"},
                                "name": {"type": "string"},
                                "grade": {"type": "integer"},
                                "avatar": {"type": "string"},
                                "total_points": {"type": "integer"},
                                "total_lessons_completed": {"type": "integer"},
                                "current_streak": {"type": "integer"},
                                "longest_streak": {"type": "integer"}
                            }
                        }
                    },
                    "total_children": {"type": "integer"}
                }
            }
        }
    )
    def get(self, request):
        """Get list of parent's children."""
        try:
            # Check if user is a parent
            if not request.user.is_parent:
                return Response({
                    'success': False,
                    'message': 'Only parents can access children list'
                }, status=status.HTTP_403_FORBIDDEN)

            children_profiles = ChildProfile.objects.filter(
                parent=request.user
            ).select_related('user')

            children_data = []
            for child_profile in children_profiles:
                child_data = {
                    'user_id': str(child_profile.user.id),
                    'name': child_profile.user.get_full_name(),
                    'grade': child_profile.grade,
                    'avatar': child_profile.avatar,
                    'total_points': child_profile.total_points,
                    'total_lessons_completed': child_profile.total_lessons_completed,
                    'current_streak': child_profile.current_streak,
                    'longest_streak': child_profile.longest_streak
                }
                children_data.append(child_data)

            return Response({
                'success': True,
                'children': children_data,
                'total_children': len(children_data)
            })

        except Exception as e:
            logger.error(f"Error fetching children list for parent {request.user.id}: {str(e)}")
            return Response({
                'success': False,
                'message': 'Failed to fetch children list',
                'error': str(e)
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)