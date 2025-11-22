"""
Applink API Integration Layer

This package provides a unified interface for interacting with all Applink APIs:
- Subscription management
- SMS notifications
- OTP verification
- Rewards redemption
- USSD menu handling

Each module supports both async and sync operations with comprehensive error handling,
retry logic, timeout handling, and response normalization.
"""

from .base import (
    ApplinkAPIError, 
    ApplinkClient, 
    ApplinkConfig, 
    ApplinkResponse,
    ApplinkResponseStatus
)
from .subscription import SubscriptionClient, SubscriptionPlan, SubscriptionStatus
from .sms import SMSClient, SMSType, SMSPriority
from .otp import OTPClient, OTPPurpose, OTPDeliveryMethod, OTPStatus
from .rewards import RewardsClient, RewardType, RewardCategory, RedemptionStatus
from .ussd import USSDClient, USSDMenuType, USSDSessionStatus

__version__ = "1.0.0"
__all__ = [
    # Base classes
    "ApplinkAPIError",
    "ApplinkClient", 
    "ApplinkConfig",
    "ApplinkResponse",
    "ApplinkResponseStatus",
    # Service clients
    "SubscriptionClient",
    "SMSClient", 
    "OTPClient",
    "RewardsClient",
    "USSDClient",
    # Enums and types
    "SubscriptionPlan",
    "SubscriptionStatus", 
    "SMSType",
    "SMSPriority",
    "OTPPurpose",
    "OTPDeliveryMethod", 
    "OTPStatus",
    "RewardType",
    "RewardCategory",
    "RedemptionStatus",
    "USSDMenuType",
    "USSDSessionStatus",
]


def get_client(service: str, **kwargs):
    """
    Factory function to get a specific service client.
    
    Args:
        service: The service name ('subscription', 'sms', 'otp', 'rewards', 'ussd')
        **kwargs: Configuration options passed to the client
    
    Returns:
        The appropriate client instance
    """
    clients = {
        'subscription': SubscriptionClient,
        'sms': SMSClient,
        'otp': OTPClient,
        'rewards': RewardsClient,
        'ussd': USSDClient,
    }
    
    if service not in clients:
        raise ValueError(f"Unknown service '{service}'. Available: {list(clients.keys())}")
    
    return clients[service](**kwargs)