"""
Lesson Models for Porakhela NCTB Curriculum
"""
from django.db import models
from django.contrib.auth import get_user_model
import uuid

User = get_user_model()


class Subject(models.Model):
    """
    NCTB Subjects (Bengali, English, Math, Science, Social Studies, etc.)
    """
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    name = models.CharField(max_length=100)
    name_bn = models.CharField(max_length=100)  # Bengali name
    description = models.TextField()
    icon_url = models.URLField(blank=True, null=True)
    color_code = models.CharField(max_length=7, default='#2196F3')  # Hex color
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'subjects'
        verbose_name = 'Subject'
        verbose_name_plural = 'Subjects'

    def __str__(self):
        return self.name


class Chapter(models.Model):
    """
    NCTB Chapters within subjects for each grade
    """
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    subject = models.ForeignKey(Subject, on_delete=models.CASCADE, related_name='chapters')
    grade = models.IntegerField()
    chapter_number = models.IntegerField()
    title = models.CharField(max_length=200)
    title_bn = models.CharField(max_length=200)
    description = models.TextField()
    learning_objectives = models.JSONField(default=list)  # List of learning objectives
    estimated_duration_minutes = models.IntegerField(default=30)
    order = models.IntegerField(default=0)
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'chapters'
        verbose_name = 'Chapter'
        verbose_name_plural = 'Chapters'
        ordering = ['subject', 'grade', 'order']
        unique_together = ['subject', 'grade', 'chapter_number']

    def __str__(self):
        return f"Grade {self.grade} - {self.subject.name} - Chapter {self.chapter_number}: {self.title}"


class Lesson(models.Model):
    """
    Core lesson model with requirements: subject, grade, title, content_json (NCTB-aligned)
    """
    LESSON_TYPE_CHOICES = [
        ('video', 'Video Lesson'),
        ('interactive', 'Interactive Content'),
        ('quiz', 'Quiz'),
        ('reading', 'Reading Material'),
        ('exercise', 'Exercise'),
    ]
    
    DIFFICULTY_CHOICES = [
        ('easy', 'Easy'),
        ('medium', 'Medium'),
        ('hard', 'Hard'),
    ]
    
    SUBJECT_CHOICES = [
        ('bangla', 'Bangla'),
        ('english', 'English'),
        ('math', 'Mathematics'),
        ('science', 'Science'),
        ('social_science', 'Social Science'),
        ('islam', 'Islam & Moral Education'),
        ('ict', 'ICT'),
    ]
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    subject = models.CharField(max_length=20, choices=SUBJECT_CHOICES, help_text="Subject for this lesson")
    grade = models.IntegerField(help_text="Grade level (1-10) for this lesson")
    title = models.CharField(max_length=200, help_text="Lesson title")
    content_json = models.JSONField(default=dict, help_text="NCTB-aligned lesson package data")
    
    # Additional comprehensive fields for full functionality
    chapter = models.ForeignKey(Chapter, on_delete=models.CASCADE, related_name='lessons', null=True, blank=True)
    title_bn = models.CharField(max_length=200, blank=True)
    description = models.TextField(blank=True)
    lesson_type = models.CharField(max_length=20, choices=LESSON_TYPE_CHOICES, default='interactive')
    difficulty = models.CharField(max_length=10, choices=DIFFICULTY_CHOICES, default='medium')
    video_url = models.URLField(blank=True, null=True)
    thumbnail_url = models.URLField(blank=True, null=True)
    audio_url = models.URLField(blank=True, null=True)
    duration_minutes = models.IntegerField(default=15)
    porapoints_reward = models.IntegerField(default=10)
    order = models.IntegerField(default=0)
    is_free = models.BooleanField(default=True)
    is_active = models.BooleanField(default=True)
    is_downloadable = models.BooleanField(default=True)
    download_size_mb = models.FloatField(default=0)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'lessons'
        verbose_name = 'Lesson'
        verbose_name_plural = 'Lessons'
        ordering = ['chapter', 'order']

    def __str__(self):
        return f"{self.chapter} - {self.title}"


