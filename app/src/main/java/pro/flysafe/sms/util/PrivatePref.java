package pro.flysafe.sms.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PrivatePref {
    public static String prefkey = "xcg";

    public static void save(Context c, String key, String val) {
        // Encrypted storage isn't used here; keep compatibility with existing prefs.
        SharedPreferences sharedPref = c.getSharedPreferences(prefkey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, val);
        editor.apply();
    }

    public static void save(Context c, String key, boolean val) {
        SharedPreferences sharedPref = c.getSharedPreferences(prefkey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, val);
        editor.apply();
    }

    public static String getString(Context c, String key, String val) {
        // Return stored value or default.
        SharedPreferences sharedPref = c.getSharedPreferences(prefkey, Context.MODE_PRIVATE);
        return sharedPref.getString(key, val);
    }

    public static boolean getBoolean(Context c, String key, boolean val) {
        SharedPreferences sharedPref = c.getSharedPreferences(prefkey, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(key, val);
    }
}
