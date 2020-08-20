import os
from django.apps import apps


def get_filename_ext(filepath):
    base_name = os.path.basename(filepath)
    name, ext = os.path.splitext(base_name)
    return name, ext


def get_unique_sku(seller, title, sku=None, no=0):
    if not sku is None:
        final_sku = sku
    else:
        final_sku = title.split(' ')[0] + str(no)
    qs_exists = apps.get_model('product', 'Product').objects.filter(sku=final_sku, seller=seller).exists()
    if qs_exists:
        return get_unique_sku(seller, title, no=no + 1)
    return final_sku


def getPksByName(className, arr, nm):
    tr = eval('[int(x.pk) for x in className.objects.filter(' + nm + '__in=arr)]')
    return tr
