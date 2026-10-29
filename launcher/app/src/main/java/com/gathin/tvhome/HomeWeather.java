package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/** One-line weather ("Cherbourg · 14°C · Cloudy") from Open-Meteo, fetched at most every 30 minutes while Home is shown. */
final class HomeWeather {
    private static final long MAX_AGE_MS = 30 * 60 * 1000L;

    static void show(Context ctx, TextView view, String city, String lat, String lon) {
        if (city.isEmpty() || lat.isEmpty() || lon.isEmpty()) return;
        SharedPreferences p = ctx.getSharedPreferences("weather", Context.MODE_PRIVATE);
        String cached = p.getString("line", "");
        if (!cached.isEmpty()) {
            view.setText(cached);
            view.setVisibility(TextView.VISIBLE);
        }
        if (System.currentTimeMillis() - p.getLong("at", 0) < MAX_AGE_MS && !cached.isEmpty()) return;
        final Handler ui = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                URL u = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                        + "&current=temperature_2m,weather_code&temperature_unit=celsius");
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                    String l;
                    while ((l = r.readLine()) != null) sb.append(l);
                }
                JSONObject cur = new JSONObject(sb.toString()).getJSONObject("current");
                String line = city + "  ·  " + Math.round(cur.getDouble("temperature_2m")) + "°C  ·  "
                        + describe(ctx, cur.getInt("weather_code"));
                p.edit().putString("line", line).putLong("at", System.currentTimeMillis()).apply();
                ui.post(() -> {
                    view.setText(line);
                    view.setVisibility(TextView.VISIBLE);
                });
            } catch (Exception ignored) {
            }
        }, "weather").start();
    }

    private static String describe(Context ctx, int code) {
        int id = R.string.wx_clear;
        if (code >= 1 && code <= 2) id = R.string.wx_partly;
        else if (code == 3) id = R.string.wx_cloudy;
        else if (code == 45 || code == 48) id = R.string.wx_fog;
        else if (code >= 51 && code <= 57) id = R.string.wx_drizzle;
        else if (code >= 61 && code <= 67) id = R.string.wx_rain;
        else if (code >= 71 && code <= 77) id = R.string.wx_snow;
        else if (code >= 80 && code <= 82) id = R.string.wx_showers;
        else if (code >= 85 && code <= 86) id = R.string.wx_snow_showers;
        else if (code >= 95) id = R.string.wx_storm;
        return ctx.getString(id);
    }

    private HomeWeather() {}
}
