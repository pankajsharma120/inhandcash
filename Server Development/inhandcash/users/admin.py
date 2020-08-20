from django.contrib import admin
from users.models import User, UserProfile, UserCreateFault

# Register your models here.


admin.site.register(User)
admin.site.register(UserProfile)
admin.site.register(UserCreateFault)