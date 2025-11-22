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
import redis
from unittest.mock import patch, MagicMock

# Add the project root to Python path
sys.path.insert(0, os.path.dirname(__file__))

# Setup Django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'porakhela.settings')
django.setup()

from django.test import TestCase, Client
from django.urls import reverse
from apps.users.models import User, ChildProfile
from apps.gamification.models import PorapointLedger
from apps.lessons.models import LessonProgress
from apps.ussd.models import USSDSession, USSDLog, RedemptionRequest
from apps.ussd.session_manager import USSDSessionManager
from apps.ussd.menu_handler import USSDMenuHandler
from datetime import datetime, timedelta
import uuid


class USSDUnitTests(TestCase):
    """Unit Tests for Individual USSD Components"""
    
    def setUp(self):
        """Set up test data"""
        print("🔧 Setting up test data...")
        
        # Create parent user
        self.parent = User.objects.create_user(
            username='testparent',
            phone_number='+8801712345678',
            first_name='Test',
            last_name='Parent'
        )
        
        # Create child profiles
        self.child1 = ChildProfile.objects.create(
            user=self.parent,
            child_name='Rafi',
            age=10
        )
        
        self.child2 = ChildProfile.objects.create(
            user=self.parent,
            child_name='Sadia',
            age=12
        )
        
        # Create Porapoints
        PorapointLedger.objects.create(
            child=self.child1,
            points=150,
            transaction_type='earned',
            description='Completed lesson'
        )
        
        PorapointLedger.objects.create(
            child=self.child2,
            points=200,
            transaction_type='earned',
            description='Quiz completion'
        )
        
        # Create pending redemption
        self.redemption = RedemptionRequest.objects.create(
            child=self.child1,
            item_name='100MB Data Pack',
            points_required=50,
            item_type='Mobile Data',
            status='pending'
        )
        
        self.session_manager = USSDSessionManager()
        self.menu_handler = USSDMenuHandler()
        
        print("✅ Test data setup complete")
    
    def test_initial_menu_display(self):
        """Test A.1: Initial menu shows correct options"""
        print("\n🧪 Testing initial menu display...")
        
        session_id = "test_session_001"
        phone_number = "+8801712345678"
        
        response = self.menu_handler.handle_request(session_id, phone_number, "")
        
        # Check response structure
        self.assertEqual(response['response_type'], 'CON')
        self.assertIn('Welcome to Porakhela Parent Portal', response['message'])
        self.assertIn('1. View Porapoints Balance', response['message'])
        self.assertIn('2. Approve Redemption Requests', response['message'])
        self.assertIn('3. View Today\'s Learning Summary', response['message'])
        self.assertIn('4. Set Screen Time Limit', response['message'])
        self.assertIn('0. Exit', response['message'])
        
        print("✅ Initial menu test passed")
    
    def test_session_progression(self):
        """Test A.2: Session progresses correctly through menu states"""
        print("\n🧪 Testing session progression...")
        
        session_id = "test_session_002"
        phone_number = "+8801712345678"
        
        # Step 1: Initial menu
        response1 = self.menu_handler.handle_request(session_id, phone_number, "")
        self.assertEqual(response1['response_type'], 'CON')
        self.assertIn('Welcome to Porakhela', response1['message'])
        
        # Step 2: Select balance view
        response2 = self.menu_handler.handle_request(session_id, phone_number, "1")
        self.assertEqual(response2['response_type'], 'CON')
        self.assertIn('Porapoints', response2['message'])
        self.assertIn('Rafi: 150 pts', response2['message'])
        self.assertIn('Sadia: 200 pts', response2['message'])
        
        # Step 3: Back to main menu
        response3 = self.menu_handler.handle_request(session_id, phone_number, "0")
        self.assertEqual(response3['response_type'], 'CON')
        self.assertIn('Welcome to Porakhela', response3['message'])
        
        print("✅ Session progression test passed")
    
    def test_redis_state_persistence(self):
        """Test A.3: Redis correctly stores and retrieves session state"""
        print("\n🧪 Testing Redis state persistence...")
        
        session_id = "test_session_003"
        
        # Create session
        session_data = {
            'phone_number': '+8801712345678',
            'current_state': 'balance_view',
            'user_id': self.parent.id,
            'created_at': datetime.now().isoformat()
        }
        
        # Store in Redis
        self.session_manager.create_session(session_id, session_data)
        
        # Retrieve from Redis
        retrieved_data = self.session_manager.get_session(session_id)
        
        self.assertIsNotNone(retrieved_data)
        self.assertEqual(retrieved_data['phone_number'], '+8801712345678')
        self.assertEqual(retrieved_data['current_state'], 'balance_view')
        self.assertEqual(retrieved_data['user_id'], self.parent.id)
        
        # Update session
        updated_data = retrieved_data.copy()
        updated_data['current_state'] = 'redemption_list'
        self.session_manager.update_session(session_id, updated_data)
        
        # Verify update
        final_data = self.session_manager.get_session(session_id)
        self.assertEqual(final_data['current_state'], 'redemption_list')
        
        print("✅ Redis state persistence test passed")
    
    def test_invalid_input_handling(self):
        """Test A.4: System handles invalid inputs gracefully"""
        print("\n🧪 Testing invalid input handling...")
        
        session_id = "test_session_004"
        phone_number = "+8801712345678"
        
        # Test invalid menu option
        response1 = self.menu_handler.handle_request(session_id, phone_number, "9")
        self.assertEqual(response1['response_type'], 'CON')
        self.assertIn('Invalid option', response1['message'])
        
        # Test non-numeric input
        response2 = self.menu_handler.handle_request(session_id, phone_number, "abc")
        self.assertEqual(response2['response_type'], 'CON')
        self.assertIn('Invalid option', response2['message'])
        
        # Test empty string when input expected
        # First go to balance view
        self.menu_handler.handle_request(session_id, phone_number, "1")
        # Then send empty input
        response3 = self.menu_handler.handle_request(session_id, phone_number, "")
        self.assertEqual(response3['response_type'], 'CON')
        self.assertIn('Invalid option', response3['message'])
        
        print("✅ Invalid input handling test passed")
    
    def test_unregistered_parent_phone(self):
        """Test A.5: Handle phone numbers not in system"""
        print("\n🧪 Testing unregistered parent phone...")
        
        session_id = "test_session_005"
        phone_number = "+8801999999999"  # Not in database
        
        response = self.menu_handler.handle_request(session_id, phone_number, "")
        self.assertEqual(response['response_type'], 'END')
        self.assertIn('not registered', response['message'])
        
        print("✅ Unregistered parent phone test passed")


