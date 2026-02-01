package pro.flysafe.sms;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import pro.flysafe.sms.util.Util;

public class LocationResponder implements LocationListener {

    public interface LocationCallback {
        void onLocation(String phone, Location location);
    }

    private final Context ctx;
    private final String phone;
    private final LocationCallback callback;
    private final LocationManager lm;
    private final Handler handler;
    private final Runnable timeout;

    public LocationResponder(Context ctx, String phone, LocationCallback callback) {
        this.ctx = ctx;
        this.phone = phone;
        this.callback = callback;
        this.lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        this.handler = new Handler(Looper.getMainLooper());
        this.timeout = () -> stopUpdates();
    }

    public void requestSingleFix() {
        try {
            if (lm == null) {
                return;
            }
            // Give up after 2 minutes to avoid long-running listeners.
            handler.postDelayed(timeout, 2 * 60 * 1000);
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // Prefer GPS provider when available.
                lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, Looper.getMainLooper());
                Util.log("LocationResponder: requesting GPS provider");
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                // Fallback to network provider if GPS is disabled.
                lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, Looper.getMainLooper());
                Util.log("LocationResponder: requesting NETWORK provider");
            } else {
                Util.log("LocationResponder: no provider enabled");
                stopUpdates();
            }
        } catch (SecurityException e) {
            Util.log(e);
        } catch (Exception e) {
            Util.log(e);
        }
    }

    private void stopUpdates() {
        try {
            // Always unregister to avoid leaking the listener.
            if (lm != null) {
                lm.removeUpdates(this);
            }
        } catch (Exception e) {
            Util.log(e);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Stop further updates and forward to the SMS reply callback.
        stopUpdates();
        handler.removeCallbacks(timeout);
        Util.log("LocationResponder: location received");
        if (callback != null) {
            callback.onLocation(phone, location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // no-op
    }

    @Override
    public void onProviderEnabled(String provider) {
        // no-op
    }

    @Override
    public void onProviderDisabled(String provider) {
        // no-op
    }
}
