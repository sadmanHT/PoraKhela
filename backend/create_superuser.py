#!/usr/bin/env python
"""
Script to create superuser
"""
import os
import sys
import django

# Setup Django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'porakhela.settings')
django.setup()

from apps.users.models import User

# Create superuser
try:
    if not User.objects.filter(phone_number='01622591777').exists():
        user = User(
            phone_number='01622591777',
            username='01622591777',  # Set username same as phone number
            first_name='Admin',
            last_name='User',
            user_type='parent',
            is_staff=True,
            is_superuser=True,
            is_active=True,
            OTP_verified=True
        )
        user.set_password('admin123')
        user.save()
        print("Superuser created successfully!")
    else:
        print("User already exists!")
except Exception as e:
    print(f"Error creating superuser: {e}")