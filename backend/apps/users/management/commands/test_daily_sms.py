"""
Management command to test daily SMS functionality
Usage: python manage.py test_daily_sms
"""

from django.core.management.base import BaseCommand, CommandError
from django.contrib.auth import get_user_model
from apps.users.tasks import generate_and_send_parent_sms
import logging

User = get_user_model()
logger = logging.getLogger(__name__)


class Command(BaseCommand):
    help = 'Test the daily SMS functionality for parents'

    def add_arguments(self, parser):
        parser.add_argument(
            '--parent-email',
            type=str,
            help='Email of the parent to test SMS for',
        )
        parser.add_argument(
            '--all-parents',
            action='store_true',
            help='Send test SMS to all parents',
        )

    def handle(self, *args, **options):
        try:
            if options['all_parents']:
                parents = User.objects.filter(is_parent=True, phone_number__isnull=False)
                self.stdout.write(f"Found {parents.count()} parents with phone numbers")
                
                for parent in parents:
                    result = generate_and_send_parent_sms(parent.id)
                    if result['success']:
                        self.stdout.write(
                            self.style.SUCCESS(f"✓ SMS sent to {parent.email}")
                        )
                    else:
                        self.stdout.write(
                            self.style.ERROR(f"✗ Failed to send SMS to {parent.email}: {result['message']}")
                        )
            
            elif options['parent_email']:
                try:
                    parent = User.objects.get(email=options['parent_email'], is_parent=True)
                    result = generate_and_send_parent_sms(parent.id)
                    
                    if result['success']:
                        self.stdout.write(
                            self.style.SUCCESS(f"✓ Test SMS sent to {parent.email}")
                        )
                        self.stdout.write(f"Message: {result['message']}")
                        self.stdout.write(f"Phone: {result['phone_number']}")
                    else:
                        self.stdout.write(
                            self.style.ERROR(f"✗ Failed to send SMS: {result['message']}")
                        )
                
                except User.DoesNotExist:
                    raise CommandError(f"Parent with email '{options['parent_email']}' not found")
            
            else:
                self.stdout.write(
                    self.style.WARNING("Please specify either --parent-email or --all-parents")
                )

        except Exception as e:
            logger.error(f"Error in test_daily_sms command: {str(e)}")
            raise CommandError(f"Command failed: {str(e)}")