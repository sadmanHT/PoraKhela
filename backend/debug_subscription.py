#!/usr/bin/env python3
"""
Debug subscription client issue
"""

import sys
import os

# Add the project directory to the Python path
sys.path.insert(0, os.path.abspath('.'))

def debug_subscription_client():
    """Debug the specific subscription client issue."""
    try:
        from applink_client.subscription import SubscriptionClient
        from applink_client.base import ApplinkConfig
        
        # Initialize subscription client with mock mode config
        config = ApplinkConfig(mock_mode=True)
        sub_client = SubscriptionClient(config=config)
        
        print("✅ Subscription client initialized successfully")
        print(f"   Mock mode: {config.mock_mode}")
        print(f"   Base URL: {config.base_url}")
        
        # Test the exact method that's failing
        print("\n🔍 Testing get_subscription_status method...")
        
        # Try to call the method and catch the exact error
        response = sub_client.get_subscription_status("254700123456")
        
        print(f"✅ Success! Response type: {type(response)}")
        print(f"   Status: {response.status}")
        print(f"   Message: {response.message}")
        print(f"   Data: {response.data}")
        
        return True
        
    except Exception as e:
        print(f"❌ Error occurred: {e}")
        print(f"   Error type: {type(e)}")
        import traceback
        print(f"   Traceback: {traceback.format_exc()}")
        return False

if __name__ == "__main__":
    debug_subscription_client()