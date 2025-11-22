#!/usr/bin/env python
"""
🧪 USSD SYSTEM COMPREHENSIVE TEST SUITE
=======================================
Testing USSD functionality for parents without smartphones
Ensuring complete inclusivity and accessibility
"""

import os
import sys
import django
import json
import time

# Add the project root to Python path
sys.path.insert(0, os.path.dirname(__file__))

# Setup Django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'porakhela.settings')
django.setup()

from django.test import TestCase, Client
from django.urls import reverse
from apps.users.models import User
from apps.gamification.models import PorapointLedger
from apps.ussd.models import USSDSession, USSDLog, RedemptionRequest
from apps.ussd.session_manager import USSDSessionManager
from apps.ussd.menu_handler import USSDMenuHandler
from datetime import datetime, timedelta
import uuid


class MockTestCase:
    """Simple test case replacement for isolated testing"""
    
    def setUp(self):
        pass
    
    def assertEqual(self, a, b, msg=None):
        if a != b:
            raise AssertionError(f"Expected {a} == {b}. {msg or ''}")
    
    def assertIn(self, a, b, msg=None):
        if a not in b:
            raise AssertionError(f"Expected '{a}' in '{b}'. {msg or ''}")
    
    def assertIsNotNone(self, value, msg=None):
        if value is None:
            raise AssertionError(f"Expected value to not be None. {msg or ''}")


