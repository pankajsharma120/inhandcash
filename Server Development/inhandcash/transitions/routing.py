from django.conf.urls import url

from . import consumers

urlpatterns = [
    url(r'^ws/offer-room/(?P<room>[^/]+)/$', consumers.OfferRoom, name="offer_room"),
    url(r'^ws/update-room/$', consumers.UpdateRoom, name="offer_room"),
]
