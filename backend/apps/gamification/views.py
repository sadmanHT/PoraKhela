"""
Gamification ViewSets for Porakhela API
"""
from rest_framework import viewsets, status, permissions
from rest_framework.decorators import action
from rest_framework.response import Response
from django.db import models
from django.utils import timezone
from datetime import date, timedelta
from apps.gamification.models import (
    PorapointLedger, Achievement, UserAchievement, 
    DailyStreak, Leaderboard, RewardCatalog, RewardRedemption
)
from apps.gamification.serializers import (
    PorapointLedgerSerializer, AchievementSerializer, UserAchievementSerializer,
    DailyStreakSerializer, LeaderboardSerializer, RewardCatalogSerializer,
    RewardRedemptionSerializer, RedeemRewardSerializer, PointsBalanceSerializer
)
from services.gamification_engine import gamification_engine
from services.applink_client import applink_client


class PorapointViewSet(viewsets.ReadOnlyModelViewSet):
    """Porapoint transaction management"""
    serializer_class = PorapointLedgerSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        user = self.request.user
        
        if user.user_type == 'child':
            return PorapointLedger.objects.filter(child=user)
        elif user.user_type == 'parent':
            # Parent can see all their children's transactions
            return PorapointLedger.objects.filter(
                child__child_profile__parent=user
            )
        
        return PorapointLedger.objects.none()
    
    @action(detail=False, methods=['get'])
    def balance(self, request):
        """Get current points balance and summary"""
        user = request.user
        
        if user.user_type != 'child':
            return Response(
                {'error': 'Points balance only available for children'}, 
                status=status.HTTP_403_FORBIDDEN
            )
        
        transactions = PorapointLedger.objects.filter(child=user)
        
        current_balance = gamification_engine.get_current_balance(user)
        total_earned = transactions.filter(
            transaction_type='earned'
        ).aggregate(total=models.Sum('amount'))['total'] or 0
        
        total_redeemed = transactions.filter(
            transaction_type='redeemed'
        ).aggregate(total=models.Sum('amount'))['total'] or 0
        total_redeemed = abs(total_redeemed)  # Make positive
        
        pending_redemptions = RewardRedemption.objects.filter(
            child=user, 
            status__in=['pending', 'processing']
        ).aggregate(total=models.Sum('total_points_cost'))['total'] or 0
        
        balance_data = {
            'current_balance': current_balance,
            'total_earned': total_earned,
            'total_redeemed': total_redeemed,
            'pending_redemptions': pending_redemptions
        }
        
        serializer = PointsBalanceSerializer(balance_data)
        return Response(serializer.data)


class AchievementViewSet(viewsets.ReadOnlyModelViewSet):
    """Achievement management"""
    serializer_class = AchievementSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        queryset = Achievement.objects.filter(is_active=True)
        
        # Show hidden achievements only if user has unlocked them
        user = self.request.user
        if user.user_type == 'child':
            unlocked_hidden = UserAchievement.objects.filter(
                child=user, 
                achievement__is_hidden=True,
                is_completed=True
            ).values_list('achievement_id', flat=True)
            
            queryset = queryset.filter(
                models.Q(is_hidden=False) | models.Q(id__in=unlocked_hidden)
            )
        
        return queryset.order_by('achievement_type', 'reward_points')
    
    @action(detail=False, methods=['get'])
    def my_progress(self, request):
        """Get user's achievement progress"""
        user = request.user
        
        if user.user_type != 'child':
            return Response(
                {'error': 'Achievement progress only available for children'}, 
                status=status.HTTP_403_FORBIDDEN
            )
        
        user_achievements = UserAchievement.objects.filter(
            child=user
        ).select_related('achievement')
        
        serializer = UserAchievementSerializer(user_achievements, many=True)
        return Response(serializer.data)


class LeaderboardViewSet(viewsets.ReadOnlyModelViewSet):
    """Leaderboard management"""
    serializer_class = LeaderboardSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        leaderboard_type = self.request.query_params.get('type', 'global_points')
        period = self.request.query_params.get('period', 'all_time')
        
        queryset = Leaderboard.objects.filter(
            leaderboard_type=leaderboard_type,
            period=period
        ).select_related('child').order_by('rank')
        
        # For grade-specific leaderboards, filter by user's grade
        user = self.request.user
        if (leaderboard_type == 'grade_points' and 
            user.user_type == 'child' and 
            hasattr(user, 'child_profile')):
            
            grade = user.child_profile.grade
            queryset = queryset.filter(
                child__child_profile__grade=grade
            )
        
        return queryset[:50]  # Top 50
    
    @action(detail=False, methods=['get'])
    def my_rank(self, request):
        """Get current user's leaderboard positions"""
        user = request.user
        
        if user.user_type != 'child':
            return Response(
                {'error': 'Leaderboard rank only available for children'}, 
                status=status.HTTP_403_FORBIDDEN
            )
        
        rankings = {}
        leaderboard_types = [
            'global_points', 'weekly_points', 'monthly_points', 
            'grade_points', 'streak_champion'
        ]
        
        for lb_type in leaderboard_types:
            try:
                entry = Leaderboard.objects.get(
                    child=user,
                    leaderboard_type=lb_type,
                    period='weekly' if 'weekly' in lb_type else 'all_time'
                )
                rankings[lb_type] = {
                    'rank': entry.rank,
                    'score': entry.score
                }
            except Leaderboard.DoesNotExist:
                rankings[lb_type] = {
                    'rank': None,
                    'score': 0
                }
        
        return Response(rankings)


