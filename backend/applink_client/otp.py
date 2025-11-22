"""
Applink OTP API Client

Handles OTP (One-Time Password) operations for secure transactions:
- Send OTP for high-value redemptions
- Verify OTP codes
- Resend OTP if needed
- Track OTP attempts and security

Used primarily for securing valuable reward redemptions and sensitive operations.
"""

import logging
import random
import string
from datetime import datetime, timedelta
from typing import Dict, Optional
from enum import Enum

from .base import (
    ApplinkClient, 
    ApplinkConfig, 
    ApplinkResponse, 
    ApplinkResponseStatus,
    ApplinkAPIError
)

logger = logging.getLogger(__name__)


class OTPDeliveryMethod(Enum):
    """OTP delivery methods."""
    
    SMS = "sms"
    VOICE_CALL = "voice_call"
    EMAIL = "email"  # If email is available
    WHATSAPP = "whatsapp"  # If WhatsApp integration exists


class OTPPurpose(Enum):
    """Purpose categories for OTP verification."""
    
    HIGH_VALUE_REDEMPTION = "high_value_redemption"  # For expensive rewards
    ACCOUNT_VERIFICATION = "account_verification"
    PASSWORD_RESET = "password_reset"
    SUBSCRIPTION_CHANGE = "subscription_change"
    PARENTAL_CONTROL = "parental_control"
    DATA_BUNDLE_PURCHASE = "data_bundle_purchase"


class OTPStatus(Enum):
    """OTP verification status."""
    
    PENDING = "pending"
    VERIFIED = "verified"
    EXPIRED = "expired"
    FAILED = "failed"
    BLOCKED = "blocked"  # Too many failed attempts


