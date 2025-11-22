"""
USSD Menu Handler with State Machine

Processes USSD inputs and generates appropriate responses based on menu state.
"""

import logging
from typing import Dict, Any, Tuple
from django.contrib.auth import get_user_model
from django.db.models import Sum, Count
from django.utils import timezone
from datetime import datetime, timedelta

from .session_manager import session_manager
from .models import RedemptionRequest, ScreenTimeSetting
from apps.users.models import ChildProfile
from apps.gamification.models import PorapointLedger
from apps.lessons.models import LessonProgress

User = get_user_model()
logger = logging.getLogger(__name__)


class USSDMenuHandler:
    """
    Handles USSD menu logic and state transitions.
    """
    
    def __init__(self):
        self.menu_handlers = {
            'main_menu': self._handle_main_menu,
            'balance_view': self._handle_balance_view,
            'redemption_approval': self._handle_redemption_approval,
            'redemption_detail': self._handle_redemption_detail,
            'learning_summary': self._handle_learning_summary,
            'screen_time_menu': self._handle_screen_time_menu,
            'screen_time_input': self._handle_screen_time_input,
        }
    
    def process_ussd_input(self, session_id: str, phone_number: str, user_input: str) -> Tuple[str, str]:
        """
        Process USSD input and return response type and message.
        
        Args:
            session_id: Telecom provider session ID
            phone_number: Parent's phone number
            user_input: User's menu selection
            
        Returns:
            Tuple of (response_type, message) where response_type is 'CON' or 'END'
        """
        # Get or create session
        session = session_manager.get_session(session_id)
        if not session:
            # First interaction - authenticate parent
            parent = self._authenticate_parent(phone_number)
            if not parent:
                return 'END', 'Sorry, this number is not registered as a parent account. Please contact support.'
            
            session = session_manager.create_session(session_id, phone_number, str(parent.id))
        
        current_state = session.get('current_state', 'main_menu')
        
        # Get handler for current state
        handler = self.menu_handlers.get(current_state, self._handle_unknown_state)
        
        try:
            response_type, message = handler(session_id, user_input, session)
            
            # Log interaction
            self._log_interaction(session_id, phone_number, user_input, current_state, f"{response_type} {message}")
            
            return response_type, message
            
        except Exception as e:
            logger.error(f"Error processing USSD input for session {session_id}: {str(e)}")
            return 'END', 'An error occurred. Please try again later.'
    
    def _authenticate_parent(self, phone_number: str):
        """
        Authenticate parent by phone number.
        
        Args:
            phone_number: Parent's phone number
            
        Returns:
            Parent User object or None
        """
        try:
            # Clean phone number format
            cleaned_number = self._clean_phone_number(phone_number)
            return User.objects.get(phone_number=cleaned_number, is_parent=True)
        except User.DoesNotExist:
            return None
    
    def _clean_phone_number(self, phone_number: str) -> str:
        """Clean and standardize phone number format."""
        # Remove spaces, dashes, and plus signs, keep only digits
        cleaned = ''.join(filter(str.isdigit, phone_number))
        
        # Add country code if missing (assuming Bangladesh +880)
        if len(cleaned) == 11 and cleaned.startswith('01'):
            cleaned = '880' + cleaned[1:]
        elif len(cleaned) == 10:
            cleaned = '880' + cleaned
        
        return '+' + cleaned
    
    def _handle_main_menu(self, session_id: str, user_input: str, session: Dict[str, Any]) -> Tuple[str, str]:
        """Handle main menu interactions."""
        if not user_input or user_input.strip() == '':
            # First time or empty input - show main menu
            menu = ("Welcome to Porakhela Parent Portal\n"
                   "1. View Porapoints Balance\n"
                   "2. Approve Redemption Requests\n"
                   "3. View Today's Learning Summary\n"
                   "4. Set Screen Time Limit\n"
                   "0. Exit")
            return 'CON', menu
        
        choice = user_input.strip()
        
        if choice == '1':
            session_manager.set_state(session_id, 'balance_view')
            return self._handle_balance_view(session_id, '', session)
        elif choice == '2':
            session_manager.set_state(session_id, 'redemption_approval')
            return self._handle_redemption_approval(session_id, '', session)
        elif choice == '3':
            session_manager.set_state(session_id, 'learning_summary')
            return self._handle_learning_summary(session_id, '', session)
        elif choice == '4':
            session_manager.set_state(session_id, 'screen_time_menu')
            return self._handle_screen_time_menu(session_id, '', session)
        elif choice == '0':
            session_manager.end_session(session_id)
            return 'END', 'Thank you for using Porakhela!'
        else:
            menu = ("Invalid option. Please try again.\n"
                   "1. View Porapoints Balance\n"
                   "2. Approve Redemption Requests\n"
                   "3. View Today's Learning Summary\n"
                   "4. Set Screen Time Limit\n"
                   "0. Exit")
            return 'CON', menu
    
    def _handle_balance_view(self, session_id: str, user_input: str, session: Dict[str, Any]) -> Tuple[str, str]:
        """Handle Porapoints balance viewing."""
        parent_id = session.get('parent_id')
        
        try:
            parent = User.objects.get(id=parent_id)
            children = ChildProfile.objects.filter(parent=parent)
            
            if not children.exists():
                return 'END', 'No children found for your account.\n0. Back'
            
            balance_text = "Your children's Porapoints:\n\n"
            
            for child_profile in children:
                child = child_profile.user
                # Get latest balance
                latest_transaction = PorapointLedger.objects.filter(
                    child=child
                ).order_by('-created_at').first()
                
                balance = latest_transaction.balance_after if latest_transaction else 0
                balance_text += f"{child.first_name}: {balance} pts\n"
            
            balance_text += "\n0. Back to Main Menu"
            
            if user_input.strip() == '0':
                session_manager.set_state(session_id, 'main_menu')
                return self._handle_main_menu(session_id, '', session)
            
            return 'CON', balance_text
            
        except Exception as e:
            logger.error(f"Error fetching balance for session {session_id}: {str(e)}")
            return 'END', 'Error fetching balance. Please try again later.'
    
    def _handle_redemption_approval(self, session_id: str, user_input: str, session: Dict[str, Any]) -> Tuple[str, str]:
        """Handle redemption request approval."""
        parent_id = session.get('parent_id')
        
        try:
            parent = User.objects.get(id=parent_id)
            pending_requests = RedemptionRequest.objects.filter(
                parent=parent,
                status='pending'
            ).order_by('-requested_at')[:5]  # Show max 5 recent requests
            
            if not pending_requests.exists():
                return 'CON', 'No pending redemption requests.\n\n0. Back to Main Menu'
            
            if not user_input or user_input.strip() == '':
                # Show pending requests
                menu_text = "Pending Redemptions:\n\n"
                for i, request in enumerate(pending_requests, 1):
                    menu_text += f"{i}. {request.child.first_name}: {request.item_name}\n"
                    menu_text += f"   {request.points_required} pts\n"
                
                menu_text += "\nSelect request to approve:\n0. Back"
                session_manager.set_session_data(session_id, 'pending_requests', [str(req.id) for req in pending_requests])
                return 'CON', menu_text
            
            choice = user_input.strip()
            
            if choice == '0':
                session_manager.set_state(session_id, 'main_menu')
                return self._handle_main_menu(session_id, '', session)
            
            # Handle request selection
            try:
                request_index = int(choice) - 1
                request_ids = session_manager.get_session_data(session_id, 'pending_requests') or []
                
                if 0 <= request_index < len(request_ids):
                    request_id = request_ids[request_index]
                    session_manager.set_session_data(session_id, 'selected_request', request_id)
                    session_manager.set_state(session_id, 'redemption_detail')
                    return self._handle_redemption_detail(session_id, '', session)
                else:
                    return 'CON', 'Invalid selection. Please try again.\n0. Back'
                    
            except ValueError:
                return 'CON', 'Invalid input. Please enter a number.\n0. Back'
                
        except Exception as e:
            logger.error(f"Error handling redemption approval for session {session_id}: {str(e)}")
            return 'END', 'Error processing redemption requests.'
    
    def _handle_redemption_detail(self, session_id: str, user_input: str, session: Dict[str, Any]) -> Tuple[str, str]:
        """Handle individual redemption request approval/rejection."""
        request_id = session_manager.get_session_data(session_id, 'selected_request')
        
        try:
            request = RedemptionRequest.objects.get(id=request_id, status='pending')
            
            if not user_input or user_input.strip() == '':
                detail_text = f"Redemption Details:\n\n"
                detail_text += f"Child: {request.child.first_name}\n"
                detail_text += f"Item: {request.item_name}\n"
                detail_text += f"Points: {request.points_required}\n"
                detail_text += f"Type: {request.get_redemption_type_display()}\n"
                if request.description:
                    detail_text += f"Details: {request.description[:50]}...\n"
                
                detail_text += "\n1. Approve\n2. Reject\n0. Back"
                return 'CON', detail_text
            
            choice = user_input.strip()
            
            if choice == '1':
                # Approve request
                request.status = 'approved'
                request.approved_at = timezone.now()
                request.approved_via_ussd = True
                request.ussd_session_id = session_id
                request.save()
                
                session_manager.set_state(session_id, 'main_menu')
                return 'END', f'Redemption approved for {request.child.first_name}!\nThey will be notified shortly.'
                
            elif choice == '2':
                # Reject request
                request.status = 'rejected'
                request.approved_at = timezone.now()
                request.approved_via_ussd = True
                request.ussd_session_id = session_id
                request.save()
                
                session_manager.set_state(session_id, 'main_menu')
                return 'END', f'Redemption rejected for {request.child.first_name}.'
                
            elif choice == '0':
                session_manager.set_state(session_id, 'redemption_approval')
                return self._handle_redemption_approval(session_id, '', session)
            else:
                return 'CON', 'Invalid option.\n1. Approve\n2. Reject\n0. Back'
                
        except RedemptionRequest.DoesNotExist:
            session_manager.set_state(session_id, 'main_menu')
            return 'CON', 'Request no longer available.\n\n0. Back to Main Menu'
    
    def _handle_learning_summary(self, session_id: str, user_input: str, session: Dict[str, Any]) -> Tuple[str, str]:
        """Handle learning summary display."""
        parent_id = session.get('parent_id')
        
        try:
            parent = User.objects.get(id=parent_id)
            children = ChildProfile.objects.filter(parent=parent)
            
            today = timezone.now().date()
            
            summary_text = f"Today's Learning ({today.strftime('%d/%m/%Y')}):\n\n"
            
            total_lessons = 0
            total_points = 0
            
            for child_profile in children:
                child = child_profile.user
                
                # Get today's lesson progress
                today_progress = LessonProgress.objects.filter(
                    child=child,
                    completed_at__date=today,
                    status='completed'
                ).count()
                
                # Get today's points
                today_points = PorapointLedger.objects.filter(
                    child=child,
                    created_at__date=today,
                    change_amount__gt=0
                ).aggregate(total=Sum('change_amount'))['total'] or 0
                
                summary_text += f"{child.first_name}:\n"
                summary_text += f"  Lessons: {today_progress}\n"
                summary_text += f"  Points: {today_points}\n\n"
                
                total_lessons += today_progress
                total_points += today_points
            
            summary_text += f"Total: {total_lessons} lessons, {total_points} pts\n"
            summary_text += "\n0. Back to Main Menu"
            
            if user_input.strip() == '0':
                session_manager.set_state(session_id, 'main_menu')
                return self._handle_main_menu(session_id, '', session)
            
            return 'CON', summary_text
            
        except Exception as e:
            logger.error(f"Error fetching learning summary for session {session_id}: {str(e)}")
            return 'END', 'Error fetching learning summary.'
    
    def _handle_screen_time_menu(self, session_id: str, user_input: str, session: Dict[str, Any]) -> Tuple[str, str]:
        """Handle screen time limit menu."""
        parent_id = session.get('parent_id')
        
        try:
            parent = User.objects.get(id=parent_id)
            children = ChildProfile.objects.filter(parent=parent)
            
            if not children.exists():
                return 'CON', 'No children found.\n\n0. Back to Main Menu'
            
            if not user_input or user_input.strip() == '':
                menu_text = "Select child for screen time:\n\n"
                for i, child_profile in enumerate(children, 1):
                    child = child_profile.user
                    # Get current daily limit
                    current_setting = ScreenTimeSetting.objects.filter(
                        child=child,
                        limit_type='daily',
                        is_active=True
                    ).first()
                    
                    limit_text = f"({current_setting.limit_minutes}min)" if current_setting else "(No limit)"
                    menu_text += f"{i}. {child.first_name} {limit_text}\n"
                
                menu_text += "\n0. Back"
                session_manager.set_session_data(session_id, 'children_list', [str(cp.user.id) for cp in children])
                return 'CON', menu_text
            
            choice = user_input.strip()
            
            if choice == '0':
                session_manager.set_state(session_id, 'main_menu')
                return self._handle_main_menu(session_id, '', session)
            
            try:
                child_index = int(choice) - 1
                children_ids = session_manager.get_session_data(session_id, 'children_list') or []
                
                if 0 <= child_index < len(children_ids):
                    child_id = children_ids[child_index]
                    session_manager.set_session_data(session_id, 'selected_child', child_id)
                    session_manager.set_state(session_id, 'screen_time_input')
                    return self._handle_screen_time_input(session_id, '', session)
                else:
                    return 'CON', 'Invalid selection. Please try again.\n0. Back'
                    
            except ValueError:
                return 'CON', 'Invalid input. Please enter a number.\n0. Back'
                
        except Exception as e:
            logger.error(f"Error handling screen time menu for session {session_id}: {str(e)}")
            return 'END', 'Error accessing screen time settings.'
    
    def _handle_screen_time_input(self, session_id: str, user_input: str, session: Dict[str, Any]) -> Tuple[str, str]:
        """Handle screen time limit input."""
        child_id = session_manager.get_session_data(session_id, 'selected_child')
        parent_id = session.get('parent_id')
        
        try:
            child = User.objects.get(id=child_id)
            parent = User.objects.get(id=parent_id)
            
            if not user_input or user_input.strip() == '':
                current_setting = ScreenTimeSetting.objects.filter(
                    child=child,
                    limit_type='daily',
                    is_active=True
                ).first()
                
                current_text = f"Current: {current_setting.limit_minutes}min" if current_setting else "Current: No limit"
                
                input_text = f"Set daily screen time for {child.first_name}\n"
                input_text += f"{current_text}\n\n"
                input_text += "Enter minutes (0 to remove limit):\n"
                input_text += "Example: 60 for 1 hour\n\n"
                input_text += "0. Back"
                
                return 'CON', input_text
            
            choice = user_input.strip()
            
            if choice == '0':
                session_manager.set_state(session_id, 'screen_time_menu')
                return self._handle_screen_time_menu(session_id, '', session)
            
            try:
                minutes = int(choice)
                
                if minutes < 0 or minutes > 1440:  # Max 24 hours
                    return 'CON', 'Invalid time. Enter 0-1440 minutes.\n0. Back'
                
                # Deactivate existing setting
                ScreenTimeSetting.objects.filter(
                    child=child,
                    limit_type='daily',
                    is_active=True
                ).update(is_active=False)
                
                if minutes > 0:
                    # Create new setting
                    ScreenTimeSetting.objects.create(
                        child=child,
                        parent=parent,
                        limit_type='daily',
                        limit_minutes=minutes,
                        is_active=True,
                        set_via_ussd=True,
                        ussd_session_id=session_id
                    )
                    
                    session_manager.end_session(session_id)
                    return 'END', f'Daily limit set to {minutes} minutes for {child.first_name}.'
                else:
                    session_manager.end_session(session_id)
                    return 'END', f'Daily limit removed for {child.first_name}.'
                    
            except ValueError:
                return 'CON', 'Invalid input. Please enter a number.\n0. Back'
                
        except Exception as e:
            logger.error(f"Error setting screen time for session {session_id}: {str(e)}")
            return 'END', 'Error setting screen time limit.'
    
    def _handle_unknown_state(self, session_id: str, user_input: str, session: Dict[str, Any]) -> Tuple[str, str]:
        """Handle unknown or invalid states."""
        session_manager.set_state(session_id, 'main_menu')
        return self._handle_main_menu(session_id, '', session)
    
    def _log_interaction(self, session_id: str, phone_number: str, user_input: str, menu_state: str, response: str):
        """Log USSD interaction to database."""
        try:
            from .models import USSDLog
            USSDLog.objects.create(
                session_id=session_id,
                phone_number=phone_number,
                user_input=user_input,
                menu_state=menu_state,
                response_sent=response
            )
        except Exception as e:
            logger.error(f"Error logging USSD interaction: {str(e)}")


# Singleton instance
menu_handler = USSDMenuHandler()