from channels.auth import AuthMiddlewareStack
from rest_framework.authtoken.models import Token
from django.contrib.auth.models import AnonymousUser
from channels.db import database_sync_to_async
from inhandcash.helpers import get_or_none
from users.models import User
import asyncio
import threading


class TokenAuthMiddleware:
    """
    Token authorization middleware for Django Channels 2
    """

    def __init__(self, inner):
        self.inner = inner

    @database_sync_to_async
    def get_user(self, token):
        self.user = get_or_none(User, auth_token=token)

    class GetUserThread(threading.Thread):
        def __init__(self, outer, prompt):
            self.outer = outer
            self.token = prompt
            threading.Thread.__init__(self)

        def run(self):
            asyncio.run(self.outer.get_user(self.token))

    def __call__(self, scope):
        headers = dict(scope['headers'])
        if b'sec-websocket-protocol' in headers:
            try:
                token_name, token_key = headers[b'sec-websocket-protocol'].decode().split()
                token_name, token_key = token_name.strip(), token_key.strip()
                if token_name == 'access_token,':
                    sp = TokenAuthMiddleware.GetUserThread(self, token_key)
                    sp.start()
                    sp.join()
                    if self.user:
                        scope['user'] = self.user
                    else:
                        scope['user'] = AnonymousUser()
            except Token.DoesNotExist:
                scope['user'] = AnonymousUser()

        elif b'authorization' in headers:
            try:
                token = headers[b'authorization'].decode()
                sp = TokenAuthMiddleware.GetUserThread(self, token)
                sp.start()
                sp.join()
                if self.user:
                    scope['user'] = self.user
                else:
                    scope['user'] = AnonymousUser()
            except Token.DoesNotExist:
                scope['user'] = AnonymousUser()
        return self.inner(scope)


TokenAuthMiddlewareStack = lambda inner: TokenAuthMiddleware(AuthMiddlewareStack(inner))
