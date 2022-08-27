package de.rasmusantons.wakeup;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.AuthorizationServiceDiscovery;
import net.openid.appauth.CodeVerifierUtil;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class LoginActivity extends AppCompatActivity {

    // TODO: Configure this to your environment
    // IDP Configuration

    // The OIDC issuer from which the configuration will be discovered. This is your base PingFederate server URL.
    private static final String OIDC_ISSUER = "https://wakeup-dev.3po.ch/o";

    // The OAuth client ID. This is configured in your PingFederate administration console under OAuth Settings > Client Management.
    // The example "ac_client" from the OAuth playground can be used here.
    private static final String OIDC_CLIENT_ID = "EQVoPEbNN4x9leJ2kjjIbSLRmWUTcMdANjVWtTOf";

    // The redirect URI that PingFederate will send the user back to after the authorization step. To avoid
    // collisions, this should be a reverse domain formatted string. You must define this in your OAuth client in PingFederate.
    private static final String OIDC_REDIRECT_URI = "de.rasmusantons.wakeup://oidc_callback";

    // The scope to send in the request
    private static final String OIDC_SCOPE = "openid profile";

    // Other constants
    // tag for logging
    private static final String TAG = "LoginActivity";
    // key for authorization state
    private static final String KEY_AUTH_STATE = "de.rasmusantons.wakeup.authState";
    private static final String KEY_USER_INFO = "userInfo";
    private static final String EXTRA_AUTH_SERVICE_DISCOVERY = "authServiceDiscovery";

    private static final int BUFFER_SIZE = 1024;

    public static AuthState mAuthState;
    private AuthorizationService mAuthService;
    public static JSONObject mUserInfoJson;
    private EditText infoText;
    private static final Lock authorizationLock = new ReentrantLock();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // TODO: DEV ONLY! Remove before deploying in production
        // For simplicity of the demo, all actions are performed on the main thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mAuthService = new AuthorizationService(this);
        infoText = findViewById(R.id.info_text);
        // Check for authorization callback
        Intent intent = getIntent();
        if (intent != null) {
            Log.d(TAG, "Intent received");
            if (mAuthState == null) {
                // Parse the authorization response
                AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
                AuthorizationException ex = AuthorizationException.fromIntent(intent);
                if (response != null || ex != null) {
                    mAuthState = new AuthState(response, ex);
                }
                if (response != null) {
                    Log.d(TAG, "Received AuthorizationResponse.");
                    exchangeAuthorizationCode(response);
                } else if (ex != null){
                    Log.i(TAG, "Authorization failed: " + ex);
                } else {
                    requestAuthorization();
                }
            } else {
                // if (mAuthState == null)
                //     requestAuthorization();
                // else
                    finish();
            }
        } else {
            Log.d(TAG, "NO Intent received");
        }

        Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(viw -> this.requestAuthorization());
        Button infoButton = findViewById(R.id.info_button);
        infoButton.setOnClickListener(view -> {
            getUserinfo();
            Log.i(TAG, String.format("%s", mAuthState.jsonSerialize()));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAuthService.dispose();
    }

    // button action handlers
    public void requestAuthorization() {
        infoText.append("requesting authorization\n");
        if (!authorizationLock.tryLock())
            Log.e(TAG, "authorization lock was already locked");
        final AuthorizationServiceConfiguration.RetrieveConfigurationCallback retrieveCallback =
                new AuthorizationServiceConfiguration.RetrieveConfigurationCallback() {
                    @Override
                    public void onFetchConfigurationCompleted(
                            @Nullable AuthorizationServiceConfiguration serviceConfiguration,
                            @Nullable AuthorizationException ex) {
                        if (ex != null) {
                            Log.w(TAG, "Failed to retrieve configuration for " + OIDC_ISSUER, ex);
                        } else {
                            Log.d(TAG, "configuration retrieved for " + OIDC_ISSUER
                                    + ", proceeding");
                            authorize(serviceConfiguration);
                        }
                        finish();
                    }
                };
        String discoveryEndpoint = OIDC_ISSUER + "/.well-known/openid-configuration/";
        AuthorizationServiceConfiguration.fetchFromUrl(Uri.parse(discoveryEndpoint), retrieveCallback);
    }

    public void refreshToken() {
        performTokenRequest(mAuthState.createTokenRefreshRequest());
    }

    public void getUserinfo() {
        Log.d(TAG, "Calling Userinfo...");
        infoText.append("requesting userinfo\n");
        if (mAuthState.getAuthorizationServiceConfiguration() == null) {
            Log.e(TAG, "Cannot make userInfo request without service configuration");
        }
        mAuthState.performActionWithFreshTokens(mAuthService, new AuthState.AuthStateAction() {
            @Override
            public void execute(String accessToken, String idToken, AuthorizationException ex) {
                if (ex != null) {
                    Log.e(TAG, "Token refresh failed when fetching user info");
                    return;
                }
                AuthorizationServiceDiscovery discoveryDoc = getDiscoveryDocFromIntent(getIntent());
                if (discoveryDoc == null) {
                    throw new IllegalStateException("no available discovery doc");
                }
                URL userInfoEndpoint;
                try {
                    userInfoEndpoint = new URL(discoveryDoc.getUserinfoEndpoint().toString());
                } catch (MalformedURLException urlEx) {
                    Log.e(TAG, "Failed to construct user info endpoint URL", urlEx);
                    return;
                }
                InputStream userInfoResponse = null;
                try {
                    HttpURLConnection conn = (HttpURLConnection) userInfoEndpoint.openConnection();
                    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                    conn.setInstanceFollowRedirects(false);
                    userInfoResponse = conn.getInputStream();
                    String response = readStream(userInfoResponse);
                    updateUserInfo(new JSONObject(response));
                } catch (IOException ioEx) {
                    Log.e(TAG, "Network error when querying userinfo endpoint", ioEx);
                } catch (JSONException jsonEx) {
                    Log.e(TAG, "Failed to parse userinfo response");
                } finally {
                    if (userInfoResponse != null) {
                        try {
                            userInfoResponse.close();
                        } catch (IOException ioEx) {
                            Log.e(TAG, "Failed to close userinfo response stream", ioEx);
                        }
                    }
                }
            }
        });
    }

    // Kick off an authorization request
    private void authorize(AuthorizationServiceConfiguration authServiceConfiguration) {
        // NOTE: Required for PingFederate 8.1 and below for the .setCodeVerifier() option below
        // to generate "plain" code_challenge_method these versions of PingFederate do not support
        // S256 PKCE.
        String codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier();

        // OPTIONAL: Add any additional parameters to the authorization request
        HashMap<String, String> additionalParams = new HashMap<>();
        // additionalParams.put("acr_values", "urn:acr:form");

        AuthorizationRequest authRequest = new AuthorizationRequest.Builder(
                authServiceConfiguration,
                OIDC_CLIENT_ID,
                ResponseTypeValues.CODE,
                Uri.parse(OIDC_REDIRECT_URI))
                .setScope(OIDC_SCOPE)
                .setCodeVerifier(codeVerifier, codeVerifier, "plain")
                .setAdditionalParameters(additionalParams)
                .build();

        Log.d(TAG, "Making auth request to " + authServiceConfiguration.authorizationEndpoint);
        mAuthService.performAuthorizationRequest(
                authRequest,
                createPostAuthorizationIntent(
                        this.getApplicationContext(),
                        authRequest,
                        authServiceConfiguration.discoveryDoc));
    }

    private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
        performTokenRequest(authorizationResponse.createTokenExchangeRequest());
    }

    private void performTokenRequest(TokenRequest tokenRequest) {
        mAuthService.performTokenRequest(tokenRequest, this::receivedTokenResponse);
    }


    private void receivedTokenResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
        Log.d(TAG, "Token request complete");
        mAuthState.update(tokenResponse, authException);
        if (tokenResponse != null) {
            SharedPreferences authPrefs = getSharedPreferences("auth", MODE_PRIVATE);
            authPrefs.edit().putString(KEY_AUTH_STATE, mAuthState.jsonSerializeString()).apply();
            this.getUserinfo();
        }
    }

    private PendingIntent createPostAuthorizationIntent(
            @NonNull Context context,
            @NonNull AuthorizationRequest request,
            @Nullable AuthorizationServiceDiscovery discoveryDoc) {
        Intent intent = new Intent(context, this.getClass());
        if (discoveryDoc != null) {
            intent.putExtra(EXTRA_AUTH_SERVICE_DISCOVERY, discoveryDoc.docJson.toString());
        }

        return PendingIntent.getActivity(context, request.hashCode(), intent, 0);
    }

    private AuthorizationServiceDiscovery getDiscoveryDocFromIntent(Intent intent) {
        if (!intent.hasExtra(EXTRA_AUTH_SERVICE_DISCOVERY)) {
            return null;
        }
        String discoveryJson = intent.getStringExtra(EXTRA_AUTH_SERVICE_DISCOVERY);
        try {
            return new AuthorizationServiceDiscovery(new JSONObject(discoveryJson));
        } catch (JSONException | AuthorizationServiceDiscovery.MissingArgumentException  ex) {
            throw new IllegalStateException("Malformed JSON in discovery doc");
        }
    }

    private void updateUserInfo(final JSONObject jsonObject) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                infoText.append(String.format("received userinfo: %s\n", jsonObject));
                Log.d(TAG, String.format("received userinfo: %s\n", jsonObject));
                mUserInfoJson = jsonObject;
                if (jsonObject != null) {
                    SharedPreferences authPrefs = getSharedPreferences("auth", MODE_PRIVATE);
                    authPrefs.edit().putString(KEY_USER_INFO, jsonObject.toString()).apply();
                    authorizationLock.unlock();

                    SharedPreferences firebasePreferences = getSharedPreferences("firebase", MODE_PRIVATE);
                    if (firebasePreferences.getBoolean("fb_token_updated", false)) {
                        String fbToken = firebasePreferences.getString("fb_token", null);
                        Log.i(TAG, String.format("need to update firebase token: %s", fbToken));
                        Executors.newSingleThreadExecutor().execute(() -> {
                            WakeupApi.updateFbToken(LoginActivity.this, fbToken);
                            firebasePreferences.edit()
                                    .putBoolean("fb_token_updated", false)
                                    .apply();
                        });
                    }

                    finish();
                }
            }
        });
    }

    private static String readStream(InputStream stream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        char[] buffer = new char[BUFFER_SIZE];
        StringBuilder sb = new StringBuilder();
        int readCount;
        while ((readCount = br.read(buffer)) != -1) {
            sb.append(buffer, 0, readCount);
        }
        return sb.toString();
    }

    public static boolean deleteLogin(Context context) {
        if (!authorizationLock.tryLock())
            return false;
        mAuthState = null;
        mUserInfoJson = null;
        SharedPreferences authPrefs = context.getSharedPreferences("auth", MODE_PRIVATE);
        authPrefs.edit().remove(KEY_AUTH_STATE).remove(KEY_USER_INFO).apply();
        authorizationLock.unlock();
        return true;
    }

    public static boolean restoreLogin(Context context) {
        if (!authorizationLock.tryLock())
            return false;
        boolean success = mAuthState != null && mAuthState.isAuthorized();
        if (!success) {
            SharedPreferences authPrefs = context.getSharedPreferences("auth", MODE_PRIVATE);
            String stateJson = authPrefs.getString(KEY_AUTH_STATE, null);
            if (stateJson != null) {
                try {
                    mAuthState = AuthState.jsonDeserialize(stateJson);
                } catch (JSONException ex) {
                    Log.e(TAG, "Malformed authorization JSON saved", ex);
                }
                String userInfoJson = authPrefs.getString(KEY_USER_INFO, null);
                Log.i(TAG, "found state");
                if (userInfoJson != null) {
                    Log.i(TAG, "found user info: " + userInfoJson);
                    try {
                        mUserInfoJson = new JSONObject(userInfoJson);
                    } catch (JSONException ex) {
                        Log.e(TAG, "Failed to parse saved user info JSON", ex);
                    }
                }
                success = true;
            }
        }
        authorizationLock.unlock();
        return success;
    }
}