class RewardViewSet(viewsets.ReadOnlyModelViewSet):
    """Reward catalog and redemption"""
    serializer_class = RewardCatalogSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        return RewardCatalog.objects.filter(is_active=True).order_by('cost_points')
    
    @action(detail=True, methods=['post'])
    def redeem(self, request, pk=None):
        """Redeem a reward"""
        reward = self.get_object()
        child = request.user
        
        if child.user_type != 'child':
            return Response(
                {'error': 'Only children can redeem rewards'}, 
                status=status.HTTP_403_FORBIDDEN
            )
        
        serializer = RedeemRewardSerializer(data=request.data)
        if serializer.is_valid():
            quantity = serializer.validated_data['quantity']
            delivery_phone = serializer.validated_data['delivery_phone']
            total_cost = reward.cost_points * quantity
            
            # Check if user has enough points
            current_balance = gamification_engine.get_current_balance(child)
            if current_balance < total_cost:
                return Response(
                    {'error': f'Insufficient points. Need {total_cost}, have {current_balance}'}, 
                    status=status.HTTP_400_BAD_REQUEST
                )
            
            # Check stock if limited
            if reward.stock_quantity is not None:
                if reward.stock_quantity < quantity:
                    return Response(
                        {'error': 'Insufficient stock'}, 
                        status=status.HTTP_400_BAD_REQUEST
                    )
            
            # Check daily limit
            if reward.daily_limit_per_user:
                today_redemptions = RewardRedemption.objects.filter(
                    child=child,
                    reward=reward,
                    created_at__date=date.today()
                ).aggregate(total=models.Sum('quantity'))['total'] or 0
                
                if today_redemptions + quantity > reward.daily_limit_per_user:
                    return Response(
                        {'error': f'Daily limit exceeded. Can redeem {reward.daily_limit_per_user - today_redemptions} more today'}, 
                        status=status.HTTP_400_BAD_REQUEST
                    )
            
            # Deduct points
            transaction = gamification_engine.deduct_points(
                child=child,
                amount=total_cost,
                source_type=f'{reward.reward_type}_purchase',
                reference_id=str(reward.id),
                description=f"Redeemed {quantity}x {reward.name}"
            )
            
            if not transaction:
                return Response(
                    {'error': 'Failed to deduct points'}, 
                    status=status.HTTP_500_INTERNAL_SERVER_ERROR
                )
            
            # Create redemption record
            redemption = RewardRedemption.objects.create(
                child=child,
                reward=reward,
                quantity=quantity,
                total_points_cost=total_cost,
                delivery_phone=delivery_phone,
                status='pending'
            )
            
            # Update stock
            if reward.stock_quantity is not None:
                reward.stock_quantity -= quantity
                reward.save()
            
            # Process redemption via Applink (mock)
            # In production, this would be async
            if reward.reward_type == 'data_bundle':
                # applink_response = await applink_client.redeem_data_bundle(
                #     delivery_phone, 'data', reward.value_unit
                # )
                pass
            elif reward.reward_type == 'talktime':
                # applink_response = await applink_client.redeem_talktime(
                #     delivery_phone, float(reward.value_amount)
                # )
                pass
            
            serializer = RewardRedemptionSerializer(redemption)
            return Response({
                'message': f'Redemption successful! {reward.name} will be delivered to {delivery_phone}',
                'redemption': serializer.data,
                'new_balance': gamification_engine.get_current_balance(child)
            })
        
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
    
    @action(detail=False, methods=['get'])
    def my_redemptions(self, request):
        """Get user's redemption history"""
        child = request.user
        
        if child.user_type != 'child':
            return Response(
                {'error': 'Redemption history only available for children'}, 
                status=status.HTTP_403_FORBIDDEN
            )
        
        redemptions = RewardRedemption.objects.filter(
            child=child
        ).select_related('reward').order_by('-created_at')
        
        serializer = RewardRedemptionSerializer(redemptions, many=True)
        return Response(serializer.data)


class DailyStreakViewSet(viewsets.ReadOnlyModelViewSet):
    """Daily streak management"""
    serializer_class = DailyStreakSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        user = self.request.user
        
        if user.user_type == 'child':
            return DailyStreak.objects.filter(child=user)
        elif user.user_type == 'parent':
            # Parent can see all their children's streaks
            return DailyStreak.objects.filter(
                child__child_profile__parent=user
            )
        
        return DailyStreak.objects.none()
    
    @action(detail=False, methods=['get'])
    def leaderboard(self, request):
        """Get streak leaderboard"""
        top_streaks = DailyStreak.objects.select_related('child').order_by(
            '-current_streak'
        )[:20]
        
        serializer = DailyStreakSerializer(top_streaks, many=True)
        return Response(serializer.data)