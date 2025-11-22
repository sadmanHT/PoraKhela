"""
Example usage of the Applink API Integration Layer

This script demonstrates how to use all the Applink API clients
for subscription, SMS, OTP, rewards, and USSD operations.
"""

import asyncio
import logging
from datetime import datetime

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Import all Applink API clients
from applink_client import (
    ApplinkConfig,
    SubscriptionClient, 
    SMSClient,
    OTPClient,
    RewardsClient, 
    USSDClient,
    SubscriptionPlan,
    SMSPriority,
    OTPPurpose,
    RewardCategory,
    USSDMenuType
)

def demonstrate_sync_usage():
    """Demonstrate synchronous usage of all Applink API clients."""
    print("=== Synchronous Applink API Client Demo ===\n")
    
    # Configure the API client (mock mode enabled by default)
    config = ApplinkConfig(
        base_url="https://api.applink.com",
        api_key="your_api_key_here",
        timeout=30,
        mock_mode=True  # Enable mock responses for demo
    )
    
    # Example user data
    phone_number = "+1234567890"
    child_name = "Alex"
    
    # 1. SUBSCRIPTION OPERATIONS
    print("1. SUBSCRIPTION OPERATIONS")
    subscription_client = SubscriptionClient(config)
    
    # Subscribe user to premium plan
    response = subscription_client.subscribe_user(
        phone_number=phone_number,
        plan=SubscriptionPlan.PREMIUM,
        duration_days=30
    )
    print(f"Subscription: {response.status} - {response.message}")
    if response.data:
        print(f"Subscription ID: {response.data.get('subscription_id')}")
        print(f"Plan: {response.data.get('plan')}")
        print(f"End Date: {response.data.get('end_date')}")
    
    # Check subscription status
    status_response = subscription_client.get_subscription_status(phone_number)
    print(f"Status Check: {status_response.status} - {status_response.message}")
    print()
    
    # 2. SMS OPERATIONS
    print("2. SMS OPERATIONS")
    sms_client = SMSClient(config)
    
    # Send daily progress SMS (the exact format you specified)
    sms_response = sms_client.send_daily_progress_sms(
        phone_number=phone_number,
        child_name=child_name,
        lessons_completed=3,
        points_earned=50,
        streak_days=5
    )
    print(f"Daily SMS: {sms_response.status} - {sms_response.message}")
    if sms_response.data:
        print(f"Message ID: {sms_response.data.get('message_id')}")
        print(f"Message: {sms_response.data.get('message')}")
    
    # Send achievement notification
    achievement_response = sms_client.send_achievement_sms(
        phone_number=phone_number,
        child_name=child_name,
        achievement_name="Math Master",
        points_earned=100,
        achievement_level="Gold"
    )
    print(f"Achievement SMS: {achievement_response.status}")
    print()
    
    # 3. OTP OPERATIONS
    print("3. OTP OPERATIONS")
    otp_client = OTPClient(config)
    
    # Send OTP for high-value redemption
    otp_response = otp_client.send_high_value_redemption_otp(
        phone_number=phone_number,
        reward_name="1GB Data Bundle",
        reward_value="1GB",
        points_cost=350,
        child_name=child_name
    )
    print(f"OTP Sent: {otp_response.status} - {otp_response.message}")
    if otp_response.data:
        print(f"OTP ID: {otp_response.data.get('otp_id')}")
        print(f"Mock OTP Code: {otp_response.data.get('mock_otp_code')} (for testing)")
    
    # Verify OTP (using mock code)
    if otp_response.data and otp_response.data.get('otp_id'):
        verify_response = otp_client.verify_otp(
            phone_number=phone_number,
            otp_code="123456",  # Mock OTP for demo
            otp_id=otp_response.data['otp_id'],
            purpose=OTPPurpose.HIGH_VALUE_REDEMPTION
        )
        print(f"OTP Verification: {verify_response.status} - {verify_response.message}")
    print()
    
    # 4. REWARDS OPERATIONS (with your exact response format)
    print("4. REWARDS OPERATIONS")
    rewards_client = RewardsClient(config)
    
    # Redeem 100MB data bundle - returns { "status": "success", "credited": "100MB" }
    reward_response = rewards_client.redeem_data_bundle(
        phone_number=phone_number,
        data_amount="100MB",
        points_to_spend=50,
        child_name=child_name
    )
    print(f"Reward Redemption: {reward_response.status}")
    print(f"Credited: {reward_response.raw_response.get('credited')}")  # Your exact format!
    if reward_response.data:
        print(f"Redemption ID: {reward_response.data.get('redemption_id')}")
        print(f"Transaction ID: {reward_response.data.get('transaction_id')}")
    
    # Get rewards catalog
    catalog_response = rewards_client.get_rewards_catalog()
    print(f"Catalog: {catalog_response.status} - Found {len(catalog_response.data.get('rewards', []))} rewards")
    
    # Redeem SMS bundle
    sms_reward_response = rewards_client.redeem_sms_bundle(
        phone_number=phone_number,
        sms_count=20,
        points_to_spend=30,
        child_name=child_name
    )
    print(f"SMS Reward: {sms_reward_response.status}")
    print(f"Credited: {sms_reward_response.raw_response.get('credited')}")
    print()
    
    # 5. USSD OPERATIONS
    print("5. USSD OPERATIONS")
    ussd_client = USSDClient(config)
    
    # Start USSD session
    ussd_response = ussd_client.start_ussd_session(
        phone_number=phone_number,
        ussd_code="*123#",
        menu_type=USSDMenuType.MAIN_MENU
    )
    print(f"USSD Session: {ussd_response.status} - {ussd_response.message}")
    if ussd_response.data:
        print("USSD Menu:")
        print(ussd_response.data.get('menu_text', '').replace('\\n', '\n'))
        session_id = ussd_response.data.get('session_id')
        
        # Handle user input (select option 2 - Redeem Rewards)
        if session_id:
            input_response = ussd_client.handle_ussd_input(
                session_id=session_id,
                phone_number=phone_number,
                user_input="2"
            )
            print(f"\nAfter selecting '2':")
            if input_response.data:
                print(input_response.data.get('menu_text', '').replace('\\n', '\n'))
    
    print("\n=== Demo completed successfully! ===")


