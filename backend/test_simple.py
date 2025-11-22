#!/usr/bin/env python
import os
import django
from django.conf import settings
from django.test.utils import get_runner

if __name__ == "__main__":
    os.environ.setdefault("DJANGO_SETTINGS_MODULE", "porakhela.settings")
    django.setup()
    
    # Simple test to see if models can be imported
    try:
        from apps.gamification.models import PorapointLedger
        print("✓ Successfully imported PorapointLedger model")
        
        # Check user field
        user_field = PorapointLedger._meta.get_field('child')
        print(f"✓ Child field remote model: {user_field.related_model}")
        
        # Try to access User model through settings
        from django.contrib.auth import get_user_model
        User = get_user_model()
        print(f"✓ User model from get_user_model(): {User}")
        
    except Exception as e:
        print(f"✗ Error: {e}")
        import traceback
        traceback.print_exc()