class OTPClient(ApplinkClient):
    """
    Client for Applink OTP API.
    
    Provides methods for sending and verifying OTP codes for secure
    transactions, particularly high-value reward redemptions.
    """
    
    def __init__(self, config: Optional[ApplinkConfig] = None):
        super().__init__(config)
        self.service_name = "otp"
    
    def send_otp(
        self,
        phone_number: str,
        purpose: OTPPurpose,
        delivery_method: OTPDeliveryMethod = OTPDeliveryMethod.SMS,
        expiry_minutes: int = 10,
        redemption_details: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Send OTP for verification.
        
        Args:
            phone_number: Recipient's phone number
            purpose: Purpose of the OTP (high_value_redemption, etc.)
            delivery_method: How to deliver the OTP
            expiry_minutes: OTP expiry time in minutes
            redemption_details: Details of the redemption being secured
            
        Returns:
            ApplinkResponse with OTP sending details
            
        Raises:
            ApplinkAPIError: When OTP sending fails
        """
        data = {
            "phone_number": phone_number,
            "purpose": purpose.value,
            "delivery_method": delivery_method.value,
            "expiry_minutes": expiry_minutes,
            "redemption_details": redemption_details or {}
        }
        
        logger.info(f"Sending {purpose.value} OTP to {phone_number} via {delivery_method.value}")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/otp/send",
            data=data
        )
    
    async def send_otp_async(
        self,
        phone_number: str,
        purpose: OTPPurpose,
        delivery_method: OTPDeliveryMethod = OTPDeliveryMethod.SMS,
        expiry_minutes: int = 10,
        redemption_details: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Asynchronously send OTP for verification.
        
        Args:
            phone_number: Recipient's phone number
            purpose: Purpose of the OTP (high_value_redemption, etc.)
            delivery_method: How to deliver the OTP
            expiry_minutes: OTP expiry time in minutes
            redemption_details: Details of the redemption being secured
            
        Returns:
            ApplinkResponse with OTP sending details
        """
        data = {
            "phone_number": phone_number,
            "purpose": purpose.value,
            "delivery_method": delivery_method.value,
            "expiry_minutes": expiry_minutes,
            "redemption_details": redemption_details or {}
        }
        
        logger.info(f"Async sending {purpose.value} OTP to {phone_number} via {delivery_method.value}")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/otp/send",
            data=data
        )
    
    def verify_otp(
        self,
        phone_number: str,
        otp_code: str,
        otp_id: str,
        purpose: OTPPurpose
    ) -> ApplinkResponse:
        """
        Verify an OTP code.
        
        Args:
            phone_number: Phone number that received the OTP
            otp_code: The OTP code to verify
            otp_id: ID of the OTP request
            purpose: Purpose the OTP was sent for
            
        Returns:
            ApplinkResponse with verification result
            
        Raises:
            ApplinkAPIError: When verification fails
        """
        data = {
            "phone_number": phone_number,
            "otp_code": otp_code,
            "otp_id": otp_id,
            "purpose": purpose.value
        }
        
        logger.info(f"Verifying {purpose.value} OTP for {phone_number}")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/otp/verify",
            data=data
        )
    
    async def verify_otp_async(
        self,
        phone_number: str,
        otp_code: str,
        otp_id: str,
        purpose: OTPPurpose
    ) -> ApplinkResponse:
        """
        Asynchronously verify an OTP code.
        
        Args:
            phone_number: Phone number that received the OTP
            otp_code: The OTP code to verify
            otp_id: ID of the OTP request
            purpose: Purpose the OTP was sent for
            
        Returns:
            ApplinkResponse with verification result
        """
        data = {
            "phone_number": phone_number,
            "otp_code": otp_code,
            "otp_id": otp_id,
            "purpose": purpose.value
        }
        
        logger.info(f"Async verifying {purpose.value} OTP for {phone_number}")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/otp/verify",
            data=data
        )
    
    def resend_otp(
        self,
        phone_number: str,
        otp_id: str,
        delivery_method: Optional[OTPDeliveryMethod] = None
    ) -> ApplinkResponse:
        """
        Resend an OTP code.
        
        Args:
            phone_number: Phone number to resend OTP to
            otp_id: ID of the original OTP request
            delivery_method: New delivery method (optional)
            
        Returns:
            ApplinkResponse with resend details
            
        Raises:
            ApplinkAPIError: When resend fails
        """
        data = {
            "phone_number": phone_number,
            "otp_id": otp_id
        }
        
        if delivery_method:
            data["delivery_method"] = delivery_method.value
        
        logger.info(f"Resending OTP {otp_id} to {phone_number}")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/otp/resend",
            data=data
        )
    
    async def resend_otp_async(
        self,
        phone_number: str,
        otp_id: str,
        delivery_method: Optional[OTPDeliveryMethod] = None
    ) -> ApplinkResponse:
        """
        Asynchronously resend an OTP code.
        """
        data = {
            "phone_number": phone_number,
            "otp_id": otp_id
        }
        
        if delivery_method:
            data["delivery_method"] = delivery_method.value
        
        logger.info(f"Async resending OTP {otp_id} to {phone_number}")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/otp/resend",
            data=data
        )
    
    def get_otp_status(self, otp_id: str) -> ApplinkResponse:
        """
        Get the status of an OTP request.
        
        Args:
            otp_id: ID of the OTP request
            
        Returns:
            ApplinkResponse with OTP status details
        """
        params = {"otp_id": otp_id}
        
        logger.info(f"Checking OTP status for {otp_id}")
        
        return self._make_request_with_retry(
            method="GET",
            endpoint="/otp/status",
            params=params
        )
    
    async def get_otp_status_async(self, otp_id: str) -> ApplinkResponse:
        """
        Asynchronously get the status of an OTP request.
        """
        params = {"otp_id": otp_id}
        
        logger.info(f"Async checking OTP status for {otp_id}")
        
        return await self._make_async_request_with_retry(
            method="GET",
            endpoint="/otp/status",
            params=params
        )
    
    def send_high_value_redemption_otp(
        self,
        phone_number: str,
        reward_name: str,
        reward_value: str,
        points_cost: int,
        child_name: str
    ) -> ApplinkResponse:
        """
        Convenience method to send OTP for high-value reward redemptions.
        
        Args:
            phone_number: Parent's phone number
            reward_name: Name of the reward being redeemed
            reward_value: Value of the reward (e.g., "500MB", "10 SMS")
            points_cost: Porapoints cost of the reward
            child_name: Name of the child redeeming
            
        Returns:
            ApplinkResponse with OTP details
        """
        redemption_details = {
            "reward_name": reward_name,
            "reward_value": reward_value,
            "points_cost": points_cost,
            "child_name": child_name,
            "timestamp": datetime.now().isoformat()
        }
        
        return self.send_otp(
            phone_number=phone_number,
            purpose=OTPPurpose.HIGH_VALUE_REDEMPTION,
            delivery_method=OTPDeliveryMethod.SMS,
            expiry_minutes=10,
            redemption_details=redemption_details
        )
    
    async def send_high_value_redemption_otp_async(
        self,
        phone_number: str,
        reward_name: str,
        reward_value: str,
        points_cost: int,
        child_name: str
    ) -> ApplinkResponse:
        """
        Asynchronously send OTP for high-value reward redemptions.
        """
        redemption_details = {
            "reward_name": reward_name,
            "reward_value": reward_value,
            "points_cost": points_cost,
            "child_name": child_name,
            "timestamp": datetime.now().isoformat()
        }
        
        return await self.send_otp_async(
            phone_number=phone_number,
            purpose=OTPPurpose.HIGH_VALUE_REDEMPTION,
            delivery_method=OTPDeliveryMethod.SMS,
            expiry_minutes=10,
            redemption_details=redemption_details
        )
    
    def _generate_mock_otp(self) -> str:
        """Generate a mock OTP code for development."""
        return ''.join(random.choices(string.digits, k=6))
    
    def _get_mock_response(
        self, 
        method: str, 
        endpoint: str, 
        data: Optional[Dict] = None,
        params: Optional[Dict] = None,
        request_id: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Generate mock responses for OTP operations.
        
        Args:
            method: HTTP method
            endpoint: API endpoint
            data: Request data
            params: Query parameters
            request_id: Request tracking ID
            
        Returns:
            Mock ApplinkResponse with realistic OTP data
        """
        mock_otp_code = self._generate_mock_otp()
        
        mock_responses = {
            "/otp/send": {
                "status": "success",
                "message": "OTP sent successfully",
                "data": {
                    "otp_id": f"otp_{request_id}",
                    "phone_number": data.get("phone_number") if data else "+1234567890",
                    "purpose": data.get("purpose") if data else "high_value_redemption",
                    "delivery_method": data.get("delivery_method") if data else "sms",
                    "status": "sent",
                    "sent_at": datetime.now().isoformat(),
                    "expires_at": (datetime.now() + timedelta(minutes=data.get("expiry_minutes", 10))).isoformat(),
                    "attempts_remaining": 3,
                    "resend_available": True,
                    # In production, OTP code would NOT be returned
                    "mock_otp_code": mock_otp_code  # Only for development/testing
                }
            },
            "/otp/verify": {
                "status": "success" if data and data.get("otp_code") == "123456" else "error",
                "message": "OTP verified successfully" if data and data.get("otp_code") == "123456" else "Invalid OTP code",
                "data": {
                    "otp_id": data.get("otp_id") if data else f"otp_{request_id}",
                    "phone_number": data.get("phone_number") if data else "+1234567890",
                    "verification_status": "verified" if data and data.get("otp_code") == "123456" else "failed",
                    "verified_at": datetime.now().isoformat() if data and data.get("otp_code") == "123456" else None,
                    "attempts_used": 1,
                    "attempts_remaining": 2 if data and data.get("otp_code") != "123456" else 0,
                    "purpose": data.get("purpose") if data else "high_value_redemption"
                }
            },
            "/otp/resend": {
                "status": "success",
                "message": "OTP resent successfully",
                "data": {
                    "otp_id": data.get("otp_id") if data else f"otp_{request_id}",
                    "phone_number": data.get("phone_number") if data else "+1234567890",
                    "delivery_method": data.get("delivery_method") if data else "sms",
                    "resent_at": datetime.now().isoformat(),
                    "expires_at": (datetime.now() + timedelta(minutes=10)).isoformat(),
                    "resend_count": 1,
                    "max_resends": 3,
                    # Mock OTP for development
                    "mock_otp_code": self._generate_mock_otp()
                }
            },
            "/otp/status": {
                "status": "success",
                "message": "OTP status retrieved",
                "data": {
                    "otp_id": params.get("otp_id") if params else f"otp_{request_id}",
                    "phone_number": "+1234567890",
                    "purpose": "high_value_redemption",
                    "delivery_method": "sms",
                    "status": "pending",
                    "sent_at": datetime.now().isoformat(),
                    "expires_at": (datetime.now() + timedelta(minutes=8)).isoformat(),
                    "attempts_used": 0,
                    "attempts_remaining": 3,
                    "resend_count": 0,
                    "max_resends": 3,
                    "is_expired": False,
                    "is_blocked": False
                }
            }
        }
        
        response_data = mock_responses.get(endpoint, {
            "status": "success",
            "message": "Mock OTP response",
            "data": {
                "otp_id": f"otp_{request_id}",
                "status": "sent"
            }
        })
        
        return self._normalize_response(response_data, request_id)