#!/usr/bin/env python
"""
Debug USSD balance view issue
"""

import os
import sys
import django

# Setup Django
sys.path.insert(0, os.path.dirname(__file__))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'porakhela.settings')
django.setup()

from apps.users.models import User, ChildProfile
from apps.gamification.models import PorapointLedger
from django.db import models

# Check what data we have
print("🔍 DEBUG: Checking test data...")

# Find parent
parent = User.objects.filter(phone_number='+8801712345678').first()
print(f"Parent found: {parent}")

if parent:
    # Find child profiles
    children = ChildProfile.objects.filter(parent=parent)
    print(f"Children found: {children.count()}")
    for child in children:
        print(f"  - {child.user.first_name} (ID: {child.user.id})")
        
        # Check porapoints
        points = PorapointLedger.objects.filter(child=child.user)
        print(f"    Points entries: {points.count()}")
        total = sum([p.change_amount for p in points])
        print(f"    Total points: {total}")
        
    # Test manual balance calculation
    print("\n📊 Manual Balance Calculation:")
    for child_profile in children:
        child_user = child_profile.user
        try:
            balance = PorapointLedger.objects.filter(child=child_user).aggregate(
                total=models.Sum('change_amount')
            )['total'] or 0
            print(f"  {child_user.first_name}: {balance} points")
        except Exception as e:
            print(f"  Error calculating balance for {child_user.first_name}: {e}")

print("\n✅ Debug complete")