import urllib.request
import urllib.request


def sendSMS(number,message):
    print(message)
    apikey = 'vw1f5OGZFLc-feF7xBVsw4e47jLosTU0bgQlnsaWhd'
    data =  urllib.parse.urlencode({'apikey': apikey, 'numbers': number,
        'message' : message, 'sender': 'BEKAIM'})
    data = data.encode('utf-8')
    request = urllib.request.Request("https://api.textlocal.in/send/?")
    f = urllib.request.urlopen(request, data)
    fr = f.read()
    print(fr)
    return(fr)


def get_or_none(model, *args, **kwargs):
    try:
        return model.objects.get(*args, **kwargs)
    except model.DoesNotExist:
        return None



