from django import forms
from .models import Device


class WakeForm(forms.Form):
    device_id = forms.CharField(max_length=150, widget=forms.HiddenInput())
    message = forms.CharField(max_length=150)
