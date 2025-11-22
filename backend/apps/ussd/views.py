"""
USSD Views for Porakhela Parental Controls

Handles incoming USSD requests from telecom providers and returns appropriate responses.
"""

import json
import logging
from django.http import JsonResponse, HttpResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods
from django.utils.decorators import method_decorator
from django.views import View
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework import status

from .menu_handler import menu_handler
from .models import USSDSession, USSDLog

logger = logging.getLogger(__name__)


@method_decorator(csrf_exempt, name='dispatch')
class USSDEndpointView(View):
    """
    Main USSD endpoint for processing telecom provider requests.
    
    Expected payload format:
    {
        "session_id": "abc123",
        "phone_number": "017XXX",
        "user_input": "1"
    }
    """
    
    def post(self, request):
        try:
            # Parse request data
            if request.content_type == 'application/json':
                data = json.loads(request.body)
            else:
                data = request.POST.dict()
            
            session_id = data.get('session_id', '').strip()
            phone_number = data.get('phone_number', '').strip()
            user_input = data.get('user_input', '').strip()
            
            # Validate required fields
            if not session_id:
                return JsonResponse({
                    'error': 'session_id is required'
                }, status=400)
            
            if not phone_number:
                return JsonResponse({
                    'error': 'phone_number is required'
                }, status=400)
            
            # Log incoming request
            logger.info(f"USSD Request - Session: {session_id}, Phone: {phone_number}, Input: '{user_input}'")
            
            # Process USSD input through menu handler
            response_type, message = menu_handler.process_ussd_input(
                session_id=session_id,
                phone_number=phone_number,
                user_input=user_input
            )
            
            # Format response according to USSD standards
            response_data = {
                'session_id': session_id,
                'response_type': response_type,
                'message': message
            }
            
            # Log outgoing response
            logger.info(f"USSD Response - Session: {session_id}, Type: {response_type}, Message: '{message[:50]}...'")
            
            # Return response in format expected by telecom provider
            return JsonResponse(response_data, status=200)
            
        except json.JSONDecodeError:
            logger.error(f"Invalid JSON in USSD request: {request.body}")
            return JsonResponse({
                'error': 'Invalid JSON format'
            }, status=400)
            
        except Exception as e:
            logger.error(f"Unexpected error in USSD endpoint: {str(e)}")
            return JsonResponse({
                'session_id': data.get('session_id', ''),
                'response_type': 'END',
                'message': 'Service temporarily unavailable. Please try again later.'
            }, status=500)
    
    def get(self, request):
        """Handle GET requests for testing purposes."""
        return JsonResponse({
            'message': 'USSD endpoint is active',
            'methods': ['POST'],
            'example_payload': {
                'session_id': 'abc123',
                'phone_number': '01712345678',
                'user_input': '1'
            }
        })