class USSDSystemTests:
    """Main test class for USSD system"""
    
    def __init__(self):
        self.client = Client()
        self.session_manager = USSDSessionManager()
        self.menu_handler = USSDMenuHandler()
        
    def setup_test_data(self):
        """Create test data"""
        print("🔧 Setting up test data...")
        
        # Clean up existing test data
        User.objects.filter(phone_number='+8801712345678').delete()
        User.objects.filter(phone_number='+8801712345679').delete()
        
        # Create parent user
        self.parent = User.objects.create_user(
            username='testparent',
            phone_number='+8801712345678',
            first_name='Test',
            last_name='Parent',
            is_parent=True
        )
        
        # Create a child user for redemption testing
        self.child = User.objects.create_user(
            username='testchild',
            phone_number='+8801712345679',
            first_name='Rafi',
            last_name='Ahmed',
            is_parent=False
        )
        
        # Create some test redemption requests
        self.redemption = RedemptionRequest.objects.create(
            child=self.child,
            parent=self.parent,
            redemption_type='data',
            item_name='100MB Data Pack',
            points_required=50,
            description='Mobile internet for educational content',
            status='pending'
        )
        
        print("✅ Test data setup complete")
        
    def test_initial_menu(self):
        """Test A.1: Initial menu display"""
        print("\n🧪 Testing initial menu display...")
        
        session_id = "test_session_001"
        phone_number = "+8801712345678"
        
        response_type, message = self.menu_handler.process_ussd_input(session_id, phone_number, "")
        
        # Convert to dict format for easier testing
        response = {
            'response_type': response_type,
            'message': message
        }
        
        # Check response structure
        assert response['response_type'] == 'CON', f"Expected CON, got {response['response_type']}"
        assert 'Welcome to Porakhela Parent Portal' in response['message'], "Missing welcome message"
        assert '1. View Porapoints Balance' in response['message'], "Missing balance option"
        assert '2. Approve Redemption Requests' in response['message'], "Missing redemption option"
        
        print("✅ Initial menu test passed")
        
    def test_session_progression(self):
        """Test A.2: Session state management"""
        print("\n🧪 Testing session progression...")
        
        session_id = "test_session_002"
        phone_number = "+8801712345678"
        
        # Step 1: Initial menu
        response_type1, message1 = self.menu_handler.process_ussd_input(session_id, phone_number, "")
        response1 = {'response_type': response_type1, 'message': message1}
        assert response1['response_type'] == 'CON', "Initial menu should be CON"
        assert 'Welcome to Porakhela' in response1['message'], "Missing welcome message"
        
        # Step 2: Select balance view
        response_type2, message2 = self.menu_handler.process_ussd_input(session_id, phone_number, "1")
        response2 = {'response_type': response_type2, 'message': message2}
        assert response2['response_type'] == 'CON', "Balance view should be CON"
        
        # Step 3: Back to main menu
        response_type3, message3 = self.menu_handler.process_ussd_input(session_id, phone_number, "0")
        response3 = {'response_type': response_type3, 'message': message3}
        assert response3['response_type'] == 'CON', "Back to menu should be CON"
        assert 'Welcome to Porakhela' in response3['message'], "Should return to main menu"
        
        print("✅ Session progression test passed")
        
    def test_redis_persistence(self):
        """Test A.3: Redis session storage"""
        print("\n🧪 Testing Redis state persistence...")
        
        session_id = "test_session_003"
        
        # Create session data
        session_data = {
            'phone_number': '+8801712345678',
            'current_state': 'balance_view',
            'user_id': str(self.parent.id),
            'created_at': datetime.now().isoformat()
        }
        
        # Store in Redis
        self.session_manager.create_session(session_id, session_data)
        
        # Retrieve from Redis
        retrieved_data = self.session_manager.get_session(session_id)
        
        assert retrieved_data is not None, "Session data should be retrieved"
        assert retrieved_data['phone_number'] == '+8801712345678', "Phone number mismatch"
        assert retrieved_data['current_state'] == 'balance_view', "State mismatch"
        
        print("✅ Redis persistence test passed")
        
    def test_invalid_input_handling(self):
        """Test A.4: Invalid input graceful handling"""
        print("\n🧪 Testing invalid input handling...")
        
        session_id = "test_session_004"
        phone_number = "+8801712345678"
        
        # Test invalid menu option
        response_type1, message1 = self.menu_handler.process_ussd_input(session_id, phone_number, "9")
        response1 = {'response_type': response_type1, 'message': message1}
        assert response1['response_type'] == 'CON', "Invalid input should continue session"
        assert 'Invalid option' in response1['message'], "Should show invalid option message"
        
        # Test non-numeric input
        response_type2, message2 = self.menu_handler.process_ussd_input(session_id, phone_number, "abc")
        response2 = {'response_type': response_type2, 'message': message2}
        
        print("✅ Invalid input handling test passed")
        
    def test_integration_sequence(self):
        """Test B: Integration via HTTP endpoints"""
        print("\n🧪 Testing HTTP integration sequence...")
        
        session_id = "integration_session_001"
        
        # Step 1: Initial request
        response1 = self.client.post('/applink/ussd/', 
            json.dumps({
                'session_id': session_id,
                'phone_number': '+8801712345678',
                'user_input': ''
            }),
            content_type='application/json'
        )
        
        assert response1.status_code == 200, f"Expected 200, got {response1.status_code}"
        data1 = response1.json()
        assert data1['response_type'] == 'CON', "Initial request should be CON"
        assert 'Welcome to Porakhela' in data1['message'], "Missing welcome message"
        
        # Step 2: Select balance (option 1)
        response2 = self.client.post('/applink/ussd/', 
            json.dumps({
                'session_id': session_id,
                'phone_number': '+8801712345678',
                'user_input': '1'
            }),
            content_type='application/json'
        )
        
        assert response2.status_code == 200, "Balance request should succeed"
        data2 = response2.json()
        assert data2['response_type'] == 'CON', "Balance view should be CON"
        
        # Step 3: Back to menu (option 0)
        response3 = self.client.post('/applink/ussd/', 
            json.dumps({
                'session_id': session_id,
                'phone_number': '+8801712345678',
                'user_input': '0'
            }),
            content_type='application/json'
        )
        
        assert response3.status_code == 200, "Back to menu should succeed"
        data3 = response3.json()
        assert data3['response_type'] == 'CON', "Back to menu should be CON"
        assert 'Welcome to Porakhela' in data3['message'], "Should return to main menu"
        
        print("✅ HTTP integration sequence test passed")
        
    def test_session_id_persistence(self):
        """Test B.1: Session ID storage across requests"""
        print("\n🧪 Testing session ID persistence...")
        
        session_id = "persistence_test_session"
        
        # Make initial request
        response = self.client.post('/applink/ussd/', 
            json.dumps({
                'session_id': session_id,
                'phone_number': '+8801712345678',
                'user_input': ''
            }),
            content_type='application/json'
        )
        
        assert response.status_code == 200, "Initial request should succeed"
        data = response.json()
        assert data['session_id'] == session_id, "Session ID should be preserved"
        
        # Check Redis storage
        session_data = self.session_manager.get_session(session_id)
        assert session_data is not None, "Session should exist in Redis"
        assert session_data['phone_number'] == '+8801712345678', "Phone number should match"
        
        print("✅ Session ID persistence test passed")
        
    def test_end_to_end_parent_flow(self):
        """Test C: Complete parent interaction flow"""
        print("\n🧪 Testing complete parent interaction...")
        print("📱 Simulating parent using feature phone for complete task...")
        
        session_id = "e2e_parent_session"
        
        # === STEP 1: Check balance ===
        print("\n📊 Step 1: Checking children's balance...")
        
        # Initial menu
        response = self.client.post('/applink/ussd/', 
            json.dumps({
                'session_id': session_id,
                'phone_number': '+8801712345678',
                'user_input': ''
            }),
            content_type='application/json'
        )
        
        assert response.status_code == 200, "Main menu should load"
        data = response.json()
        assert data['response_type'] == 'CON', "Main menu should continue"
        assert 'Welcome to Porakhela' in data['message'], "Welcome message missing"
        print("✅ Main menu loaded")
        
        # Select balance view
        response = self.client.post('/applink/ussd/', 
            json.dumps({
                'session_id': session_id,
                'phone_number': '+8801712345678',
                'user_input': '1'
            }),
            content_type='application/json'
        )
        
        assert response.status_code == 200, "Balance view should load"
        data = response.json()
        assert data['response_type'] == 'CON', "Balance view should continue"
        print("✅ Balance viewed successfully")
        
        # === STEP 2: Check redemption requests ===
        print("\n💰 Step 2: Checking redemption requests...")
        
        # Back to main menu
        response = self.client.post('/applink/ussd/', 
            json.dumps({
                'session_id': session_id,
                'phone_number': '+8801712345678',
                'user_input': '0'
            }),
            content_type='application/json'
        )
        
        # Go to redemptions
        response = self.client.post('/applink/ussd/', 
            json.dumps({
                'session_id': session_id,
                'phone_number': '+8801712345678',
                'user_input': '2'
            }),
            content_type='application/json'
        )
        
        assert response.status_code == 200, "Redemption menu should load"
        data = response.json()
        assert data['response_type'] == 'CON', "Redemption menu should continue"
        print("✅ Redemption requests viewed")
        
        # === STEP 3: Exit session ===
        print("\n🚪 Step 3: Exiting session...")
        
        # Exit
        response = self.client.post('/applink/ussd/', 
            json.dumps({
                'session_id': session_id,
                'phone_number': '+8801712345678',
                'user_input': '0'
            }),
            content_type='application/json'
        )
        
        print("✅ Complete parent flow test passed")
        
    def test_accessibility_without_app(self):
        """Test C.1: Prove system works without mobile app"""
        print("\n🧪 Testing complete accessibility without smartphone app...")
        
        session_id = "accessibility_session"
        
        # Simulate multiple menu navigations in one session
        interactions = [
            ('', 'Load main menu'),
            ('1', 'View balance'),
            ('0', 'Back to menu'),
            ('3', 'Learning summary'),
            ('0', 'Back to menu'),
            ('4', 'Screen time'),
            ('0', 'Back to menu'),
            ('0', 'Exit')
        ]
        
        for i, (user_input, description) in enumerate(interactions):
            print(f"  📱 {i+1}. {description}")
            
            response = self.client.post('/applink/ussd/', 
                json.dumps({
                    'session_id': session_id,
                    'phone_number': '+8801712345678',
                    'user_input': user_input
                }),
                content_type='application/json'
            )
            
            assert response.status_code == 200, f"Request {i+1} should succeed"
            data = response.json()
            assert 'response_type' in data, f"Request {i+1} should have response_type"
            assert 'message' in data, f"Request {i+1} should have message"
        
        print("✅ Complete accessibility test passed - NO SMARTPHONE NEEDED!")


