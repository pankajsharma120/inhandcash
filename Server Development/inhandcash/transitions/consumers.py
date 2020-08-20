import json

from channels.db import database_sync_to_async
from channels.generic.websocket import AsyncWebsocketConsumer
from django.core.exceptions import ValidationError

from inhandcash.helpers import get_or_none
from transitions.models import Transition
from django.utils import timezone as dtimezone


class UpdateRoom(AsyncWebsocketConsumer):
    async def connect(self):
        self.user = self.scope['user']
        if not self.user.is_authenticated:
            self.close()
            return
        self.room_group_name = 'update_join_%s' % self.user.pk
        await self.channel_layer.group_add(
            self.room_group_name,
            self.channel_name
        )
        await self.accept()

    async def status(self, event):
        await self.send(text_data=json.dumps({
            'type': event['type'],
            'transition': event['transition'],
            'status': event['status']
        }))


class OfferRoom(AsyncWebsocketConsumer):
    @database_sync_to_async
    def get_trans(self):
        return get_or_none(Transition, pk=self.room_name)

    @database_sync_to_async
    def check_user(self):
        if self.user.is_authenticated and (self.user == self.trans.sender or self.user == self.trans.receiver):
            return True
        return False

    @database_sync_to_async
    def get_sender_receiver_name(self):
        return self.trans.sender.user_profile.name, self.trans.receiver.user_profile.name

    async def connect(self):
        self.room_name = self.scope['url_route']['kwargs']['room']
        self.trans = await self.get_trans()
        self.room_group_name = 'custom_join_%s' % self.room_name
        await self.channel_layer.group_add(
            self.room_group_name,
            self.channel_name
        )
        self.user = self.scope['user']

        is_user_ok = await self.check_user()
        if is_user_ok:
            time_diff = dtimezone.now() - self.trans.created_at
            remaining_time = 120 - time_diff.total_seconds()
            sender, receiver = await self.get_sender_receiver_name()
            message = {"minutes": remaining_time // 60, 'seconds': int(remaining_time % 60),
                       "amount": float(self.trans.amount), "sender": sender, "receiver": receiver}
            await self.accept()
            await self.send_prompt(message, "303")
        else:
            await self.close()

    @database_sync_to_async
    def try_to_accept_offer(self):
        trans = Transition.objects.get(pk=self.room_name)
        if trans.status == 'created':
            if self.user == trans.receiver:
                trans.status = 'success'
                trans.save()
                return "Successfully accepted.", '202'
        else:
            return "Request can't be proceed", '403'

    @database_sync_to_async
    def try_cancel_offer(self):
        trans = Transition.objects.get(pk=self.room_name)
        if trans.status == 'created':
            if self.user == trans.sender:
                trans.status = 'canceled_by_sender'
                trans.save()
                return "Canceled by Sender", '401'
            elif self.user == trans.receiver:
                trans.status = 'canceled_by_receiver'
                trans.save()
                return "Canceled by Receiver", '402'
            else:
                raise ValidationError("Unauthorized user!")
        else:
            return "Request can't be proceed", '403'

    async def send_prompt(self, prompt, status):
        await self.channel_layer.group_send(
            self.room_group_name,
            {
                'type': 'status',
                'message': prompt,
                'status': status
            })

    async def status(self, event):
        await self.send(text_data=json.dumps({
            'type': event['type'],
            'message': event['message'],
            'status': event['status']
        }))

    async def receive(self, text_data=None, bytes_data=None):
        data = json.loads(text_data)
        print(data, type(data))
        if data == 400:
            message, status = await self.try_cancel_offer()
            await self.send_prompt(message, status)
            print(message, status)
        if data == 202:
            message, status = await self.try_to_accept_offer()
            await self.send_prompt(message, status)
            print(message, status)
        return await super().receive(text_data, bytes_data)
