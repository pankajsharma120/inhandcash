import random

from django.utils import timezone as dtimezone
from phonenumber_field.phonenumber import to_python
from rest_framework import status, generics
from rest_framework.authentication import TokenAuthentication
from rest_framework.parsers import MultiPartParser, FormParser, FileUploadParser, JSONParser
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from inhandcash.helpers import get_or_none, sendSMS
from users.models import UserCreateFault, UserProfile
from users.serializers import UserProfilePhnSerializer, UserProfileSerializer, UserRegisterSerilizer, UserInfoSerializer


# Create your views here.

class GetMyInfo(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return Response(UserInfoSerializer(self.request.user.user_profile).data, status=status.HTTP_202_ACCEPTED)


class SetFcm(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        fcm = request.data.get('fcm')
        userprofile = request.user.user_profile
        userprofile.fcm_token = fcm
        userprofile.save()
        return Response({}, status=status.HTTP_202_ACCEPTED)


class UpdateProfile(generics.UpdateAPIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]
    serializer_class = UserProfileSerializer
    parser_classes = [JSONParser, MultiPartParser, FormParser, FileUploadParser]

    def update(self, request, *args, **kwargs):
        instance = request.user.user_profile
        print(request.data)
        serializer = UserRegisterSerilizer(instance, data=request.data)
        if not serializer.is_valid():
            print(serializer.errors)
            return Response({"errors": serializer.errors}, status=status.HTTP_400_BAD_REQUEST)
        serializer.save()
        return Response({}, status.HTTP_202_ACCEPTED)


class LoginORCreate(APIView):
    authentication_classes = []
    permission_classes = []

    def get_sms_messge(self, new_pin):
        return "<#> {code} is your account confirmation code. Expires in 10 minutes. NEVER SHARE THIS " \
               "CODE.\n49CJLcsfXoB".format(code=new_pin)

    def handel_timeout(self, user, new_pin):
        user.pin = new_pin
        user.pin_datetime = dtimezone.now()
        user.save()
        errors = {"pin": "Pin expired, we have resend the pin, please enter new one !"}
        sendSMS(user.phonenumber.as_international.replace(" ", ''), self.get_sms_messge(new_pin))
        return Response({"errors": errors}, status.HTTP_400_BAD_REQUEST)

    def post(self, request):
        serializer = UserProfilePhnSerializer(data=request.data)
        phn = request.data.get('phonenumber')
        pin = request.data.get('pin')
        fault_user = get_or_none(UserCreateFault, phonenumber=phn)
        real_user = get_or_none(UserProfile, phonenumber=phn)
        new_pin = str(random.randint(100000, 999999))
        print(phn, to_python(phn))
        if not phn or not to_python(phn).is_valid():
            return Response({"errors": {"phonenumber": "Not a valid number"}}, status=status.HTTP_400_BAD_REQUEST)

        if real_user:
            if not pin:
                real_user.pin = new_pin
                real_user.pin_datetime = dtimezone.now()
                real_user.save()
                sendSMS(real_user.phonenumber.as_international.replace(" ", ''), self.get_sms_messge(new_pin))
                return Response(request.data, status=status.HTTP_206_PARTIAL_CONTENT)
            else:
                time_diff = dtimezone.now() - real_user.pin_datetime
                time_diff = time_diff.total_seconds()
                if time_diff > 1200:
                    return self.handel_timeout(real_user, new_pin);
                elif real_user.pin != pin:
                    errors = {"pin": "Wrong Pin, please enter correct pin !"}
                    return Response({"errors": errors}, status.HTTP_400_BAD_REQUEST)
                elif real_user.pin == pin:
                    data = {}
                    data['token'] = real_user.user.auth_token.key
                    if not real_user.name:
                        return Response(data, status=status.HTTP_201_CREATED)
                    data['is_profiled'] = bool(real_user.name)
                    return Response(data, status=status.HTTP_208_ALREADY_REPORTED)
        else:
            if fault_user is None:
                fault_user = UserCreateFault.objects.create(phonenumber=phn, pin=new_pin, datetime=dtimezone.now())
                sendSMS(fault_user.phonenumber.as_international.replace(" ", ''), self.get_sms_messge(new_pin))
                return Response(request.data, status=status.HTTP_206_PARTIAL_CONTENT)
            elif fault_user and not pin:
                fault_user.pin = new_pin
                fault_user.datetime = dtimezone.now()
                fault_user.save()
                sendSMS(fault_user.phonenumber.as_international.replace(" ", ''), self.get_sms_messge(new_pin))
                return Response(request.data, status=status.HTTP_206_PARTIAL_CONTENT)
            elif fault_user and pin:
                time_diff = dtimezone.now() - fault_user.datetime
                time_diff = time_diff.total_seconds()
                print(time_diff)
                if time_diff > 1200:
                    return self.handel_timeout(fault_user, new_pin);
                elif fault_user.pin != pin:
                    errors = {"pin": "Wrong Pin, please enter correct pin !"}
                    return Response({"errors": errors}, status.HTTP_400_BAD_REQUEST)
                elif fault_user.pin == pin:
                    if not serializer.is_valid():
                        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
                    user = serializer.save()
                    fault_user.delete()
                    data = {}
                    data['token'] = user.user.auth_token.key
                    return Response(data, status=status.HTTP_201_CREATED)
            else:
                errors = {"pin": "Please enter pin !"}
                return Response({"errors": errors}, status.HTTP_400_BAD_REQUEST)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class GetVpaDetails(APIView):
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request, *args, **kwargs):
        vpa = self.kwargs.get('vpa')
        userprofile = get_or_none(UserProfile, vpa=vpa)
        if userprofile:
            return Response({'vpa': userprofile.vpa, 'name': userprofile.name}, status=status.HTTP_202_ACCEPTED)
        return Response({"errors": {"vpa": ['Invalid UPI address']}}, status=status.HTTP_400_BAD_REQUEST)
