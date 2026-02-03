package pro.flysafe.sms.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONObject;

public class Util {
    public static final String API_ENDPOINT = "https://flysafe.pro";

    public static void log(Object msg) {
        // Centralized logging for SMS-only app.
        Log.i("FlySafeSMS", String.valueOf(msg));
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static void save(Context c, String key, boolean val) {
        try {
            // Store preferences in default shared prefs.
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(key, val);
            editor.apply();
        } catch (Exception e) {
            log(e);
        }
    }

    public static void save(Context c, String key, String val) {
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(key, val);
            editor.apply();
        } catch (Exception e) {
            log(e);
        }
    }

    public static boolean getBoolean(Context c, String key, boolean val) {
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
            return sharedPref.getBoolean(key, val);
        } catch (Exception e) {
            log(e);
        }
        return val;
    }

    public static String getString(Context c, String key, String val) {
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
            return sharedPref.getString(key, val);
        } catch (Exception e) {
            log(e);
        }
        return val;
    }

    public static String id(Context ctx) {
        try {
            // Device identifier used by backend for session binding.
            return Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            log(e);
        }
        return "";
    }

    public static JSONObject getUserData(Context ctx) {
        try {
            // User session is stored as raw JSON string.
            String user = PrivatePref.getString(ctx, "user", "");
            if (!isEmpty(user)) {
                return new JSONObject(user);
            }
        } catch (Exception e) {
            log(e);
        }
        return new JSONObject();
    }

    public static String getUserAttr(Context ctx, String key) {
        try {
            // Read a single field from stored user JSON.
            JSONObject j = getUserData(ctx);
            if (j.has(key)) {
                return j.getString(key);
            }
        } catch (Exception e) {
            log(e);
        }
        return null;
    }

    public static String getUserName(Context ctx) {
        return getUserAttr(ctx, "name");
    }

    public static String getPhone(Context ctx) {
        return getUserAttr(ctx, "phone");
    }

    public static String getUID(Context ctx) {
        return getUserAttr(ctx, "uid");
    }

    public static String getUserId(Context ctx) {
        try {
            // Compose user id string in the same way as the main app.
            JSONObject j = getUserData(ctx);
            if (j.has("uid")) {
                if (j.has("mtoken") && !Util.isEmpty(j.getString("mtoken"))) {
                    String mtoken = j.getString("mtoken");
                    if (!isEmpty(mtoken)) {
                        return j.getString("uid") + "__" + j.getString("mtoken");
                    }
                }
                String pass = j.optString("pass", "");
                if (!isEmpty(pass)) {
                    return j.getString("uid") + '_' + pass;
                }
            }
        } catch (Exception e) {
            log("getUserId Failed");
        }

        return id(ctx) + "_null";
    }

    public static void openUrl(Context ctx, String url) {
        try {
            if (isEmpty(url)) {
                return;
            }
            Uri uri = Uri.parse(url);
            ctx.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            log(e);
        }
    }
}
