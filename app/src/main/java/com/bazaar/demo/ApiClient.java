package com.bazaar.demo;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;

/**
 * Plain HTTP client against the local mock backend.
 *
 * Contract: every call emits exactly one logcat line:
 *   I/BAZAAR_NET: <status_code> <METHOD> <path>
 * A status of 0 means the request never completed (transport failure).
 */
public final class ApiClient {

    public static final String TAG = "BAZAAR_NET";
    public static final String BASE = "http://10.0.2.2:8100";

    public interface Callback {
        void onResult(int code, String body);
    }

    public static void call(final String method, final String path, final String body, final Callback cb) {
        new Thread(() -> {
            int code = -1;
            String resp = "";
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(BASE + path).openConnection();
                c.setRequestMethod(method);
                c.setConnectTimeout(4000);
                c.setReadTimeout(4000);
                c.setRequestProperty("Content-Type", "application/json");
                if (body != null) {
                    c.setDoOutput(true);
                    try (OutputStream os = c.getOutputStream()) {
                        os.write(body.getBytes(StandardCharsets.UTF_8));
                    }
                }
                code = c.getResponseCode();
                InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
                resp = readAll(is);
            } catch (Exception ex) {
                code = 0;
                resp = ex.getClass().getSimpleName();
            }
            Log.i(TAG, code + " " + method + " " + path);

            final int fc = code;
            final String fr = resp;
            if (cb != null) {
                new Handler(Looper.getMainLooper()).post(() -> cb.onResult(fc, fr));
            }
        }).start();
    }

    private static String readAll(InputStream is) {
        if (is == null) return "";
        try (InputStream in = is; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}
