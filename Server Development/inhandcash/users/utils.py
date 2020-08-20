import random
import string


def unique_username_generator(instance, klass, id=None):
    if id is not None:
        final_id = id
    else:
        final_id = ''.join(random.SystemRandom().choice(string.ascii_uppercase + string.digits) for _ in range(6))
    qs_exists = klass.objects.filter(username=final_id).exists()
    if qs_exists:
        id = ''.join(random.SystemRandom().choice(string.ascii_uppercase + string.digits) for _ in range(6))
        return unique_username_generator(id=id)
    return final_id
