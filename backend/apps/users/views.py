"""
User ViewSets for Porakhela API
"""
from rest_framework import viewsets, status, permissions
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken
from django.contrib.auth import get_user_model
from django.db import models
from datetime import datetime, timedelta
from apps.users.models import ChildProfile, OTPVerification, ParentalPIN
from apps.users.serializers import (
    UserSerializer, RegisterSerializer, LoginSerializer,
    OTPRequestSerializer, OTPVerifySerializer, ChildProfileSerializer,
    CreateChildProfileSerializer, ParentalPINSerializer
)
from services.applink_client import applink_client
from utils.helpers import generate_otp

User = get_user_model()


class AuthViewSet(viewsets.GenericViewSet):
    """Authentication endpoints"""
    
    @action(detail=False, methods=['post'], permission_classes=[permissions.AllowAny])
    def register(self, request):
        """Register new user"""
        serializer = RegisterSerializer(data=request.data)
        if serializer.is_valid():
            user = serializer.save()
            
            # Generate and send OTP
            otp_code = generate_otp()
            OTPVerification.objects.create(
                phone_number=user.phone_number,
                otp_code=otp_code,
                otp_type='registration',
                expires_at=datetime.now() + timedelta(minutes=10)
            )
            
            # Send OTP via Applink SMS
            # In production, uncomment this:
            # await applink_client.send_otp(
            #     user.phone_number, 
            #     otp_code, 
            #     f"Your Porakhela verification code is: {otp_code}"
            # )
            
            return Response({
                'message': 'Registration successful. Please verify your phone number.',
                'user_id': user.id,
                'otp_sent': True
            }, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
    
    @action(detail=False, methods=['post'], permission_classes=[permissions.AllowAny])
    def login(self, request):
        """User login"""
        serializer = LoginSerializer(data=request.data)
        if serializer.is_valid():
            user = serializer.validated_data['user']
            
            # Generate tokens
            refresh = RefreshToken.for_user(user)
            
            return Response({
                'message': 'Login successful',
                'user': UserSerializer(user).data,
                'tokens': {
                    'refresh': str(refresh),
                    'access': str(refresh.access_token),
                }
            })
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
    
    @action(detail=False, methods=['post'], permission_classes=[permissions.AllowAny])
    def request_otp(self, request):
        """Request OTP for phone verification"""
        serializer = OTPRequestSerializer(data=request.data)
        if serializer.is_valid():
            phone_number = serializer.validated_data['phone_number']
            otp_type = serializer.validated_data['otp_type']
            
            # Generate OTP
            otp_code = generate_otp()
            OTPVerification.objects.create(
                phone_number=phone_number,
                otp_code=otp_code,
                otp_type=otp_type,
                expires_at=datetime.now() + timedelta(minutes=10)
            )
            
            # Send OTP via Applink SMS
            # await applink_client.send_otp(phone_number, otp_code, f"Your Porakhela code: {otp_code}")
            
            return Response({
                'message': 'OTP sent successfully',
                'expires_in': 600  # 10 minutes
            })
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
    
    @action(detail=False, methods=['post'], permission_classes=[permissions.AllowAny])
    def verify_otp(self, request):
        """Verify OTP"""
        serializer = OTPVerifySerializer(data=request.data)
        if serializer.is_valid():
            phone_number = serializer.validated_data['phone_number']
            otp_code = serializer.validated_data['otp_code']
            otp_type = serializer.validated_data['otp_type']
            
            try:
                otp_obj = OTPVerification.objects.get(
                    phone_number=phone_number,
                    otp_code=otp_code,
                    otp_type=otp_type,
                    is_verified=False,
                    expires_at__gt=datetime.now()
                )
                
                # Mark as verified
                otp_obj.is_verified = True
                otp_obj.save()
                
                # Update user verification status
                if otp_type == 'registration':
                    user = User.objects.get(phone_number=phone_number)
                    user.is_phone_verified = True
                    user.save()
                    
                    # Generate tokens for immediate login
                    refresh = RefreshToken.for_user(user)
                    return Response({
                        'message': 'Phone number verified successfully',
                        'user': UserSerializer(user).data,
                        'tokens': {
                            'refresh': str(refresh),
                            'access': str(refresh.access_token),
                        }
                    })
                
                return Response({'message': 'OTP verified successfully'})
                
            except OTPVerification.DoesNotExist:
                return Response(
                    {'error': 'Invalid or expired OTP'}, 
                    status=status.HTTP_400_BAD_REQUEST
                )
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class UserViewSet(viewsets.ModelViewSet):
    """User management endpoints"""
    serializer_class = UserSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        user = self.request.user
        if user.user_type == 'parent':
            # Parents can see their own profile and their children
            return User.objects.filter(
                models.Q(id=user.id) | models.Q(child_profile__parent=user)
            )
        else:
            # Children can only see their own profile
            return User.objects.filter(id=user.id)
    
    @action(detail=False, methods=['get'])
    def me(self, request):
        """Get current user profile"""
        serializer = self.get_serializer(request.user)
        return Response(serializer.data)
    
    @action(detail=False, methods=['get'])
    def children(self, request):
        """Get parent's children profiles"""
        if request.user.user_type != 'parent':
            return Response(
                {'error': 'Only parents can access this endpoint'}, 
                status=status.HTTP_403_FORBIDDEN
            )
        
        children_profiles = ChildProfile.objects.filter(parent=request.user)
        serializer = ChildProfileSerializer(children_profiles, many=True)
        return Response(serializer.data)


class ChildProfileViewSet(viewsets.ModelViewSet):
    """Child profile management"""
    serializer_class = ChildProfileSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        user = self.request.user
        if user.user_type == 'parent':
            return ChildProfile.objects.filter(parent=user)
        else:
            # Child can only see their own profile
            return ChildProfile.objects.filter(user=user)
    
    def get_serializer_class(self):
        if self.action == 'create':
            return CreateChildProfileSerializer
        return ChildProfileSerializer
    
    def create(self, request, *args, **kwargs):
        """Create child profile (parents only)"""
        if request.user.user_type != 'parent':
            return Response(
                {'error': 'Only parents can create child profiles'}, 
                status=status.HTTP_403_FORBIDDEN
            )
        return super().create(request, *args, **kwargs)
    
    @action(detail=True, methods=['post'])
    def verify_pin(self, request, pk=None):
        """Verify parental PIN for child account actions"""
        child_profile = self.get_object()
        
        # Only parent can verify PIN
        if request.user != child_profile.parent:
            return Response(
                {'error': 'Access denied'}, 
                status=status.HTTP_403_FORBIDDEN
            )
        
        serializer = ParentalPINSerializer(data=request.data, context={'request': request})
        if serializer.is_valid():
            return Response({'message': 'PIN verified successfully'})
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
    
    @action(detail=True, methods=['post'])
    def update_screen_time(self, request, pk=None):
        """Update daily screen time limit"""
        child_profile = self.get_object()
        
        if request.user != child_profile.parent:
            return Response(
                {'error': 'Only parent can update screen time'}, 
                status=status.HTTP_403_FORBIDDEN
            )
        
        screen_time = request.data.get('daily_screen_time_limit')
        if screen_time and isinstance(screen_time, int) and screen_time > 0:
            child_profile.daily_screen_time_limit = screen_time
            child_profile.save()
            return Response({'message': 'Screen time updated successfully'})
        
        return Response(
            {'error': 'Invalid screen time value'}, 
            status=status.HTTP_400_BAD_REQUEST
        )