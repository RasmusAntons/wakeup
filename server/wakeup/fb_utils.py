import firebase_admin
from firebase_admin import messaging


cred_obj = firebase_admin.credentials.Certificate('wakeup-43bcc-d292e59c9bac.json')
firebase_admin.initialize_app(cred_obj)


def wake_device(device, message):
    fb_message = messaging.Message(data={'message': message}, token=device.fb_token)
    res = messaging.send(fb_message)
    print('sent message:', res)
