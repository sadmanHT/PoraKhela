#!/usr/bin/env python3
"""
API Testing Script for Parent Dashboard System

This script tests the parent dashboard endpoints and provides
comprehensive validation of the implemented functionality.
"""

import requests
import json
import sys
from datetime import datetime


def test_endpoint(url, method='GET', headers=None, data=None, description=""):
    """Test an API endpoint and return results"""
    print(f"\n🧪 Testing: {description}")
    print(f"📍 {method} {url}")
    
    try:
        if method == 'GET':
            response = requests.get(url, headers=headers, timeout=5)
        elif method == 'POST':
            response = requests.post(url, headers=headers, json=data, timeout=5)
        else:
            print(f"❌ Unsupported method: {method}")
            return False
        
        print(f"📊 Status Code: {response.status_code}")
        
        # Try to parse JSON response
        try:
            json_response = response.json()
            print(f"📄 Response: {json.dumps(json_response, indent=2)[:300]}...")
        except:
            print(f"📄 Response (text): {response.text[:300]}...")
        
        return response.status_code < 400
        
    except requests.exceptions.ConnectionError:
        print("❌ Connection Error: Django server not running or not accessible")
        return False
    except requests.exceptions.Timeout:
        print("❌ Timeout Error: Request took too long")
        return False
    except Exception as e:
        print(f"❌ Error: {str(e)}")
        return False


def main():
    """Main testing function"""
    print("🚀 PARENT DASHBOARD API TESTING SUITE")
    print("=" * 60)
    print(f"⏰ Test started at: {datetime.now()}")
    
    base_url = "http://localhost:8000"
    
    # Test cases
    test_cases = [
        {
            'url': f"{base_url}/api/parent/dashboard/",
            'method': 'GET',
            'description': "Parent Dashboard (Unauthenticated)"
        },
        {
            'url': f"{base_url}/api/parent/children/",
            'method': 'GET', 
            'description': "Children List (Unauthenticated)"
        },
        {
            'url': f"{base_url}/api/schema/swagger-ui/",
            'method': 'GET',
            'description': "API Documentation"
        },
        {
            'url': f"{base_url}/admin/",
            'method': 'GET',
            'description': "Django Admin Panel"
        }
    ]
    
    results = []
    
    for test_case in test_cases:
        success = test_endpoint(
            url=test_case['url'],
            method=test_case['method'],
            headers=test_case.get('headers'),
            data=test_case.get('data'),
            description=test_case['description']
        )
        results.append(success)
    
    # Summary
    print("\n" + "=" * 60)
    print("📋 TEST SUMMARY")
    print("=" * 60)
    
    passed = sum(results)
    total = len(results)
    
    for i, test_case in enumerate(test_cases):
        status = "✅ PASS" if results[i] else "❌ FAIL"
        print(f"{status} {test_case['description']}")
    
    print(f"\n📊 Overall Results: {passed}/{total} tests passed")
    
    if passed == total:
        print("🎉 All tests passed! The API is working correctly.")
    else:
        print("⚠️ Some tests failed. Check the server configuration.")
    
    # Additional information
    print("\n" + "=" * 60)
    print("📚 NEXT STEPS")
    print("=" * 60)
    print("1. 🔐 Create test user accounts to test authenticated endpoints")
    print("2. 📱 Test SMS functionality with: python manage.py test_daily_sms --all-parents")
    print("3. ⏰ Verify Celery beat scheduling for daily SMS at 8:00 PM")
    print("4. 🗄️ Test Redis caching performance")
    print("5. 📊 Create sample data for comprehensive testing")


if __name__ == "__main__":
    main()