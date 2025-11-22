"""
User URLs for Porakhela API
"""
from django.urls import path, include
from rest_framework.routers import DefaultRouter
from apps.users.views import AuthViewSet, UserViewSet, ChildProfileViewSet
from .auth_views import (
    RequestOTPView,
    VerifyOTPView,
    CreateChildProfileView,
    ListChildrenView,
    user_profile_view,
    refresh_token_view
)

router = DefaultRouter()
router.register(r'auth', AuthViewSet, basename='auth')
router.register(r'users', UserViewSet, basename='users')
router.register(r'children', ChildProfileViewSet, basename='children')

# OTP Authentication endpoints
auth_patterns = [
    path('request-otp/', RequestOTPView.as_view(), name='request_otp'),
    path('verify-otp/', VerifyOTPView.as_view(), name='verify_otp'),
    path('profile/', user_profile_view, name='user_profile'),
    path('refresh/', refresh_token_view, name='refresh_token'),
]

# Parent-specific endpoints
parent_patterns = [
    path('parent/create-child/', CreateChildProfileView.as_view(), name='create_child'),
    path('parent/children/', ListChildrenView.as_view(), name='list_children'),
]

# Dashboard endpoints (dynamically imported)
dashboard_patterns = []
try:
    from .dashboard_views import ParentDashboardView, SetScreenTimeLimitView, ChildrenListView
    dashboard_patterns = [
        path('parent/dashboard/', ParentDashboardView.as_view(), name='parent_dashboard'),
        path('parent/set-screen-time-limit/', SetScreenTimeLimitView.as_view(), name='set_screen_time_limit'),
        path('parent/children-list/', ChildrenListView.as_view(), name='parent_children_list'),
    ]
except ImportError as e:
    print(f"Warning: Could not import dashboard views: {e}")

urlpatterns = auth_patterns + parent_patterns + dashboard_patterns + [
    path('', include(router.urls)),
]