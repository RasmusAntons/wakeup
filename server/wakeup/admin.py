from django.contrib import admin
from .models import User, Device

admin.register(User, Device)(admin.ModelAdmin)
