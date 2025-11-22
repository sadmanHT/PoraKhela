"""
Authentication Views for OTP-based Phone Authentication

Provides:
- POST /auth/request-otp/ - Request OTP for phone number
- POST /auth/verify-otp/ - Verify OTP and get JWT tokens
- POST /parent/create-child/ - Create child profile (authenticated parents only)
"""
from rest_framework import status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView
from drf_spectacular.utils import extend_schema, OpenApiExample
import logging

from .auth_serializers import (
    RequestOTPSerializer,
    VerifyOTPSerializer,
    AuthenticationResponseSerializer,
    CreateChildProfileSerializer,
    ChildProfileResponseSerializer
)
from .auth_service import otp_service
from .models import User, ChildProfile

logger = logging.getLogger(__name__)


class RequestOTPView(APIView):
    """
    Request OTP for phone number authentication.
    
    Steps:
    1. Validate phone number format
    2. Check rate limiting (max 5 requests per 5 minutes)
    3. Generate 6-digit OTP
    4. Store in Redis with 3-minute TTL
    5. Send OTP via Applink SMS
    6. Create user account if doesn't exist
    """
    permission_classes = [AllowAny]
    
    @extend_schema(
        request=RequestOTPSerializer,
        responses={
            200: {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean"},
                    "message": {"type": "string"},
                    "ttl_seconds": {"type": "integer"},
                    "remaining_attempts": {"type": "integer"}
                }
            },
            429: {
                "type": "object", 
                "properties": {
                    "success": {"type": "boolean"},
                    "message": {"type": "string"},
                    "code": {"type": "string"}
                }
            }
        },
        summary="Request OTP for Phone Authentication",
        description="Send OTP to phone number for authentication. Creates user if doesn't exist.",
        examples=[
            OpenApiExample(
                "Request OTP",
                value={"phone_number": "01712345678"},
                request_only=True
            )
        ]
    )
    def post(self, request):
        """Handle OTP request."""
        serializer = RequestOTPSerializer(data=request.data)
        
        if not serializer.is_valid():
            return Response({
                'success': False,
                'message': 'Invalid phone number format',
                'errors': serializer.errors
            }, status=status.HTTP_400_BAD_REQUEST)
        
        phone_number = serializer.validated_data['phone_number']
        
        logger.info(f"OTP request for phone number: {phone_number}")
        
        # Request OTP via service
        result = otp_service.request_otp(phone_number)
        
        if result['success']:
            return Response(result, status=status.HTTP_200_OK)
        
        # Handle rate limiting
        if result.get('code') == 'RATE_LIMIT_EXCEEDED':
            return Response(result, status=status.HTTP_429_TOO_MANY_REQUESTS)
        
        return Response(result, status=status.HTTP_400_BAD_REQUEST)


class VerifyOTPView(APIView):
    """
    Verify OTP code and authenticate user.
    
    Steps:
    1. Validate phone number and OTP format
    2. Retrieve stored OTP from Redis
    3. Verify code and check expiry
    4. Mark user as OTP_verified=True
    5. Generate JWT access and refresh tokens
    6. Return tokens and user info
    """
    permission_classes = [AllowAny]
    
    @extend_schema(
        request=VerifyOTPSerializer,
        responses={
            200: AuthenticationResponseSerializer,
            400: {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean"},
                    "message": {"type": "string"},
                    "code": {"type": "string"}
                }
            }
        },
        summary="Verify OTP and Authenticate",
        description="Verify OTP code and return JWT tokens for authenticated access.",
        examples=[
            OpenApiExample(
                "Verify OTP",
                value={
                    "phone_number": "01712345678",
                    "otp": "123456"
                },
                request_only=True
            )
        ]
    )
    def post(self, request):
        """Handle OTP verification."""
        serializer = VerifyOTPSerializer(data=request.data)
        
        if not serializer.is_valid():
            return Response({
                'success': False,
                'message': 'Invalid request data',
                'errors': serializer.errors
            }, status=status.HTTP_400_BAD_REQUEST)
        
        phone_number = serializer.validated_data['phone_number']
        otp_code = serializer.validated_data['otp']
        
        logger.info(f"OTP verification attempt for phone number: {phone_number}")
        
        # Verify OTP via service
        result = otp_service.verify_otp(phone_number, otp_code)
        
        if result['success']:
            # Format response according to specification
            response_data = {
                'token': result['tokens']['access'],
                'refresh': result['tokens']['refresh'],
                'is_parent': result['user']['is_parent'],
                'user_id': result['user']['id'],
                'phone_number': result['user']['phone_number'],
                'has_children': result['user']['has_children'],
                'children_count': result['user']['children_count']
            }
            
            return Response(response_data, status=status.HTTP_200_OK)
        
        return Response({
            'success': False,
            'message': result['message'],
            'code': result.get('code', 'VERIFICATION_FAILED')
        }, status=status.HTTP_400_BAD_REQUEST)


