package pro.flysafe.sms;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import pro.flysafe.sms.util.PrivatePref;
import pro.flysafe.sms.util.UpdateChecker;
import pro.flysafe.sms.util.Util;

public class LoginActivity extends AppCompatActivity {

    private static final int RESULT_SIGN_IN = 33;

    private RequestQueue queue;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        queue = Volley.newRequestQueue(this);
        UpdateChecker.checkForUpdateIfNeeded(this);

        // Email/password login remains, but registration is intentionally removed.
        findViewById(R.id.loginButton).setOnClickListener(view -> login());
        // If we already have a stored session, skip login entirely.
        if (!PrivatePref.getString(this, "user", "").isEmpty()) {
            loggedin();
        }

        setupGoogleSignIn();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void setupGoogleSignIn() {
        // Configure Google Sign-In to request an ID token for server-side login.
        if (!isPlayServicesAvailable()) {
            SignInButton gbutton = findViewById(R.id.googleSignInButton);
            gbutton.setVisibility(android.view.View.GONE);
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Attach to the standard Google Sign-In button.
        SignInButton gbutton = findViewById(R.id.googleSignInButton);
        // Match the main app's Google button look (wide + dark scheme).
        gbutton.setSize(SignInButton.SIZE_WIDE);
        gbutton.setColorScheme(SignInButton.COLOR_DARK);
        // Normalize the internal text view so the icon/text align cleanly without changing style.
        TextView gtext = (TextView) gbutton.getChildAt(0);
        gtext.setText(getResources().getString(R.string.signin_with_google));
        gtext.setGravity(Gravity.CENTER);
        gtext.setCompoundDrawablePadding(dpToPx(12));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        gtext.setLayoutParams(lp);
        gbutton.setOnClickListener(view -> startActivityForResult(googleSignInClient.getSignInIntent(), RESULT_SIGN_IN));
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            final String idToken = account.getIdToken();
            // If we don't get a token, we cannot authenticate with the server.
            if (Util.isEmpty(idToken)) {
                showError(getResources().getString(R.string.error_generic));
                return;
            }

            // Exchange Google ID token for a FlySafe session.
            StringRequest cfrm = new StringRequest(Request.Method.GET, Util.API_ENDPOINT + "/google/login2/" + idToken,
                    response -> {
                        Util.log(response);
                        parse(response);
                    }, error -> showError(getResources().getString(R.string.error_generic))
            );

            cfrm.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            cfrm.setTag(this);
            queue.add(cfrm);
        } catch (Exception e) {
            Util.log(e);
            showError(getResources().getString(R.string.error_generic));
        }
    }

    private void loggedin() {
        // Once authenticated, go straight to the main screen.
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    private void login() {
        // Basic email/password login against FlySafe backend.
        final TextView tv_mail = findViewById(R.id.email);
        final TextView tv_pass = findViewById(R.id.password);
        final Button signin = findViewById(R.id.loginButton);
        signin.setEnabled(false);

        StringRequest postRequest = new StringRequest(Request.Method.POST, Util.API_ENDPOINT + "/user/login/",
                response -> {
                    Util.log(response);
                    parse(response);
                    signin.setEnabled(true);
                },
                error -> {
                    Util.log(error.toString());
                    signin.setEnabled(true);
                    showError(getResources().getString(R.string.error_generic));
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                // Backend expects email/pass plus device id.
                Map<String, String> params = new HashMap<>();
                params.put("email", tv_mail.getText().toString());
                params.put("pass", tv_pass.getText().toString());
                params.put("mobile", Util.id(getApplicationContext()));

                return params;
            }
        };
        postRequest.setTag(this);
        queue.add(postRequest);
    }

    private boolean parse(String response) {
        try {
            // Store full user JSON on successful login for reuse by other features.
            JSONObject obj = new JSONObject(response);
            if (obj.has("error")) {
                showError(getResources().getString(R.string.error_login));
            }

            if (obj.has("uid")) {
                PrivatePref.save(this, "user", response);
                loggedin();
                return true;
            }
        } catch (Exception e) {
            Util.log(e);
        }
        return false;
    }

    private void showError(String err) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(err);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean isPlayServicesAvailable() {
        int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        return result == ConnectionResult.SUCCESS;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
