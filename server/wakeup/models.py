from django.contrib.auth.models import AbstractUser
from django.utils.translation import gettext_lazy as _
from .validators import username_validators
from django.db import models

class User(AbstractUser):
    username = models.CharField(
        _('username'),
        max_length=150,
        unique=True,
        help_text=_('Required. 150 characters or fewer. Letters, digits and @/./+/-/_ only.'),
        validators=username_validators,
        error_messages={
            'unique': _("A user with that username already exists."),
        },
    )

class Device(models.Model):
    owner = models.ForeignKey(User, on_delete=models.CASCADE, related_name='devices')
    name = models.CharField(max_length=150)
    fb_token = models.CharField(max_length=256, null=True, blank=True)
