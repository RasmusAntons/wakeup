package de.rasmusantons.wakeup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

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
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

public class WakeupApi {
    private static final String TAG = "WakeupApi";

    @Nullable
    private static String apiCall(URL url, String method, @Nullable String body) {
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
    private static String apiGet(URL url) {
        return apiCall(url, "GET", null);
    }

    @Nullable
    private static String apiPost(URL url, String body) {
        return apiCall(url, "POST", body);
    }

    @Nullable
    private static JSONArray getJSONArray(URL url) {
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
    public static List<JSONObject> getDevices(@Nullable String owner, @Nullable String androidId) {
        try {
            Uri.Builder uriBuilder = new Uri.Builder()
                    .scheme("https")
                    .authority("wakeup-dev.3po.ch")  // todo: read from settings
                    .appendPath("api")
                    .appendPath("devices");
            if (owner != null)
                uriBuilder.appendQueryParameter("owner", owner);
            if (androidId != null)
                uriBuilder.appendQueryParameter("android_id", androidId);
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
    public static String registerNewDevice(String androidId, @Nullable String fbToken) {
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
        Uri.Builder uriBuilder = new Uri.Builder()
                .scheme("https")
                .authority("wakeup-dev.3po.ch")  // todo: read from settings
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
    public static String getOwnDeviceUrl(Context context, @Nullable String fbToken) {
        @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String ownUserId;
        try {
            ownUserId = LoginActivity.mUserInfoJson.getString("sub");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        List<JSONObject> devices = getDevices(ownUserId, androidId);
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

    public static boolean updateFbToken(Context context, String fbToken) {
        String ownDeviceId = getOwnDeviceUrl(context, fbToken);
        Uri.Builder uriBuilder = new Uri.Builder()
                .scheme("https")
                .authority("wakeup-dev.3po.ch")  // todo: read from settings
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
