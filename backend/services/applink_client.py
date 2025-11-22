"""
Applink API Client for Porakhela
Mock implementation for development/testing
"""
import httpx
import logging
from typing import Dict, Any, Optional
from django.conf import settings
from dataclasses import dataclass

logger = logging.getLogger(__name__)


@dataclass
class ApplinkResponse:
    """Standard response format for Applink API calls"""
    success: bool
    data: Dict[str, Any]
    message: str
    transaction_id: Optional[str] = None
    error_code: Optional[str] = None


class ApplinkAPIClient:
    """
    Applink API Client for integrating with Banglalink services
    Currently implements mock responses for development
    """
    
    def __init__(self):
        self.base_url = settings.APPLINK_BASE_URL
        self.api_key = settings.APPLINK_API_KEY
        self.client_id = settings.APPLINK_CLIENT_ID
        self.client_secret = settings.APPLINK_CLIENT_SECRET
        self.client = httpx.AsyncClient(timeout=30.0)
    
    def _get_headers(self) -> Dict[str, str]:
        """Get standard headers for API requests"""
        return {
            'Authorization': f'Bearer {self.api_key}',
            'Content-Type': 'application/json',
            'X-Client-ID': self.client_id,
        }
    
    async def send_otp(self, phone_number: str, otp_code: str, message: str) -> ApplinkResponse:
        """
        Send OTP via SMS through Applink SMS API
        """
        # Mock implementation
        logger.info(f"[MOCK] Sending OTP {otp_code} to {phone_number}")
        
        # Simulate API call
        mock_response = {
            "status": "success",
            "message_id": f"msg_{phone_number}_{otp_code}",
            "delivery_status": "sent"
        }
        
        return ApplinkResponse(
            success=True,
            data=mock_response,
            message="OTP sent successfully",
            transaction_id=mock_response["message_id"]
        )
    
    async def verify_subscription(self, msisdn: str) -> ApplinkResponse:
        """
        Verify if user has active Banglalink subscription
        """
        # Mock implementation
        logger.info(f"[MOCK] Verifying subscription for {msisdn}")
        
        # Simulate subscription check
        mock_response = {
            "msisdn": msisdn,
            "is_active": True,
            "plan_type": "prepaid",
            "subscription_status": "active",
            "balance": "50.75"
        }
        
        return ApplinkResponse(
            success=True,
            data=mock_response,
            message="Subscription verified"
        )
    
    async def check_ussd_balance(self, msisdn: str) -> ApplinkResponse:
        """
        Check account balance via USSD
        """
        # Mock implementation
        logger.info(f"[MOCK] Checking USSD balance for {msisdn}")
        
        mock_response = {
            "msisdn": msisdn,
            "balance": "45.50",
            "currency": "BDT",
            "data_balance": "2.5 GB",
            "validity": "2024-12-31"
        }
        
        return ApplinkResponse(
            success=True,
            data=mock_response,
            message="Balance retrieved"
        )
    
    async def redeem_data_bundle(self, msisdn: str, bundle_type: str, amount: str) -> ApplinkResponse:
        """
        Redeem data bundle through Applink Rewards API
        """
        # Mock implementation
        logger.info(f"[MOCK] Redeeming {amount} {bundle_type} for {msisdn}")
        
        mock_response = {
            "transaction_id": f"txn_data_{msisdn}_{amount}",
            "msisdn": msisdn,
            "bundle_type": bundle_type,
            "amount": amount,
            "status": "completed",
            "validity": "30 days"
        }
        
        return ApplinkResponse(
            success=True,
            data=mock_response,
            message=f"Data bundle {amount} credited successfully",
            transaction_id=mock_response["transaction_id"]
        )
    
    async def redeem_talktime(self, msisdn: str, amount: float) -> ApplinkResponse:
        """
        Redeem talktime credit through Applink Rewards API
        """
        # Mock implementation
        logger.info(f"[MOCK] Redeeming {amount} BDT talktime for {msisdn}")
        
        mock_response = {
            "transaction_id": f"txn_talktime_{msisdn}_{amount}",
            "msisdn": msisdn,
            "amount": amount,
            "currency": "BDT",
            "status": "completed",
            "new_balance": "95.25"
        }
        
        return ApplinkResponse(
            success=True,
            data=mock_response,
            message=f"Talktime {amount} BDT credited successfully",
            transaction_id=mock_response["transaction_id"]
        )
    
    async def get_transaction_status(self, transaction_id: str) -> ApplinkResponse:
        """
        Check status of a transaction
        """
        # Mock implementation
        logger.info(f"[MOCK] Checking status for transaction {transaction_id}")
        
        mock_response = {
            "transaction_id": transaction_id,
            "status": "completed",
            "timestamp": "2024-01-15T10:30:00Z",
            "details": {
                "type": "data_bundle" if "data" in transaction_id else "talktime",
                "amount": "1 GB" if "data" in transaction_id else "50 BDT"
            }
        }
        
        return ApplinkResponse(
            success=True,
            data=mock_response,
            message="Transaction status retrieved"
        )
    
    async def close(self):
        """Close the HTTP client"""
        await self.client.aclose()


# Singleton instance
applink_client = ApplinkAPIClient()