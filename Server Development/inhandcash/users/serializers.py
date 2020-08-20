from rest_framework import serializers
from .models import UserProfile
from users.models import User


class UserInfoSerializer(serializers.ModelSerializer):
    qr_url = serializers.CharField(source='qr_code.url')

    class Meta:
        model = UserProfile
        fields = ['phonenumber', 'name', 'vpa', 'qr_url']


class UserProfilePhnSerializer(serializers.ModelSerializer):
    class Meta:
        model = UserProfile
        fields = ['pin_datetime', 'phonenumber', 'pin']


class UserProfileSerializer(serializers.ModelSerializer):
    class Meta:
        model = UserProfile
        fields = ['gender', 'profile_image', 'pin_datetime', 'name']


class UserRegisterSerilizer(serializers.ModelSerializer):
    name = serializers.CharField(required=True)

    class Meta:
        model = UserProfile
        fields = ['name', 'profile_image']


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ['email']
