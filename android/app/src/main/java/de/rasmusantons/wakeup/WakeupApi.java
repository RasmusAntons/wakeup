package de.rasmusantons.wakeup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class WakeupApi {
    private static final String TAG = "WakeupApi";
    private final Context context;
    private final Uri baseUri;


    public WakeupApi(Context context) {
        this.context = context;
        String wakeupServerUrl = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.wakeup_server_url), null);
        baseUri = Uri.parse(wakeupServerUrl);
    }


    @Nullable
    private String apiCall(URL url, String method, @Nullable String body) {
        try {
            if (LoginActivity.mAuthState == null)
                return null;
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            String accessToken = LoginActivity.mAuthState.getAccessToken();
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoInput(true);
            if (method != null && !"GET".equals(method))
                connection.setRequestMethod(method);
            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(body.length());
                OutputStream requestStream = connection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(requestStream, StandardCharsets.UTF_8));
                writer.write(body);
                writer.flush();
                writer.close();
                requestStream.close();
            }
            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
                InputStream responseBody = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(responseBody, StandardCharsets.UTF_8);
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int n;
                while ((n = reader.read(buffer, 0, 1024)) != -1) {
                    sb.append(buffer, 0, n);
                }
                return sb.toString();
            } else {
                Log.e(TAG, "accessToken: " + accessToken);
                Log.e(TAG, LoginActivity.mAuthState.jsonSerializeString());
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    private String apiGet(URL url) {
        return apiCall(url, "GET", null);
    }

    @Nullable
    private String apiPost(URL url, String body) {
        return apiCall(url, "POST", body);
    }

    @Nullable
    private JSONArray getJSONArray(URL url) {
        String apiResponse = apiGet(url);
        if (apiResponse == null)
            return null;
        try {
            return new JSONArray(apiResponse);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    public List<JSONObject> getDevices() {
        try {
            Uri.Builder uriBuilder = baseUri.buildUpon().appendPath("api").appendPath("devices");
            URL devicesEndpoint = new URL(uriBuilder.toString());
            JSONArray devices = getJSONArray(devicesEndpoint);
            if (devices == null)
                return null;
            ArrayList<JSONObject> result = new ArrayList<>(devices.length());
            for (int i = 0; i < devices.length(); ++i)
                result.add(devices.getJSONObject(i));
            return result;
        } catch (MalformedURLException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    public String registerNewDevice(String androidId, @Nullable String fbToken) {
        String ownUserId;
        try {
            ownUserId = LoginActivity.mUserInfoJson.getString("sub");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        JSONObject newDevice = new JSONObject();
        try {
            newDevice.put("owner", ownUserId);
            newDevice.put("android_id", androidId);
            newDevice.put("name", Build.MODEL);
            if (fbToken != null)
                newDevice.put("fb_token", fbToken);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        Uri.Builder uriBuilder = baseUri.buildUpon()
                .appendPath("api")
                .appendPath("devices");
        try {
            URL devicesEndpoint = new URL(uriBuilder.toString());
            String response = apiPost(devicesEndpoint, newDevice.toString());
            if (response == null)
                return null;
            JSONObject responseObject = new JSONObject(response);
            return responseObject.getString("id");
        } catch (MalformedURLException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    public String getOwnDeviceUrl(@Nullable String fbToken) {
        @SuppressLint("HardwareIds") String androidId =
                Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        List<JSONObject> devices = getDevices();
        if (devices == null || androidId == null)
            return null;
        try {
            for (JSONObject device : devices) {
                if (androidId.equals(device.getString("android_id")))
                    return device.getString("id");
            }
            if (fbToken != null)
                return registerNewDevice(androidId, fbToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateFbToken(String fbToken) {
        String ownDeviceId = getOwnDeviceUrl(fbToken);
        Uri.Builder uriBuilder = baseUri.buildUpon()
                .appendPath("api")
                .appendPath("devices")
                .appendPath(ownDeviceId);
        JSONObject deviceUpdate = new JSONObject();
        try {
            URL deviceEndpoint = new URL(uriBuilder.toString());
            deviceUpdate.put("fb_token", fbToken);
            String response = apiCall(deviceEndpoint, "PATCH", deviceUpdate.toString());
            return response != null;
        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
