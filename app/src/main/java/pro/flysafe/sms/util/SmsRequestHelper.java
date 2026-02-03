package pro.flysafe.sms.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import pro.flysafe.sms.R;
import pro.flysafe.sms.model.Friend;

public class SmsRequestHelper {
    // For backwards compatibility with main FlySafe app
    private static final String SMS_SUFFIX = "0qgXwnSWj2t";
    private static final String PREF_SMS_MY_PHONE = "pref_sms_my_phone";

    public static void sendRequest(Context ctx, Friend friend, String type) {
        // Construct the exact SMS request format expected by FlySafe.
        String myPhone = getMyPhone(ctx);
        if (Util.isEmpty(myPhone)) {
            promptForPhone(ctx, friend, type);
            return;
        }
        String uid = Util.getUID(ctx);
        if (friend == null || Util.isEmpty(friend.uid) || Util.isEmpty(friend.lockey) || Util.isEmpty(uid)) {
            Toast.makeText(ctx, R.string.sms_friend_data_missing, Toast.LENGTH_LONG).show();
            return;
        }
        String phone = friend.phone;

        if (Util.isEmpty(phone) || phone.length() < 5) {
            // Let the user pick a recipient in their SMS app when we don't have a phone on file.
            String msg = "<#> " + myPhone + " #FlySafe " + uid + " " + friend.lockey + " " + type + " " + SMS_SUFFIX;
            composeSMS(ctx, msg, "");
            return;
        }

        // Format: "<#> <myPhone> #FlySafe <uid> <lockey> <type> 0qgXwnSWj2t"
        String msg = "<#> " + myPhone + " #FlySafe " + uid + " " + friend.lockey + " " + type + " " + SMS_SUFFIX;
        composeSMS(ctx, msg, phone);
    }

    private static void composeSMS(Context ctx, String msg, String phone) {
        // Use ACTION_SENDTO so the user chooses their default SMS app.
        Intent sendIntent = new Intent(Intent.ACTION_SENDTO);
        sendIntent.setData(Uri.parse("smsto:" + (phone == null ? "" : phone)));
        sendIntent.putExtra("sms_body", msg);
        sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(sendIntent);
    }

    private static String getMyPhone(Context ctx) {
        String phone = Util.getPhone(ctx);
        if (!Util.isEmpty(phone)) {
            return phone;
        }
        return Util.getString(ctx, PREF_SMS_MY_PHONE, "");
    }

    private static void promptForPhone(Context ctx, Friend friend, String type) {
        EditText input = new EditText(ctx);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setHint(R.string.sms_enter_phone_hint);
        int outerPad = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16,
                ctx.getResources().getDisplayMetrics());
        int innerPad = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                12,
                ctx.getResources().getDisplayMetrics());
        input.setPadding(innerPad, innerPad, innerPad, innerPad);

        TextView helper = new TextView(ctx);
        helper.setText(R.string.sms_enter_phone_legacy_note);
        helper.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        helper.setPadding(0, innerPad / 2, 0, 0);
        TypedValue colorAttr = new TypedValue();
        if (ctx.getTheme().resolveAttribute(android.R.attr.textColorSecondary, colorAttr, true)) {
            int color = colorAttr.resourceId != 0
                    ? ContextCompat.getColor(ctx, colorAttr.resourceId)
                    : colorAttr.data;
            helper.setTextColor(color);
        }

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(outerPad, outerPad, outerPad, outerPad);
        container.addView(input);
        container.addView(helper);

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
                .setTitle(R.string.sms_enter_phone_title)
                .setMessage(R.string.sms_enter_phone_message)
                .setView(container)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String value = input.getText() == null ? "" : input.getText().toString().trim();
                    if (!value.matches("^\\+\\d+$")) {
                        Toast.makeText(ctx, R.string.sms_enter_phone_invalid, Toast.LENGTH_LONG).show();
                        return;
                    }
                    Util.save(ctx, PREF_SMS_MY_PHONE, value);
                    sendRequest(ctx, friend, type);
                })
                .setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }
}
