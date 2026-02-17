package pro.flysafe.sms;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import pro.flysafe.sms.util.PrivatePref;
import pro.flysafe.sms.util.SmsRequestHelper;
import pro.flysafe.sms.util.UpdateChecker;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ = 1010;
    private static final int BACKGROUND_LOCATION_REQ = 1011;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Guard: if no stored session, force login.
        if (PrivatePref.getString(this, "user", "").isEmpty()) {
            startActivity(new android.content.Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.friends_title);

        // Opt into edge-to-edge and apply system bar insets manually.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        applyWindowInsets();

        if (savedInstanceState == null) {
            // Single-screen app: embed the friends + SMS settings fragment.
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new FriendsFragment())
                    .commit();
        }

        // Request all SMS + location permissions up front to enable auto-replies.
        requestPermissionsIfNeeded();
        UpdateChecker.checkForUpdateIfNeeded(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Action bar menu with logout entry.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_help) {
            startActivity(new Intent(this, HelpActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_edit_phone) {
            SmsRequestHelper.promptForPhone(this, null, null);
            return true;
        }
        if (item.getItemId() == R.id.action_logout) {
            confirmLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmLogout() {
        // Confirm before clearing session data.
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> performLogout())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void performLogout() {
        // Clear stored session + cached data + local preferences.
        PrivatePref.clearAll(this);
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
        startActivity(new android.content.Intent(this, LoginActivity.class));
        finish();
    }

    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }

        // These permissions are required for emergency SMS automation replies.
        String[] required = new String[] {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        boolean needs = false;
        for (String p : required) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needs = true;
                break;
            }
        }

        if (needs) {
            ActivityCompat.requestPermissions(this, required, PERMISSION_REQ);
        } else {
            // Foreground permissions granted; request background if needed.
            requestBackgroundLocationIfNeeded();
        }
    }

    private void requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // On Android 10+, background location is a separate permission.
        // Prompt the user and route to system dialog/settings as needed.
        new AlertDialog.Builder(this)
                .setTitle(R.string.enable_background_location)
                .setMessage(R.string.enable_background_location_desc)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        ActivityCompat.requestPermissions(
                                this,
                                new String[] { Manifest.permission.ACCESS_BACKGROUND_LOCATION },
                                BACKGROUND_LOCATION_REQ
                        )
                )
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ) {
            // After foreground permissions are granted, request background location.
            requestBackgroundLocationIfNeeded();
        }
    }

    private void applyWindowInsets() {
        final OnApplyWindowInsetsListener listener = (view, insets) -> {
            // Keep original padding and add system bar insets without double-applying.
            int[] initial = (int[]) view.getTag(R.id.initial_padding);
            if (initial == null) {
                initial = new int[] {
                        view.getPaddingLeft(),
                        view.getPaddingTop(),
                        view.getPaddingRight(),
                        view.getPaddingBottom()
                };
                view.setTag(R.id.initial_padding, initial);
            }

            int insetLeft = insets.getInsets(WindowInsetsCompat.Type.systemBars()).left;
            int insetTop = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int insetRight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right;
            int insetBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            if (view.getId() == R.id.toolbar) {
                view.setPadding(
                        initial[0] + insetLeft,
                        initial[1] + insetTop,
                        initial[2] + insetRight,
                        initial[3]
                );
            } else if (view.getId() == R.id.fragmentContainer) {
                view.setPadding(
                        initial[0] + insetLeft,
                        initial[1],
                        initial[2] + insetRight,
                        initial[3] + insetBottom
                );
            }

            return insets;
        };

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), listener);
    }
}
