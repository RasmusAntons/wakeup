from django.contrib import admin
from .models import User, Device

admin.register(User)(admin.ModelAdmin)

@admin.register(Device)
class DeviceAdmin(admin.ModelAdmin):
    list_display = ('name', 'owner', 'id')
