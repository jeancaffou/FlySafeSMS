package pro.flysafe.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pro.flysafe.sms.util.Util;
import pro.flysafe.sms.util.PrivatePref;

public class SmsReceiver extends BroadcastReceiver {
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Util.log("SmsReceiver onReceive action=" + intent.getAction());
            if (SMS_RECEIVED.equals(intent.getAction())) {
                // Parse raw SMS PDUs to reconstruct message(s).
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Object[] smsextras = (Object[]) bundle.get("pdus");
                    if (smsextras != null) {
                        Util.log("SmsReceiver PDUs count=" + smsextras.length);
                        for (Object pdu : smsextras) {
                            SmsMessage smsmsg = SmsMessage.createFromPdu((byte[]) pdu);
                            String incomingNumber = smsmsg.getOriginatingAddress();
                            String msg = smsmsg.getMessageBody();
                            Util.log("SMS received from " + maskPhone(incomingNumber));
                            handleMessage(context, incomingNumber, msg);
                        }
                    } else {
                        Util.log("SmsReceiver PDUs missing");
                    }
                } else {
                    Util.log("SmsReceiver extras missing");
                }
            }
        } catch (Exception e) {
            Util.log(e);
        }
    }

    private void handleMessage(Context ctx, String sender, String msg) {
        try {
            Util.log("SmsReceiver handleMessage sender=" + maskPhone(sender));
            // Respect user toggle to disable SMS auto-replies entirely.
            if (!Util.getBoolean(ctx, "pref_sms_loc", true)) {
                Util.log("SmsReceiver ignored: pref_sms_loc disabled");
                return;
            }

            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            // Optional roaming gate, mirrors behavior of the main app.
            if (tm != null && tm.isNetworkRoaming() && !Util.getBoolean(ctx, "pref_sms_roaming", true)) {
                Util.log("SmsReceiver ignored: roaming disabled");
                return;
            }

            // Parse and validate incoming request format.
            ParsedMessage parsed = parseMessage(ctx, sender, msg);
            if (parsed == null) {
                Util.log("SMS ignored (format/permission check failed) from " + maskPhone(sender));
                return;
            }

            // Notify the user that an authorized friend requested location.
            showToast(ctx, parsed.friendName + " requested your SMS location");

            // Reply using last known location or trigger a fresh GPS request.
            if (parsed.type.equals("LastKnownLocation")) {
                Util.log("SMS request: LastKnownLocation -> " + maskPhone(parsed.replyTo));
                sendLastKnownLocation(ctx, parsed.replyTo, parsed.friendName);
                return;
            }

            if (parsed.type.equals("GPS")) {
                Util.log("SMS request: GPS -> " + maskPhone(parsed.replyTo));
                requestGpsLocation(ctx, parsed.replyTo, parsed.friendName);
                return;
            }

            if (parsed.type.equals("ALL")) {
                Util.log("SMS request: ALL -> " + maskPhone(parsed.replyTo));
                requestGpsLocation(ctx, parsed.replyTo, parsed.friendName);
            }
        } catch (Exception e) {
            Util.log(e);
        }
    }

    private ParsedMessage parseMessage(Context ctx, String sender, String msg) {
        if (Util.isEmpty(msg)) {
            return null;
        }

        String trimmed = msg.trim();
        String replyTo = sender;
        String payload = trimmed;

        if (trimmed.startsWith("<#>")) {
            // Legacy SMS Retriever-style format:
            // "<#> <replyPhone> #FlySafe <uid> <lockey> <type> 0qgXwnSWj2t"
            //
            // We intentionally ignore the embedded <replyPhone> value even though we still parse
            // the message structure for backwards compatibility. Back when FlySafe used the
            // SmsRetriever API, Android did not expose the sender's phone number to the app,
            // so the reply number had to be included inside the SMS payload itself. Now that
            // FlySafeSMS receives the real sender address from the telephony stack, the
            // payload phone number is treated as legacy metadata only. We keep it in the
            // format so older clients can still send valid requests, but we always reply to
            // the actual sender rather than trusting the embedded value.
            String[] parts = trimmed.split(" ");
            if (parts.length < 6) {
                Util.log("SMS parse failed: too few parts");
                return null;
            }
            String legacyReplyTo = parts[1].trim();
            Util.log("SMS legacy reply phone ignored: " + maskPhone(legacyReplyTo));
            List<String> rest = new ArrayList<>();
            for (int i = 2; i < parts.length; i++) {
                rest.add(parts[i]);
            }
            payload = String.join(" ", rest);
        }

        // Support friend-based (#FlySafe) format.
        if (!payload.startsWith("#FlySafe")) {
            return null;
        }

        String[] params = payload.split(" ");
        if (payload.startsWith("#FlySafe")) {
            // Friend-based request: "#FlySafe <uid> <lockey> <type>"
            if (params.length < 4) {
                Util.log("SMS parse failed: friend payload too short");
                return null;
            }
            String uid = params[1];
            String code = params[2];
            String type = params[3];
            String friendName = getFriendNameIfAllowed(ctx, uid, code);
            if (friendName == null) {
                Util.log("SMS parse failed: friend permission check failed");
                return null;
            }
            return new ParsedMessage(replyTo, type, friendName);
        }

        return null;
    }

    private String getFriendNameIfAllowed(Context ctx, String uid, String code) {
        try {
            // Validate friend + lockey from cached friends list.
            JSONArray a = new JSONArray(PrivatePref.getString(ctx, FriendsFragment.CACHE_KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                String friend = o.optString("friend", o.optString("uid", ""));
                String lockey = o.optString("lockey", "");
                String loc = o.optString("loc", "0");
                if (friend.equals(uid)) {
                    if ("1".equals(loc)) {
                        if (!Util.isEmpty(lockey) && lockey.equals(code)) {
                            String name = o.optString("fname", o.optString("name", "Friend"));
                            return Util.isEmpty(name) ? "Friend" : name;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Util.log(e);
        }
        return null;
    }

    private void sendLastKnownLocation(Context ctx, String phone, String friendName) {
        try {
            // Return the most recent cached GPS/network location without new fixes.
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            Location location = lm != null ? lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) : null;
            if (location == null && lm != null) {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (location != null) {
                sendSmsWithLocation(ctx, phone, location, friendName);
            } else {
                Util.log("SMS reply skipped: no last known location");
            }
        } catch (Exception e) {
            Util.log(e);
        }
    }

    private void requestGpsLocation(Context ctx, String phone, String friendName) {
        // Request a fresh fix, then send a reply when we get it.
        LocationResponder responder = new LocationResponder(ctx, phone, (replyPhone, location) ->
                sendSmsWithLocation(ctx, replyPhone, location, friendName));
        responder.requestSingleFix();
    }

    private void sendSmsWithLocation(Context ctx, String phone, Location location, String friendName) {
        try {
            if (location == null) {
                return;
            }
            // Format response message to match legacy FlySafe format.
            String text = SmsFormatter.formatLocation(location);
            SmsManager sm = SmsManager.getDefault();
            sm.sendTextMessage(phone, null, text, null, null);
            Util.log("SMS reply sent to " + maskPhone(phone));
            String latLon = String.format(Locale.US, "%.5f,%.5f", location.getLatitude(), location.getLongitude());
            showToast(ctx, "Sent " + friendName + " " + latLon);
        } catch (Exception e) {
            Util.log(e);
        }
    }

    private String maskPhone(String phone) {
        if (Util.isEmpty(phone)) {
            return "unknown";
        }
        String digits = phone.replaceAll("\\\\D", "");
        if (digits.length() <= 4) {
            return "***" + digits;
        }
        return "***" + digits.substring(digits.length() - 4);
    }

    private static class ParsedMessage {
        final String replyTo;
        final String type;
        final String friendName;

        ParsedMessage(String replyTo, String type, String friendName) {
            this.replyTo = replyTo;
            this.type = type;
            this.friendName = Util.isEmpty(friendName) ? "Friend" : friendName;
        }
    }

    private void showToast(Context ctx, String message) {
        // Ensure toast is posted on the main thread.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(ctx.getApplicationContext(), message, Toast.LENGTH_LONG).show());
    }
}