class CreateChildProfileView(APIView):
    """
    Create child profile for authenticated parent.
    
    Requirements:
    - User must be authenticated with valid JWT
    - User must have OTP_verified=True
    - User must be parent (is_parent=True)
    
    Creates:
    - Child User account with unique phone_number
    - ChildProfile linked to parent
    """
    permission_classes = [IsAuthenticated]
    
    @extend_schema(
        request=CreateChildProfileSerializer,
        responses={
            201: ChildProfileResponseSerializer,
            400: {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean"},
                    "message": {"type": "string"},
                    "errors": {"type": "object"}
                }
            },
            403: {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean"},
                    "message": {"type": "string"}
                }
            }
        },
        summary="Create Child Profile",
        description="Create a child profile linked to the authenticated parent.",
        examples=[
            OpenApiExample(
                "Create Child",
                value={
                    "name": "Rafi Ahmad",
                    "grade": 3,
                    "avatar": "lion",
                    "date_of_birth": "2015-05-15"
                },
                request_only=True
            )
        ]
    )
    def post(self, request):
        """Create child profile."""
        # Check if user is parent and OTP verified
        if not request.user.is_parent:
            return Response({
                'success': False,
                'message': 'Only parent accounts can create child profiles'
            }, status=status.HTTP_403_FORBIDDEN)
        
        if not request.user.OTP_verified:
            return Response({
                'success': False,
                'message': 'Phone number must be verified to create child profiles'
            }, status=status.HTTP_403_FORBIDDEN)
        
        serializer = CreateChildProfileSerializer(
            data=request.data,
            context={'request': request}
        )
        
        if not serializer.is_valid():
            return Response({
                'success': False,
                'message': 'Invalid child profile data',
                'errors': serializer.errors
            }, status=status.HTTP_400_BAD_REQUEST)
        
        try:
            child_profile = serializer.save()
            
            logger.info(f"Child profile created: {child_profile.user.get_full_name()} for parent {request.user.phone_number}")
            
            # Return child profile data
            response_serializer = ChildProfileResponseSerializer(child_profile)
            
            return Response({
                'success': True,
                'message': f'Child profile created successfully for {child_profile.user.get_full_name()}',
                'child': response_serializer.data
            }, status=status.HTTP_201_CREATED)
            
        except Exception as e:
            logger.error(f"Error creating child profile for {request.user.phone_number}: {str(e)}")
            logger.error(f"Exception type: {type(e).__name__}")
            logger.error(f"Full traceback: ", exc_info=True)
            return Response({
                'success': False,
                'message': f'Failed to create child profile: {str(e)}',
                'error_type': type(e).__name__
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class ListChildrenView(APIView):
    """
    List all children for authenticated parent.
    """
    permission_classes = [IsAuthenticated]
    
    @extend_schema(
        responses={200: ChildProfileResponseSerializer(many=True)},
        summary="List Parent's Children",
        description="Get list of all children linked to the authenticated parent."
    )
    def get(self, request):
        """List parent's children."""
        if not request.user.is_parent:
            return Response({
                'success': False,
                'message': 'Only parent accounts can access child lists'
            }, status=status.HTTP_403_FORBIDDEN)
        
        children = ChildProfile.objects.filter(parent=request.user).select_related('user')
        serializer = ChildProfileResponseSerializer(children, many=True)
        
        return Response({
            'success': True,
            'children': serializer.data,
            'total_children': len(serializer.data)
        }, status=status.HTTP_200_OK)


# Function-based views for simpler endpoints
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def user_profile_view(request):
    """
    Get authenticated user profile information.
    """
    user = request.user
    
    profile_data = {
        'user_id': str(user.id),
        'phone_number': user.phone_number,
        'first_name': user.first_name,
        'last_name': user.last_name,
        'is_parent': user.is_parent,
        'user_type': user.user_type,
        'OTP_verified': user.OTP_verified,
        'created_at': user.created_at,
        'last_login': user.last_login
    }
    
    if user.is_parent:
        children_count = user.children.count()
        profile_data.update({
            'children_count': children_count,
            'has_children': children_count > 0
        })
    else:
        # Child user - get profile data
        if hasattr(user, 'child_profile'):
            child_profile = user.child_profile
            profile_data.update({
                'parent_phone': child_profile.parent.phone_number,
                'grade': child_profile.grade,
                'total_points': child_profile.total_points,
                'total_lessons_completed': child_profile.total_lessons_completed,
                'current_streak': child_profile.current_streak
            })
    
    return Response({
        'success': True,
        'profile': profile_data
    }, status=status.HTTP_200_OK)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def refresh_token_view(request):
    """
    Refresh JWT access token using refresh token.
    """
    from rest_framework_simplejwt.tokens import RefreshToken
    from rest_framework_simplejwt.exceptions import TokenError
    
    refresh_token = request.data.get('refresh')
    
    if not refresh_token:
        return Response({
            'success': False,
            'message': 'Refresh token is required'
        }, status=status.HTTP_400_BAD_REQUEST)
    
    try:
        refresh = RefreshToken(refresh_token)
        access_token = refresh.access_token
        
        return Response({
            'success': True,
            'access': str(access_token)
        }, status=status.HTTP_200_OK)
        
    except TokenError:
        return Response({
            'success': False,
            'message': 'Invalid refresh token'
        }, status=status.HTTP_401_UNAUTHORIZED)