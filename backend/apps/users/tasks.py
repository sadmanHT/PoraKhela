"""
Celery tasks for Porakhela - Daily SMS and background processing
"""
from celery import shared_task
from django.utils import timezone
from django.db.models import Count, Sum, Q
from django.conf import settings
import datetime
import logging

from apps.users.models import User, ChildProfile
from apps.lessons.models import LessonProgress
from apps.gamification.models import PorapointLedger
from applink_client.sms import SMSClient
from applink_client.base import ApplinkResponseStatus

logger = logging.getLogger(__name__)


@shared_task
def send_daily_sms_reports():
    """
    Daily SMS task that runs at 8:00 PM Bangladesh time.
    Sends learning progress SMS to all parents.
    """
    try:
        logger.info("Starting daily SMS report generation")
        
        # Get today's date range (Bangladesh time)
        today = timezone.now().date()
        today_start = datetime.datetime.combine(today, datetime.time.min)
        today_end = datetime.datetime.combine(today, datetime.time.max)
        
        # Get all parent users who have children
        parents_with_children = User.objects.filter(
            is_parent=True,
            OTP_verified=True,
            children__isnull=False
        ).distinct()
        
        sms_sent_count = 0
        sms_failed_count = 0
        
        for parent in parents_with_children:
            try:
                # Generate daily SMS for this parent
                sms_sent = generate_and_send_parent_sms(parent.id, today_start, today_end)
                if sms_sent:
                    sms_sent_count += 1
                else:
                    sms_failed_count += 1
                    
            except Exception as e:
                logger.error(f"Error sending SMS to parent {parent.phone_number}: {str(e)}")
                sms_failed_count += 1
        
        logger.info(f"Daily SMS report completed. Sent: {sms_sent_count}, Failed: {sms_failed_count}")
        return {
            'status': 'completed',
            'sms_sent': sms_sent_count,
            'sms_failed': sms_failed_count
        }
        
    except Exception as e:
        logger.error(f"Error in daily SMS task: {str(e)}")
        return {
            'status': 'failed',
            'error': str(e)
        }


@shared_task
def generate_and_send_parent_sms(parent_id, today_start, today_end):
    """
    Generate and send SMS for a specific parent with today's learning summary.
    Returns True if SMS was sent successfully, False otherwise.
    """
    try:
        parent = User.objects.get(id=parent_id, is_parent=True)
        
        # Get all children for this parent
        children = ChildProfile.objects.filter(parent=parent).select_related('user')
        
        if not children.exists():
            logger.warning(f"No children found for parent {parent.phone_number}")
            return False
        
        # Aggregate daily statistics for all children
        total_lessons_today = 0
        total_points_today = 0
        active_children = []
        
        for child_profile in children:
            child = child_profile.user
            
            # Get lessons completed today
            lessons_today = LessonProgress.objects.filter(
                child=child,
                completed_at__gte=today_start,
                completed_at__lte=today_end,
                status='completed'
            ).count()
            
            # Get points earned today
            points_today = PorapointLedger.objects.filter(
                child=child,
                created_at__gte=today_start,
                created_at__lte=today_end,
                change_amount__gt=0  # Only positive points (earnings)
            ).aggregate(
                total=Sum('change_amount')
            )['total'] or 0
            
            if lessons_today > 0 or points_today > 0:
                active_children.append({
                    'name': child.get_full_name(),
                    'lessons': lessons_today,
                    'points': points_today
                })
            
            total_lessons_today += lessons_today
            total_points_today += points_today
        
        # Generate SMS message
        sms_message = generate_positive_sms_message(
            parent_name=parent.first_name,
            children=active_children,
            total_lessons=total_lessons_today,
            total_points=total_points_today
        )
        
        if not sms_message:
            logger.info(f"No activity today for parent {parent.phone_number}, skipping SMS")
            return True  # Consider this a success (no spam)
        
        # Send SMS via Applink
        sms_sent = send_sms_via_applink(parent.phone_number, sms_message)
        
        if sms_sent:
            logger.info(f"Daily SMS sent successfully to {parent.phone_number}")
            return True
        else:
            logger.error(f"Failed to send SMS to {parent.phone_number}")
            return False
            
    except User.DoesNotExist:
        logger.error(f"Parent with ID {parent_id} not found")
        return False
    except Exception as e:
        logger.error(f"Error generating SMS for parent {parent_id}: {str(e)}")
        return False


