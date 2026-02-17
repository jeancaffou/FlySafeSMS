package pro.flysafe.sms;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import pro.flysafe.sms.util.Util;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.help_title);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        applyWindowInsets();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Button battery = findViewById(R.id.openBatterySettings);
        Button location = findViewById(R.id.openLocationSettings);

        battery.setOnClickListener(v -> openBatterySettings());
        location.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void openBatterySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        } catch (Exception e) {
            Util.log(e);
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void applyWindowInsets() {
        final OnApplyWindowInsetsListener listener = (view, insets) -> {
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
            } else if (view.getId() == R.id.helpScroll) {
                view.setPadding(
                        initial[0] + insetLeft,
                        initial[1],
                        initial[2] + insetRight,
                        initial[3] + insetBottom
                );
            }

            return insets;
        };

        View toolbar = findViewById(R.id.toolbar);
        View scroll = findViewById(R.id.helpScroll);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, listener);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, listener);
    }
}