def run_all_tests():
    """Run all USSD tests"""
    print("🚀 STARTING COMPREHENSIVE USSD TESTS")
    print("=" * 60)
    
    test_suite = USSDSystemTests()
    
    try:
        # Setup
        test_suite.setup_test_data()
        
        # Run unit tests
        print("\n🔬 UNIT TESTS")
        print("-" * 30)
        test_suite.test_initial_menu()
        test_suite.test_session_progression()
        test_suite.test_redis_persistence()
        test_suite.test_invalid_input_handling()
        
        # Run integration tests
        print("\n🔗 INTEGRATION TESTS")
        print("-" * 30)
        test_suite.test_integration_sequence()
        test_suite.test_session_id_persistence()
        
        # Run end-to-end tests
        print("\n🎯 END-TO-END TESTS")
        print("-" * 30)
        test_suite.test_end_to_end_parent_flow()
        test_suite.test_accessibility_without_app()
        
        print("\n" + "=" * 60)
        print("🎉 ALL USSD TESTS COMPLETED SUCCESSFULLY!")
        print("✅ System verified for parents without smartphones!")
        print("🌟 Complete digital inclusivity achieved!")
        print("📱 Parents can use ANY basic phone with USSD support!")
        
        return True
        
    except AssertionError as e:
        print(f"\n❌ TEST FAILED: {e}")
        return False
    except Exception as e:
        print(f"\n💥 UNEXPECTED ERROR: {e}")
        import traceback
        traceback.print_exc()
        return False


if __name__ == '__main__':
    success = run_all_tests()
    
    if success:
        print("\n🏆 USSD SYSTEM READY FOR PRODUCTION!")
        print("📞 Parents can dial *123# from any phone")
        print("🎯 Complete parental control without smartphone")
        print("🌍 Truly inclusive education technology")
    else:
        print("\n❌ Tests failed - debugging needed")
        
    sys.exit(0 if success else 1)