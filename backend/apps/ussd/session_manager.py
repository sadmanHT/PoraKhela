"""
USSD Session Manager for Redis-based State Management

Handles USSD session state storage and retrieval using Redis backend.
"""

import redis
import json
import logging
from django.conf import settings
from typing import Dict, Any, Optional

logger = logging.getLogger(__name__)


class USSDSessionManager:
    """
    Manages USSD session state using Redis for fast access and automatic expiration.
    """
    
    def __init__(self):
        self.redis_client = redis.Redis(
            host=getattr(settings, 'REDIS_HOST', 'localhost'),
            port=getattr(settings, 'REDIS_PORT', 6379),
            db=getattr(settings, 'REDIS_USSD_DB', 1),  # Use different DB for USSD
            decode_responses=True
        )
        self.session_timeout = 300  # 5 minutes timeout
    
    def create_session(self, session_id: str, phone_number: str, parent_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Create a new USSD session with initial state.
        
        Args:
            session_id: Telecom provider session ID
            phone_number: Parent's phone number
            parent_id: Associated parent user ID (if authenticated)
            
        Returns:
            Initial session data
        """
        session_data = {
            'session_id': session_id,
            'phone_number': phone_number,
            'parent_id': parent_id,
            'current_state': 'main_menu',
            'previous_state': None,
            'session_data': {},
            'created_at': self._get_current_timestamp(),
            'updated_at': self._get_current_timestamp()
        }
        
        key = self._get_session_key(session_id)
        self.redis_client.setex(key, self.session_timeout, json.dumps(session_data))
        
        logger.info(f"Created USSD session {session_id} for {phone_number}")
        return session_data
    
    def get_session(self, session_id: str) -> Optional[Dict[str, Any]]:
        """
        Retrieve session data from Redis.
        
        Args:
            session_id: Telecom provider session ID
            
        Returns:
            Session data or None if not found
        """
        key = self._get_session_key(session_id)
        session_json = self.redis_client.get(key)
        
        if session_json:
            return json.loads(session_json)
        return None
    
    def update_session(self, session_id: str, updates: Dict[str, Any]) -> bool:
        """
        Update session data and extend timeout.
        
        Args:
            session_id: Telecom provider session ID
            updates: Dictionary of fields to update
            
        Returns:
            True if successful, False if session not found
        """
        session_data = self.get_session(session_id)
        if not session_data:
            return False
        
        # Update fields
        session_data.update(updates)
        session_data['updated_at'] = self._get_current_timestamp()
        
        key = self._get_session_key(session_id)
        self.redis_client.setex(key, self.session_timeout, json.dumps(session_data))
        
        logger.info(f"Updated USSD session {session_id}: {list(updates.keys())}")
        return True
    
    def set_state(self, session_id: str, new_state: str, state_data: Optional[Dict[str, Any]] = None) -> bool:
        """
        Update session state and optionally store additional data.
        
        Args:
            session_id: Telecom provider session ID
            new_state: New menu state
            state_data: Additional state-specific data
            
        Returns:
            True if successful, False if session not found
        """
        session_data = self.get_session(session_id)
        if not session_data:
            return False
        
        updates = {
            'previous_state': session_data.get('current_state'),
            'current_state': new_state
        }
        
        if state_data:
            current_session_data = session_data.get('session_data', {})
            current_session_data.update(state_data)
            updates['session_data'] = current_session_data
        
        return self.update_session(session_id, updates)
    
    def get_state(self, session_id: str) -> Optional[str]:
        """
        Get current session state.
        
        Args:
            session_id: Telecom provider session ID
            
        Returns:
            Current state or None if session not found
        """
        session_data = self.get_session(session_id)
        if session_data:
            return session_data.get('current_state')
        return None
    
    def get_session_data(self, session_id: str, key: str = None) -> Any:
        """
        Get session-specific data.
        
        Args:
            session_id: Telecom provider session ID
            key: Specific data key (optional)
            
        Returns:
            Session data value or entire session_data dict
        """
        session_data = self.get_session(session_id)
        if not session_data:
            return None
        
        session_values = session_data.get('session_data', {})
        if key:
            return session_values.get(key)
        return session_values
    
    def set_session_data(self, session_id: str, key: str, value: Any) -> bool:
        """
        Set session-specific data.
        
        Args:
            session_id: Telecom provider session ID
            key: Data key
            value: Data value
            
        Returns:
            True if successful, False if session not found
        """
        return self.set_state(session_id, self.get_state(session_id), {key: value})
    
    def end_session(self, session_id: str) -> bool:
        """
        End and remove session from Redis.
        
        Args:
            session_id: Telecom provider session ID
            
        Returns:
            True if session was removed, False if not found
        """
        key = self._get_session_key(session_id)
        deleted = self.redis_client.delete(key)
        
        if deleted:
            logger.info(f"Ended USSD session {session_id}")
        
        return deleted > 0
    
    def extend_session(self, session_id: str, additional_seconds: int = None) -> bool:
        """
        Extend session timeout.
        
        Args:
            session_id: Telecom provider session ID
            additional_seconds: Additional seconds (default: reset to full timeout)
            
        Returns:
            True if successful, False if session not found
        """
        key = self._get_session_key(session_id)
        timeout = additional_seconds or self.session_timeout
        
        if self.redis_client.exists(key):
            self.redis_client.expire(key, timeout)
            return True
        return False
    
    def _get_session_key(self, session_id: str) -> str:
        """Generate Redis key for session."""
        return f"ussd:session:{session_id}"
    
    def _get_current_timestamp(self) -> str:
        """Get current timestamp as string."""
        from datetime import datetime
        return datetime.now().isoformat()


# Singleton instance
session_manager = USSDSessionManager()