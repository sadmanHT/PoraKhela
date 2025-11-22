#!/usr/bin/env python
"""
Debug USSD system to identify exact issues
"""

import os
import sys
import django

# Add the project root to Python path
sys.path.insert(0, os.path.dirname(__file__))

# Setup Django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'porakhela.settings')
django.setup()

from apps.users.models import User
from apps.ussd.models import RedemptionRequest
from apps.ussd.menu_handler import USSDMenuHandler
import json

# Clean up
User.objects.filter(phone_number='+8801712345678').delete()
User.objects.filter(phone_number='+8801712345679').delete()

# Create test users
parent = User.objects.create_user(
    username='debugparent',
    phone_number='+8801712345678',
    first_name='Debug',
    last_name='Parent',
    is_parent=True
)

child = User.objects.create_user(
    username='debugchild',
    phone_number='+8801712345679',
    first_name='Rafi',
    last_name='Ahmed',
    is_parent=False
)

print("👤 Created test users:")
print(f"  Parent: {parent}")
print(f"  Child: {child}")

# Test menu handler
handler = USSDMenuHandler()

print("\n🧪 Testing USSD Menu Handler...")

# Test 1: Initial menu
print("\n1. Initial Menu:")
try:
    response_type, message = handler.process_ussd_input("debug_session_1", "+8801712345678", "")
    print(f"   Type: {response_type}")
    print(f"   Message: {message[:100]}...")
except Exception as e:
    print(f"   ERROR: {e}")
    import traceback
    traceback.print_exc()

# Test 2: Select balance (option 1)
print("\n2. Balance View (option 1):")
try:
    response_type, message = handler.process_ussd_input("debug_session_1", "+8801712345678", "1")
    print(f"   Type: {response_type}")
    print(f"   Message: {message[:200]}...")
except Exception as e:
    print(f"   ERROR: {e}")
    import traceback
    traceback.print_exc()

# Test 3: Check HTTP endpoint
print("\n3. HTTP Endpoint Test:")
try:
    from django.test import Client
    client = Client()
    
    response = client.post('/applink/ussd/', 
        json.dumps({
            'session_id': 'http_debug_session',
            'phone_number': '+8801712345678',
            'user_input': ''
        }),
        content_type='application/json'
    )
    
    print(f"   HTTP Status: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"   Response Type: {data.get('response_type')}")
        print(f"   Message: {data.get('message', '')[:100]}...")
    else:
        print(f"   Response: {response.content}")
        
except Exception as e:
    print(f"   ERROR: {e}")
    import traceback
    traceback.print_exc()

print("\n✅ Debug complete")