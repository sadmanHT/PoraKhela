"""
Base Applink API client with common functionality.

Provides shared error handling, retry logic, timeout handling,
and response normalization for all Applink service clients.
"""

import asyncio
import json
import logging
import time
from typing import Any, Dict, Optional, Union
from dataclasses import dataclass
from enum import Enum

import httpx


logger = logging.getLogger(__name__)


class ApplinkAPIError(Exception):
    """Base exception for all Applink API errors."""
    
    def __init__(self, message: str, status_code: Optional[int] = None, response_data: Optional[Dict] = None):
        super().__init__(message)
        self.status_code = status_code
        self.response_data = response_data or {}


class ApplinkTimeoutError(ApplinkAPIError):
    """Raised when API request times out."""
    pass


class ApplinkRetryExhaustedError(ApplinkAPIError):
    """Raised when all retry attempts have been exhausted."""
    pass


class ApplinkServiceUnavailableError(ApplinkAPIError):
    """Raised when the Applink service is unavailable."""
    pass


@dataclass
class ApplinkConfig:
    """Configuration for Applink API clients."""
    
    base_url: str = "https://api.applink.com"
    api_key: str = ""
    timeout: int = 30
    max_retries: int = 3
    retry_delay: float = 1.0
    retry_backoff: float = 2.0
    mock_mode: bool = True  # Enable mock responses for development


class ApplinkResponseStatus(Enum):
    """Standard response statuses across all Applink APIs."""
    
    SUCCESS = "success"
    ERROR = "error" 
    PENDING = "pending"
    FAILED = "failed"
    TIMEOUT = "timeout"


@dataclass
class ApplinkResponse:
    """Normalized response format for all Applink API calls."""
    
    status: ApplinkResponseStatus
    message: str
    data: Dict[str, Any]
    raw_response: Dict[str, Any]
    request_id: Optional[str] = None
    timestamp: Optional[str] = None


