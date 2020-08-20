import threading
import time
from decimal import Decimal

import channels
from channels import layers
from asgiref.sync import async_to_sync
from django.conf import settings
from django.core.validators import MinValueValidator
from django.db import models

from django.db.models.signals import post_save
from django.dispatch import receiver
from django.urls import reverse

TRANSITION_STATES = (
    ("created", "Created"),
    ("success", "Success"),
    ("canceled_by_sender", "Canceled by Sender"),
    ("canceled_by_receiver", "Canceled by Receiver"),
    ("timeout", "Timeout")
)


class MakeTimeoutThread(threading.Thread):
    def __init__(self, pk):
        self.pk = pk
        threading.Thread.__init__(self)

    def run(self):
        time.sleep(settings.OFFER_TIME)
        now_instance = Transition.objects.get(pk=self.pk)

        if now_instance.status == 'created':
            now_instance.status = 'timeout'
            now_instance.save()

            group_name = 'custom_join_%s' % str(self.pk)

            channel_layer = channels.layers.get_channel_layer()

            async_to_sync(channel_layer.group_send)(
                group_name,
                {
                    'type': 'status',
                    'message': "TimeOut",
                    'status': '403'
                }
            )


class Transition(models.Model):
    sender = models.ForeignKey(settings.AUTH_USER_MODEL, related_name='transition_sender', on_delete=models.CASCADE)
    receiver = models.ForeignKey(settings.AUTH_USER_MODEL, related_name='transition_receiver', on_delete=models.CASCADE)
    status = models.CharField(choices=TRANSITION_STATES, max_length=120, default='created')
    amount = models.DecimalField(decimal_places=2, blank=False, null=False,
                                 max_digits=10, validators=[MinValueValidator(Decimal('1.00'))])
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return str(self.sender) + " " + str(self.receiver) + " " + str(self.status)

    def get_room_url(self):
        return reverse("offer_room", kwargs={"room": self.pk}, urlconf=settings.TRANSITION_CHANNELS_URLCONF)

    def run_timer(self):
        mt = MakeTimeoutThread(self.pk)
        mt.start()

    def send_update(self, user):
        group_name = 'update_join_%s' % str(user.pk)
        channel_layer = layers.get_channel_layer()
        from transitions.serializers import TransitionDetailSerializer
        async_to_sync(channel_layer.group_send)(
            group_name,
            {
                'type': 'status',
                'transition': TransitionDetailSerializer(instance=self).data,
                'status': '403'
            }
        )


@receiver(post_save, sender=Transition)
def run_post_timer(sender, instance=None, created=False, **kwargs):
    if created:
        instance.run_timer()
    else:
        if not instance.status in {"created", "timeout"}:
            instance.send_update(instance.sender)
            instance.send_update(instance.receiver)