async def demonstrate_async_usage():
    """Demonstrate asynchronous usage of all Applink API clients."""
    print("\n=== Asynchronous Applink API Client Demo ===\n")
    
    config = ApplinkConfig(mock_mode=True)
    phone_number = "+1234567890"
    child_name = "Emma"
    
    # Create clients
    subscription_client = SubscriptionClient(config)
    sms_client = SMSClient(config)
    otp_client = OTPClient(config)
    rewards_client = RewardsClient(config)
    ussd_client = USSDClient(config)
    
    # Run multiple operations concurrently
    tasks = [
        subscription_client.get_subscription_status_async(phone_number),
        sms_client.send_daily_progress_sms_async(
            phone_number=phone_number,
            child_name=child_name,
            lessons_completed=5,
            points_earned=75,
            streak_days=3
        ),
        rewards_client.get_rewards_catalog_async(category=RewardCategory.COMMUNICATION),
        ussd_client.get_ussd_menu_async(USSDMenuType.REWARDS_MENU, phone_number)
    ]
    
    # Execute all tasks concurrently
    results = await asyncio.gather(*tasks, return_exceptions=True)
    
    print("Async Results:")
    for i, result in enumerate(results):
        if isinstance(result, Exception):
            print(f"Task {i+1} failed: {result}")
        else:
            print(f"Task {i+1}: {result.status} - {result.message}")
    
    print("\n=== Async demo completed! ===")


def demonstrate_error_handling():
    """Demonstrate error handling and retry logic."""
    print("\n=== Error Handling Demo ===\n")
    
    # Configure with low timeout to trigger errors
    config = ApplinkConfig(
        timeout=1,  # Very short timeout
        max_retries=2,
        retry_delay=0.5,
        mock_mode=False  # Disable mock mode to test real errors
    )
    
    sms_client = SMSClient(config)
    
    try:
        # This will likely timeout and retry
        response = sms_client.send_custom_sms(
            phone_number="+1234567890",
            message="Test message",
            priority=SMSPriority.HIGH
        )
        print(f"Unexpected success: {response.status}")
    except Exception as e:
        print(f"Expected error caught: {type(e).__name__}: {e}")
    
    print("Error handling demo completed.")


if __name__ == "__main__":
    # Run synchronous demo
    demonstrate_sync_usage()
    
    # Run asynchronous demo
    asyncio.run(demonstrate_async_usage())
    
    # Run error handling demo
    demonstrate_error_handling()
    
    print("\n🎉 All Applink API client demos completed successfully!")
    print("\nKey Features Demonstrated:")
    print("✅ Subscription management (subscribe/unsubscribe/status)")
    print("✅ SMS notifications with exact format: 'Your child completed 3 lessons today and earned 50 Porapoints!'")
    print("✅ OTP send/verify for high-value redemptions")
    print("✅ Rewards redemption with exact response: { 'status': 'success', 'credited': '100MB' }")
    print("✅ USSD menu responses and navigation")
    print("✅ Both sync and async support")
    print("✅ Error handling with retry logic")
    print("✅ Timeout handling")
    print("✅ Response normalization")
    print("✅ Mock mode for development/testing")