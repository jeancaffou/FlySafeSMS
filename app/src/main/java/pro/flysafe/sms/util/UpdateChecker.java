package pro.flysafe.sms.util;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

import pro.flysafe.sms.R;

public class UpdateChecker {
    private static final String RELEASES_LATEST_URL = "https://api.github.com/repos/jeancaffou/FlySafeSMS/releases/latest";
    private static final String PREF_LAST_PROMPTED_TAG = "pref_update_prompted_tag";
    private static volatile boolean checkedThisRun = false;

    public static void checkForUpdateIfNeeded(Activity activity) {
        if (activity == null || activity.isFinishing() || checkedThisRun) {
            return;
        }
        checkedThisRun = true;

        StringRequest req = new StringRequest(Request.Method.GET, RELEASES_LATEST_URL,
                response -> handleLatestRelease(activity, response),
                error -> Util.log("Update check failed: " + error));

        Volley.newRequestQueue(activity.getApplicationContext()).add(req);
    }

    private static void handleLatestRelease(Activity activity, String response) {
        try {
            JSONObject obj = new JSONObject(response);
            String latestTag = sanitizeTag(obj.optString("tag_name", ""));
            String current = sanitizeTag(getCurrentVersionName(activity));
            if (Util.isEmpty(latestTag) || Util.isEmpty(current)) {
                return;
            }
            if (compareVersions(latestTag, current) <= 0) {
                return;
            }

            String lastPrompted = Util.getString(activity, PREF_LAST_PROMPTED_TAG, "");
            if (latestTag.equals(lastPrompted)) {
                return;
            }

            String apkUrl = findApkUrl(obj.optJSONArray("assets"));
            if (Util.isEmpty(apkUrl)) {
                return;
            }

            if (activity.isFinishing()) {
                return;
            }
            String finalApkUrl = apkUrl;
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.update_available_title)
                    .setMessage(activity.getString(R.string.update_available_message, latestTag, current))
                    .setPositiveButton(R.string.update_now, (d, w) -> {
                        Util.save(activity, PREF_LAST_PROMPTED_TAG, latestTag);
                        startDownload(activity, finalApkUrl, latestTag);
                    })
                    .setNegativeButton(R.string.update_later, (d, w) ->
                            Util.save(activity, PREF_LAST_PROMPTED_TAG, latestTag))
                    .show();
        } catch (Exception e) {
            Util.log(e);
        }
    }

    private static void startDownload(Context ctx, String url, String versionTag) {
        try {
            String fileName = String.format(Locale.US, "FlySafeSMS-%s.apk", versionTag);
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setTitle(fileName);
            req.setDescription("FlySafeSMS update");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setMimeType("application/vnd.android.package-archive");
            req.setAllowedOverMetered(true);
            req.setAllowedOverRoaming(true);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(req);
                Toast.makeText(ctx, R.string.update_download_started, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ctx, R.string.update_download_failed, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Util.log(e);
            Toast.makeText(ctx, R.string.update_download_failed, Toast.LENGTH_LONG).show();
        }
    }

    private static String findApkUrl(JSONArray assets) {
        if (assets == null) {
            return "";
        }
        for (int i = 0; i < assets.length(); i++) {
            try {
                JSONObject a = assets.getJSONObject(i);
                String name = a.optString("name", "").toLowerCase(Locale.US);
                String url = a.optString("browser_download_url", "");
                if (name.endsWith(".apk") && !Util.isEmpty(url)) {
                    return url;
                }
            } catch (Exception ignored) {
                // Ignore malformed release assets.
            }
        }
        return "";
    }

    private static String sanitizeTag(String tag) {
        if (tag == null) {
            return "";
        }
        String cleaned = tag.trim();
        if (cleaned.startsWith("v") || cleaned.startsWith("V")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned;
    }

    private static int compareVersions(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int max = Math.max(pa.length, pb.length);
        for (int i = 0; i < max; i++) {
            int va = parseVersionPart(pa, i);
            int vb = parseVersionPart(pb, i);
            if (va != vb) {
                return Integer.compare(va, vb);
            }
        }
        return 0;
    }

    private static int parseVersionPart(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        try {
            String numeric = parts[index].replaceAll("[^0-9].*$", "");
            if (Util.isEmpty(numeric)) {
                return 0;
            }
            return Integer.parseInt(numeric);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String getCurrentVersionName(Context ctx) {
        try {
            PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return info.versionName;
        } catch (Exception e) {
            Util.log(e);
            return "";
        }
    }
}
