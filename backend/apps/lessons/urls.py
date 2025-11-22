"""
Lesson URLs for Porakhela API
"""
from django.urls import path, include
from rest_framework.routers import DefaultRouter
from apps.lessons.views import (
    SubjectViewSet, ChapterViewSet, LessonViewSet, LessonProgressViewSet
)
from apps.lessons.lesson_completion_api import LessonCompletionAPIView

router = DefaultRouter()
router.register(r'subjects', SubjectViewSet, basename='subjects')
router.register(r'chapters', ChapterViewSet, basename='chapters')
router.register(r'lessons', LessonViewSet, basename='lessons')
router.register(r'progress', LessonProgressViewSet, basename='progress')

urlpatterns = [
    path('', include(router.urls)),
    # Core lesson completion API - the revenue-driving endpoint
    path('complete/', LessonCompletionAPIView.as_view(), name='lesson-complete'),
]