class USSDIntegrationTests(TestCase):
    """Integration Tests for Complete USSD Flow"""
    
    def setUp(self):
        """Set up test data"""
        print("🔧 Setting up integration test data...")
        
        # Create parent user
        self.parent = User.objects.create_user(
            username='integrationparent',
            phone_number='+8801712345678',
            first_name='Integration',
            last_name='Parent'
        )
        
        # Create children
        self.child1 = ChildProfile.objects.create(
            user=self.parent,
            child_name='TestChild1',
            age=8
        )
        
        self.child2 = ChildProfile.objects.create(
            user=self.parent,
            child_name='TestChild2',
            age=10
        )
        
        # Add points
        PorapointLedger.objects.create(
            child=self.child1,
            points=100,
            transaction_type='earned',
            description='Test points'
        )
        
        PorapointLedger.objects.create(
            child=self.child2,
            points=150,
            transaction_type='earned',
            description='Test points'
        )
        
        self.client = Client()
        
        print("✅ Integration test setup complete")
    
    def test_real_ussd_sequence(self):
        """Test B: Simulate real USSD sequence via HTTP"""
        print("\n🧪 Testing real USSD sequence...")
        
        session_id = "integration_session_001"
        
        # Step 1: POST with no input (initial menu)
        response1 = self.client.post('/applink/ussd/', {
            'session_id': session_id,
            'phone_number': '+8801712345678',
            'user_input': ''
        }, content_type='application/json')
        
        self.assertEqual(response1.status_code, 200)
        data1 = response1.json()
        self.assertEqual(data1['response_type'], 'CON')
        self.assertIn('Welcome to Porakhela', data1['message'])
        
        # Step 2: POST with "1" (view balance)
        response2 = self.client.post('/applink/ussd/', {
            'session_id': session_id,
            'phone_number': '+8801712345678',
            'user_input': '1'
        }, content_type='application/json')
        
        self.assertEqual(response2.status_code, 200)
        data2 = response2.json()
        self.assertEqual(data2['response_type'], 'CON')
        self.assertIn('TestChild1: 100 pts', data2['message'])
        self.assertIn('TestChild2: 150 pts', data2['message'])
        
        # Step 3: POST with "0" (back to main menu)
        response3 = self.client.post('/applink/ussd/', {
            'session_id': session_id,
            'phone_number': '+8801712345678',
            'user_input': '0'
        }, content_type='application/json')
        
        self.assertEqual(response3.status_code, 200)
        data3 = response3.json()
        self.assertEqual(data3['response_type'], 'CON')
        self.assertIn('Welcome to Porakhela', data3['message'])
        
        print("✅ Real USSD sequence test passed")
    
    def test_session_id_storage(self):
        """Test B.1: Session ID is properly stored and retrieved"""
        print("\n🧪 Testing session ID storage...")
        
        session_id = "storage_test_session"
        
        # Make initial request
        response = self.client.post('/applink/ussd/', {
            'session_id': session_id,
            'phone_number': '+8801712345678',
            'user_input': ''
        }, content_type='application/json')
        
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['session_id'], session_id)
        
        # Check if session exists in Redis
        session_manager = USSDSessionManager()
        session_data = session_manager.get_session(session_id)
        self.assertIsNotNone(session_data)
        self.assertEqual(session_data['phone_number'], '+8801712345678')
        
        print("✅ Session ID storage test passed")
    
    def test_menu_navigation_consistency(self):
        """Test B.2: Menu returns are consistent"""
        print("\n🧪 Testing menu navigation consistency...")
        
        session_id = "nav_test_session"
        
        # Navigate to balance, then back, then to redemption
        responses = []
        
        # Initial menu
        responses.append(self.client.post('/applink/ussd/', {
            'session_id': session_id,
            'phone_number': '+8801712345678',
            'user_input': ''
        }, content_type='application/json'))
        
        # Go to balance
        responses.append(self.client.post('/applink/ussd/', {
            'session_id': session_id,
            'phone_number': '+8801712345678',
            'user_input': '1'
        }, content_type='application/json'))
        
        # Back to main
        responses.append(self.client.post('/applink/ussd/', {
            'session_id': session_id,
            'phone_number': '+8801712345678',
            'user_input': '0'
        }, content_type='application/json'))
        
        # Go to redemptions
        responses.append(self.client.post('/applink/ussd/', {
            'session_id': session_id,
            'phone_number': '+8801712345678',
            'user_input': '2'
        }, content_type='application/json'))
        
        # Verify all responses are valid
        for i, response in enumerate(responses):
            self.assertEqual(response.status_code, 200, f"Response {i+1} failed")
            data = response.json()
            self.assertIn('response_type', data, f"Response {i+1} missing response_type")
            self.assertIn('message', data, f"Response {i+1} missing message")
        
        print("✅ Menu navigation consistency test passed")


