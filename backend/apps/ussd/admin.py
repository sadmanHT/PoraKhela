from django.contrib import admin
from .models import USSDSession, USSDLog, RedemptionRequest, ScreenTimeSetting

@admin.register(USSDSession)
class USSDSessionAdmin(admin.ModelAdmin):
    list_display = ['session_id', 'phone_number', 'parent', 'current_state', 'is_active', 'updated_at']
    list_filter = ['current_state', 'is_active', 'created_at']
    search_fields = ['session_id', 'phone_number', 'parent__phone_number']
    readonly_fields = ['created_at', 'updated_at']

@admin.register(USSDLog)
class USSDLogAdmin(admin.ModelAdmin):
    list_display = ['session_id', 'phone_number', 'menu_state', 'user_input', 'created_at']
    list_filter = ['menu_state', 'created_at']
    search_fields = ['session_id', 'phone_number']
    readonly_fields = ['id', 'created_at']

@admin.register(RedemptionRequest)
class RedemptionRequestAdmin(admin.ModelAdmin):
    list_display = ['child', 'parent', 'item_name', 'points_required', 'status', 'approved_via_ussd', 'requested_at']
    list_filter = ['status', 'redemption_type', 'approved_via_ussd', 'requested_at']
    search_fields = ['child__first_name', 'parent__phone_number', 'item_name']

@admin.register(ScreenTimeSetting)
class ScreenTimeSettingAdmin(admin.ModelAdmin):
    list_display = ['child', 'parent', 'limit_type', 'limit_minutes', 'is_active', 'set_via_ussd', 'updated_at']
    list_filter = ['limit_type', 'is_active', 'set_via_ussd', 'updated_at']
    search_fields = ['child__first_name', 'parent__phone_number']