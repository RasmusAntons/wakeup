from oauth2_provider.oauth2_validators import OAuth2Validator
from oauth2_provider.scopes import SettingsScopes

class WakeupOAuth2Validator(OAuth2Validator):
    def get_userinfo_claims(self, request):
        claims = super().get_userinfo_claims(request)
        if 'profile' in request.scopes:
            claims['username'] = request.user.username
        return claims
