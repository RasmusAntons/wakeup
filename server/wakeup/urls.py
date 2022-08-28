from django.urls import path, include
from rest_framework import routers
from . import views
from .models import Device


router = routers.DefaultRouter(trailing_slash=False)
router.register(r'users', views.UserViewSet)
router.register(r'devices', views.DeviceViewSet)
router.register(r'alarms', views.AlarmsViewSet)


urlpatterns = [
    path('', views.index, name='index'),
    path('friends', views.friends, name='friends'),
    path('devices', views.devices, name='devices'),
    path('alarms', views.alarms, name='alarms'),
    path('send_alarm/<uuid:user_id>', views.send_alarm, name='send_alarm'),
    path("select2/", include("django_select2.urls")),
    path('api/me', views.CurrentUserView.as_view()),
    path('api/', include(router.urls)),
]
