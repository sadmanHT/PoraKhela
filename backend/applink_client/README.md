# Applink API Integration Layer

A comprehensive Python client library for integrating with all Applink APIs, providing unified access to subscription management, SMS notifications, OTP verification, rewards redemption, and USSD menu handling.

## Features

### 🔄 **All Applink Services Covered**
- **Subscription API**: Subscribe/unsubscribe users, check status, manage billing
- **SMS API**: Send daily progress notifications like "Your child completed 3 lessons today and earned 50 Porapoints!"
- **OTP API**: Send/verify OTP for high-value redemptions and secure transactions
- **Rewards API**: Redeem Porapoints with exact response format `{ "status": "success", "credited": "100MB" }`
- **USSD API**: Generate interactive USSD menu responses and handle navigation

### ⚡ **Dual Operation Modes**
- **Synchronous**: Traditional blocking operations for simple integration
- **Asynchronous**: Non-blocking operations for high-performance applications

### 🛡️ **Enterprise-Grade Reliability**
- **Error Handling**: Custom exception hierarchy with detailed error context
- **Retry Logic**: Exponential backoff with configurable retry attempts
- **Timeout Handling**: Request-level and global timeout configuration
- **Response Normalization**: Unified response format across all services

### 🧪 **Development Support**
- **Mock Mode**: Built-in mock responses for development and testing
- **Comprehensive Logging**: Detailed request/response logging for debugging
- **Type Hints**: Full type annotation support for better IDE experience

## Installation

```bash
pip install -r requirements.txt
```

## Quick Start

```python
from applink_client import (
    ApplinkConfig, 
    SubscriptionClient, 
    SMSClient, 
    RewardsClient,
    SubscriptionPlan
)

# Configure the client
config = ApplinkConfig(
    base_url="https://api.applink.com",
    api_key="your_api_key",
    mock_mode=True  # Enable for development
)

# Subscribe a user
subscription_client = SubscriptionClient(config)
response = subscription_client.subscribe_user(
    phone_number="+1234567890",
    plan=SubscriptionPlan.PREMIUM,
    duration_days=30
)
print(f"Subscription: {response.status}")

# Send daily progress SMS
sms_client = SMSClient(config)
sms_response = sms_client.send_daily_progress_sms(
    phone_number="+1234567890",
    child_name="Alex", 
    lessons_completed=3,
    points_earned=50,
    streak_days=5
)
# Output: "🎉 Alex completed 3 lessons today and earned 50 Porapoints! That's a 5-day learning streak! 🔥"

# Redeem rewards (exact format)
rewards_client = RewardsClient(config)
reward_response = rewards_client.redeem_data_bundle(
    phone_number="+1234567890",
    data_amount="100MB",
    points_to_spend=50,
    child_name="Alex"
)
# Returns: { "status": "success", "credited": "100MB" }
print(f"Credited: {reward_response.raw_response['credited']}")
```

## Async Usage

```python
import asyncio

async def async_example():
    # All clients support async operations
    response = await sms_client.send_daily_progress_sms_async(
        phone_number="+1234567890",
        child_name="Emma",
        lessons_completed=5,
        points_earned=75
    )
    print(f"Async SMS: {response.status}")

asyncio.run(async_example())
```

## API Services

### 📱 Subscription Management
```python
subscription_client = SubscriptionClient(config)

# Subscribe user
response = subscription_client.subscribe_user(
    phone_number="+1234567890",
    plan=SubscriptionPlan.PREMIUM,
    duration_days=30
)

# Check status
status = subscription_client.get_subscription_status("+1234567890")

# Unsubscribe
unsubscribe = subscription_client.unsubscribe_user(
    phone_number="+1234567890",
    reason="user_request",
    immediate=True
)
```

### 📧 SMS Notifications
```python
sms_client = SMSClient(config)

# Daily progress (your exact format)
daily_sms = sms_client.send_daily_progress_sms(
    phone_number="+1234567890",
    child_name="Alex",
    lessons_completed=3,
    points_earned=50
)

# Achievement notifications
achievement_sms = sms_client.send_achievement_sms(
    phone_number="+1234567890",
    child_name="Alex",
    achievement_name="Math Master",
    points_earned=100,
    achievement_level="Gold"
)

# Custom messages
custom_sms = sms_client.send_custom_sms(
    phone_number="+1234567890",
    message="Your custom message here",
    priority=SMSPriority.HIGH
)
```

