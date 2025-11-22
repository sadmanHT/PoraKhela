"""
URL configuration for USSD endpoints.
"""

from django.urls import path
from . import views

app_name = 'ussd'

urlpatterns = [
    # Main USSD endpoint for telecom providers
    path('', views.ussd_endpoint, name='ussd_endpoint'),
    
    # Alternative class-based view
    path('gateway/', views.USSDEndpointView.as_view(), name='ussd_gateway'),
    
    # Simple endpoint for basic telecom providers
    path('simple/', views.ussd_simple_endpoint, name='ussd_simple'),
    
    # Test endpoint for development
    path('test/', views.ussd_test_endpoint, name='ussd_test'),
]