class USSDEndToEndTests(TestCase):
    """End-to-End Tests for Complete Parent Interactions"""
    
    def setUp(self):
        """Set up complete test scenario"""
        print("🔧 Setting up E2E test scenario...")
        
        # Create realistic parent scenario
        self.parent = User.objects.create_user(
            username='realparent',
            phone_number='+8801712345678',
            first_name='Fatima',
            last_name='Ahmed'
        )
        
        # Create children with realistic data
        self.child1 = ChildProfile.objects.create(
            user=self.parent,
            child_name='Rafi',
            age=9
        )
        
        self.child2 = ChildProfile.objects.create(
            user=self.parent,
            child_name='Sadia',
            age=11
        )
        
        # Create realistic points and activities
        PorapointLedger.objects.create(
            child=self.child1,
            points=75,
            transaction_type='earned',
            description='Math lesson completed'
        )
        
        PorapointLedger.objects.create(
            child=self.child2,
            points=120,
            transaction_type='earned',
            description='Science quiz passed'
        )
        
        # Create pending redemption
        self.redemption = RedemptionRequest.objects.create(
            child=self.child1,
            item_name='Educational Game Access',
            points_required=50,
            item_type='Digital Content',
            status='pending'
        )
        
        self.client = Client()
        
        print("✅ E2E test scenario setup complete")
    
    def test_complete_parent_interaction(self):
        """Test C: Complete parent interaction without mobile app"""
        print("\n🧪 Testing complete parent interaction...")
        print("📱 Simulating parent Fatima using feature phone...")
        
        session_id = "e2e_session_fatima"
        
        # === STEP 1: Check children's balance ===
        print("\n📊 Step 1: Checking children's Porapoints balance...")
        
        # Initial menu
        response = self.client.post('/applink/ussd/', {
            'session_id': session_id,
            'phone_number': '+8801712345678',
            'user_input': ''
        }, content_type='application/json')
        
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['response_type'], 'CON')
        self.assertIn('Welcome to Porakhela', data['message'])
        print("✅ Main menu displayed")
        
        # Select balance view
        response = self.client.post('/applink/ussd/', {
            'session_id': session_id,
            'phone_number': '+8801712345678',
            'user_input': '1'
        }, content_type='application/json')
        
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['response_type'], 'CON')
        self.assertIn('Rafi: 75 pts', data['message'])
        self.assertIn('Sadia: 120 pts', data['message'])
        print("✅ Balance checked - Rafi: 75 pts, Sadia: 120 pts")
        
        # === STEP 2: Approve redemption request ===
        print("\n✅ Step 2: Approving redemption request...")
        
        # Back to main menu
        response = self.client.post('/applink/ussd/', {
            'session_id': session_id,
            'phone_number': '+8801712345678',
            'user_input': '0'
        }, content_type='application/json')
        
        # Go to redemptions
        response = self.client.post('/applink/ussd/', {
            'session_id': session_id,
            'phone_number': '+8801712345678',
            'user_input': '2'
        }, content_type='application/json')
        
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['response_type'], 'CON')
        self.assertIn('Educational Game Access', data['message'])
        self.assertIn('50 pts', data['message'])
        print("✅ Redemption request found")
        
        # Select first redemption
        response = self.client.post('/applink/ussd/', {
            'session_id': session_id,
            'phone_number': '+8801712345678',
            'user_input': '1'
        }, content_type='application/json')
        
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['response_type'], 'CON')
        self.assertIn('Redemption Details', data['message'])
        print("✅ Redemption details shown")
        
        # Approve redemption
        response = self.client.post('/applink/ussd/', {
            'session_id': session_id,
            'phone_number': '+8801712345678',
            'user_input': '1'
        }, content_type='application/json')
        
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['response_type'], 'END')
        self.assertIn('approved', data['message'])
        print("✅ Redemption approved successfully")
        
        # === STEP 3: View learning summary (new session) ===
        print("\n📚 Step 3: Viewing learning summary...")
        
        new_session_id = "e2e_session_fatima_2"
        
        # New session - main menu
        response = self.client.post('/applink/ussd/', {
            'session_id': new_session_id,
            'phone_number': '+8801712345678',
            'user_input': ''
        }, content_type='application/json')
        
        # Select learning summary
        response = self.client.post('/applink/ussd/', {
            'session_id': new_session_id,
            'phone_number': '+8801712345678',
            'user_input': '3'
        }, content_type='application/json')
        
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['response_type'], 'CON')
        self.assertIn('Learning Summary', data['message'])
        print("✅ Learning summary accessed")
        
        print("\n🎉 COMPLETE PARENT INTERACTION TEST PASSED!")
        print("🏆 Fatima successfully used USSD without smartphone!")
        print("📱 Proved complete inclusivity for all parents!")
        
    def test_accessibility_without_mobile_app(self):
        """Test C.1: Verify system works completely without mobile app"""
        print("\n🧪 Testing accessibility without mobile app...")
        
        # This test proves the system is inclusive
        session_id = "accessibility_test"
        
        # Simulate multiple interactions in one session
        interactions = [
            ('', 'Initial menu'),
            ('1', 'Balance check'),
            ('0', 'Back to menu'),
            ('4', 'Screen time'),
            ('1', 'Select first child'),
            ('120', 'Set 120 minutes'),
            ('0', 'Confirm and exit')
        ]
        
        for i, (user_input, description) in enumerate(interactions):
            print(f"  📱 {i+1}. {description}")
            
            response = self.client.post('/applink/ussd/', {
                'session_id': session_id,
                'phone_number': '+8801712345678',
                'user_input': user_input
            }, content_type='application/json')
            
            self.assertEqual(response.status_code, 200)
            data = response.json()
            self.assertIn('response_type', data)
            self.assertIn('message', data)
            
            # Final interaction should end session
            if i == len(interactions) - 1:
                self.assertEqual(data['response_type'], 'END')
            else:
                self.assertEqual(data['response_type'], 'CON')
        
        print("✅ Complete accessibility test passed - no mobile app needed!")


def run_comprehensive_tests():
    """Run all USSD tests"""
    print("🚀 STARTING COMPREHENSIVE USSD TESTS")
    print("=" * 50)
    
    import unittest
    
    # Create test suite
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    # Add test classes
    suite.addTests(loader.loadTestsFromTestCase(USSDUnitTests))
    suite.addTests(loader.loadTestsFromTestCase(USSDIntegrationTests))
    suite.addTests(loader.loadTestsFromTestCase(USSDEndToEndTests))
    
    # Run tests
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    
    print("\n" + "=" * 50)
    if result.wasSuccessful():
        print("🎉 ALL USSD TESTS PASSED!")
        print("✅ System ready for parents without smartphones!")
        print("🌟 Complete inclusivity achieved!")
    else:
        print("❌ Some tests failed!")
        print(f"Failures: {len(result.failures)}")
        print(f"Errors: {len(result.errors)}")
        
        for test, error in result.failures + result.errors:
            print(f"\n❌ {test}: {error}")
    
    return result.wasSuccessful()


if __name__ == '__main__':
    success = run_comprehensive_tests()
    sys.exit(0 if success else 1)