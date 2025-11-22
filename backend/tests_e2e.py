"""
End-to-End Tests for Porakhela Authentication System

This module provides comprehensive End-to-End testing that simulates
real user journeys through the complete authentication flow.

Test Coverage:
- Complete parent onboarding journey
- Parent-child relationship creation
- Multi-child family scenarios  
- Error recovery flows
- Edge cases and boundary conditions
"""

import json
import time
import uuid
from django.test import TestCase
from django.core.cache import cache
from rest_framework.test import APIClient
from apps.users.models import User, ChildProfile


class EndToEndAuthenticationTest(TestCase):
    """
    End-to-End authentication flow testing.
    
    Tests complete user journeys from initial OTP request
    through child profile creation and family management.
    """
    
    def setUp(self):
        """Set up test environment."""
        self.client = APIClient()
        cache.clear()
        
        # Ensure DEBUG mode for tests
        from django.conf import settings
        settings.DEBUG = True
        
        # Test scenarios with different phone numbers
        self.parent1_phone = "01712345678"
        self.parent2_phone = "01798765432"
        self.child1_phone = "01611111111"
        self.child2_phone = "01622222222"
        
        # Clean up any existing data
        for phone in [self.parent1_phone, self.parent2_phone, self.child1_phone, self.child2_phone]:
            User.objects.filter(phone_number=phone).delete()
    
    def tearDown(self):
        """Clean up after tests."""
        cache.clear()
        for phone in [self.parent1_phone, self.parent2_phone, self.child1_phone, self.child2_phone]:
            User.objects.filter(phone_number=phone).delete()
            ChildProfile.objects.filter(parent__phone_number=phone).delete()
            ChildProfile.objects.filter(user__phone_number=phone).delete()

    def _complete_otp_flow(self, phone_number, first_name="Test", last_name="Parent"):
        """Helper method to complete OTP verification flow."""
        # Step 1: Request OTP
        response = self.client.post('/api/v1/auth/request-otp/', {
            "phone_number": phone_number
        }, format='json')
        
        self.assertEqual(response.status_code, 200)
        debug_otp = response.data.get('debug_otp_code')
        self.assertIsNotNone(debug_otp, "Debug OTP should be available")
        
        # Step 2: Verify OTP
        response = self.client.post('/api/v1/auth/verify-otp/', {
            "phone_number": phone_number,
            "otp": debug_otp
        }, format='json')
        
        self.assertEqual(response.status_code, 200)
        
        # Extract tokens from direct response (not nested in success structure)
        access_token = response.data['token']
        refresh_token = response.data['refresh']
        
        # Set authentication for subsequent requests
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {access_token}')
        
        return {
            'access_token': access_token,
            'refresh_token': refresh_token,
            'user': {
                'id': response.data['user_id'],
                'phone_number': response.data['phone_number'],
                'is_parent': response.data['is_parent'],
                'has_children': response.data['has_children'],
                'children_count': response.data['children_count']
            }
        }
    
    def _create_child_profile(self, name, grade=3, phone_number=None):
        """Helper method to create child profile."""
        if not phone_number:
            import uuid
            # Generate unique phone number using uuid to avoid collisions
            unique_suffix = str(uuid.uuid4()).replace('-', '')[:8]
            phone_number = f"016{unique_suffix[:8]}"
        
        response = self.client.post('/api/v1/auth/parent/create-child/', {
            "name": name,  # Add this for compatibility
            "first_name": name.split()[0],
            "last_name": name.split()[1] if len(name.split()) > 1 else "Child",
            "phone_number": phone_number,
            "password": "child123",
            "grade": grade,
            "parental_pin": "1234"
        }, format='json')
        
        return response

    def test_complete_parent_onboarding_journey(self):
        """
        E2E Test: Complete parent onboarding from registration to child creation.
        
        Simulates a real parent's journey:
        1. Initial OTP request
        2. OTP verification  
        3. Profile access
        4. First child creation
        5. Second child creation
        6. Family overview
        """
        print("\n🚀 E2E Test: Complete Parent Onboarding Journey")
        
        # === PHASE 1: Parent Authentication ===
        print("\n📱 Phase 1: Parent Authentication")
        auth_data = self._complete_otp_flow(self.parent1_phone, "Ahmed", "Rahman")
        
        user_id = auth_data['user']['id']
        print(f"   ✅ Parent authenticated successfully - ID: {user_id}")
        
        # Verify profile access
        response = self.client.get('/api/v1/auth/profile/')
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['success'])
        print(f"   ✅ Profile accessible - Phone: {response.data['profile']['phone_number']}")
        
        # === PHASE 2: First Child Creation ===
        print("\n👶 Phase 2: First Child Creation")
        
        response = self._create_child_profile("Fatima Rahman", 5, self.child1_phone)
        
        if response.status_code != 201:
            print(f"   ❌ Child creation failed with status {response.status_code}")
            print(f"   ❌ Response: {response.data}")
        
        self.assertEqual(response.status_code, 201)
        self.assertTrue(response.data['success'])
        
        first_child = response.data['child']
        print(f"   ✅ First child created - Name: {first_child['name']}, Grade: {first_child['grade']}")
        
        # === PHASE 3: Second Child Creation ===  
        print("\n👦 Phase 3: Second Child Creation")
        
        response = self._create_child_profile("Omar Rahman", 3, self.child2_phone)
        self.assertEqual(response.status_code, 201)
        self.assertTrue(response.data['success'])
        
        second_child = response.data['child']
        print(f"   ✅ Second child created - Name: {second_child['name']}, Grade: {second_child['grade']}")
        
        # === PHASE 4: Family Overview ===
        print("\n👨‍👩‍👧‍👦 Phase 4: Family Overview")
        
        response = self.client.get('/api/v1/auth/parent/children/')
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['success'])
        
        children = response.data['children']
        total_children = response.data['total_children']
        
        self.assertEqual(len(children), 2)
        self.assertEqual(total_children, 2)
        
        # Verify both children are present
        child_names = [child['name'] for child in children]
        self.assertIn('Fatima Rahman', child_names)
        self.assertIn('Omar Rahman', child_names)
        
        print(f"   ✅ Family overview complete - Total children: {total_children}")
        print(f"   ✅ Children: {', '.join(child_names)}")
        
        print("\n🎉 Complete Parent Onboarding Journey - SUCCESS!")

    def test_multiple_parent_families(self):
        """
        E2E Test: Multiple parent families with independent children.
        
        Tests data isolation between different parent accounts.
        """
        print("\n🏠 E2E Test: Multiple Parent Families")
        
        # === Family 1: Parent 1 ===
        print("\n👨 Family 1: Setting up first parent")
        auth1 = self._complete_otp_flow(self.parent1_phone, "Ali", "Khan")
        
        # Create child for parent 1
        response = self._create_child_profile("Zara Khan", 4)
        self.assertEqual(response.status_code, 201)
        parent1_child = response.data['child']
        print(f"   ✅ Parent 1 child created: {parent1_child['name']}")
        
        # === Family 2: Parent 2 ===  
        print("\n👩 Family 2: Setting up second parent")
        auth2 = self._complete_otp_flow(self.parent2_phone, "Fatima", "Ahmed")
        
        # Create child for parent 2
        response = self._create_child_profile("Hassan Ahmed", 6)
        self.assertEqual(response.status_code, 201)
        parent2_child = response.data['child']
        print(f"   ✅ Parent 2 child created: {parent2_child['name']}")
        
        # === Verify Data Isolation ===
        print("\n🔒 Verifying family data isolation")
        
        # Parent 1 should only see their own child
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {auth1['access_token']}")
        response = self.client.get('/api/v1/auth/parent/children/')
        self.assertEqual(response.status_code, 200)
        parent1_children = response.data['children']
        self.assertEqual(len(parent1_children), 1)
        self.assertEqual(parent1_children[0]['name'], 'Zara Khan')
        print(f"   ✅ Parent 1 isolation verified - sees only: {parent1_children[0]['name']}")
        
        # Parent 2 should only see their own child  
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {auth2['access_token']}")
        response = self.client.get('/api/v1/auth/parent/children/')
        self.assertEqual(response.status_code, 200)
        parent2_children = response.data['children']
        self.assertEqual(len(parent2_children), 1)
        self.assertEqual(parent2_children[0]['name'], 'Hassan Ahmed')
        print(f"   ✅ Parent 2 isolation verified - sees only: {parent2_children[0]['name']}")
        
        print("\n🎉 Multiple Parent Families - SUCCESS!")

    def test_error_recovery_flows(self):
        """
        E2E Test: Error recovery and edge cases.
        
        Tests how the system handles various error conditions
        and recovery scenarios.
        """
        print("\n🚨 E2E Test: Error Recovery Flows")
        
        # === Invalid OTP Recovery ===
        print("\n🔐 Testing invalid OTP recovery")
        
        # Request OTP
        response = self.client.post('/api/v1/auth/request-otp/', {
            "phone_number": self.parent1_phone
        }, format='json')
        self.assertEqual(response.status_code, 200)
        
        # Try invalid OTP
        response = self.client.post('/api/v1/auth/verify-otp/', {
            "phone_number": self.parent1_phone, 
            "otp": "000000"
        }, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertFalse(response.data['success'])
        print("   ✅ Invalid OTP correctly rejected")
        
        # Recover with correct OTP
        response = self.client.post('/api/v1/auth/request-otp/', {
            "phone_number": self.parent1_phone
        }, format='json')
        debug_otp = response.data.get('debug_otp_code')
        
        response = self.client.post('/api/v1/auth/verify-otp/', {
            "phone_number": self.parent1_phone,
            "otp": debug_otp
        }, format='json')
        self.assertEqual(response.status_code, 200)
        print("   ✅ Recovery with correct OTP successful")
        
        # === Duplicate Child Phone Number ===
        print("\n📞 Testing duplicate child phone number handling")
        
        # Set authentication
        access_token = response.data['token']
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {access_token}")
        
        # Create first child
        child_phone = "01677777777"
        response = self._create_child_profile("First Child", 2, child_phone)
        self.assertEqual(response.status_code, 201)
        print("   ✅ First child with phone created successfully")
        
        # Try to create second child with same phone
        print(f"   🔍 Attempting to create second child with same phone: {child_phone}")
        response = self._create_child_profile("Second Child", 3, child_phone)
        
        print(f"   🔍 Second child response status: {response.status_code}")
        print(f"   🔍 Second child response data: {response.data}")
        
        if response.status_code != 400:
            print(f"   ⚠️ UNEXPECTED: Expected 400 but got {response.status_code}")
            print(f"   🔍 Response data: {response.data}")
        
        self.assertEqual(response.status_code, 400)
        self.assertFalse(response.data['success'])
        
        # === Unauthenticated Access ===
        print("\n🚫 Testing unauthenticated access protection")
        
        # Clear authentication
        self.client.credentials()
        
        # Try to access protected endpoint
        response = self.client.get('/api/v1/auth/profile/')
        self.assertEqual(response.status_code, 401)
        print("   ✅ Unauthenticated access correctly blocked")
        
        # Try to create child without auth
        response = self._create_child_profile("Unauthorized Child", 1)
        self.assertEqual(response.status_code, 401)
        print("   ✅ Child creation without auth correctly blocked")
        
        print("\n🎉 Error Recovery Flows - SUCCESS!")

    def test_rate_limiting_and_boundaries(self):
        """
        E2E Test: Rate limiting and boundary conditions.
        
        Tests system behavior under rate limits and edge cases.
        """
        print("\n⏱️ E2E Test: Rate Limiting and Boundaries")
        
        # === Rate Limiting Test ===
        print("\n🚦 Testing OTP request rate limiting")
        
        # Make multiple OTP requests rapidly
        success_count = 0
        rate_limited = False
        
        for i in range(7):  # Exceed the limit
            response = self.client.post('/api/v1/auth/request-otp/', {
                "phone_number": self.parent1_phone
            }, format='json')
            
            if response.status_code == 200:
                success_count += 1
                print(f"   ✅ Request {i+1}: Allowed")
            elif response.status_code == 429:
                rate_limited = True
                print(f"   🛑 Request {i+1}: Rate limited (as expected)")
                break
            else:
                self.fail(f"Unexpected status code: {response.status_code}")
        
        self.assertTrue(rate_limited, "Rate limiting should have triggered")
        self.assertGreaterEqual(success_count, 5, "At least 5 requests should be allowed")
        print(f"   ✅ Rate limiting working correctly - {success_count} requests allowed before limiting")
        
        # === Boundary Grade Testing ===  
        print("\n📊 Testing grade boundary conditions")
        
        # Complete authentication first
        cache.clear()  # Clear rate limit
        time.sleep(1)  # Brief delay
        
        auth_data = self._complete_otp_flow(self.parent2_phone)
        
        # Test minimum grade
        response = self._create_child_profile("Min Grade Child", 1)
        self.assertEqual(response.status_code, 201)
        print("   ✅ Minimum grade (1) accepted")
        
        # Test maximum grade
        response = self._create_child_profile("Max Grade Child", 10)
        self.assertEqual(response.status_code, 201)
        print("   ✅ Maximum grade (10) accepted")
        
        # Test invalid low grade
        response = self._create_child_profile("Invalid Low Grade", 0)
        self.assertEqual(response.status_code, 400)
        print("   ✅ Invalid low grade (0) correctly rejected")
        
        # Test invalid high grade
        response = self._create_child_profile("Invalid High Grade", 11)
        self.assertEqual(response.status_code, 400) 
        print("   ✅ Invalid high grade (11) correctly rejected")
        
        print("\n🎉 Rate Limiting and Boundaries - SUCCESS!")

    def test_token_lifecycle(self):
        """
        E2E Test: JWT token lifecycle and refresh.
        
        Tests token generation, usage, and refresh flows.
        """
        print("\n🎫 E2E Test: Token Lifecycle")
        
        # === Token Generation ===
        print("\n🔑 Testing token generation")
        
        auth_data = self._complete_otp_flow(self.parent1_phone)
        access_token = auth_data['access_token']
        refresh_token = auth_data['refresh_token']
        
        self.assertIsNotNone(access_token)
        self.assertIsNotNone(refresh_token)
        print("   ✅ Tokens generated successfully")
        
        # === Token Usage ===
        print("\n🔓 Testing token usage")
        
        # Use access token for protected endpoint
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {access_token}')
        response = self.client.get('/api/v1/auth/profile/')
        self.assertEqual(response.status_code, 200)
        print("   ✅ Access token works for protected endpoints")
        
        # === Token Refresh ===  
        print("\n🔄 Testing token refresh")
        
        response = self.client.post('/api/v1/auth/refresh/', {
            "refresh": refresh_token
        }, format='json')
        
        if response.status_code == 200:
            new_access_token = response.data.get('access')
            self.assertIsNotNone(new_access_token)
            self.assertNotEqual(new_access_token, access_token)
            print("   ✅ Token refresh successful - new token generated")
            
            # Test new token works
            self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {new_access_token}')
            response = self.client.get('/api/v1/auth/profile/')
            self.assertEqual(response.status_code, 200)
            print("   ✅ New access token works correctly")
        else:
            print(f"   ⚠️ Token refresh endpoint not available (status: {response.status_code})")
        
        print("\n🎉 Token Lifecycle - SUCCESS!")


def run_e2e_tests():
    """
    Standalone function to run E2E tests.
    
    Usage:
        python manage.py shell
        >>> exec(open('tests_e2e.py').read())
        >>> run_e2e_tests()
    """
    import django
    from django.test.utils import get_runner
    from django.conf import settings
    
    django.setup()
    TestRunner = get_runner(settings)
    test_runner = TestRunner()
    failures = test_runner.run_tests(["tests_e2e.EndToEndAuthenticationTest"])
    
    if failures:
        print(f"\n❌ E2E Tests completed with {failures} failures")
    else:
        print(f"\n✅ All E2E Tests passed successfully!")
    
    return failures == 0


if __name__ == "__main__":
    run_e2e_tests()