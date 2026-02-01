package pro.flysafe.sms;

import android.location.Location;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmsFormatter {
    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    public static String formatLocation(Location location) {
        // Keep legacy response format so existing clients can parse it.
        String time = DF.format(new Date(location.getTime()));
        String lat = String.valueOf(location.getLatitude());
        String lon = String.valueOf(location.getLongitude());
        return "http://maps.google.com/maps?q=" + lat + "%2C" + lon +
                " ALT: " + Math.round(location.getAltitude()) + "m SPD: " +
                Math.round(location.getSpeed()) + " km/h BRG: " +
                heading(location.getBearing()) + " ACC: " + Math.round(location.getAccuracy()) + "m TIME: " +
                time;
    }

    private static String heading(double x) {
        if (x > 0) {
            // Convert bearing degrees to compass abbreviation.
            String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
            return directions[(int) Math.round(((x % 360) / 45))];
        }
        return "/";
    }
}
