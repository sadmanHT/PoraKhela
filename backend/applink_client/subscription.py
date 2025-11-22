"""
Applink Subscription API Client

Handles user subscription management including:
- Subscribe users to Applink services
- Unsubscribe users from services
- Check subscription status
- Handle subscription billing and renewals

Supports both sync and async operations with comprehensive error handling.
"""

import logging
from datetime import datetime, timedelta
from typing import Dict, Optional, List
from enum import Enum

from .base import (
    ApplinkClient, 
    ApplinkConfig, 
    ApplinkResponse, 
    ApplinkResponseStatus,
    ApplinkAPIError
)

logger = logging.getLogger(__name__)


class SubscriptionPlan(Enum):
    """Available subscription plans."""
    
    BASIC = "basic"
    PREMIUM = "premium"
    FAMILY = "family"
    STUDENT = "student"


class SubscriptionStatus(Enum):
    """Subscription status values."""
    
    ACTIVE = "active"
    INACTIVE = "inactive"
    SUSPENDED = "suspended"
    EXPIRED = "expired"
    PENDING = "pending"
    CANCELLED = "cancelled"


class SubscriptionClient(ApplinkClient):
    """
    Client for Applink Subscription API.
    
    Provides methods for managing user subscriptions including subscribing,
    unsubscribing, checking status, and handling billing operations.
    """
    
    def __init__(self, config: Optional[ApplinkConfig] = None):
        super().__init__(config)
        self.service_name = "subscription"
    
    def subscribe_user(
        self, 
        phone_number: str, 
        plan: SubscriptionPlan = SubscriptionPlan.BASIC,
        duration_days: int = 30,
        metadata: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Subscribe a user to an Applink service plan.
        
        Args:
            phone_number: User's phone number (international format)
            plan: Subscription plan to activate
            duration_days: Subscription duration in days
            metadata: Additional subscription metadata
            
        Returns:
            ApplinkResponse with subscription details
            
        Raises:
            ApplinkAPIError: When subscription fails
        """
        data = {
            "phone_number": phone_number,
            "plan": plan.value,
            "duration_days": duration_days,
            "metadata": metadata or {}
        }
        
        logger.info(f"Subscribing user {phone_number} to {plan.value} plan for {duration_days} days")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/subscription/subscribe",
            data=data
        )
    
    async def subscribe_user_async(
        self, 
        phone_number: str, 
        plan: SubscriptionPlan = SubscriptionPlan.BASIC,
        duration_days: int = 30,
        metadata: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Asynchronously subscribe a user to an Applink service plan.
        
        Args:
            phone_number: User's phone number (international format)
            plan: Subscription plan to activate
            duration_days: Subscription duration in days
            metadata: Additional subscription metadata
            
        Returns:
            ApplinkResponse with subscription details
            
        Raises:
            ApplinkAPIError: When subscription fails
        """
        data = {
            "phone_number": phone_number,
            "plan": plan.value,
            "duration_days": duration_days,
            "metadata": metadata or {}
        }
        
        logger.info(f"Async subscribing user {phone_number} to {plan.value} plan for {duration_days} days")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/subscription/subscribe",
            data=data
        )
    
    def unsubscribe_user(
        self, 
        phone_number: str, 
        reason: Optional[str] = None,
        immediate: bool = False
    ) -> ApplinkResponse:
        """
        Unsubscribe a user from Applink services.
        
        Args:
            phone_number: User's phone number 
            reason: Reason for unsubscribing (optional)
            immediate: Whether to cancel immediately or at period end
            
        Returns:
            ApplinkResponse with unsubscription confirmation
            
        Raises:
            ApplinkAPIError: When unsubscription fails
        """
        data = {
            "phone_number": phone_number,
            "reason": reason or "user_request",
            "immediate": immediate
        }
        
        logger.info(f"Unsubscribing user {phone_number}, immediate: {immediate}")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/subscription/unsubscribe",
            data=data
        )
    
    async def unsubscribe_user_async(
        self, 
        phone_number: str, 
        reason: Optional[str] = None,
        immediate: bool = False
    ) -> ApplinkResponse:
        """
        Asynchronously unsubscribe a user from Applink services.
        
        Args:
            phone_number: User's phone number 
            reason: Reason for unsubscribing (optional)
            immediate: Whether to cancel immediately or at period end
            
        Returns:
            ApplinkResponse with unsubscription confirmation
            
        Raises:
            ApplinkAPIError: When unsubscription fails
        """
        data = {
            "phone_number": phone_number,
            "reason": reason or "user_request",
            "immediate": immediate
        }
        
        logger.info(f"Async unsubscribing user {phone_number}, immediate: {immediate}")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/subscription/unsubscribe",
            data=data
        )
    
    def get_subscription_status(self, phone_number: str) -> ApplinkResponse:
        """
        Get current subscription status for a user.
        
        Args:
            phone_number: User's phone number
            
        Returns:
            ApplinkResponse with subscription status and details
            
        Raises:
            ApplinkAPIError: When status check fails
        """
        params = {"phone_number": phone_number}
        
        logger.info(f"Checking subscription status for {phone_number}")
        
        return self._make_request_with_retry(
            method="GET",
            endpoint="/subscription/status",
            params=params
        )
    
    async def get_subscription_status_async(self, phone_number: str) -> ApplinkResponse:
        """
        Asynchronously get current subscription status for a user.
        
        Args:
            phone_number: User's phone number
            
        Returns:
            ApplinkResponse with subscription status and details
            
        Raises:
            ApplinkAPIError: When status check fails
        """
        params = {"phone_number": phone_number}
        
        logger.info(f"Async checking subscription status for {phone_number}")
        
        return await self._make_async_request_with_retry(
            method="GET",
            endpoint="/subscription/status",
            params=params
        )
    
    def renew_subscription(
        self, 
        phone_number: str, 
        duration_days: int = 30,
        plan: Optional[SubscriptionPlan] = None
    ) -> ApplinkResponse:
        """
        Renew an existing subscription.
        
        Args:
            phone_number: User's phone number
            duration_days: Renewal duration in days
            plan: New plan (if upgrading/downgrading)
            
        Returns:
            ApplinkResponse with renewal confirmation
            
        Raises:
            ApplinkAPIError: When renewal fails
        """
        data = {
            "phone_number": phone_number,
            "duration_days": duration_days
        }
        
        if plan:
            data["plan"] = plan.value
        
        logger.info(f"Renewing subscription for {phone_number}, duration: {duration_days} days")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/subscription/renew",
            data=data
        )
    
    async def renew_subscription_async(
        self, 
        phone_number: str, 
        duration_days: int = 30,
        plan: Optional[SubscriptionPlan] = None
    ) -> ApplinkResponse:
        """
        Asynchronously renew an existing subscription.
        
        Args:
            phone_number: User's phone number
            duration_days: Renewal duration in days
            plan: New plan (if upgrading/downgrading)
            
        Returns:
            ApplinkResponse with renewal confirmation
            
        Raises:
            ApplinkAPIError: When renewal fails
        """
        data = {
            "phone_number": phone_number,
            "duration_days": duration_days
        }
        
        if plan:
            data["plan"] = plan.value
        
        logger.info(f"Async renewing subscription for {phone_number}, duration: {duration_days} days")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/subscription/renew",
            data=data
        )
    
    def get_subscription_plans(self) -> ApplinkResponse:
        """
        Get available subscription plans and pricing.
        
        Returns:
            ApplinkResponse with available plans and pricing information
            
        Raises:
            ApplinkAPIError: When request fails
        """
        logger.info("Fetching available subscription plans")
        
        return self._make_request_with_retry(
            method="GET",
            endpoint="/subscription/plans"
        )
    
    async def get_subscription_plans_async(self) -> ApplinkResponse:
        """
        Asynchronously get available subscription plans and pricing.
        
        Returns:
            ApplinkResponse with available plans and pricing information
            
        Raises:
            ApplinkAPIError: When request fails
        """
        logger.info("Async fetching available subscription plans")
        
        return await self._make_async_request_with_retry(
            method="GET",
            endpoint="/subscription/plans"
        )
    
    def _get_mock_response(
        self, 
        method: str, 
        endpoint: str, 
        data: Optional[Dict] = None,
        params: Optional[Dict] = None,
        request_id: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Generate mock responses for subscription operations.
        
        Args:
            method: HTTP method
            endpoint: API endpoint
            data: Request data
            params: Query parameters
            request_id: Request tracking ID
            
        Returns:
            Mock ApplinkResponse with realistic subscription data
        """
        mock_responses = {
            "/subscription/subscribe": {
                "status": "success",
                "message": "User successfully subscribed",
                "data": {
                    "subscription_id": f"sub_{request_id}",
                    "phone_number": data.get("phone_number") if data else "+1234567890",
                    "plan": data.get("plan") if data else "basic",
                    "status": "active",
                    "start_date": datetime.now().isoformat(),
                    "end_date": (datetime.now() + timedelta(days=data.get("duration_days", 30) if data else 30)).isoformat(),
                    "auto_renew": True,
                    "billing_cycle": "monthly"
                }
            },
            "/subscription/unsubscribe": {
                "status": "success", 
                "message": "User successfully unsubscribed",
                "data": {
                    "phone_number": data.get("phone_number") if data else "+1234567890",
                    "unsubscribed_at": datetime.now().isoformat(),
                    "effective_date": datetime.now().isoformat() if data and data.get("immediate") else (datetime.now() + timedelta(days=30)).isoformat(),
                    "reason": data.get("reason") if data else "user_request"
                }
            },
            "/subscription/status": {
                "status": "success",
                "message": "Subscription status retrieved",
                "data": {
                    "phone_number": params.get("phone_number") if params else "+1234567890",
                    "subscription_status": "active",
                    "plan": "premium",
                    "start_date": (datetime.now() - timedelta(days=15)).isoformat(),
                    "end_date": (datetime.now() + timedelta(days=15)).isoformat(),
                    "days_remaining": 15,
                    "auto_renew": True,
                    "last_payment": (datetime.now() - timedelta(days=15)).isoformat(),
                    "next_billing_date": (datetime.now() + timedelta(days=15)).isoformat()
                }
            },
            "/subscription/renew": {
                "status": "success",
                "message": "Subscription renewed successfully", 
                "data": {
                    "phone_number": data.get("phone_number") if data else "+1234567890",
                    "renewed_at": datetime.now().isoformat(),
                    "new_end_date": (datetime.now() + timedelta(days=data.get("duration_days", 30) if data else 30)).isoformat(),
                    "plan": data.get("plan", "basic") if data else "basic",
                    "payment_amount": 9.99,
                    "currency": "USD"
                }
            },
            "/subscription/plans": {
                "status": "success",
                "message": "Available subscription plans",
                "data": {
                    "plans": [
                        {
                            "id": "basic",
                            "name": "Basic Plan",
                            "price": 4.99,
                            "currency": "USD",
                            "duration": "monthly",
                            "features": ["5GB Data", "SMS Notifications", "Basic Support"]
                        },
                        {
                            "id": "premium", 
                            "name": "Premium Plan",
                            "price": 9.99,
                            "currency": "USD",
                            "duration": "monthly",
                            "features": ["Unlimited Data", "SMS Notifications", "Priority Support", "Family Sharing"]
                        },
                        {
                            "id": "family",
                            "name": "Family Plan",
                            "price": 19.99,
                            "currency": "USD", 
                            "duration": "monthly",
                            "features": ["Unlimited Data", "Up to 5 Users", "SMS Notifications", "Priority Support", "Parental Controls"]
                        }
                    ]
                }
            }
        }
        
        response_data = mock_responses.get(endpoint, {
            "status": "success",
            "message": "Mock response",
            "data": {}
        })
        
        return self._normalize_response(response_data, request_id)