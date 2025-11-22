"""
Authentication URL Configuration

Defines URL patterns for OTP-based authentication:
- POST /auth/request-otp/ - Request OTP for phone number
- POST /auth/verify-otp/ - Verify OTP and get JWT tokens
- GET /auth/profile/ - Get user profile information
- POST /auth/refresh/ - Refresh JWT access token
- POST /parent/create-child/ - Create child profile
- GET /parent/children/ - List parent's children
- GET /parent/dashboard/ - Parent dashboard data
- POST /parent/set-screen-time-limit/ - Set child screen time limit
"""
from django.urls import path
from .auth_views import (
    RequestOTPView,
    VerifyOTPView,
    CreateChildProfileView,
    ListChildrenView,
    user_profile_view,
    refresh_token_view
)

app_name = 'auth'

# Authentication endpoints
auth_urlpatterns = [
    path('request-otp/', RequestOTPView.as_view(), name='request_otp'),
    path('verify-otp/', VerifyOTPView.as_view(), name='verify_otp'),
    path('profile/', user_profile_view, name='user_profile'),
    path('refresh/', refresh_token_view, name='refresh_token'),
]

# Parent-specific endpoints
parent_urlpatterns = [
    path('create-child/', CreateChildProfileView.as_view(), name='create_child'),
    path('children/', ListChildrenView.as_view(), name='list_children'),
]

# Dashboard endpoints (will be imported dynamically)
dashboard_urlpatterns = []

# Try to import dashboard views and add their URLs
try:
    from .dashboard_views import ParentDashboardView, SetScreenTimeLimitView
    dashboard_urlpatterns = [
        path('dashboard/', ParentDashboardView.as_view(), name='parent_dashboard'),
        path('set-screen-time-limit/', SetScreenTimeLimitView.as_view(), name='set_screen_time_limit'),
    ]
except ImportError:
    pass

# Combine all patterns
urlpatterns = auth_urlpatterns + parent_urlpatterns + dashboard_urlpatterns