# Alternative implementation using DRF for better API documentation
@api_view(['POST', 'GET'])
@permission_classes([AllowAny])
def ussd_endpoint(request):
    """
    USSD Gateway Endpoint
    
    Processes USSD requests from telecom providers and returns menu responses.
    
    POST Parameters:
    - session_id: Unique session identifier from telecom provider
    - phone_number: Parent's phone number (e.g., "01712345678")
    - user_input: User's menu selection (e.g., "1", "2", "", etc.)
    
    Response Format:
    {
        "session_id": "abc123",
        "response_type": "CON" | "END",
        "message": "Menu text or final message"
    }
    
    Response Types:
    - CON: Continue session, show menu to user
    - END: End session, show final message
    """
    
    if request.method == 'GET':
        return Response({
            'message': 'Porakhela USSD Gateway',
            'status': 'active',
            'endpoint': '/applink/ussd/',
            'supported_features': [
                'Porapoints Balance View',
                'Redemption Request Approval',
                'Learning Summary',
                'Screen Time Management'
            ],
            'example_request': {
                'session_id': 'tel_session_123',
                'phone_number': '01712345678',
                'user_input': '1'
            }
        })
    
    try:
        # Extract data from request
        data = request.data
        session_id = data.get('session_id', '').strip()
        phone_number = data.get('phone_number', '').strip()
        user_input = data.get('user_input', '').strip()
        
        # Validate required fields
        if not session_id:
            return Response({
                'error': 'session_id is required',
                'example': 'tel_session_123'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        if not phone_number:
            return Response({
                'error': 'phone_number is required',
                'example': '01712345678'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        # Process USSD request
        response_type, message = menu_handler.process_ussd_input(
            session_id=session_id,
            phone_number=phone_number,
            user_input=user_input
        )
        
        return Response({
            'session_id': session_id,
            'response_type': response_type,
            'message': message,
            'timestamp': timezone.now().isoformat()
        }, status=status.HTTP_200_OK)
        
    except Exception as e:
        logger.error(f"Error processing USSD request: {str(e)}")
        return Response({
            'error': 'Internal server error',
            'session_id': data.get('session_id', '') if 'data' in locals() else '',
            'response_type': 'END',
            'message': 'Service temporarily unavailable. Please try again later.'
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


@api_view(['POST'])
@permission_classes([AllowAny])
def ussd_test_endpoint(request):
    """
    Test endpoint for USSD flow simulation.
    
    Allows testing USSD interactions without telecom provider integration.
    """
    try:
        phone_number = request.data.get('phone_number')
        menu_path = request.data.get('menu_path', [])  # List of user inputs
        
        if not phone_number:
            return Response({
                'error': 'phone_number is required'
            }, status=status.HTTP_400_BAD_REQUEST)
        
        # Simulate USSD session
        import uuid
        test_session_id = f"test_{uuid.uuid4().hex[:8]}"
        
        responses = []
        
        for i, user_input in enumerate([''] + menu_path):  # Start with empty input for main menu
            response_type, message = menu_handler.process_ussd_input(
                session_id=test_session_id,
                phone_number=phone_number,
                user_input=user_input
            )
            
            responses.append({
                'step': i + 1,
                'user_input': user_input,
                'response_type': response_type,
                'message': message
            })
            
            # Stop if session ended
            if response_type == 'END':
                break
        
        return Response({
            'test_session_id': test_session_id,
            'phone_number': phone_number,
            'menu_path': menu_path,
            'responses': responses
        }, status=status.HTTP_200_OK)
        
    except Exception as e:
        logger.error(f"Error in USSD test endpoint: {str(e)}")
        return Response({
            'error': 'Test failed',
            'details': str(e)
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


# Simple HTTP endpoint for basic telecom providers
@csrf_exempt
@require_http_methods(["POST"])
def ussd_simple_endpoint(request):
    """
    Simple HTTP endpoint for basic telecom provider integration.
    Returns plain text response suitable for USSD display.
    """
    try:
        # Parse form data or JSON
        if request.content_type == 'application/json':
            import json
            data = json.loads(request.body)
        else:
            data = request.POST.dict()
        
        session_id = data.get('sessionId') or data.get('session_id', '')
        phone_number = data.get('phoneNumber') or data.get('phone_number', '')
        user_input = data.get('text') or data.get('user_input', '')
        
        if not session_id or not phone_number:
            return HttpResponse("END Service unavailable", content_type='text/plain')
        
        # Process through menu handler
        response_type, message = menu_handler.process_ussd_input(
            session_id=session_id,
            phone_number=phone_number,
            user_input=user_input
        )
        
        # Return plain text response
        response_text = f"{response_type} {message}"
        return HttpResponse(response_text, content_type='text/plain')
        
    except Exception as e:
        logger.error(f"Error in simple USSD endpoint: {str(e)}")
        return HttpResponse("END Service error", content_type='text/plain')


# Import timezone for timestamps
from django.utils import timezone