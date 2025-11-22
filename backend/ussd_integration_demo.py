"""
USSD Integration Demo - Telecom Provider Simulation

This script simulates how a telecom provider (Grameenphone, Banglalink, etc.) 
would send USSD requests to the Porakhela USSD Gateway.
"""

import json
import requests
import time
from datetime import datetime

# USSD Gateway Configuration
USSD_GATEWAY_URL = 'http://localhost:8000/applink/ussd/'

class USSDSimulator:
    """Simulates USSD interactions from a telecom provider perspective."""
    
    def __init__(self):
        self.session_id = f"tel_session_{datetime.now().timestamp()}"
        self.phone_number = "+8801712345678"  # Test parent phone
    
    def send_ussd_request(self, user_input, description=""):
        """Send USSD request to Porakhela gateway."""
        payload = {
            "session_id": self.session_id,
            "phone_number": self.phone_number,
            "user_input": user_input
        }
        
        print(f"\n📡 Telecom → Porakhela: {description}")
        print(f"   Session: {self.session_id}")
        print(f"   Phone: {self.phone_number}")
        print(f"   Input: '{user_input}'")
        
        try:
            response = requests.post(USSD_GATEWAY_URL, json=payload, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                response_type = data.get('response_type')
                message = data.get('message')
                
                print(f"\n📱 Porakhela → Telecom ({response_type}):")
                print("   " + message.replace('\n', '\n   '))
                
                return response_type, message
            else:
                print(f"   ❌ Error: HTTP {response.status_code}")
                return 'END', 'Service unavailable'
                
        except requests.exceptions.RequestException as e:
            print(f"   ❌ Connection Error: {str(e)}")
            return 'END', 'Service unavailable'
    
    def simulate_full_flow(self):
        """Simulate a complete USSD interaction."""
        print("🎭 USSD Integration Demo - Telecom Provider Simulation")
        print("=" * 60)
        print(f"📞 Parent dials *123# from phone: {self.phone_number}")
        
        # Step 1: Initial USSD connection
        response_type, message = self.send_ussd_request('', 'Initial USSD connection')
        if response_type != 'CON':
            return
        
        time.sleep(1)
        
        # Step 2: Parent selects option 1 (Balance)
        print(f"\n👤 Parent presses: 1")
        response_type, message = self.send_ussd_request('1', 'View Porapoints Balance')
        if response_type != 'CON':
            return
            
        time.sleep(1)
        
        # Step 3: Parent goes back to main menu
        print(f"\n👤 Parent presses: 0")
        response_type, message = self.send_ussd_request('0', 'Back to main menu')
        if response_type != 'CON':
            return
            
        time.sleep(1)
        
        # Step 4: Parent checks redemption requests
        print(f"\n👤 Parent presses: 2")
        response_type, message = self.send_ussd_request('2', 'Check redemption requests')
        if response_type != 'CON':
            return
        
        # Check if there are pending redemptions
        if 'Pending Redemptions' in message and '1.' in message:
            time.sleep(1)
            
            # Step 5: Select first redemption
            print(f"\n👤 Parent presses: 1")
            response_type, message = self.send_ussd_request('1', 'Select first redemption')
            if response_type != 'CON':
                return
                
            time.sleep(1)
            
            # Step 6: Approve the redemption
            print(f"\n👤 Parent presses: 1")
            response_type, message = self.send_ussd_request('1', 'Approve redemption')
            
            if response_type == 'END':
                print(f"\n✅ Session ended successfully")
        else:
            print(f"\n📋 No pending redemptions to process")
        
        print("\n" + "=" * 60)
        print("🎯 USSD Integration Demo Complete!")
        
        # Show integration summary
        self.show_integration_guide()
    
    def show_integration_guide(self):
        """Show integration guide for telecom providers."""
        print("\n📋 TELECOM PROVIDER INTEGRATION GUIDE")
        print("-" * 40)
        
        print("\n🔗 API Endpoint:")
        print(f"   POST {USSD_GATEWAY_URL}")
        
        print("\n📡 Request Format:")
        print("   {")
        print('     "session_id": "unique_session_from_telecom",')
        print('     "phone_number": "017XXXXXXXX",')
        print('     "user_input": "1"')
        print("   }")
        
        print("\n📱 Response Format:")
        print("   {")
        print('     "session_id": "unique_session_from_telecom",')
        print('     "response_type": "CON" | "END",')
        print('     "message": "Menu text or final message"')
        print("   }")
        
        print("\n🔄 Response Types:")
        print("   • CON: Continue session - display message and wait for user input")
        print("   • END: End session - display final message and close")
        
        print("\n⚡ Integration Steps:")
        print("   1. Telecom receives *123# from customer")
        print("   2. Send POST request with session_id, phone_number, user_input=''")
        print("   3. Display returned message to customer")
        print("   4. When customer enters input, send new request with same session_id")
        print("   5. Continue until response_type='END'")
        
        print("\n🛡️ Security Notes:")
        print("   • Use HTTPS in production")
        print("   • Whitelist telecom provider IP addresses")
        print("   • Implement rate limiting")
        print("   • Add authentication tokens if required")
        
        print("\n📞 Supported Features:")
        print("   • Porapoints Balance View")
        print("   • Redemption Request Approval/Rejection") 
        print("   • Daily Learning Summary")
        print("   • Screen Time Management")
        print("   • Multi-child family support")
        
        print(f"\n🧪 Test with curl:")
        print(f'   curl -X POST {USSD_GATEWAY_URL} \\')
        print(f'     -H "Content-Type: application/json" \\')
        print(f'     -d \'{{"session_id":"test123", "phone_number":"+8801712345678", "user_input":""}}\'')


def main():
    """Run the USSD integration demo."""
    # Check if Django server is running
    try:
        response = requests.get(USSD_GATEWAY_URL, timeout=5)
        print("✅ Django server detected - running full demo")
        
        simulator = USSDSimulator()
        simulator.simulate_full_flow()
        
    except requests.exceptions.RequestException:
        print("⚠️  Django server not running!")
        print("\n🚀 To run the demo:")
        print("   1. cd f:\\Applink\\backend")
        print("   2. python manage.py runserver")
        print("   3. python ussd_integration_demo.py")
        
        # Still show integration guide
        print("\n📋 TELECOM PROVIDER INTEGRATION GUIDE")
        print("-" * 40)
        simulator = USSDSimulator()
        simulator.show_integration_guide()


if __name__ == '__main__':
    main()