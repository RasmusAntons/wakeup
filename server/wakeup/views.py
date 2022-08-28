from django.shortcuts import render
from django.http import Http404, HttpResponse
from django.shortcuts import redirect, reverse
from django.contrib import messages
from django.contrib.auth.decorators import permission_required, login_required
from rest_framework import viewsets, permissions
from rest_framework.generics import RetrieveUpdateDestroyAPIView
from .models import User, Device, Alarm
from .serializers import UserSerializer, DeviceSerializer, CurrentUserSerializer, AlarmSerializer
from .forms import WakeForm, AddFriendForm, RemoveFriendForm
from .fb_utils import send_alarm_to_device
from .permissions import IsOwnerOrReadOnly


def index(req):
    users = User.objects.all()
    context = {'users': users}
    return render(req, 'wakeup/index.html', context=context)


@login_required
def send_alarm(req, user_id: str):
    if req.method == 'POST':
        alarm_form = WakeForm(req.POST)
        if alarm_form.is_valid():
            alarm_form.instance.user = req.user
            alarm_form.save(True)
            messages.add_message(req, messages.SUCCESS, f'Sent alarm to {alarm_form.instance.target.username}')
            return redirect(reverse('index'))
    else:
        try:
            target = req.user.friends_reverse.get(pk=user_id)
        except User.DoesNotExist:
            raise Http404('invalid user')
        alarm_form = WakeForm({'target': target})
    print(f'{alarm_form.fields=}')
    context = {'alarm_form': alarm_form}
    return render(req, 'wakeup/send_alarm.html', context=context)


@login_required
def devices(req):
    if req.method == 'POST':
        try:
            device = req.user.devices.get(pk=req.POST.get('device'))
        except Device.DoesNotExist:
            messages.add_message(req, messages.ERROR, 'device not found')
        else:
            if req.POST.get('action') in ('activate', 'deactivate'):
                device.active = req.POST.get('action') == 'activate'
                device.save()
            elif req.POST.get('action') == 'delete':
                device.delete()
    return render(req, 'wakeup/devices.html')


@login_required
def friends(req):
    if req.method == 'POST' and req.POST.get('action') == 'add':
        add_friend_form = AddFriendForm(req.POST, request=req)
        if add_friend_form.is_valid():
            req.user.friends.add(add_friend_form.cleaned_data.get('user'))
            add_friend_form = AddFriendForm()
    else:
        add_friend_form = AddFriendForm()
    if req.method == 'POST' and req.POST.get('action') == 'remove':
        remove_friend_form = RemoveFriendForm(req.POST, request=req)
        if remove_friend_form.is_valid():
            req.user.friends.remove(remove_friend_form.cleaned_data.get('user'))
    context = {'add_friend_form': add_friend_form}
    return render(req, 'wakeup/friends.html', context=context)


@login_required
def alarms(req):
    return render(req, 'wakeup/alarms.html')


class UserViewSet(viewsets.ModelViewSet):
    queryset = User.objects.all()
    serializer_class = UserSerializer
    permission_classes = [permissions.IsAuthenticated]


class CurrentUserView(RetrieveUpdateDestroyAPIView):
    queryset = User.objects.all()
    serializer_class = CurrentUserSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_object(self):
        return self.request.user

class DeviceViewSet(viewsets.ModelViewSet):
    queryset = Device.objects.all()
    serializer_class = DeviceSerializer
    permission_classes = [permissions.IsAuthenticated, IsOwnerOrReadOnly]

    def get_queryset(self):
        return Device.objects.filter(owner=self.request.user)

class AlarmsViewSet(viewsets.ModelViewSet):
    queryset = Alarm.objects.all()
    serializer_class = AlarmSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return Alarm.objects.filter(user=self.request.user) | Alarm.objects.filter(target=self.request.user)
