from .models import User, Device
from rest_framework import serializers


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ['id', 'username', 'devices']


class DeviceSerializer(serializers.ModelSerializer):
    class Meta:
        model = Device
        fields = ['id', 'owner', 'android_id', 'name', 'fb_token']
        extra_kwargs = {
            'fb_token': {'write_only': True}
        }
