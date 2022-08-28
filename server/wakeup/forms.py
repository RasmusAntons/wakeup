from django import forms
from django_select2 import forms as s2forms
from .models import User, Device, Alarm


class WakeForm(forms.ModelForm):
    class Meta:
        model = Alarm
        fields = ['target', 'message']
        widgets = {
            'target': forms.HiddenInput()
        }


class AddFriendForm(forms.Form):
    user = forms.ModelChoiceField(User.objects.all(), widget=s2forms.ModelSelect2Widget(search_fields=['username__istartswith']))

    def __init__(self, *args, **kwargs):
        self.request = kwargs.pop('request', None)
        super(AddFriendForm, self).__init__(*args, **kwargs)

    def clean(self):
        cleaned_data = super().clean()
        user = cleaned_data.get('user')
        if user == self.request.user:
            self.add_error('user', 'cannot add yourself')
        elif user in self.request.user.friends.all():
            self.add_error('user', 'user is already added')

class RemoveFriendForm(forms.Form):
    user = forms.ModelChoiceField(User.objects.all(), widget=forms.HiddenInput())

    def __init__(self, *args, **kwargs):
        self.request = kwargs.pop('request', None)
        super(RemoveFriendForm, self).__init__(*args, **kwargs)

    def clean(self):
        cleaned_data = super().clean()
        user = cleaned_data.get('user')
        if user not in self.request.user.friends.all():
            self.add_error('user', 'user is not added')