class ApplinkClient:
    """
    Base client class with common functionality for all Applink services.
    
    Provides:
    - Error handling and custom exceptions
    - Retry logic with exponential backoff
    - Timeout handling 
    - Response normalization
    - Both sync and async support
    - Request/response logging
    """
    
    def __init__(self, config: Optional[ApplinkConfig] = None):
        self.config = config or ApplinkConfig()
        self._sync_client = None
        self._async_client = None
    
    @property
    def sync_client(self) -> httpx.Client:
        """Lazy-loaded synchronous HTTP client."""
        if self._sync_client is None:
            self._sync_client = httpx.Client(
                base_url=self.config.base_url,
                timeout=self.config.timeout,
                headers=self._get_headers()
            )
        return self._sync_client
    
    @property
    def async_client(self) -> httpx.AsyncClient:
        """Lazy-loaded asynchronous HTTP client."""
        if self._async_client is None:
            self._async_client = httpx.AsyncClient(
                base_url=self.config.base_url,
                timeout=self.config.timeout,
                headers=self._get_headers()
            )
        return self._async_client
    
    def _get_headers(self) -> Dict[str, str]:
        """Get default headers for API requests."""
        return {
            "Authorization": f"Bearer {self.config.api_key}",
            "Content-Type": "application/json",
            "User-Agent": "Applink-Client/1.0.0",
        }
    
    def _normalize_response(self, response_data: Dict[str, Any], request_id: Optional[str] = None) -> ApplinkResponse:
        """
        Normalize API response to standard format.
        
        Args:
            response_data: Raw response from the API
            request_id: Optional request ID for tracking
            
        Returns:
            Normalized ApplinkResponse object
        """
        # Handle different response formats from various Applink services
        status_mapping = {
            "ok": ApplinkResponseStatus.SUCCESS,
            "success": ApplinkResponseStatus.SUCCESS,
            "error": ApplinkResponseStatus.ERROR,
            "failed": ApplinkResponseStatus.ERROR,
            "pending": ApplinkResponseStatus.PENDING,
            "timeout": ApplinkResponseStatus.TIMEOUT,
        }
        
        raw_status = str(response_data.get("status", "success")).lower()
        status = status_mapping.get(raw_status, ApplinkResponseStatus.SUCCESS)
        
        return ApplinkResponse(
            status=status,
            message=response_data.get("message", ""),
            data=response_data.get("data", response_data),
            raw_response=response_data,
            request_id=request_id,
            timestamp=response_data.get("timestamp", str(int(time.time())))
        )
    
    def _make_request_with_retry(
        self, 
        method: str, 
        endpoint: str, 
        data: Optional[Dict] = None,
        params: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Make synchronous HTTP request with retry logic.
        
        Args:
            method: HTTP method ('GET', 'POST', etc.)
            endpoint: API endpoint path
            data: Request body data
            params: Query parameters
            
        Returns:
            Normalized ApplinkResponse
            
        Raises:
            ApplinkRetryExhaustedError: When all retries are exhausted
            ApplinkAPIError: For other API errors
        """
        last_error = None
        delay = self.config.retry_delay
        
        for attempt in range(self.config.max_retries + 1):
            try:
                request_id = f"req_{int(time.time())}_{attempt}"
                
                logger.info(f"Making {method} request to {endpoint} (attempt {attempt + 1})")
                
                if self.config.mock_mode:
                    # Return mock response in development
                    return self._get_mock_response(method, endpoint, data, params, request_id)
                
                response = self.sync_client.request(
                    method=method,
                    url=endpoint,
                    json=data,
                    params=params
                )
                
                if response.status_code == 200:
                    response_data = response.json()
                    return self._normalize_response(response_data, request_id)
                elif response.status_code >= 500:
                    # Server error - retry
                    raise ApplinkServiceUnavailableError(
                        f"Server error: {response.status_code}",
                        status_code=response.status_code
                    )
                else:
                    # Client error - don't retry
                    raise ApplinkAPIError(
                        f"API error: {response.status_code}",
                        status_code=response.status_code,
                        response_data=response.json() if response.content else {}
                    )
                    
            except (httpx.TimeoutException, ApplinkTimeoutError) as e:
                last_error = ApplinkTimeoutError(f"Request timeout on attempt {attempt + 1}")
                
            except ApplinkServiceUnavailableError as e:
                last_error = e
                
            except ApplinkAPIError:
                # Don't retry client errors
                raise
                
            except Exception as e:
                last_error = ApplinkAPIError(f"Unexpected error: {str(e)}")
            
            if attempt < self.config.max_retries:
                logger.warning(f"Request failed, retrying in {delay}s: {last_error}")
                time.sleep(delay)
                delay *= self.config.retry_backoff
            
        # All retries exhausted
        raise ApplinkRetryExhaustedError(f"All retries exhausted. Last error: {last_error}")
    
    async def _make_async_request_with_retry(
        self, 
        method: str, 
        endpoint: str, 
        data: Optional[Dict] = None,
        params: Optional[Dict] = None
    ) -> ApplinkResponse:
        """
        Make asynchronous HTTP request with retry logic.
        
        Args:
            method: HTTP method ('GET', 'POST', etc.)
            endpoint: API endpoint path
            data: Request body data
            params: Query parameters
            
        Returns:
            Normalized ApplinkResponse
            
        Raises:
            ApplinkRetryExhaustedError: When all retries are exhausted
            ApplinkAPIError: For other API errors
        """
        last_error = None
        delay = self.config.retry_delay
        
        for attempt in range(self.config.max_retries + 1):
            try:
                request_id = f"req_{int(time.time())}_{attempt}"
                
                logger.info(f"Making async {method} request to {endpoint} (attempt {attempt + 1})")
                
                if self.config.mock_mode:
                    # Return mock response in development
                    await asyncio.sleep(0.1)  # Simulate network delay
                    return self._get_mock_response(method, endpoint, data, params, request_id)
                
                response = await self.async_client.request(
                    method=method,
                    url=endpoint,
                    json=data,
                    params=params
                )
                
                if response.status_code == 200:
                    response_data = response.json()
                    return self._normalize_response(response_data, request_id)
                elif response.status_code >= 500:
                    # Server error - retry
                    raise ApplinkServiceUnavailableError(
                        f"Server error: {response.status_code}",
                        status_code=response.status_code
                    )
                else:
                    # Client error - don't retry
                    raise ApplinkAPIError(
                        f"API error: {response.status_code}",
                        status_code=response.status_code,
                        response_data=response.json() if response.content else {}
                    )
                    
            except (httpx.TimeoutException, ApplinkTimeoutError) as e:
                last_error = ApplinkTimeoutError(f"Request timeout on attempt {attempt + 1}")
                
            except ApplinkServiceUnavailableError as e:
                last_error = e
                
            except ApplinkAPIError:
                # Don't retry client errors
                raise
                
            except Exception as e:
                last_error = ApplinkAPIError(f"Unexpected error: {str(e)}")
            
            if attempt < self.config.max_retries:
                logger.warning(f"Async request failed, retrying in {delay}s: {last_error}")
                await asyncio.sleep(delay)
                delay *= self.config.retry_backoff
            
        # All retries exhausted
        raise ApplinkRetryExhaustedError(f"All retries exhausted. Last error: {last_error}")
    
    def _get_mock_response(
        self, 
        method: str, 
        endpoint: str, 
        data: Optional[Dict] = None,
        params: Optional[Dict] = None,
        request_id: Optional[str] = None
    ) -> ApplinkResponse:
        """
        Generate mock response for development/testing.
        Override this method in service-specific clients.
        
        Args:
            method: HTTP method
            endpoint: API endpoint 
            data: Request data
            params: Query parameters
            request_id: Request tracking ID
            
        Returns:
            Mock ApplinkResponse
        """
        return ApplinkResponse(
            status=ApplinkResponseStatus.SUCCESS,
            message="Mock response",
            data={"result": "success"},
            raw_response={"status": "success", "data": {"result": "success"}},
            request_id=request_id,
            timestamp=str(int(time.time()))
        )
    
    def close(self):
        """Close HTTP clients."""
        if self._sync_client:
            self._sync_client.close()
        if self._async_client:
            asyncio.create_task(self._async_client.aclose())
    
    def __enter__(self):
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()
    
    async def __aenter__(self):
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if self._async_client:
            await self._async_client.aclose()