class LessonProgress(models.Model):
    """
    Core progress model with requirements: child, lesson, score, time_spent, completed_at
    """
    STATUS_CHOICES = [
        ('not_started', 'Not Started'),
        ('in_progress', 'In Progress'),
        ('completed', 'Completed'),
        ('reviewed', 'Reviewed'),
    ]
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    child = models.ForeignKey(User, on_delete=models.CASCADE, related_name='lesson_progress', help_text="Child who is taking the lesson")
    lesson = models.ForeignKey(Lesson, on_delete=models.CASCADE, related_name='student_progress', help_text="Lesson being taken")
    score = models.FloatField(default=0, help_text="Score achieved in the lesson (0-100)")
    time_spent = models.IntegerField(default=0, help_text="Time spent on lesson in minutes") 
    completed_at = models.DateTimeField(null=True, blank=True, help_text="When the lesson was completed")
    
    # Additional comprehensive tracking fields
    status = models.CharField(max_length=15, choices=STATUS_CHOICES, default='not_started')
    completion_percentage = models.IntegerField(default=0)
    attempts = models.IntegerField(default=0)
    porapoints_earned = models.IntegerField(default=0)
    started_at = models.DateTimeField(null=True, blank=True)
    last_accessed_at = models.DateTimeField(auto_now=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'lesson_progress'
        verbose_name = 'Lesson Progress'
        verbose_name_plural = 'Lesson Progress'
        unique_together = ['child', 'lesson']
        indexes = [
            models.Index(fields=['child', 'status']),
            models.Index(fields=['lesson', 'completion_percentage']),
        ]

    def __str__(self):
        return f"{self.child.get_full_name()} - {self.lesson.title} ({self.status})"


class QuizQuestion(models.Model):
    """
    Quiz questions for lessons
    """
    QUESTION_TYPE_CHOICES = [
        ('mcq', 'Multiple Choice'),
        ('true_false', 'True/False'),
        ('fill_blank', 'Fill in the Blank'),
        ('drag_drop', 'Drag and Drop'),
        ('matching', 'Matching'),
    ]
    
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    lesson = models.ForeignKey(Lesson, on_delete=models.CASCADE, related_name='quiz_questions')
    
    question_text = models.TextField()
    question_text_bn = models.TextField()
    question_type = models.CharField(max_length=15, choices=QUESTION_TYPE_CHOICES)
    options = models.JSONField(default=list)  # List of options for MCQ
    correct_answer = models.JSONField(default=dict)  # Flexible answer structure
    explanation = models.TextField(blank=True)
    explanation_bn = models.TextField(blank=True)
    
    points = models.IntegerField(default=1)
    order = models.IntegerField(default=0)
    is_active = models.BooleanField(default=True)
    
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'quiz_questions'
        verbose_name = 'Quiz Question'
        verbose_name_plural = 'Quiz Questions'
        ordering = ['lesson', 'order']

    def __str__(self):
        return f"{self.lesson.title} - Q{self.order}: {self.question_text[:50]}..."


class QuizAttempt(models.Model):
    """
    Track quiz attempts by students
    """
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    child = models.ForeignKey(User, on_delete=models.CASCADE, related_name='quiz_attempts')
    lesson = models.ForeignKey(Lesson, on_delete=models.CASCADE, related_name='quiz_attempts')
    
    answers = models.JSONField(default=dict)  # question_id: answer mapping
    score = models.FloatField(default=0)
    total_questions = models.IntegerField()
    correct_answers = models.IntegerField(default=0)
    time_taken_seconds = models.IntegerField(default=0)
    
    started_at = models.DateTimeField(auto_now_add=True)
    completed_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        db_table = 'quiz_attempts'
        verbose_name = 'Quiz Attempt'
        verbose_name_plural = 'Quiz Attempts'
        indexes = [
            models.Index(fields=['child', 'lesson']),
            models.Index(fields=['completed_at']),
        ]

    def __str__(self):
        return f"{self.child.get_full_name()} - {self.lesson.title} Quiz - {self.score}%"