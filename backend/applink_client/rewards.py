"""
Applink Rewards API Client

Handles Porapoints reward redemption including:
- Redeem Porapoints for various rewards (data bundles, SMS, airtime)
- Check available rewards catalog
- Track redemption history
- Manage reward activation and delivery

Provides the exact mock response format: { "status": "success", "credited": "100MB" }
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


class RewardType(Enum):
    """Types of available rewards."""
    
    DATA_BUNDLE = "data_bundle"
    SMS_BUNDLE = "sms_bundle" 
    AIRTIME = "airtime"
    VOICE_MINUTES = "voice_minutes"
    INTERNATIONAL_CALL = "international_call"
    PREMIUM_CONTENT = "premium_content"
    PHYSICAL_GIFT = "physical_gift"


class RewardCategory(Enum):
    """Reward categories for organization."""
    
    COMMUNICATION = "communication"  # SMS, voice, data
    ENTERTAINMENT = "entertainment"  # Premium content, games
    EDUCATIONAL = "educational"     # Learning resources
    PHYSICAL = "physical"           # Physical items, vouchers


class RedemptionStatus(Enum):
    """Status of a reward redemption."""
    
    PENDING = "pending"
    PROCESSING = "processing"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"
    EXPIRED = "expired"


class RewardsClient(ApplinkClient):
    """
    Client for Applink Rewards API.
    
    Provides methods for redeeming Porapoints for various rewards
    and managing the rewards catalog and redemption process.
    """
    
    def __init__(self, config: Optional[ApplinkConfig] = None):
        super().__init__(config)
        self.service_name = "rewards"
    
    def redeem_reward(
        self,
        phone_number: str,
        reward_id: str,
        points_to_spend: int,
        child_name: Optional[str] = None,
        metadata: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Redeem Porapoints for a specific reward.
        
        Returns the exact format: { "status": "success", "credited": "100MB" }
        
        Args:
            phone_number: User's phone number
            reward_id: ID of the reward to redeem
            points_to_spend: Number of Porapoints to spend
            child_name: Name of the child redeeming (optional)
            metadata: Additional redemption metadata
            
        Returns:
            ApplinkResponse with redemption details and credited amount
            
        Raises:
            ApplinkAPIError: When redemption fails
        """
        data = {
            "phone_number": phone_number,
            "reward_id": reward_id,
            "points_to_spend": points_to_spend,
            "child_name": child_name,
            "metadata": metadata or {}
        }
        
        logger.info(f"Redeeming reward {reward_id} for {phone_number}, spending {points_to_spend} points")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/rewards/redeem",
            data=data
        )
    
    async def redeem_reward_async(
        self,
        phone_number: str,
        reward_id: str,
        points_to_spend: int,
        child_name: Optional[str] = None,
        metadata: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Asynchronously redeem Porapoints for a specific reward.
        
        Args:
            phone_number: User's phone number
            reward_id: ID of the reward to redeem
            points_to_spend: Number of Porapoints to spend
            child_name: Name of the child redeeming (optional)
            metadata: Additional redemption metadata
            
        Returns:
            ApplinkResponse with redemption details and credited amount
        """
        data = {
            "phone_number": phone_number,
            "reward_id": reward_id,
            "points_to_spend": points_to_spend,
            "child_name": child_name,
            "metadata": metadata or {}
        }
        
        logger.info(f"Async redeeming reward {reward_id} for {phone_number}, spending {points_to_spend} points")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/rewards/redeem",
            data=data
        )
    
    def redeem_data_bundle(
        self,
        phone_number: str,
        data_amount: str,
        points_to_spend: int,
        child_name: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Convenience method to redeem data bundle.
        
        Args:
            phone_number: User's phone number
            data_amount: Data amount (e.g., "100MB", "1GB")
            points_to_spend: Porapoints to spend
            child_name: Name of child redeeming
            
        Returns:
            ApplinkResponse with exact format: { "status": "success", "credited": "100MB" }
        """
        return self.redeem_reward(
            phone_number=phone_number,
            reward_id=f"data_{data_amount.lower().replace('gb', 'g').replace('mb', 'm')}",
            points_to_spend=points_to_spend,
            child_name=child_name,
            metadata={"reward_type": "data_bundle", "data_amount": data_amount}
        )
    
    async def redeem_data_bundle_async(
        self,
        phone_number: str,
        data_amount: str,
        points_to_spend: int,
        child_name: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Asynchronously redeem data bundle.
        """
        return await self.redeem_reward_async(
            phone_number=phone_number,
            reward_id=f"data_{data_amount.lower().replace('gb', 'g').replace('mb', 'm')}",
            points_to_spend=points_to_spend,
            child_name=child_name,
            metadata={"reward_type": "data_bundle", "data_amount": data_amount}
        )
    
    def redeem_sms_bundle(
        self,
        phone_number: str,
        sms_count: int,
        points_to_spend: int,
        child_name: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Convenience method to redeem SMS bundle.
        
        Args:
            phone_number: User's phone number
            sms_count: Number of SMS messages
            points_to_spend: Porapoints to spend
            child_name: Name of child redeeming
            
        Returns:
            ApplinkResponse with format: { "status": "success", "credited": "20 SMS" }
        """
        return self.redeem_reward(
            phone_number=phone_number,
            reward_id=f"sms_{sms_count}",
            points_to_spend=points_to_spend,
            child_name=child_name,
            metadata={"reward_type": "sms_bundle", "sms_count": sms_count}
        )
    
    async def redeem_sms_bundle_async(
        self,
        phone_number: str,
        sms_count: int,
        points_to_spend: int,
        child_name: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Asynchronously redeem SMS bundle.
        """
        return await self.redeem_reward_async(
            phone_number=phone_number,
            reward_id=f"sms_{sms_count}",
            points_to_spend=points_to_spend,
            child_name=child_name,
            metadata={"reward_type": "sms_bundle", "sms_count": sms_count}
        )
    
    def redeem_airtime(
        self,
        phone_number: str,
        airtime_amount: float,
        currency: str,
        points_to_spend: int,
        child_name: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Convenience method to redeem airtime credit.
        
        Args:
            phone_number: User's phone number
            airtime_amount: Airtime amount (e.g., 5.00)
            currency: Currency code (e.g., "USD", "KES")
            points_to_spend: Porapoints to spend
            child_name: Name of child redeeming
            
        Returns:
            ApplinkResponse with format: { "status": "success", "credited": "$5.00 airtime" }
        """
        return self.redeem_reward(
            phone_number=phone_number,
            reward_id=f"airtime_{int(airtime_amount * 100)}_{currency.lower()}",
            points_to_spend=points_to_spend,
            child_name=child_name,
            metadata={
                "reward_type": "airtime", 
                "airtime_amount": airtime_amount,
                "currency": currency
            }
        )
    
    async def redeem_airtime_async(
        self,
        phone_number: str,
        airtime_amount: float,
        currency: str,
        points_to_spend: int,
        child_name: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Asynchronously redeem airtime credit.
        """
        return await self.redeem_reward_async(
            phone_number=phone_number,
            reward_id=f"airtime_{int(airtime_amount * 100)}_{currency.lower()}",
            points_to_spend=points_to_spend,
            child_name=child_name,
            metadata={
                "reward_type": "airtime", 
                "airtime_amount": airtime_amount,
                "currency": currency
            }
        )
    
    def get_rewards_catalog(
        self,
        category: Optional[RewardCategory] = None,
        min_points: Optional[int] = None,
        max_points: Optional[int] = None
    ) -> ApplinkResponse:
        """
        Get available rewards catalog.
        
        Args:
            category: Filter by reward category (optional)
            min_points: Minimum points required (optional)
            max_points: Maximum points required (optional)
            
        Returns:
            ApplinkResponse with available rewards and their point costs
        """
        params = {}
        
        if category:
            params["category"] = category.value
        if min_points:
            params["min_points"] = min_points
        if max_points:
            params["max_points"] = max_points
        
        logger.info(f"Fetching rewards catalog with filters: {params}")
        
        return self._make_request_with_retry(
            method="GET",
            endpoint="/rewards/catalog",
            params=params
        )
    
    async def get_rewards_catalog_async(
        self,
        category: Optional[RewardCategory] = None,
        min_points: Optional[int] = None,
        max_points: Optional[int] = None
    ) -> ApplinkResponse:
        """
        Asynchronously get available rewards catalog.
        """
        params = {}
        
        if category:
            params["category"] = category.value
        if min_points:
            params["min_points"] = min_points
        if max_points:
            params["max_points"] = max_points
        
        logger.info(f"Async fetching rewards catalog with filters: {params}")
        
        return await self._make_async_request_with_retry(
            method="GET",
            endpoint="/rewards/catalog",
            params=params
        )
    
    def get_redemption_history(
        self,
        phone_number: str,
        limit: int = 50,
        days_back: int = 30
    ) -> ApplinkResponse:
        """
        Get redemption history for a user.
        
        Args:
            phone_number: User's phone number
            limit: Maximum number of records to return
            days_back: How many days back to look
            
        Returns:
            ApplinkResponse with redemption history
        """
        params = {
            "phone_number": phone_number,
            "limit": limit,
            "days_back": days_back
        }
        
        logger.info(f"Fetching redemption history for {phone_number}")
        
        return self._make_request_with_retry(
            method="GET",
            endpoint="/rewards/history",
            params=params
        )
    
    async def get_redemption_history_async(
        self,
        phone_number: str,
        limit: int = 50,
        days_back: int = 30
    ) -> ApplinkResponse:
        """
        Asynchronously get redemption history for a user.
        """
        params = {
            "phone_number": phone_number,
            "limit": limit,
            "days_back": days_back
        }
        
        logger.info(f"Async fetching redemption history for {phone_number}")
        
        return await self._make_async_request_with_retry(
            method="GET",
            endpoint="/rewards/history",
            params=params
        )
    
    def get_redemption_status(self, redemption_id: str) -> ApplinkResponse:
        """
        Get status of a specific redemption.
        
        Args:
            redemption_id: ID of the redemption to check
            
        Returns:
            ApplinkResponse with redemption status and details
        """
        params = {"redemption_id": redemption_id}
        
        logger.info(f"Checking status of redemption {redemption_id}")
        
        return self._make_request_with_retry(
            method="GET",
            endpoint="/rewards/status",
            params=params
        )
    
    async def get_redemption_status_async(self, redemption_id: str) -> ApplinkResponse:
        """
        Asynchronously get status of a specific redemption.
        """
        params = {"redemption_id": redemption_id}
        
        logger.info(f"Async checking status of redemption {redemption_id}")
        
        return await self._make_async_request_with_retry(
            method="GET",
            endpoint="/rewards/status",
            params=params
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
        Generate mock responses for rewards operations.
        
        Returns the exact format: { "status": "success", "credited": "100MB" }
        
        Args:
            method: HTTP method
            endpoint: API endpoint
            data: Request data
            params: Query parameters
            request_id: Request tracking ID
            
        Returns:
            Mock ApplinkResponse with the specified format
        """
        mock_responses = {
            "/rewards/redeem": {
                "status": "success",
                "credited": self._get_credited_amount(data),
                "message": "Reward redeemed successfully",
                "data": {
                    "redemption_id": f"redemption_{request_id}",
                    "phone_number": data.get("phone_number") if data else "+1234567890",
                    "reward_id": data.get("reward_id") if data else "data_100m",
                    "points_spent": data.get("points_to_spend") if data else 50,
                    "credited": self._get_credited_amount(data),
                    "child_name": data.get("child_name") if data else "Alex",
                    "redeemed_at": datetime.now().isoformat(),
                    "activation_status": "activated",
                    "expires_at": (datetime.now() + timedelta(days=30)).isoformat(),
                    "transaction_id": f"txn_{request_id}"
                }
            },
            "/rewards/catalog": {
                "status": "success",
                "message": "Rewards catalog retrieved",
                "data": {
                    "rewards": [
                        {
                            "id": "data_100m",
                            "name": "100MB Data Bundle",
                            "description": "Get 100MB of mobile data instantly",
                            "type": "data_bundle",
                            "category": "communication", 
                            "points_cost": 50,
                            "credited_amount": "100MB",
                            "validity_days": 30,
                            "available": True
                        },
                        {
                            "id": "data_500m",
                            "name": "500MB Data Bundle",
                            "description": "Get 500MB of mobile data instantly",
                            "type": "data_bundle",
                            "category": "communication",
                            "points_cost": 200,
                            "credited_amount": "500MB",
                            "validity_days": 30,
                            "available": True
                        },
                        {
                            "id": "sms_20",
                            "name": "20 SMS Bundle",
                            "description": "Get 20 SMS messages",
                            "type": "sms_bundle",
                            "category": "communication",
                            "points_cost": 30,
                            "credited_amount": "20 SMS",
                            "validity_days": 30,
                            "available": True
                        },
                        {
                            "id": "airtime_500_usd",
                            "name": "$5.00 Airtime",
                            "description": "Get $5.00 airtime credit",
                            "type": "airtime",
                            "category": "communication",
                            "points_cost": 100,
                            "credited_amount": "$5.00 airtime",
                            "validity_days": 365,
                            "available": True
                        }
                    ],
                    "total_rewards": 4,
                    "categories": ["communication", "entertainment", "educational"]
                }
            },
            "/rewards/history": {
                "status": "success",
                "message": "Redemption history retrieved",
                "data": {
                    "phone_number": params.get("phone_number") if params else "+1234567890",
                    "redemptions": [
                        {
                            "redemption_id": f"redemption_001",
                            "reward_name": "100MB Data Bundle",
                            "credited": "100MB",
                            "points_spent": 50,
                            "child_name": "Alex",
                            "redeemed_at": (datetime.now() - timedelta(days=2)).isoformat(),
                            "status": "completed",
                            "expires_at": (datetime.now() + timedelta(days=28)).isoformat()
                        },
                        {
                            "redemption_id": f"redemption_002",
                            "reward_name": "20 SMS Bundle",
                            "credited": "20 SMS",
                            "points_spent": 30,
                            "child_name": "Emma",
                            "redeemed_at": (datetime.now() - timedelta(days=5)).isoformat(),
                            "status": "completed",
                            "expires_at": (datetime.now() + timedelta(days=25)).isoformat()
                        }
                    ],
                    "total_redemptions": 2,
                    "total_points_spent": 80
                }
            },
            "/rewards/status": {
                "status": "success",
                "message": "Redemption status retrieved",
                "data": {
                    "redemption_id": params.get("redemption_id") if params else f"redemption_{request_id}",
                    "status": "completed",
                    "reward_name": "100MB Data Bundle",
                    "credited": "100MB",
                    "phone_number": "+1234567890",
                    "points_spent": 50,
                    "redeemed_at": datetime.now().isoformat(),
                    "activated_at": datetime.now().isoformat(),
                    "expires_at": (datetime.now() + timedelta(days=30)).isoformat(),
                    "transaction_id": f"txn_{request_id}",
                    "provider_status": "activated"
                }
            }
        }
        
        response_data = mock_responses.get(endpoint, {
            "status": "success",
            "credited": "100MB",
            "message": "Mock reward response",
            "data": {}
        })
        
        return self._normalize_response(response_data, request_id)
    
    def _get_credited_amount(self, data: Optional[Dict]) -> str:
        """
        Extract the credited amount from request data for mock responses.
        
        Args:
            data: Request data containing reward information
            
        Returns:
            Credited amount string (e.g., "100MB", "20 SMS", "$5.00 airtime")
        """
        if not data:
            return "100MB"
        
        reward_id = data.get("reward_id", "")
        metadata = data.get("metadata", {})
        
        # Handle data bundles
        if "data_" in reward_id:
            if "data_amount" in metadata:
                return metadata["data_amount"]
            elif "100m" in reward_id:
                return "100MB"
            elif "500m" in reward_id:
                return "500MB"
            elif "1g" in reward_id:
                return "1GB"
            else:
                return "100MB"
        
        # Handle SMS bundles
        elif "sms_" in reward_id:
            if "sms_count" in metadata:
                return f"{metadata['sms_count']} SMS"
            else:
                # Extract number from reward_id
                import re
                match = re.search(r'sms_(\d+)', reward_id)
                if match:
                    return f"{match.group(1)} SMS"
                return "20 SMS"
        
        # Handle airtime
        elif "airtime_" in reward_id:
            if "airtime_amount" in metadata and "currency" in metadata:
                currency = metadata["currency"]
                amount = metadata["airtime_amount"]
                if currency.upper() == "USD":
                    return f"${amount:.2f} airtime"
                else:
                    return f"{amount:.2f} {currency} airtime"
            else:
                return "$5.00 airtime"
        
        # Default fallback
        return "100MB"