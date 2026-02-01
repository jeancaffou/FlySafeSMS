package pro.flysafe.sms.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import pro.flysafe.sms.R;
import pro.flysafe.sms.model.Friend;

public class SmsRequestHelper {
    // For backwards compatibility with main FlySafe app
    private static final String SMS_SUFFIX = "0qgXwnSWj2t";

    public static void sendRequest(Context ctx, Friend friend, String type) {
        // Construct the exact SMS request format expected by FlySafe.
        String myPhone = Util.getPhone(ctx);
        if (Util.isEmpty(myPhone)) {
            Toast.makeText(ctx, R.string.sms_no_phone_set, Toast.LENGTH_LONG).show();
            return;
        }
        String uid = Util.getUID(ctx);
        if (friend == null || Util.isEmpty(friend.uid) || Util.isEmpty(friend.lockey) || Util.isEmpty(uid)) {
            Toast.makeText(ctx, R.string.sms_no_friend_phone, Toast.LENGTH_LONG).show();
            return;
        }
        String phone = friend.phone;

        if (Util.isEmpty(phone)) {
            Toast.makeText(ctx, R.string.sms_no_friend_phone, Toast.LENGTH_LONG).show();
            return;
        }

        // Format: "<#> <myPhone> #FlySafe <uid> <lockey> <type> 0qgXwnSWj2t"
        String msg = "<#> " + myPhone + " #FlySafe " + uid + " " + friend.lockey + " " + type + " " + SMS_SUFFIX;
        composeSMS(ctx, msg, phone);
    }

    private static void composeSMS(Context ctx, String msg, String phone) {
        // Use ACTION_SENDTO so the user chooses their default SMS app.
        Intent sendIntent = new Intent(Intent.ACTION_SENDTO);
        sendIntent.setData(Uri.parse("smsto:" + phone));
        sendIntent.putExtra("sms_body", msg);
        sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(sendIntent);
    }
}
