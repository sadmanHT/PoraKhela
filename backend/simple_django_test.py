"""
Django Shell Test - Simplified Version

Run this in Django shell: 
python manage.py shell
exec(open('simple_django_test.py').read())
"""

print("🚀 Testing Applink in Django Shell...")

try:
    # Add path
    import sys
    import os
    sys.path.insert(0, os.getcwd())
    
    # Import Applink clients
    from applink_client.base import ApplinkConfig
    from applink_client.subscription import SubscriptionClient, SubscriptionPlan
    
    print("✅ Imports successful")
    
    # Configure
    config = ApplinkConfig(mock_mode=True)
    client = SubscriptionClient(config)
    
    # Define subscribe function as requested
    def subscribe(phone):
        return client.subscribe_user(phone, SubscriptionPlan.PREMIUM, 30)
    
    print("✅ Configuration complete")
    
    # Test the exact call
    print("\n📱 Executing: subscribe('017xxx')")
    response = subscribe('017xxx')
    
    print(f"✅ Status: {response.status.value}")
    print(f"✅ Message: {response.message}")
    print(f"✅ Plan: {response.data.get('plan')}")
    print(f"✅ Phone: {response.data.get('phone_number')}")
    print(f"✅ Subscription ID: {response.data.get('subscription_id')}")
    
    print("\n🎉 SUCCESS! All Applink mock calls work in Django!")
    
except Exception as e:
    print(f"❌ Error: {e}")
    print("\nTry manual commands:")
    print("from applink_client.subscription import SubscriptionClient, SubscriptionPlan")
    print("from applink_client.base import ApplinkConfig")
    print("config = ApplinkConfig(mock_mode=True)")
    print("client = SubscriptionClient(config)")
    print("def subscribe(phone): return client.subscribe_user(phone, SubscriptionPlan.PREMIUM, 30)")
    print("response = subscribe('017xxx')")
    print("print(response.status.value, response.message)")