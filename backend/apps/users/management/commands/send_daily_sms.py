"""
Manual SMS Trigger Command for Testing Daily SMS Functionality

This management command allows manual triggering of the daily SMS
system for testing purposes, bypassing the scheduled Celery beat.
"""

from django.core.management.base import BaseCommand, CommandError
from django.contrib.auth import get_user_model
from apps.users.tasks import send_daily_sms_reports, generate_and_send_parent_sms
from django.utils import timezone
import logging

User = get_user_model()
logger = logging.getLogger(__name__)


class Command(BaseCommand):
    help = 'Manually trigger daily SMS reports for testing'

    def add_arguments(self, parser):
        parser.add_argument(
            '--test-mode',
            action='store_true',
            help='Run in test mode (no actual SMS sent)',
        )
        parser.add_argument(
            '--parent-phone',
            type=str,
            help='Test SMS for specific parent phone number',
        )
        parser.add_argument(
            '--all-parents',
            action='store_true',
            help='Send SMS to all parents with children',
        )

    def handle(self, *args, **options):
        self.stdout.write(self.style.SUCCESS('🚀 Manual SMS Trigger Started'))
        self.stdout.write('='*60)
        
        try:
            if options['test_mode']:
                self.stdout.write(self.style.WARNING('⚠️ Running in TEST MODE - No SMS will be sent'))
            
            if options['parent_phone']:
                # Test specific parent
                self.test_specific_parent(options['parent_phone'], options['test_mode'])
            elif options['all_parents']:
                # Test all parents
                self.test_all_parents(options['test_mode'])
            else:
                # Run full daily SMS task
                self.run_daily_sms_task(options['test_mode'])
            
            self.stdout.write('='*60)
            self.stdout.write(self.style.SUCCESS('✅ Manual SMS Trigger Completed'))
            
        except Exception as e:
            logger.error(f"Error in manual SMS trigger: {str(e)}")
            raise CommandError(f"SMS trigger failed: {str(e)}")

    def test_specific_parent(self, phone_number, test_mode):
        """Test SMS for a specific parent"""
        self.stdout.write(f"📱 Testing SMS for parent: {phone_number}")
        
        try:
            parent = User.objects.get(phone_number=phone_number, is_parent=True)
            self.stdout.write(f"Found parent: {parent.get_full_name()}")
            
            if test_mode:
                self.stdout.write("TEST MODE: Would generate SMS for this parent")
                # Simulate SMS generation without sending
                result = {
                    'success': True,
                    'message': 'Test SMS generated successfully',
                    'phone_number': phone_number,
                    'test_mode': True
                }
            else:
                result = generate_and_send_parent_sms(parent.id)
            
            if result['success']:
                self.stdout.write(self.style.SUCCESS(f"✅ SMS successful: {result['message']}"))
                if 'phone_number' in result:
                    self.stdout.write(f"📞 Phone: {result['phone_number']}")
            else:
                self.stdout.write(self.style.ERROR(f"❌ SMS failed: {result['message']}"))
                
        except User.DoesNotExist:
            self.stdout.write(self.style.ERROR(f"❌ Parent not found: {phone_number}"))
        except Exception as e:
            self.stdout.write(self.style.ERROR(f"❌ Error: {str(e)}"))

    def test_all_parents(self, test_mode):
        """Test SMS for all parents"""
        self.stdout.write("📱 Testing SMS for all parents with children...")
        
        parents_with_children = User.objects.filter(
            is_parent=True,
            children__isnull=False
        ).distinct()
        
        self.stdout.write(f"Found {parents_with_children.count()} parents with children")
        
        success_count = 0
        failure_count = 0
        
        for parent in parents_with_children:
            self.stdout.write(f"\n🔄 Processing: {parent.get_full_name()} ({parent.phone_number})")
            
            try:
                if test_mode:
                    self.stdout.write("TEST MODE: Would generate SMS")
                    result = {
                        'success': True,
                        'message': 'Test SMS generated',
                        'test_mode': True
                    }
                else:
                    result = generate_and_send_parent_sms(parent.id)
                
                if result['success']:
                    self.stdout.write(self.style.SUCCESS(f"✅ Success"))
                    success_count += 1
                else:
                    self.stdout.write(self.style.ERROR(f"❌ Failed: {result['message']}"))
                    failure_count += 1
                    
            except Exception as e:
                self.stdout.write(self.style.ERROR(f"❌ Error: {str(e)}"))
                failure_count += 1
        
        self.stdout.write(f"\n📊 Results: {success_count} success, {failure_count} failures")

    def run_daily_sms_task(self, test_mode):
        """Run the full daily SMS task"""
        self.stdout.write("📨 Running full daily SMS task...")
        
        if test_mode:
            self.stdout.write(self.style.WARNING("TEST MODE: Simulating task execution"))
            
            # Simulate the task logic without actually sending
            parents_count = User.objects.filter(is_parent=True, children__isnull=False).distinct().count()
            
            self.stdout.write(f"Would process {parents_count} parents")
            self.stdout.write("✅ Task simulation completed")
        else:
            try:
                # Run actual Celery task
                result = send_daily_sms_reports()
                self.stdout.write(f"Task result: {result}")
                self.stdout.write(self.style.SUCCESS("✅ Daily SMS task completed"))
            except Exception as e:
                self.stdout.write(self.style.ERROR(f"❌ Task failed: {str(e)}"))
                raise

    def show_system_status(self):
        """Show system status for debugging"""
        self.stdout.write("\n" + "="*60)
        self.stdout.write("🔍 SYSTEM STATUS")
        self.stdout.write("="*60)
        
        # Count users
        total_users = User.objects.count()
        parents = User.objects.filter(is_parent=True).count()
        children = User.objects.filter(is_parent=False).count()
        
        self.stdout.write(f"👥 Total Users: {total_users}")
        self.stdout.write(f"👨‍👩‍👧‍👦 Parents: {parents}")
        self.stdout.write(f"🧒 Children: {children}")
        
        # Count relationships
        from apps.users.models import ChildProfile
        child_profiles = ChildProfile.objects.count()
        
        self.stdout.write(f"👶 Child Profiles: {child_profiles}")
        
        # Check recent activity
        from apps.lessons.models import LessonProgress
        from django.utils import timezone
        from datetime import timedelta
        
        today = timezone.now().date()
        recent_lessons = LessonProgress.objects.filter(
            completed_at__date=today,
            status='completed'
        ).count()
        
        self.stdout.write(f"📚 Lessons completed today: {recent_lessons}")
        
        # Cache status
        from django.core.cache import cache
        try:
            cache.set('test_key', 'test_value', 10)
            cache_value = cache.get('test_key')
            cache_status = "✅ Working" if cache_value == 'test_value' else "❌ Failed"
        except Exception:
            cache_status = "❌ Error"
        
        self.stdout.write(f"🗄️ Redis Cache: {cache_status}")

    def validate_sms_configuration(self):
        """Validate SMS configuration"""
        self.stdout.write("\n" + "="*60)
        self.stdout.write("📱 SMS CONFIGURATION CHECK")
        self.stdout.write("="*60)
        
        from django.conf import settings
        
        # Check Applink settings
        applink_base = getattr(settings, 'APPLINK_BASE_URL', 'Not configured')
        applink_key = getattr(settings, 'APPLINK_API_KEY', 'Not configured')
        
        self.stdout.write(f"🔗 Applink Base URL: {applink_base}")
        self.stdout.write(f"🔑 API Key: {'Configured' if applink_key != 'Not configured' else 'Not configured'}")
        
        # Check Celery settings
        celery_broker = getattr(settings, 'CELERY_BROKER_URL', 'Not configured')
        celery_timezone = getattr(settings, 'CELERY_TIMEZONE', 'Not configured')
        
        self.stdout.write(f"⚡ Celery Broker: {celery_broker}")
        self.stdout.write(f"🌍 Celery Timezone: {celery_timezone}")
        
        # Check beat schedule
        beat_schedule = getattr(settings, 'CELERY_BEAT_SCHEDULE', {})
        sms_task = beat_schedule.get('send-daily-parent-sms', {})
        
        if sms_task:
            schedule = sms_task.get('schedule', 'Not configured')
            self.stdout.write(f"⏰ SMS Schedule: {schedule}")
        else:
            self.stdout.write("⏰ SMS Schedule: Not configured")