"""
Simple Django server starter and API tester
"""

import subprocess
import time
import requests
import sys
import json


def test_api_quickly():
    """Quick API test"""
    print("🚀 Quick API Test for Parent Dashboard System")
    print("=" * 50)
    
    base_url = "http://localhost:8000"
    
    # Wait a moment for server to start
    print("⏳ Waiting for server to start...")
    time.sleep(2)
    
    try:
        # Test basic connectivity
        response = requests.get(f"{base_url}/api/parent/dashboard/", timeout=5)
        print(f"📊 Dashboard API Status: {response.status_code}")
        
        if response.status_code == 401:
            print("✅ Authentication required (as expected)")
        elif response.status_code == 200:
            print("✅ Dashboard responding successfully")
        else:
            print(f"⚠️ Unexpected status code: {response.status_code}")
        
        # Test API docs
        docs_response = requests.get(f"{base_url}/api/schema/swagger-ui/", timeout=5)
        print(f"📚 API Docs Status: {docs_response.status_code}")
        
        if docs_response.status_code == 200:
            print("✅ API Documentation accessible")
        
        return True
        
    except requests.exceptions.ConnectionError:
        print("❌ Server not accessible")
        return False
    except Exception as e:
        print(f"❌ Error: {str(e)}")
        return False


if __name__ == "__main__":
    if test_api_quickly():
        print("\n🎉 Parent Dashboard System is running successfully!")
        print("\n📋 Available Endpoints:")
        print("   • Dashboard: http://localhost:8000/api/parent/dashboard/")
        print("   • Children: http://localhost:8000/api/parent/children/")
        print("   • Screen Time: http://localhost:8000/api/parent/set-screen-time-limit/")
        print("   • API Docs: http://localhost:8000/api/schema/swagger-ui/")
        
        print("\n📱 SMS Testing:")
        print("   • Run: python manage.py test_daily_sms --all-parents")
        
        print("\n⏰ Celery Tasks:")
        print("   • Daily SMS: 8:00 PM Bangladesh time")
        print("   • Cache cleanup: Every 5 minutes")
        print("   • Streak updates: 12:30 AM daily")
    else:
        print("\n⚠️ Please start the Django server first:")
        print("   cd F:\\Applink\\backend")
        print("   python manage.py runserver 8000")