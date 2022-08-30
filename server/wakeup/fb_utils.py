import firebase_admin
import firebase_admin.exceptions
from firebase_admin import messaging


cred_obj = firebase_admin.credentials.Certificate('wakeup-43bcc-d292e59c9bac.json')
firebase_admin.initialize_app(cred_obj)


def send_alarm_to_device(device, alarm):
    fb_message = messaging.Message(data={'alarm': str(alarm.id), 'message': alarm.message or ''}, token=device.fb_token)
    try:
        messaging.send(fb_message)
    except (firebase_admin.exceptions.InvalidArgumentError, firebase_admin._messaging_utils.UnregisteredError):
        device.fb_token = None
        device.save()
        return False
    return True


def send_alarm_to_user(user, alarm):
    success = False
    for device in user.devices.filter(fb_token__isnull=False, active=True):
        success = success or send_alarm_to_device(device, alarm)
    return success
