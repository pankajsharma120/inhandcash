import django
django.setup()

from channels.routing import ProtocolTypeRouter
from channels.routing import ProtocolTypeRouter, URLRouter
from inhandcash.token_auth import TokenAuthMiddlewareStack
import transitions.routing

application = ProtocolTypeRouter({
    # Empty for now (http->django views is added by default)
    'websocket': TokenAuthMiddlewareStack(
            URLRouter(
                transitions.routing.urlpatterns
            )
        ),
})