"""
Applink SMS API Client

Handles SMS notifications and communications including:
- Daily progress notifications 
- Achievement alerts
- Reward notifications
- Custom SMS messages
- Bulk SMS campaigns

Supports rich message templates and personalization.
"""

import logging
from datetime import datetime
from typing import Dict, Optional, List, Union
from enum import Enum

from .base import (
    ApplinkClient, 
    ApplinkConfig, 
    ApplinkResponse, 
    ApplinkResponseStatus,
    ApplinkAPIError
)

logger = logging.getLogger(__name__)


class SMSType(Enum):
    """Types of SMS messages."""
    
    DAILY_PROGRESS = "daily_progress"
    ACHIEVEMENT = "achievement" 
    REWARD_NOTIFICATION = "reward_notification"
    STREAK_BONUS = "streak_bonus"
    SUBSCRIPTION_ALERT = "subscription_alert"
    CUSTOM = "custom"
    BULK_CAMPAIGN = "bulk_campaign"


class SMSPriority(Enum):
    """SMS delivery priority levels."""
    
    LOW = "low"
    NORMAL = "normal"
    HIGH = "high"
    URGENT = "urgent"


class SMSClient(ApplinkClient):
    """
    Client for Applink SMS API.
    
    Provides methods for sending various types of SMS notifications
    including daily progress updates, achievements, and custom messages.
    """
    
    def __init__(self, config: Optional[ApplinkConfig] = None):
        super().__init__(config)
        self.service_name = "sms"
    
    def send_daily_progress_sms(
        self,
        phone_number: str,
        child_name: str,
        lessons_completed: int,
        points_earned: int,
        streak_days: Optional[int] = None,
        additional_data: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Send daily progress SMS notification.
        
        Example: "Your child completed 3 lessons today and earned 50 Porapoints!"
        
        Args:
            phone_number: Recipient's phone number
            child_name: Name of the child
            lessons_completed: Number of lessons completed today
            points_earned: Porapoints earned today
            streak_days: Current learning streak (optional)
            additional_data: Additional context data
            
        Returns:
            ApplinkResponse with SMS delivery details
            
        Raises:
            ApplinkAPIError: When SMS sending fails
        """
        # Build the message using the template format you specified
        if streak_days and streak_days > 1:
            message = (
                f"🎉 {child_name} completed {lessons_completed} lessons today and earned "
                f"{points_earned} Porapoints! That's a {streak_days}-day learning streak! 🔥"
            )
        else:
            message = (
                f"📚 {child_name} completed {lessons_completed} lessons today and earned "
                f"{points_earned} Porapoints! Keep up the great work! 💪"
            )
        
        data = {
            "phone_number": phone_number,
            "message": message,
            "message_type": SMSType.DAILY_PROGRESS.value,
            "priority": SMSPriority.NORMAL.value,
            "template_data": {
                "child_name": child_name,
                "lessons_completed": lessons_completed,
                "points_earned": points_earned,
                "streak_days": streak_days
            },
            "metadata": additional_data or {}
        }
        
        logger.info(f"Sending daily progress SMS to {phone_number} for {child_name}")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/sms/send",
            data=data
        )
    
    async def send_daily_progress_sms_async(
        self,
        phone_number: str,
        child_name: str,
        lessons_completed: int,
        points_earned: int,
        streak_days: Optional[int] = None,
        additional_data: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Asynchronously send daily progress SMS notification.
        
        Args:
            phone_number: Recipient's phone number
            child_name: Name of the child
            lessons_completed: Number of lessons completed today
            points_earned: Porapoints earned today
            streak_days: Current learning streak (optional)
            additional_data: Additional context data
            
        Returns:
            ApplinkResponse with SMS delivery details
        """
        # Build the message using the template format
        if streak_days and streak_days > 1:
            message = (
                f"🎉 {child_name} completed {lessons_completed} lessons today and earned "
                f"{points_earned} Porapoints! That's a {streak_days}-day learning streak! 🔥"
            )
        else:
            message = (
                f"📚 {child_name} completed {lessons_completed} lessons today and earned "
                f"{points_earned} Porapoints! Keep up the great work! 💪"
            )
        
        data = {
            "phone_number": phone_number,
            "message": message,
            "message_type": SMSType.DAILY_PROGRESS.value,
            "priority": SMSPriority.NORMAL.value,
            "template_data": {
                "child_name": child_name,
                "lessons_completed": lessons_completed,
                "points_earned": points_earned,
                "streak_days": streak_days
            },
            "metadata": additional_data or {}
        }
        
        logger.info(f"Async sending daily progress SMS to {phone_number} for {child_name}")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/sms/send",
            data=data
        )
    
    def send_achievement_sms(
        self,
        phone_number: str,
        child_name: str,
        achievement_name: str,
        points_earned: int,
        achievement_level: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Send achievement unlock SMS notification.
        
        Args:
            phone_number: Recipient's phone number
            child_name: Name of the child
            achievement_name: Name of the unlocked achievement
            points_earned: Bonus points for the achievement
            achievement_level: Achievement level (Bronze, Silver, Gold)
            
        Returns:
            ApplinkResponse with SMS delivery details
        """
        level_prefix = f"{achievement_level} " if achievement_level else ""
        
        message = (
            f"🏆 Amazing! {child_name} just unlocked the {level_prefix}{achievement_name} "
            f"achievement and earned {points_earned} bonus Porapoints! 🎯"
        )
        
        data = {
            "phone_number": phone_number,
            "message": message,
            "message_type": SMSType.ACHIEVEMENT.value,
            "priority": SMSPriority.HIGH.value,
            "template_data": {
                "child_name": child_name,
                "achievement_name": achievement_name,
                "points_earned": points_earned,
                "achievement_level": achievement_level
            }
        }
        
        logger.info(f"Sending achievement SMS to {phone_number} for {achievement_name}")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/sms/send",
            data=data
        )
    
    async def send_achievement_sms_async(
        self,
        phone_number: str,
        child_name: str,
        achievement_name: str,
        points_earned: int,
        achievement_level: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Asynchronously send achievement unlock SMS notification.
        """
        level_prefix = f"{achievement_level} " if achievement_level else ""
        
        message = (
            f"🏆 Amazing! {child_name} just unlocked the {level_prefix}{achievement_name} "
            f"achievement and earned {points_earned} bonus Porapoints! 🎯"
        )
        
        data = {
            "phone_number": phone_number,
            "message": message,
            "message_type": SMSType.ACHIEVEMENT.value,
            "priority": SMSPriority.HIGH.value,
            "template_data": {
                "child_name": child_name,
                "achievement_name": achievement_name,
                "points_earned": points_earned,
                "achievement_level": achievement_level
            }
        }
        
        logger.info(f"Async sending achievement SMS to {phone_number} for {achievement_name}")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/sms/send",
            data=data
        )
    
    def send_reward_notification_sms(
        self,
        phone_number: str,
        child_name: str,
        reward_name: str,
        reward_value: str,
        points_spent: int
    ) -> ApplinkResponse:
        """
        Send reward redemption notification SMS.
        
        Args:
            phone_number: Recipient's phone number
            child_name: Name of the child
            reward_name: Name of the redeemed reward
            reward_value: Value of the reward (e.g., "100MB", "5 SMS")
            points_spent: Porapoints spent on the reward
            
        Returns:
            ApplinkResponse with SMS delivery details
        """
        message = (
            f"🎁 {child_name} just redeemed {reward_value} {reward_name} for {points_spent} "
            f"Porapoints! The reward has been activated. Enjoy! ✨"
        )
        
        data = {
            "phone_number": phone_number,
            "message": message,
            "message_type": SMSType.REWARD_NOTIFICATION.value,
            "priority": SMSPriority.HIGH.value,
            "template_data": {
                "child_name": child_name,
                "reward_name": reward_name,
                "reward_value": reward_value,
                "points_spent": points_spent
            }
        }
        
        logger.info(f"Sending reward notification SMS to {phone_number} for {reward_name}")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/sms/send",
            data=data
        )
    
    async def send_reward_notification_sms_async(
        self,
        phone_number: str,
        child_name: str,
        reward_name: str,
        reward_value: str,
        points_spent: int
    ) -> ApplinkResponse:
        """
        Asynchronously send reward redemption notification SMS.
        """
        message = (
            f"🎁 {child_name} just redeemed {reward_value} {reward_name} for {points_spent} "
            f"Porapoints! The reward has been activated. Enjoy! ✨"
        )
        
        data = {
            "phone_number": phone_number,
            "message": message,
            "message_type": SMSType.REWARD_NOTIFICATION.value,
            "priority": SMSPriority.HIGH.value,
            "template_data": {
                "child_name": child_name,
                "reward_name": reward_name,
                "reward_value": reward_value,
                "points_spent": points_spent
            }
        }
        
        logger.info(f"Async sending reward notification SMS to {phone_number} for {reward_name}")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/sms/send",
            data=data
        )
    
    def send_custom_sms(
        self,
        phone_number: str,
        message: str,
        priority: SMSPriority = SMSPriority.NORMAL,
        metadata: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Send a custom SMS message.
        
        Args:
            phone_number: Recipient's phone number
            message: Custom message content
            priority: Message priority level
            metadata: Additional metadata
            
        Returns:
            ApplinkResponse with SMS delivery details
        """
        data = {
            "phone_number": phone_number,
            "message": message,
            "message_type": SMSType.CUSTOM.value,
            "priority": priority.value,
            "metadata": metadata or {}
        }
        
        logger.info(f"Sending custom SMS to {phone_number}")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/sms/send",
            data=data
        )
    
    async def send_custom_sms_async(
        self,
        phone_number: str,
        message: str,
        priority: SMSPriority = SMSPriority.NORMAL,
        metadata: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Asynchronously send a custom SMS message.
        """
        data = {
            "phone_number": phone_number,
            "message": message,
            "message_type": SMSType.CUSTOM.value,
            "priority": priority.value,
            "metadata": metadata or {}
        }
        
        logger.info(f"Async sending custom SMS to {phone_number}")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/sms/send",
            data=data
        )
    
    def send_bulk_sms(
        self,
        phone_numbers: List[str],
        message: str,
        priority: SMSPriority = SMSPriority.NORMAL,
        campaign_name: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Send bulk SMS to multiple recipients.
        
        Args:
            phone_numbers: List of recipient phone numbers
            message: Message content
            priority: Message priority level
            campaign_name: Name for the SMS campaign
            
        Returns:
            ApplinkResponse with bulk SMS delivery details
        """
        data = {
            "phone_numbers": phone_numbers,
            "message": message,
            "message_type": SMSType.BULK_CAMPAIGN.value,
            "priority": priority.value,
            "campaign_name": campaign_name or f"Campaign_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        }
        
        logger.info(f"Sending bulk SMS to {len(phone_numbers)} recipients")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/sms/send-bulk",
            data=data
        )
    
    async def send_bulk_sms_async(
        self,
        phone_numbers: List[str],
        message: str,
        priority: SMSPriority = SMSPriority.NORMAL,
        campaign_name: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Asynchronously send bulk SMS to multiple recipients.
        """
        data = {
            "phone_numbers": phone_numbers,
            "message": message,
            "message_type": SMSType.BULK_CAMPAIGN.value,
            "priority": priority.value,
            "campaign_name": campaign_name or f"Campaign_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        }
        
        logger.info(f"Async sending bulk SMS to {len(phone_numbers)} recipients")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/sms/send-bulk",
            data=data
        )
    
    def get_sms_status(self, message_id: str) -> ApplinkResponse:
        """
        Get delivery status of a sent SMS.
        
        Args:
            message_id: ID of the SMS message
            
        Returns:
            ApplinkResponse with SMS status details
        """
        params = {"message_id": message_id}
        
        logger.info(f"Checking SMS status for message {message_id}")
        
        return self._make_request_with_retry(
            method="GET",
            endpoint="/sms/status",
            params=params
        )
    
    async def get_sms_status_async(self, message_id: str) -> ApplinkResponse:
        """
        Asynchronously get delivery status of a sent SMS.
        """
        params = {"message_id": message_id}
        
        logger.info(f"Async checking SMS status for message {message_id}")
        
        return await self._make_async_request_with_retry(
            method="GET",
            endpoint="/sms/status",
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
        Generate mock responses for SMS operations.
        
        Args:
            method: HTTP method
            endpoint: API endpoint
            data: Request data
            params: Query parameters
            request_id: Request tracking ID
            
        Returns:
            Mock ApplinkResponse with realistic SMS data
        """
        mock_responses = {
            "/sms/send": {
                "status": "success",
                "message": "SMS sent successfully",
                "data": {
                    "message_id": f"sms_{request_id}",
                    "phone_number": data.get("phone_number") if data else "+1234567890",
                    "message": data.get("message") if data else "Test message",
                    "message_type": data.get("message_type") if data else "custom",
                    "priority": data.get("priority") if data else "normal",
                    "status": "sent",
                    "sent_at": datetime.now().isoformat(),
                    "delivery_status": "pending",
                    "estimated_delivery": "2-5 minutes",
                    "cost": 0.05,
                    "currency": "USD"
                }
            },
            "/sms/send-bulk": {
                "status": "success",
                "message": "Bulk SMS campaign initiated",
                "data": {
                    "campaign_id": f"campaign_{request_id}",
                    "campaign_name": data.get("campaign_name") if data else "Bulk Campaign",
                    "total_recipients": len(data.get("phone_numbers", [])) if data else 0,
                    "messages_sent": len(data.get("phone_numbers", [])) if data else 0,
                    "messages_failed": 0,
                    "status": "processing",
                    "initiated_at": datetime.now().isoformat(),
                    "estimated_completion": "5-10 minutes",
                    "total_cost": len(data.get("phone_numbers", [])) * 0.05 if data else 0,
                    "currency": "USD"
                }
            },
            "/sms/status": {
                "status": "success",
                "message": "SMS status retrieved",
                "data": {
                    "message_id": params.get("message_id") if params else f"sms_{request_id}",
                    "status": "delivered",
                    "delivery_status": "delivered",
                    "sent_at": datetime.now().isoformat(),
                    "delivered_at": datetime.now().isoformat(),
                    "phone_number": "+1234567890",
                    "message_type": "daily_progress",
                    "attempts": 1,
                    "cost": 0.05,
                    "currency": "USD"
                }
            }
        }
        
        response_data = mock_responses.get(endpoint, {
            "status": "success",
            "message": "Mock SMS response",
            "data": {
                "message_id": f"sms_{request_id}",
                "status": "sent"
            }
        })
        
        return self._normalize_response(response_data, request_id)