#!/usr/bin/env python3
"""
Debug child profile creation issue
"""

import requests
import json


def debug_child_creation():
    """Test child creation with detailed debugging."""
    base_url = "http://localhost:8000"
    phone = "01712345678"
    
    print("🐛 DEBUGGING CHILD PROFILE CREATION")
    print("=" * 60)
    
    # Step 1: Get fresh OTP and token
    print("\n📱 Step 1: Getting fresh OTP...")
    response = requests.post(
        f"{base_url}/api/v1/auth/request-otp/",
        json={"phone_number": phone},
        headers={'Content-Type': 'application/json'}
    )
    
    if response.status_code == 200:
        otp_data = response.json()
        otp_code = otp_data.get('debug_otp_code')
        print(f"✅ OTP: {otp_code}")
        
        # Step 2: Verify OTP and get token
        verify_response = requests.post(
            f"{base_url}/api/v1/auth/verify-otp/",
            json={"phone_number": phone, "otp": otp_code},
            headers={'Content-Type': 'application/json'}
        )
        
        if verify_response.status_code == 200:
            token_data = verify_response.json()
            access_token = token_data.get('token')
            print(f"✅ Token obtained")
            
            # Step 3: Test child creation with minimal data first
            print(f"\n👶 Step 3: Testing child creation...")
            child_data = {
                "name": "Test Child",
                "grade": 3
            }
            
            child_response = requests.post(
                f"{base_url}/api/v1/auth/parent/create-child/",
                json=child_data,
                headers={
                    'Content-Type': 'application/json',
                    'Authorization': f'Bearer {access_token}'
                }
            )
            
            print(f"Status Code: {child_response.status_code}")
            print(f"Response Headers: {dict(child_response.headers)}")
            
            try:
                response_json = child_response.json()
                print(f"Response JSON: {json.dumps(response_json, indent=2)}")
            except:
                print(f"Response Text: {child_response.text}")
                
        else:
            print(f"❌ OTP verification failed: {verify_response.status_code}")
            print(f"Response: {verify_response.text}")
    else:
        print(f"❌ OTP request failed: {response.status_code}")


if __name__ == "__main__":
    debug_child_creation()