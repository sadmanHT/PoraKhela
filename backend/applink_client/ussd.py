"""
Applink USSD API Client

Handles USSD (Unstructured Supplementary Service Data) menu interactions:
- Generate USSD menu responses 
- Handle user navigation through USSD menus
- Provide interactive menu structures
- Support session-based USSD interactions

USSD menus are commonly used in mobile networks for service access.
"""

import logging
from datetime import datetime
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


class USSDMenuType(Enum):
    """Types of USSD menus."""
    
    MAIN_MENU = "main_menu"
    SUBSCRIPTION_MENU = "subscription_menu" 
    REWARDS_MENU = "rewards_menu"
    ACCOUNT_INFO = "account_info"
    SUPPORT_MENU = "support_menu"
    BALANCE_CHECK = "balance_check"
    DATA_BUNDLE_MENU = "data_bundle_menu"
    SMS_BUNDLE_MENU = "sms_bundle_menu"


class USSDSessionStatus(Enum):
    """Status of a USSD session."""
    
    ACTIVE = "active"
    ENDED = "ended"
    TIMEOUT = "timeout"
    ERROR = "error"


class USSDClient(ApplinkClient):
    """
    Client for Applink USSD API.
    
    Provides methods for generating USSD menu responses and handling
    interactive USSD sessions for mobile network integration.
    """
    
    def __init__(self, config: Optional[ApplinkConfig] = None):
        super().__init__(config)
        self.service_name = "ussd"
    
    def start_ussd_session(
        self,
        phone_number: str,
        ussd_code: str,
        menu_type: USSDMenuType = USSDMenuType.MAIN_MENU,
        user_context: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Start a new USSD session and return the initial menu.
        
        Args:
            phone_number: User's phone number
            ussd_code: USSD code dialed (e.g., *123#)
            menu_type: Type of menu to display
            user_context: Additional user context
            
        Returns:
            ApplinkResponse with USSD menu content
            
        Raises:
            ApplinkAPIError: When session creation fails
        """
        data = {
            "phone_number": phone_number,
            "ussd_code": ussd_code,
            "menu_type": menu_type.value,
            "user_context": user_context or {}
        }
        
        logger.info(f"Starting USSD session for {phone_number} with code {ussd_code}")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/ussd/start-session",
            data=data
        )
    
    async def start_ussd_session_async(
        self,
        phone_number: str,
        ussd_code: str,
        menu_type: USSDMenuType = USSDMenuType.MAIN_MENU,
        user_context: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Asynchronously start a new USSD session and return the initial menu.
        """
        data = {
            "phone_number": phone_number,
            "ussd_code": ussd_code,
            "menu_type": menu_type.value,
            "user_context": user_context or {}
        }
        
        logger.info(f"Async starting USSD session for {phone_number} with code {ussd_code}")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/ussd/start-session",
            data=data
        )
    
    def handle_ussd_input(
        self,
        session_id: str,
        phone_number: str,
        user_input: str,
        current_menu: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Handle user input in an active USSD session.
        
        Args:
            session_id: ID of the active USSD session
            phone_number: User's phone number
            user_input: User's menu selection or input
            current_menu: Current menu context
            
        Returns:
            ApplinkResponse with next menu or action result
            
        Raises:
            ApplinkAPIError: When input handling fails
        """
        data = {
            "session_id": session_id,
            "phone_number": phone_number,
            "user_input": user_input,
            "current_menu": current_menu
        }
        
        logger.info(f"Handling USSD input '{user_input}' for session {session_id}")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/ussd/handle-input",
            data=data
        )
    
    async def handle_ussd_input_async(
        self,
        session_id: str,
        phone_number: str,
        user_input: str,
        current_menu: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Asynchronously handle user input in an active USSD session.
        """
        data = {
            "session_id": session_id,
            "phone_number": phone_number,
            "user_input": user_input,
            "current_menu": current_menu
        }
        
        logger.info(f"Async handling USSD input '{user_input}' for session {session_id}")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/ussd/handle-input",
            data=data
        )
    
    def get_ussd_menu(
        self,
        menu_type: USSDMenuType,
        phone_number: str,
        context: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Get a specific USSD menu without starting a session.
        
        Args:
            menu_type: Type of menu to retrieve
            phone_number: User's phone number for personalization
            context: Additional context for menu generation
            
        Returns:
            ApplinkResponse with menu content
        """
        params = {
            "menu_type": menu_type.value,
            "phone_number": phone_number
        }
        
        if context:
            params.update(context)
        
        logger.info(f"Getting {menu_type.value} menu for {phone_number}")
        
        return self._make_request_with_retry(
            method="GET",
            endpoint="/ussd/get-menu",
            params=params
        )
    
    async def get_ussd_menu_async(
        self,
        menu_type: USSDMenuType,
        phone_number: str,
        context: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Asynchronously get a specific USSD menu without starting a session.
        """
        params = {
            "menu_type": menu_type.value,
            "phone_number": phone_number
        }
        
        if context:
            params.update(context)
        
        logger.info(f"Async getting {menu_type.value} menu for {phone_number}")
        
        return await self._make_async_request_with_retry(
            method="GET",
            endpoint="/ussd/get-menu",
            params=params
        )
    
    def end_ussd_session(self, session_id: str, phone_number: str) -> ApplinkResponse:
        """
        End an active USSD session.
        
        Args:
            session_id: ID of the session to end
            phone_number: User's phone number
            
        Returns:
            ApplinkResponse with session end confirmation
        """
        data = {
            "session_id": session_id,
            "phone_number": phone_number
        }
        
        logger.info(f"Ending USSD session {session_id} for {phone_number}")
        
        return self._make_request_with_retry(
            method="POST",
            endpoint="/ussd/end-session",
            data=data
        )
    
    async def end_ussd_session_async(self, session_id: str, phone_number: str) -> ApplinkResponse:
        """
        Asynchronously end an active USSD session.
        """
        data = {
            "session_id": session_id,
            "phone_number": phone_number
        }
        
        logger.info(f"Async ending USSD session {session_id} for {phone_number}")
        
        return await self._make_async_request_with_retry(
            method="POST",
            endpoint="/ussd/end-session",
            data=data
        )
    
    def get_session_status(self, session_id: str) -> ApplinkResponse:
        """
        Get the status of a USSD session.
        
        Args:
            session_id: ID of the session to check
            
        Returns:
            ApplinkResponse with session status details
        """
        params = {"session_id": session_id}
        
        logger.info(f"Checking status of USSD session {session_id}")
        
        return self._make_request_with_retry(
            method="GET",
            endpoint="/ussd/session-status",
            params=params
        )
    
    async def get_session_status_async(self, session_id: str) -> ApplinkResponse:
        """
        Asynchronously get the status of a USSD session.
        """
        params = {"session_id": session_id}
        
        logger.info(f"Async checking status of USSD session {session_id}")
        
        return await self._make_async_request_with_retry(
            method="GET",
            endpoint="/ussd/session-status",
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
        Generate mock responses for USSD operations with realistic menu structures.
        
        Args:
            method: HTTP method
            endpoint: API endpoint
            data: Request data
            params: Query parameters
            request_id: Request tracking ID
            
        Returns:
            Mock ApplinkResponse with USSD menu content
        """
        mock_responses = {
            "/ussd/start-session": self._get_mock_start_session_response(data, request_id),
            "/ussd/handle-input": self._get_mock_handle_input_response(data, request_id),
            "/ussd/get-menu": self._get_mock_menu_response(params, request_id),
            "/ussd/end-session": {
                "status": "success",
                "message": "USSD session ended",
                "data": {
                    "session_id": data.get("session_id") if data else f"session_{request_id}",
                    "phone_number": data.get("phone_number") if data else "+1234567890",
                    "session_status": "ended",
                    "ended_at": datetime.now().isoformat(),
                    "duration_seconds": 120,
                    "total_interactions": 3
                }
            },
            "/ussd/session-status": {
                "status": "success",
                "message": "Session status retrieved",
                "data": {
                    "session_id": params.get("session_id") if params else f"session_{request_id}",
                    "phone_number": "+1234567890",
                    "session_status": "active",
                    "started_at": datetime.now().isoformat(),
                    "current_menu": "main_menu",
                    "interactions_count": 2,
                    "last_activity": datetime.now().isoformat(),
                    "expires_at": (datetime.now()).isoformat()
                }
            }
        }
        
        response_data = mock_responses.get(endpoint, {
            "status": "success",
            "message": "Mock USSD response",
            "data": {
                "menu_text": "Welcome to Applink\n1. Check Balance\n2. Buy Data\n0. Exit"
            }
        })
        
        return self._normalize_response(response_data, request_id)
    
    def _get_mock_start_session_response(self, data: Optional[Dict], request_id: Optional[str]) -> Dict:
        """Generate mock response for starting a USSD session."""
        menu_type = data.get("menu_type", "main_menu") if data else "main_menu"
        phone_number = data.get("phone_number", "+1234567890") if data else "+1234567890"
        
        menu_texts = {
            "main_menu": f"Welcome to Applink!\nYour child has 150 Porapoints\n\n1. Check Balance\n2. Redeem Rewards\n3. Subscription\n4. Support\n0. Exit",
            "subscription_menu": "Subscription Menu\n1. Current Plan: Premium\n2. Upgrade Plan\n3. Billing History\n9. Back\n0. Exit",
            "rewards_menu": "Rewards Menu\n1. Data Bundles\n2. SMS Bundles\n3. Airtime\n4. Redemption History\n9. Back\n0. Exit",
            "account_info": f"Account Info\nPhone: {phone_number}\nBalance: 150 Porapoints\nPlan: Premium\nStatus: Active\n9. Back\n0. Exit",
            "support_menu": "Support Menu\n1. FAQ\n2. Contact Support\n3. Report Issue\n9. Back\n0. Exit"
        }
        
        return {
            "status": "success",
            "message": "USSD session started",
            "data": {
                "session_id": f"session_{request_id}",
                "phone_number": phone_number,
                "session_status": "active",
                "menu_type": menu_type,
                "menu_text": menu_texts.get(menu_type, menu_texts["main_menu"]),
                "started_at": datetime.now().isoformat(),
                "expires_at": (datetime.now()).isoformat(),
                "continue_session": True
            }
        }
    
    def _get_mock_handle_input_response(self, data: Optional[Dict], request_id: Optional[str]) -> Dict:
        """Generate mock response for handling USSD input."""
        user_input = data.get("user_input", "1") if data else "1"
        session_id = data.get("session_id", f"session_{request_id}") if data else f"session_{request_id}"
        phone_number = data.get("phone_number", "+1234567890") if data else "+1234567890"
        
        # Mock navigation logic
        menu_responses = {
            "1": {
                "menu_text": f"Account Balance\nPhone: {phone_number}\nPorapoints: 150\nLast earned: Today (+25)\nStreak: 5 days\n\n9. Back\n0. Exit",
                "menu_type": "balance_check",
                "continue_session": True
            },
            "2": {
                "menu_text": "Redeem Rewards\n1. 100MB Data (50 points)\n2. 20 SMS (30 points)\n3. $5 Airtime (100 points)\n4. View History\n9. Back\n0. Exit",
                "menu_type": "rewards_menu",
                "continue_session": True
            },
            "3": {
                "menu_text": "Subscription\nCurrent: Premium Plan\nStatus: Active\nExpires: 2024-12-22\nAuto-renew: Enabled\n\n1. View Details\n2. Change Plan\n9. Back\n0. Exit",
                "menu_type": "subscription_menu",
                "continue_session": True
            },
            "4": {
                "menu_text": "Support\n1. Common Questions\n2. Contact Support\n3. Report a Problem\n4. Service Status\n9. Back\n0. Exit",
                "menu_type": "support_menu",
                "continue_session": True
            },
            "0": {
                "menu_text": "Thank you for using Applink!\nHave a great day!",
                "menu_type": "exit",
                "continue_session": False
            },
            "9": {
                "menu_text": "Welcome to Applink!\nYour child has 150 Porapoints\n\n1. Check Balance\n2. Redeem Rewards\n3. Subscription\n4. Support\n0. Exit",
                "menu_type": "main_menu", 
                "continue_session": True
            }
        }
        
        response_data = menu_responses.get(user_input, {
            "menu_text": "Invalid selection. Please try again.\n\n9. Back\n0. Exit",
            "menu_type": "error",
            "continue_session": True
        })
        
        return {
            "status": "success",
            "message": "USSD input processed",
            "data": {
                "session_id": session_id,
                "phone_number": phone_number,
                "user_input": user_input,
                "menu_text": response_data["menu_text"],
                "menu_type": response_data["menu_type"],
                "continue_session": response_data["continue_session"],
                "interaction_count": 2,
                "processed_at": datetime.now().isoformat()
            }
        }
    
    def _get_mock_menu_response(self, params: Optional[Dict], request_id: Optional[str]) -> Dict:
        """Generate mock response for getting a specific menu."""
        menu_type = params.get("menu_type", "main_menu") if params else "main_menu"
        phone_number = params.get("phone_number", "+1234567890") if params else "+1234567890"
        
        menu_definitions = {
            "main_menu": {
                "title": "Applink Main Menu",
                "text": f"Welcome to Applink!\nYour child has 150 Porapoints\n\n1. Check Balance\n2. Redeem Rewards\n3. Subscription\n4. Support\n0. Exit",
                "options": [
                    {"key": "1", "label": "Check Balance", "action": "balance_check"},
                    {"key": "2", "label": "Redeem Rewards", "action": "rewards_menu"},
                    {"key": "3", "label": "Subscription", "action": "subscription_menu"},
                    {"key": "4", "label": "Support", "action": "support_menu"},
                    {"key": "0", "label": "Exit", "action": "exit"}
                ]
            },
            "data_bundle_menu": {
                "title": "Data Bundles",
                "text": "Available Data Bundles:\n1. 100MB - 50 points\n2. 500MB - 200 points\n3. 1GB - 350 points\n4. 2GB - 600 points\n\n9. Back\n0. Exit",
                "options": [
                    {"key": "1", "label": "100MB", "points": 50},
                    {"key": "2", "label": "500MB", "points": 200},
                    {"key": "3", "label": "1GB", "points": 350},
                    {"key": "4", "label": "2GB", "points": 600}
                ]
            },
            "sms_bundle_menu": {
                "title": "SMS Bundles", 
                "text": "Available SMS Bundles:\n1. 20 SMS - 30 points\n2. 50 SMS - 70 points\n3. 100 SMS - 120 points\n4. 200 SMS - 200 points\n\n9. Back\n0. Exit",
                "options": [
                    {"key": "1", "label": "20 SMS", "points": 30},
                    {"key": "2", "label": "50 SMS", "points": 70},
                    {"key": "3", "label": "100 SMS", "points": 120},
                    {"key": "4", "label": "200 SMS", "points": 200}
                ]
            }
        }
        
        menu_data = menu_definitions.get(menu_type, menu_definitions["main_menu"])
        
        return {
            "status": "success",
            "message": "Menu retrieved",
            "data": {
                "menu_type": menu_type,
                "phone_number": phone_number,
                "title": menu_data["title"],
                "menu_text": menu_data["text"],
                "options": menu_data["options"],
                "generated_at": datetime.now().isoformat(),
                "session_required": True
            }
        }