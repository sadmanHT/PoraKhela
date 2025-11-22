"""
Lesson ViewSets for Porakhela API
"""
from rest_framework import viewsets, status, permissions
from rest_framework.decorators import action
from rest_framework.response import Response
from django.db import models
from django.utils import timezone
from datetime import datetime
from apps.lessons.models import (
    Subject, Chapter, Lesson, LessonProgress, 
    QuizQuestion, QuizAttempt
)
from apps.lessons.serializers import (
    SubjectSerializer, ChapterSerializer, LessonSerializer,
    LessonProgressSerializer, QuizAttemptSerializer,
    StartLessonSerializer, UpdateProgressSerializer, SubmitQuizSerializer
)
from services.gamification_engine import gamification_engine


class SubjectViewSet(viewsets.ReadOnlyModelViewSet):
    """Subject endpoints"""
    serializer_class = SubjectSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        return Subject.objects.filter(is_active=True).order_by('name')


class ChapterViewSet(viewsets.ReadOnlyModelViewSet):
    """Chapter endpoints"""
    serializer_class = ChapterSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        queryset = Chapter.objects.filter(is_active=True).select_related('subject')
        
        # Filter by grade if user is a child
        user = self.request.user
        if user.user_type == 'child' and hasattr(user, 'child_profile'):
            grade = user.child_profile.grade
            queryset = queryset.filter(grade=grade)
        
        # Filter by subject if provided
        subject_id = self.request.query_params.get('subject', None)
        if subject_id:
            queryset = queryset.filter(subject_id=subject_id)
        
        # Filter by grade if provided
        grade = self.request.query_params.get('grade', None)
        if grade:
            queryset = queryset.filter(grade=grade)
        
        return queryset.order_by('subject', 'order')


