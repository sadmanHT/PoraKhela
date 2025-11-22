"""
Middleware to enforce OTP verification for API access.

Blocks all authenticated API access unless user has OTP_verified=True.
Allows unauthenticated access and OTP-related endpoints.
"""
from django.http import JsonResponse
from django.utils.deprecation import MiddlewareMixin
from django.urls import reverse
from django.conf import settings
import logging

logger = logging.getLogger(__name__)


class EnsureOTPVerifiedMiddleware(MiddlewareMixin):
    """
    Middleware to ensure OTP verification for authenticated API access.
    
    Rules:
    1. Unauthenticated requests → Allow (handled by permission classes)
    2. OTP-related endpoints → Allow (needed for verification flow)
    3. Authenticated users with OTP_verified=False → Block
    4. Authenticated users with OTP_verified=True → Allow
    """
    
    # Endpoints that should be accessible without OTP verification
    EXEMPT_PATHS = [
        '/auth/request-otp/',
        '/auth/verify-otp/',
        '/admin/',  # Django admin
        '/api/schema/',  # API documentation
        '/api/docs/',  # Swagger UI
        '/health/',  # Health check
        '/static/',  # Static files
        '/media/',  # Media files
    ]
    
    # API path prefixes that require OTP verification
    PROTECTED_API_PREFIXES = [
        '/api/',
        '/parent/',
        '/child/',
        '/lessons/',
        '/gamification/',
        '/rewards/',
    ]
    
    def __init__(self, get_response):
        """Initialize middleware."""
        self.get_response = get_response
        super().__init__(get_response)
    
    def process_request(self, request):
        """Process incoming request to check OTP verification."""
        
        # Skip processing for exempt paths
        if self._is_exempt_path(request.path):
            return None
        
        # Skip processing for non-protected paths
        if not self._is_protected_path(request.path):
            return None
        
        # Skip processing for unauthenticated requests
        # (Permission classes will handle this)
        if not hasattr(request, 'user') or not request.user.is_authenticated:
            return None
        
        # Check if user has verified OTP
        if not getattr(request.user, 'OTP_verified', False):
            logger.warning(f"Blocked unverified user {request.user.phone_number} from accessing {request.path}")
            
            return JsonResponse({
                'success': False,
                'message': 'Phone number verification required. Please complete OTP verification.',
                'code': 'OTP_VERIFICATION_REQUIRED',
                'required_action': 'complete_otp_verification'
            }, status=403)
        
        # User is authenticated and OTP verified → Allow
        return None
    
    def _is_exempt_path(self, path):
        """Check if path is exempt from OTP verification."""
        for exempt_path in self.EXEMPT_PATHS:
            if path.startswith(exempt_path):
                return True
        return False
    
    def _is_protected_path(self, path):
        """Check if path requires OTP verification."""
        for protected_prefix in self.PROTECTED_API_PREFIXES:
            if path.startswith(protected_prefix):
                return True
        return False


class SecurityHeadersMiddleware(MiddlewareMixin):
    """
    Add security headers for API protection.
    """
    
    def process_response(self, request, response):
        """Add security headers to response."""
        
        # Prevent clickjacking
        response['X-Frame-Options'] = 'DENY'
        
        # Prevent content type sniffing
        response['X-Content-Type-Options'] = 'nosniff'
        
        # Enable XSS protection
        response['X-XSS-Protection'] = '1; mode=block'
        
        # Strict transport security (HTTPS only in production)
        if getattr(settings, 'SECURE_SSL_REDIRECT', False):
            response['Strict-Transport-Security'] = 'max-age=31536000; includeSubDomains'
        
        # Content security policy for API
        if request.path.startswith('/api/'):
            response['Content-Security-Policy'] = "default-src 'none'; frame-ancestors 'none';"
        
        return response


class RequestLoggingMiddleware(MiddlewareMixin):
    """
    Log API requests for monitoring and debugging.
    """
    
    def process_request(self, request):
        """Log incoming API requests."""
        if request.path.startswith('/api/'):
            user_info = "anonymous"
            if hasattr(request, 'user') and request.user.is_authenticated:
                user_info = f"{request.user.phone_number} (verified={getattr(request.user, 'OTP_verified', False)})"
            
            logger.info(f"API Request: {request.method} {request.path} - User: {user_info}")
        
        return None
    
    def process_response(self, request, response):
        """Log API response status."""
        if request.path.startswith('/api/'):
            logger.info(f"API Response: {request.method} {request.path} - Status: {response.status_code}")
        
        return response