def generate_positive_sms_message(parent_name, children, total_lessons, total_points):
    """
    Generate positive, encouraging SMS message based on children's activity.
    Returns None if no activity to report.
    """
    if total_lessons == 0 and total_points == 0:
        return None
    
    # Create positive message based on activity level
    if total_lessons == 0:
        # Points only, maybe from logins or other activities
        message = f"Hi {parent_name}! Your children earned {total_points} Porapoints today. "
        message += "Encourage them to complete lessons tomorrow for even more learning fun! 🌟"
        
    elif total_lessons == 1:
        message = f"Great news {parent_name}! Your children completed 1 lesson and earned {total_points} Porapoints today. "
        message += "They're building great learning habits! 📚✨"
        
    elif total_lessons <= 3:
        message = f"Excellent {parent_name}! Your children completed {total_lessons} lessons and earned {total_points} Porapoints today. "
        message += "They're on fire! 🔥📖"
        
    else:
        message = f"Amazing {parent_name}! Your children completed {total_lessons} lessons and earned {total_points} Porapoints today. "
        message += "Outstanding dedication to learning! 🏆🌟"
    
    # Add individual child highlights if multiple children were active
    if len(children) > 1:
        active_children_names = [child['name'] for child in children if child['lessons'] > 0]
        if len(active_children_names) > 0:
            if len(active_children_names) == 1:
                message += f" {active_children_names[0]} was particularly active today!"
            else:
                names = ", ".join(active_children_names[:-1])
                message += f" {names} and {active_children_names[-1]} were all active learners today!"
    
    # Add motivation for tomorrow
    message += " Keep up the excellent work! - Porakhela Team"
    
    return message


@shared_task
def send_sms_via_applink(phone_number, message):
    """
    Send SMS using Applink SMS client.
    Returns True if successful, False otherwise.
    """
    try:
        sms_client = SMSClient()
        
        # For testing/demo, we'll use the custom SMS client
        response = sms_client.send_custom_sms(
            phone_number=phone_number,
            message=message
        )
        
        if response.status == ApplinkResponseStatus.SUCCESS:
            logger.info(f"SMS sent successfully to {phone_number}: {message[:50]}...")
            return True
        else:
            logger.error(f"SMS failed for {phone_number}: {response.message}")
            return False
            
    except Exception as e:
        logger.error(f"Error sending SMS to {phone_number}: {str(e)}")
        return False


@shared_task
def update_daily_streaks():
    """
    Update daily learning streaks for all children.
    This task should run daily after the learning day ends.
    """
    try:
        logger.info("Starting daily streak update")
        
        today = timezone.now().date()
        yesterday = today - datetime.timedelta(days=1)
        
        # Get all child profiles
        children = ChildProfile.objects.select_related('user')
        updated_count = 0
        
        for child_profile in children:
            child = child_profile.user
            
            # Check if child completed any lessons today
            lessons_completed_today = LessonProgress.objects.filter(
                child=child,
                completed_at__date=today,
                status='completed'
            ).exists()
            
            if lessons_completed_today:
                # Extend or start streak
                if child_profile.last_activity_date == yesterday:
                    # Continue streak
                    child_profile.current_streak += 1
                else:
                    # Start new streak
                    child_profile.current_streak = 1
                
                # Update longest streak if necessary
                if child_profile.current_streak > child_profile.longest_streak:
                    child_profile.longest_streak = child_profile.current_streak
                
                child_profile.last_activity_date = today
                child_profile.save(update_fields=['current_streak', 'longest_streak', 'last_activity_date'])
                updated_count += 1
                
            else:
                # Break streak if no activity and last activity was yesterday
                if child_profile.last_activity_date == yesterday:
                    child_profile.current_streak = 0
                    child_profile.save(update_fields=['current_streak'])
                    updated_count += 1
        
        logger.info(f"Daily streak update completed. Updated {updated_count} children")
        return {
            'status': 'completed',
            'children_updated': updated_count
        }
        
    except Exception as e:
        logger.error(f"Error in daily streak update: {str(e)}")
        return {
            'status': 'failed',
            'error': str(e)
        }


@shared_task
def clear_dashboard_cache():
    """
    Clear dashboard cache for all parents.
    This ensures fresh data after daily aggregations.
    """
    try:
        from django.core.cache import cache
        
        # Get all parent IDs
        parent_ids = User.objects.filter(is_parent=True).values_list('id', flat=True)
        
        cleared_count = 0
        for parent_id in parent_ids:
            cache_key = f"parent_dashboard_{parent_id}"
            if cache.delete(cache_key):
                cleared_count += 1
        
        logger.info(f"Cleared dashboard cache for {cleared_count} parents")
        return {
            'status': 'completed',
            'caches_cleared': cleared_count
        }
        
    except Exception as e:
        logger.error(f"Error clearing dashboard cache: {str(e)}")
        return {
            'status': 'failed',
            'error': str(e)
        }