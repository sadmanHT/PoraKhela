#!/usr/bin/env python
"""
Simple USSD Menu Test
"""

import os
import sys
import django

# Setup Django
sys.path.insert(0, os.path.dirname(__file__))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'porakhela.settings')
django.setup()

from apps.users.models import User, ChildProfile
from apps.ussd.menu_handler import USSDMenuHandler
from apps.ussd.session_manager import USSDSessionManager

# Find the parent we created
parent = User.objects.filter(phone_number='+8801712345678').first()
if not parent:
    print("❌ No parent found")
    exit(1)

print(f"👤 Found parent: {parent.first_name} {parent.last_name}")

# Check children
children = ChildProfile.objects.filter(parent=parent)
print(f"👶 Found {children.count()} children")

# Test menu handler directly
handler = USSDMenuHandler()

print("\n🧪 Testing menu handler...")

# Test 1: Initial menu
try:
    session_id = "simple_test"
    phone = "+8801712345678"
    response_type, message = handler.process_ussd_input(session_id, phone, "")
    print(f"Initial menu: {response_type} - {message[:100]}...")
except Exception as e:
    print(f"Error in initial menu: {e}")
    import traceback
    traceback.print_exc()

# Test 2: Balance view (if initial worked)
try:
    response_type, message = handler.process_ussd_input(session_id, phone, "1")
    print(f"Balance view: {response_type} - {message[:100]}...")
except Exception as e:
    print(f"Error in balance view: {e}")
    import traceback
    traceback.print_exc()

print("\n✅ Simple test complete")