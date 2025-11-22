"""
Lesson Serializers for Porakhela API
"""
from rest_framework import serializers
from apps.lessons.models import (
    Subject, Chapter, Lesson, LessonProgress, 
    QuizQuestion, QuizAttempt
)


class SubjectSerializer(serializers.ModelSerializer):
    """Subject serializer"""
    
    class Meta:
        model = Subject
        fields = ['id', 'name', 'name_bn', 'description', 'icon_url', 
                 'color_code', 'is_active']


class ChapterSerializer(serializers.ModelSerializer):
    """Chapter serializer"""
    subject = SubjectSerializer(read_only=True)
    lessons_count = serializers.SerializerMethodField()
    
    class Meta:
        model = Chapter
        fields = ['id', 'subject', 'grade', 'chapter_number', 'title', 
                 'title_bn', 'description', 'learning_objectives', 
                 'estimated_duration_minutes', 'lessons_count', 'is_active']
    
    def get_lessons_count(self, obj):
        return obj.lessons.filter(is_active=True).count()


class QuizQuestionSerializer(serializers.ModelSerializer):
    """Quiz question serializer"""
    
    class Meta:
        model = QuizQuestion
        fields = ['id', 'question_text', 'question_text_bn', 'question_type', 
                 'options', 'points', 'order']
        # Note: correct_answer and explanation excluded for security


class LessonSerializer(serializers.ModelSerializer):
    """Lesson serializer"""
    chapter = ChapterSerializer(read_only=True)
    quiz_questions = QuizQuestionSerializer(many=True, read_only=True)
    user_progress = serializers.SerializerMethodField()
    
    class Meta:
        model = Lesson
        fields = ['id', 'chapter', 'title', 'title_bn', 'description', 
                 'lesson_type', 'difficulty', 'content_data', 'video_url', 
                 'thumbnail_url', 'audio_url', 'duration_minutes', 
                 'porapoints_reward', 'is_free', 'is_downloadable', 
                 'download_size_mb', 'quiz_questions', 'user_progress']
    
    def get_user_progress(self, obj):
        request = self.context.get('request')
        if request and request.user.is_authenticated:
            try:
                progress = LessonProgress.objects.get(
                    child=request.user, 
                    lesson=obj
                )
                return LessonProgressSerializer(progress).data
            except LessonProgress.DoesNotExist:
                return None
        return None


class LessonProgressSerializer(serializers.ModelSerializer):
    """Lesson progress serializer"""
    lesson_title = serializers.CharField(source='lesson.title', read_only=True)
    child_name = serializers.CharField(source='child.get_full_name', read_only=True)
    
    class Meta:
        model = LessonProgress
        fields = ['id', 'child_name', 'lesson_title', 'status', 
                 'completion_percentage', 'time_spent_minutes', 'quiz_score', 
                 'attempts', 'porapoints_earned', 'started_at', 'completed_at', 
                 'last_accessed_at']
        read_only_fields = ['id', 'porapoints_earned', 'started_at', 
                           'completed_at', 'last_accessed_at']


class QuizAttemptSerializer(serializers.ModelSerializer):
    """Quiz attempt serializer"""
    lesson_title = serializers.CharField(source='lesson.title', read_only=True)
    child_name = serializers.CharField(source='child.get_full_name', read_only=True)
    
    class Meta:
        model = QuizAttempt
        fields = ['id', 'child_name', 'lesson_title', 'score', 
                 'total_questions', 'correct_answers', 'time_taken_seconds', 
                 'started_at', 'completed_at']
        read_only_fields = ['id', 'started_at']


class StartLessonSerializer(serializers.Serializer):
    """Start lesson serializer"""
    lesson_id = serializers.UUIDField()


class UpdateProgressSerializer(serializers.Serializer):
    """Update lesson progress serializer"""
    completion_percentage = serializers.IntegerField(min_value=0, max_value=100)
    time_spent_minutes = serializers.IntegerField(min_value=0)


class SubmitQuizSerializer(serializers.Serializer):
    """Submit quiz answers serializer"""
    answers = serializers.DictField()  # question_id: answer mapping
    time_taken_seconds = serializers.IntegerField(min_value=0)