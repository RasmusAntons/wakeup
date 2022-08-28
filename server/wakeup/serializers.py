from .models import User, Device, Alarm
from rest_framework import serializers


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ['id', 'username']
        extra_kwargs = {
            'username': {'read_only': True}
        }


class CurrentUserSerializer(UserSerializer):
    class Meta:
        model = User
        fields = ['id', 'username', 'devices', 'friends', 'friends_reverse']
        extra_kwargs = {
            'friends_reverse': {'read_only': True}
        }


class DeviceSerializer(serializers.ModelSerializer):
    class Meta:
        model = Device
        fields = ['id', 'owner', 'android_id', 'name', 'fb_token', 'active']
        extra_kwargs = {
            'fb_token': {'write_only': True}
        }


class AlarmSerializer(serializers.ModelSerializer):
    class Meta:
        model = Alarm
        fields = ['id', 'user', 'target', 'creation_time', 'message', 'status']
        extra_kwargs = {
            'user': {'read_only': True},
            'message': {'read_only': True}
        }
