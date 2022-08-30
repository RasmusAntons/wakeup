import uuid

from django.contrib.auth.models import AbstractUser
from django.utils.translation import gettext_lazy as _
from .validators import username_validators
from django.db import models
from django.db.models.signals import post_save
from .fb_utils import send_alarm_to_user

class User(AbstractUser):
    class Meta:
        ordering = ['username']

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
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
    friends = models.ManyToManyField('User', related_name='friends_reverse', blank=True)

    @property
    def active_devices(self):
        return len(self.devices.filter(fb_token__isnull=False, active=True))

    @property
    def alarms_all(self):
        return self.alarms_incoming.all() | self.alarms_outgoing.all()

class Device(models.Model):
    class Meta:
        ordering = ['name']

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    owner = models.ForeignKey(User, on_delete=models.CASCADE, related_name='devices')
    name = models.CharField(max_length=150)
    android_id = models.CharField(max_length=16)
    fb_token = models.CharField(max_length=256, null=True, blank=True)
    active = models.BooleanField(default=True)

    def __str__(self):
        return self.name

class Alarm(models.Model):
    class Meta:
        ordering = ['-creation_time']

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='alarms_outgoing', editable=False)
    target = models.ForeignKey(User, on_delete=models.CASCADE, related_name='alarms_incoming')
    creation_time = models.DateTimeField(auto_now=True, editable=False)
    message = models.CharField(max_length=256, null=True, blank=True)

    class Status(models.IntegerChoices):
        NEW = 1
        SENT = 2
        RECEIVED = 3
        COMPLETED = 4
        FAILED = 5

    status = models.IntegerField(choices=Status.choices, default=Status.NEW)

    @property
    def status_text(self):
        return self.Status(self.status).name

    @classmethod
    def post_create(cls, sender, instance, created, *args, **kwargs):
        if not created:
            return
        if send_alarm_to_user(instance.target, instance):
            instance.status = Alarm.Status.SENT
        else:
            instance.status = Alarm.Status.FAILED
        instance.save()


post_save.connect(Alarm.post_create, sender=Alarm)
