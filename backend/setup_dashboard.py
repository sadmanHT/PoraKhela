"""
Development Setup Script for Parent Dashboard + Daily SMS System

This script helps set up and run the complete parent dashboard system including:
1. Django server
2. Celery worker for background tasks
3. Celery beat scheduler for daily SMS at 8:00 PM
4. Redis cache server

Prerequisites:
- Redis server running on localhost:6379
- PostgreSQL database configured
- All dependencies installed (pip install -r requirements.txt)
"""

import subprocess
import sys
import time
import os
from concurrent.futures import ThreadPoolExecutor


def run_command(command, name, wait_time=2):
    """Run a command and handle output"""
    print(f"🚀 Starting {name}...")
    try:
        if sys.platform == "win32":
            # Windows PowerShell
            process = subprocess.Popen(
                ["powershell", "-Command", command],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                creationflags=subprocess.CREATE_NEW_CONSOLE
            )
        else:
            # Unix/Linux/Mac
            process = subprocess.Popen(
                command.split(),
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True
            )
        
        time.sleep(wait_time)
        
        if process.poll() is None:
            print(f"✅ {name} started successfully (PID: {process.pid})")
            return process
        else:
            stdout, stderr = process.communicate()
            print(f"❌ {name} failed to start:")
            print(f"STDOUT: {stdout}")
            print(f"STDERR: {stderr}")
            return None
    
    except Exception as e:
        print(f"❌ Error starting {name}: {str(e)}")
        return None


def main():
    """Main setup function"""
    print("🏗️  Setting up Parent Dashboard + Daily SMS System")
    print("=" * 60)
    
    # Check if we're in the correct directory
    if not os.path.exists("manage.py"):
        print("❌ Please run this script from the backend directory (where manage.py is located)")
        sys.exit(1)
    
    # Database migrations
    print("📊 Running database migrations...")
    migration_result = subprocess.run([
        "python", "manage.py", "migrate"
    ], capture_output=True, text=True)
    
    if migration_result.returncode == 0:
        print("✅ Database migrations completed")
    else:
        print("❌ Migration failed:")
        print(migration_result.stderr)
        sys.exit(1)
    
    # Create Celery beat tables
    print("⏰ Setting up Celery beat scheduler...")
    beat_migration = subprocess.run([
        "python", "manage.py", "migrate", "django_celery_beat"
    ], capture_output=True, text=True)
    
    if beat_migration.returncode == 0:
        print("✅ Celery beat scheduler tables created")
    else:
        print("⚠️  Celery beat migration warning (might already exist):")
        print(beat_migration.stderr)
    
    # Commands to run
    commands = [
        {
            "cmd": "python manage.py runserver 8000",
            "name": "Django Server (Port 8000)",
            "wait": 3
        },
        {
            "cmd": "celery -A porakhela worker --loglevel=info --pool=solo",
            "name": "Celery Worker",
            "wait": 3
        },
        {
            "cmd": "celery -A porakhela beat --loglevel=info --scheduler django_celery_beat.schedulers:DatabaseScheduler",
            "name": "Celery Beat Scheduler",
            "wait": 3
        }
    ]
    
    processes = []
    
    print("\n🚀 Starting all services...")
    print("=" * 60)
    
    for cmd_info in commands:
        process = run_command(cmd_info["cmd"], cmd_info["name"], cmd_info["wait"])
        if process:
            processes.append((process, cmd_info["name"]))
        time.sleep(2)  # Stagger startup
    
    if not processes:
        print("❌ No services started successfully")
        sys.exit(1)
    
    print("\n" + "=" * 60)
    print("🎉 SETUP COMPLETE!")
    print("=" * 60)
    print("\n📋 RUNNING SERVICES:")
    for process, name in processes:
        print(f"   ✅ {name} (PID: {process.pid})")
    
    print("\n🔗 USEFUL LINKS:")
    print("   🌐 Django Server: http://localhost:8000")
    print("   📚 API Docs: http://localhost:8000/api/schema/swagger-ui/")
    print("   📊 Parent Dashboard: http://localhost:8000/api/parent/dashboard/")
    
    print("\n📱 TESTING SMS:")
    print("   🧪 Test SMS Command: python manage.py test_daily_sms --all-parents")
    print("   ⏰ Daily SMS Schedule: 8:00 PM Bangladesh Time (UTC+6)")
    
    print("\n📋 CELERY BEAT TASKS:")
    print("   📨 Daily SMS Reports: 8:00 PM daily")
    print("   🧹 Cache Cleanup: Every 5 minutes")
    print("   🔥 Streak Updates: 12:30 AM daily")
    
    print("\n⚠️  IMPORTANT NOTES:")
    print("   • Make sure Redis server is running on localhost:6379")
    print("   • PostgreSQL database should be properly configured")
    print("   • Check logs if any service fails to start")
    print("   • Press Ctrl+C to stop all services")
    
    try:
        print("\n⌛ Running... Press Ctrl+C to stop all services")
        while True:
            time.sleep(10)
            # Check if processes are still running
            running_count = sum(1 for p, _ in processes if p.poll() is None)
            if running_count == 0:
                print("⚠️  All processes have stopped")
                break
    
    except KeyboardInterrupt:
        print("\n🛑 Stopping all services...")
        for process, name in processes:
            if process.poll() is None:
                print(f"   🛑 Stopping {name}...")
                process.terminate()
        
        time.sleep(2)
        
        # Force kill if needed
        for process, name in processes:
            if process.poll() is None:
                print(f"   💀 Force killing {name}...")
                process.kill()
        
        print("✅ All services stopped")


if __name__ == "__main__":
    main()