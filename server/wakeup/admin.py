from django.contrib import admin
from .models import User, Device, Alarm

admin.register(User, Alarm)(admin.ModelAdmin)

@admin.register(Device)
class DeviceAdmin(admin.ModelAdmin):
    list_display = ('name', 'owner', 'id')
