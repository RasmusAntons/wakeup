from django.shortcuts import render
from django.http import Http404, HttpResponse
from django.contrib.auth.decorators import permission_required, login_required
from rest_framework import viewsets, permissions
from .models import User, Device
from .serializers import UserSerializer, DeviceSerializer
from .forms import WakeForm
from .fb_utils import wake_device


def index(req):
    users = User.objects.all()
    context = {'users': users}
    return render(req, 'wakeup/index.html', context=context)


@login_required
def wake(req, device_id: str):
    if req.method == 'POST':
        wake_form = WakeForm(req.POST)
        if wake_form.is_valid():
            device = Device.objects.get(pk=wake_form.cleaned_data['device_id'])
            wake_device(device, wake_form.cleaned_data['message'])
            return HttpResponse('successfully sent wakeup message')
    try:
        device = Device.objects.get(pk=device_id)
    except Device.DoesNotExist:
        raise Http404('no such device')
    wake_form = WakeForm({'device_id': device_id})
    context = {'device': device, 'wake_form': wake_form}
    return render(req, 'wakeup/wake.html', context=context)


class UserViewSet(viewsets.ModelViewSet):
    queryset = User.objects.all()
    serializer_class = UserSerializer
    permission_classes = [permissions.IsAuthenticated]


class DeviceViewSet(viewsets.ModelViewSet):
    queryset = Device.objects.all()
    serializer_class = DeviceSerializer
    permission_classes = [permissions.IsAuthenticated]
