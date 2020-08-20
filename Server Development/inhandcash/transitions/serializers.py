from rest_framework import serializers
from transitions.models import Transition


class TransitionDetailSerializer(serializers.ModelSerializer):
    sender_name = serializers.CharField(source='sender.user_profile.name')
    receiver_name = serializers.CharField(source='receiver.user_profile.name')
    created_at = serializers.DateTimeField(format="%d-%m-%Y %H:%M:%S")
    status_name = serializers.CharField(source='get_status_display')

    class Meta:
        model = Transition
        fields = ['status_name', 'sender_name', 'receiver_name', 'status', 'amount', 'created_at', 'updated_at']
