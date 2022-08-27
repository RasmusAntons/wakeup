from django.urls import path, include
from rest_framework import routers
from . import views
from .models import Device


router = routers.DefaultRouter(trailing_slash=False)
router.register(r'users', views.UserViewSet)
router.register(r'devices', views.DeviceViewSet)


urlpatterns = [
    path('', views.index, name='index'),
    path('api/', include(router.urls)),
    path('wake/<int:device_id>', views.wake, name='wake'),
]
