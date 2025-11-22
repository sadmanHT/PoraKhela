"""
Django Admin Configuration for Lesson Models
"""
from django.contrib import admin
from apps.lessons.models import Subject, Chapter, Lesson, LessonProgress, QuizQuestion, QuizAttempt


@admin.register(Subject)
class SubjectAdmin(admin.ModelAdmin):
    """Admin for Subject model"""
    list_display = ['name', 'name_bn', 'color_code', 'is_active', 'created_at']
    list_filter = ['is_active', 'created_at']
    search_fields = ['name', 'name_bn']
    readonly_fields = ['created_at']


@admin.register(Chapter)
class ChapterAdmin(admin.ModelAdmin):
    """Admin for Chapter model"""
    list_display = ['title', 'subject', 'grade', 'chapter_number', 'order', 'is_active', 'estimated_duration_minutes']
    list_filter = ['subject', 'grade', 'is_active', 'created_at']
    search_fields = ['title', 'title_bn']
    readonly_fields = ['created_at']
    ordering = ['subject', 'grade', 'order']


@admin.register(Lesson)
class LessonAdmin(admin.ModelAdmin):
    """Admin for Lesson model"""
    list_display = ['title', 'chapter', 'subject', 'grade', 'lesson_type', 'difficulty', 'porapoints_reward', 'is_active']
    list_filter = ['subject', 'grade', 'lesson_type', 'difficulty', 'is_free', 'is_active', 'created_at']
    search_fields = ['title', 'title_bn', 'description']
    readonly_fields = ['created_at', 'updated_at']
    ordering = ['subject', 'grade', 'order']
    
    fieldsets = (
        ('Basic Info', {'fields': ('chapter', 'title', 'title_bn', 'subject', 'grade')}),
        ('Content', {'fields': ('description', 'content_json', 'lesson_type', 'difficulty')}),
        ('Media', {'fields': ('video_url', 'thumbnail_url', 'audio_url')}),
        ('Settings', {'fields': ('duration_minutes', 'porapoints_reward', 'order')}),
        ('Availability', {'fields': ('is_free', 'is_active', 'is_downloadable', 'download_size_mb')}),
        ('Timestamps', {'fields': ('created_at', 'updated_at'), 'classes': ('collapse',)}),
    )


@admin.register(LessonProgress)
class LessonProgressAdmin(admin.ModelAdmin):
    """Admin for LessonProgress model"""
    list_display = ['child', 'lesson', 'status', 'completion_percentage', 'score', 'porapoints_earned', 'completed_at']
    list_filter = ['status', 'lesson__subject', 'lesson__grade', 'completed_at', 'created_at']
    search_fields = ['child__user__first_name', 'child__user__last_name', 'lesson__title']
    readonly_fields = ['created_at']
    
    def get_queryset(self, request):
        return super().get_queryset(request).select_related('child__user', 'lesson')


@admin.register(QuizQuestion)
class QuizQuestionAdmin(admin.ModelAdmin):
    """Admin for QuizQuestion model"""
    list_display = ['question_text', 'lesson', 'question_type', 'points', 'order', 'is_active']
    list_filter = ['question_type', 'lesson__subject', 'lesson__grade', 'is_active', 'created_at']
    search_fields = ['question_text', 'question_text_bn', 'lesson__title']
    readonly_fields = ['created_at']
    ordering = ['lesson', 'order']


@admin.register(QuizAttempt)
class QuizAttemptAdmin(admin.ModelAdmin):
    """Admin for QuizAttempt model"""
    list_display = ['child', 'lesson', 'score', 'correct_answers', 'total_questions', 'time_taken_seconds', 'completed_at']
    list_filter = ['lesson__subject', 'lesson__grade', 'completed_at', 'started_at']
    search_fields = ['child__user__first_name', 'child__user__last_name', 'lesson__title']
    readonly_fields = ['started_at']
    
    def get_queryset(self, request):
        return super().get_queryset(request).select_related('child__user', 'lesson')