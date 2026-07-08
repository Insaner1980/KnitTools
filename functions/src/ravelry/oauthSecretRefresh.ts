import { ravelryClientId, ravelryClientSecret } from "../config";
import { refreshOAuth2AccessToken, type OAuthTokenRefreshRequest } from "./oauth2";

export function refreshRavelryAccessToken(refreshRequest: OAuthTokenRefreshRequest) {
  return refreshOAuth2AccessToken({
    ...refreshRequest,
    clientId: ravelryClientId.value(),
    clientSecret: ravelryClientSecret.value(),
  });
}