### 🔐 OTP Security
```python
otp_client = OTPClient(config)

# Send OTP for high-value redemptions
otp_response = otp_client.send_high_value_redemption_otp(
    phone_number="+1234567890",
    reward_name="1GB Data Bundle",
    reward_value="1GB", 
    points_cost=350,
    child_name="Alex"
)

# Verify OTP
verify_response = otp_client.verify_otp(
    phone_number="+1234567890",
    otp_code="123456",
    otp_id=otp_response.data['otp_id'],
    purpose=OTPPurpose.HIGH_VALUE_REDEMPTION
)
```

### 🎁 Rewards Redemption
```python
rewards_client = RewardsClient(config)

# Data bundles (exact response format)
data_response = rewards_client.redeem_data_bundle(
    phone_number="+1234567890",
    data_amount="100MB",
    points_to_spend=50
)
# Returns: { "status": "success", "credited": "100MB" }

# SMS bundles  
sms_response = rewards_client.redeem_sms_bundle(
    phone_number="+1234567890",
    sms_count=20,
    points_to_spend=30
)
# Returns: { "status": "success", "credited": "20 SMS" }

# Airtime
airtime_response = rewards_client.redeem_airtime(
    phone_number="+1234567890",
    airtime_amount=5.00,
    currency="USD",
    points_to_spend=100
)
# Returns: { "status": "success", "credited": "$5.00 airtime" }
```

### 📞 USSD Menus
```python
ussd_client = USSDClient(config)

# Start USSD session
session_response = ussd_client.start_ussd_session(
    phone_number="+1234567890",
    ussd_code="*123#",
    menu_type=USSDMenuType.MAIN_MENU
)

# Handle user input
input_response = ussd_client.handle_ussd_input(
    session_id=session_response.data['session_id'],
    phone_number="+1234567890", 
    user_input="2"  # Select rewards menu
)

# Get specific menus
menu_response = ussd_client.get_ussd_menu(
    menu_type=USSDMenuType.REWARDS_MENU,
    phone_number="+1234567890"
)
```

## Configuration Options

```python
config = ApplinkConfig(
    base_url="https://api.applink.com",           # API base URL
    api_key="your_api_key",                       # Authentication key
    timeout=30,                                   # Request timeout (seconds)
    max_retries=3,                               # Maximum retry attempts
    retry_delay=1.0,                             # Initial retry delay
    retry_backoff=2.0,                           # Backoff multiplier
    mock_mode=True                               # Enable mock responses
)
```

## Error Handling

```python
from applink_client import ApplinkAPIError, ApplinkTimeoutError, ApplinkRetryExhaustedError

try:
    response = sms_client.send_daily_progress_sms(...)
except ApplinkTimeoutError as e:
    print(f"Request timed out: {e}")
except ApplinkRetryExhaustedError as e:
    print(f"All retries failed: {e}")
except ApplinkAPIError as e:
    print(f"API error: {e.status_code} - {e}")
```

## Testing

Run the example script to see all features in action:

```bash
python applink_client/example_usage.py
```

This will demonstrate:
- ✅ All 5 API services (Subscription, SMS, OTP, Rewards, USSD)
- ✅ Sync and async operations
- ✅ Mock responses matching your exact format requirements
- ✅ Error handling and retry logic
- ✅ Response normalization

## Architecture

```
applink_client/
├── __init__.py          # Public API exports
├── base.py              # Base client with shared functionality  
├── subscription.py      # Subscription management
├── sms.py              # SMS notifications
├── otp.py              # OTP verification
├── rewards.py          # Rewards redemption
├── ussd.py             # USSD menu handling
├── example_usage.py    # Comprehensive usage examples
└── requirements.txt    # Dependencies
```

## Production Integration

For production use:

1. **Disable Mock Mode**: Set `mock_mode=False` in config
2. **Configure Real Endpoints**: Update `base_url` to production API
3. **Add API Keys**: Set proper authentication credentials
4. **Adjust Timeouts**: Configure appropriate timeout values
5. **Enable Logging**: Set up structured logging for monitoring

The client library is designed to seamlessly transition from development (with mocks) to production (with real APIs) by simply changing the configuration.

## Revenue + Trust + Engagement

This integration layer directly supports your business model:

- **Revenue**: Subscription management and billing automation
- **Trust**: Secure OTP verification for high-value transactions  
- **Engagement**: Automated SMS notifications keeping families informed

The exact response formats (like `{ "status": "success", "credited": "100MB" }`) ensure compatibility with your existing systems while providing a modern, type-safe integration experience.

---

*Built for Applink's gamification education platform - where learning meets rewards!* 🎓📱