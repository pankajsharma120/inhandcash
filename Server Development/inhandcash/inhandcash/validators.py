from django.core.validators import RegexValidator
from django.core.exceptions import ValidationError

alphanumeric_plus = RegexValidator(r'^[a-zA-Z0-9][ A-Za-z0-9_-]*$', 'Aww! looks critical.')


def file_size(value):
    limit = 2 * 1024 * 1024
    if value.size > limit:
        raise ValidationError('File too large. Size should not exceed 2 MiB.')
