import random
from io import BytesIO

import pyqrcode
import requests
from PIL import Image
from django.conf import settings
from django.contrib.auth.base_user import BaseUserManager
from django.contrib.auth.models import AbstractUser
from django.core.files.base import ContentFile
from django.db import models
from django.db.models.signals import post_save, pre_save
from django.dispatch import receiver
from phonenumber_field.modelfields import PhoneNumberField
from rest_framework.authtoken.models import Token

# Create your models here.
from inhandcash.utils import get_filename_ext
from inhandcash.validators import file_size
from users.utils import unique_username_generator


def upload_qr_path(instance, filename):
    return "users/qrs/{filename}".format(filename=filename)


GENDER_CHOICES = (
    ('Male', 'Male'),
    ('Female', 'Female'),
    ('Other', 'Other'),
)


def upload_profile_path(instance, filename):
    new_filename = random.randint(1, 3910209312)
    name, ext = get_filename_ext(filename)
    final_filename = '{new_filename}{ext}'.format(new_filename=new_filename, ext=ext)
    return "accounts/media/images/{user}/profile_images/{final_filename}".format(
        new_filename=new_filename,
        final_filename=final_filename, user=instance)


class UserCreateFault(models.Model):
    phonenumber = PhoneNumberField(verbose_name="phonenumber", max_length=13, blank=False, null=False, unique=True)
    datetime = models.DateTimeField(null=False, blank=False)
    pin = models.CharField(max_length=6, null=True, blank=False)

    def __str__(self):
        return str(self.phonenumber)


class User(AbstractUser):
    pass


class UserProfile(models.Model):
    vpa = models.CharField(max_length=225, null=True, blank=True)
    fcm_token = models.CharField(max_length=225, null=True, blank=True)
    qr_code = models.ImageField(null=True, blank=True, upload_to=upload_qr_path)
    user = models.OneToOneField(User, related_name='user_profile', on_delete=models.CASCADE)
    phonenumber = PhoneNumberField(verbose_name="phonenumber", max_length=13, blank=False, null=False, unique=True)
    name = models.CharField(max_length=120, null=True, blank=False)
    gender = models.CharField(max_length=50, choices=GENDER_CHOICES, null=True, blank=True)
    profile_image = models.ImageField(upload_to=upload_profile_path, default='media/images/defaults/profile_image.png',
                                      validators=[file_size])
    pin = models.CharField(max_length=6, null=True, blank=False)
    pin_datetime = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return str(self.phonenumber)


def generate_qrcode(sender, instance, created, *args, **kwargs):
    if not instance.qr_code:
        qrobj = pyqrcode.create('inhandcash://' + instance.vpa)
        with open('test.png', 'wb') as f:
            qrobj.png(f, scale=10)
            # Now open that png image to put the logo
        img = Image.open('test.png')
        img = img.convert("RGBA")
        width, height = img.size
        # How big the logo we want to put in the qr code png
        logo_size = 50
        # Open the logo image
        response = requests.get('https://dzd85t08vmtip.cloudfront.net/static/images/logo/72.png')
        logo = Image.open(BytesIO(response.content))
        # Calculate xmin, ymin, xmax, ymax to put the logo
        xmin = ymin = int((width / 2) - (logo_size / 2))
        xmax = ymax = int((width / 2) + (logo_size / 2))
        # resize the logo as calculated
        logo = logo.resize((xmax - xmin, ymax - ymin))
        # put the logo in the qr code
        img.paste(logo, (xmin, ymin, xmax, ymax))
        temp = BytesIO()
        img.save(temp, 'PNG')
        temp.seek(0)
        instance.qr_code.save('qr.png', ContentFile(temp.read()), save=True)
        temp.close()


post_save.connect(generate_qrcode, sender=UserProfile)


def pre_user_create(sender, instance, *args, **kwargs):
    if not instance.vpa:
        instance.vpa = instance.phonenumber.as_national.replace(" ", "").lstrip("0") + "@ihcupi"
    if not hasattr(instance, 'user'):
        username = unique_username_generator(instance, User)
        instance.user = User.objects.create_user(username)


@receiver(post_save, sender=settings.AUTH_USER_MODEL)
def create_auth_token(sender, instance=None, created=False, **kwargs):
    if created:
        Token.objects.create(user=instance)


pre_save.connect(pre_user_create, sender=UserProfile)
