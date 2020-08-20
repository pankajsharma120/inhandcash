from django.db.models import Q
from django.shortcuts import render, get_object_or_404
from rest_framework import status, generics, mixins
from rest_framework.authentication import TokenAuthentication
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from transitions.models import Transition
from transitions.serializers import TransitionDetailSerializer
from users.models import User, UserProfile
from pyfcm import FCMNotification


# Create your views here.


class GetTransitionsList(generics.GenericAPIView, mixins.ListModelMixin):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]
    serializer_class = TransitionDetailSerializer

    def get_queryset(self, *args, **kwargs):
        filter_query = self.request.GET.get("type", "all")
        trans = Transition.objects.filter(
            ~Q(status='created') & (Q(sender=self.request.user) | Q(receiver=self.request.user))).order_by(
            '-created_at')
        if filter_query == "success":
            trans = trans.filter(status="success")
        elif filter_query == "failed":
            trans = trans.filter(~Q(status='success'))
        return trans

    def get(self, request, *args, **kwargs):
        queryset = self.filter_queryset(self.get_queryset())
        page = self.paginate_queryset(queryset)
        if page is not None:
            serializer = self.get_serializer(page, many=True)
            result = self.get_paginated_response(serializer.data)
            data = result.data
        else:
            serializer = self.get_serializer(queryset, many=True)
            data = serializer.data
        payload = {
            'return_code': '0000',
            'return_message': 'Success',
            'data': data
        }
        return Response(data, status=status.HTTP_200_OK)


class CreateNewOffer(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        userprofile = get_object_or_404(UserProfile, vpa=request.data.get("vpa"))
        receiver = get_object_or_404(User, user_profile=userprofile)
        transition = Transition.objects.create(sender=request.user, receiver=receiver,
                                               amount=request.data.get("amount"))
        push_service = FCMNotification(
            api_key='AAAAYRL8BhU:APA91bHlXDXlUQETyZrH1V-3k_vlIy1MkHaa6XRKdF2KNDZeoHoXbNVJhzuTLvnhQtVAsyxUvB8HeKeihSRJCGBgkV04dvJ6Scq2aZNajbvAE5UbEOluUETKoIAsJDbuWcC9vJknG6F9')

        registration_id = userprofile.fcm_token
        if not registration_id:
            return Response({"errors": {"receiver": "Some problem with receivers end, please ask to reinstall app."}},
                            status=status.HTTP_400_BAD_REQUEST)
        message_title = "Offer " + str(transition.amount)
        message_body = str(transition.amount) + " offered from " + request.user.user_profile.name
        data_message = {"title": "Offer " + str(transition.amount),
                        'message': str(transition.amount) + " offered from " + request.user.user_profile.name,
                        "room_url": transition.get_room_url(), "to_activity": "offerSplashActivity"}
        result = push_service.notify_single_device(message_title=message_title, message_body=message_body,
                                                   registration_id=registration_id,
                                                   data_message=data_message, badge="offer")
        return Response({"room_url": transition.get_room_url(), "amount": transition.amount},
                        status=status.HTTP_202_ACCEPTED)