class LessonViewSet(viewsets.ReadOnlyModelViewSet):
    """Lesson endpoints"""
    serializer_class = LessonSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        queryset = Lesson.objects.filter(is_active=True).select_related(
            'chapter', 'chapter__subject'
        ).prefetch_related('quiz_questions')
        
        # Filter by chapter if provided
        chapter_id = self.request.query_params.get('chapter', None)
        if chapter_id:
            queryset = queryset.filter(chapter_id=chapter_id)
        
        # Filter by difficulty if provided
        difficulty = self.request.query_params.get('difficulty', None)
        if difficulty:
            queryset = queryset.filter(difficulty=difficulty)
        
        return queryset.order_by('chapter', 'order')
    
    @action(detail=True, methods=['post'])
    def start_lesson(self, request, pk=None):
        """Start a lesson"""
        lesson = self.get_object()
        child = request.user
        
        # Only children can start lessons
        if child.user_type != 'child':
            return Response(
                {'error': 'Only children can start lessons'}, 
                status=status.HTTP_403_FORBIDDEN
            )
        
        # Create or get lesson progress
        progress, created = LessonProgress.objects.get_or_create(
            child=child,
            lesson=lesson,
            defaults={
                'status': 'in_progress',
                'started_at': timezone.now()
            }
        )
        
        if not created and progress.status == 'not_started':
            progress.status = 'in_progress'
            progress.started_at = timezone.now()
            progress.save()
        
        # Update daily streak
        gamification_engine.update_daily_streak(child)
        
        serializer = LessonProgressSerializer(progress)
        return Response(serializer.data)
    
    @action(detail=True, methods=['post'])
    def update_progress(self, request, pk=None):
        """Update lesson progress"""
        lesson = self.get_object()
        child = request.user
        
        if child.user_type != 'child':
            return Response(
                {'error': 'Only children can update progress'}, 
                status=status.HTTP_403_FORBIDDEN
            )
        
        serializer = UpdateProgressSerializer(data=request.data)
        if serializer.is_valid():
            try:
                progress = LessonProgress.objects.get(child=child, lesson=lesson)
                
                # Update progress
                progress.completion_percentage = serializer.validated_data['completion_percentage']
                progress.time_spent_minutes = serializer.validated_data['time_spent_minutes']
                
                # Mark as completed if 100%
                if progress.completion_percentage >= 100 and progress.status != 'completed':
                    progress.status = 'completed'
                    progress.completed_at = timezone.now()
                    
                    # Award points
                    points = gamification_engine.calculate_lesson_points(child, progress)
                    gamification_engine.award_points(
                        child=child,
                        amount=points,
                        source_type='lesson_completion',
                        reference_id=str(lesson.id),
                        description=f"Completed lesson: {lesson.title}"
                    )
                    progress.porapoints_earned = points
                    
                    # Check achievements
                    gamification_engine.check_achievements(child)
                    
                    # Update leaderboards
                    gamification_engine.update_leaderboards(child)
                
                progress.save()
                
                serializer = LessonProgressSerializer(progress)
                return Response(serializer.data)
                
            except LessonProgress.DoesNotExist:
                return Response(
                    {'error': 'Lesson not started yet'}, 
                    status=status.HTTP_400_BAD_REQUEST
                )
        
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
    
    @action(detail=True, methods=['post'])
    def submit_quiz(self, request, pk=None):
        """Submit quiz answers"""
        lesson = self.get_object()
        child = request.user
        
        if child.user_type != 'child':
            return Response(
                {'error': 'Only children can submit quizzes'}, 
                status=status.HTTP_403_FORBIDDEN
            )
        
        serializer = SubmitQuizSerializer(data=request.data)
        if serializer.is_valid():
            answers = serializer.validated_data['answers']
            time_taken = serializer.validated_data['time_taken_seconds']
            
            # Get quiz questions
            questions = QuizQuestion.objects.filter(lesson=lesson, is_active=True)
            
            if not questions.exists():
                return Response(
                    {'error': 'No quiz questions found for this lesson'}, 
                    status=status.HTTP_400_BAD_REQUEST
                )
            
            # Calculate score
            total_questions = questions.count()
            correct_answers = 0
            
            for question in questions:
                user_answer = answers.get(str(question.id))
                if user_answer and user_answer == question.correct_answer:
                    correct_answers += 1
            
            score = (correct_answers / total_questions) * 100 if total_questions > 0 else 0
            
            # Create quiz attempt
            quiz_attempt = QuizAttempt.objects.create(
                child=child,
                lesson=lesson,
                answers=answers,
                score=score,
                total_questions=total_questions,
                correct_answers=correct_answers,
                time_taken_seconds=time_taken,
                completed_at=timezone.now()
            )
            
            # Update lesson progress
            try:
                progress = LessonProgress.objects.get(child=child, lesson=lesson)
                progress.quiz_score = score
                progress.attempts += 1
                progress.save()
            except LessonProgress.DoesNotExist:
                # Create progress if it doesn't exist
                progress = LessonProgress.objects.create(
                    child=child,
                    lesson=lesson,
                    quiz_score=score,
                    attempts=1,
                    status='in_progress'
                )
            
            # Check achievements for quiz performance
            gamification_engine.check_achievements(child)
            
            serializer = QuizAttemptSerializer(quiz_attempt)
            return Response({
                'quiz_attempt': serializer.data,
                'message': f'Quiz completed! Score: {score:.1f}%'
            })
        
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
    
    @action(detail=True, methods=['get'])
    def download_content(self, request, pk=None):
        """Get downloadable content for offline use"""
        lesson = self.get_object()
        child = request.user
        
        if not lesson.is_downloadable:
            return Response(
                {'error': 'This lesson is not available for download'}, 
                status=status.HTTP_400_BAD_REQUEST
            )
        
        # Generate secure download URL
        from utils.helpers import generate_lesson_download_url
        download_url = generate_lesson_download_url(str(lesson.id), str(child.id))
        
        return Response({
            'download_url': download_url,
            'size_mb': lesson.download_size_mb,
            'expires_in': 3600  # 1 hour
        })


class LessonProgressViewSet(viewsets.ReadOnlyModelViewSet):
    """Lesson progress tracking"""
    serializer_class = LessonProgressSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        user = self.request.user
        
        if user.user_type == 'child':
            return LessonProgress.objects.filter(child=user).select_related('lesson', 'child')
        elif user.user_type == 'parent':
            # Parent can see all their children's progress
            return LessonProgress.objects.filter(
                child__child_profile__parent=user
            ).select_related('lesson', 'child')
        
        return LessonProgress.objects.none()
    
    @action(detail=False, methods=['get'])
    def summary(self, request):
        """Get progress summary"""
        user = request.user
        
        if user.user_type == 'child':
            queryset = self.get_queryset()
            
            summary = {
                'total_lessons': queryset.count(),
                'completed_lessons': queryset.filter(status='completed').count(),
                'in_progress_lessons': queryset.filter(status='in_progress').count(),
                'total_time_spent': queryset.aggregate(
                    total=models.Sum('time_spent_minutes')
                )['total'] or 0,
                'average_score': queryset.filter(
                    quiz_score__isnull=False
                ).aggregate(avg=models.Avg('quiz_score'))['avg'] or 0,
                'current_streak': gamification_engine.get_current_streak(user),
                'total_points': gamification_engine.get_current_balance(user)
            }
            
            return Response(summary)
        
        return Response(
            {'error': 'Progress summary only available for children'}, 
            status=status.HTTP_403_FORBIDDEN
        )