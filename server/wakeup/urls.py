from django.urls import path
from . import views
from .models import Device

urlpatterns = [
    path('', views.index, name='index'),
    path('wake/<int:device_id>', views.wake, name='wake'),
]
