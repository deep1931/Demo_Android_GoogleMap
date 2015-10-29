package vphs.heshan.googlemapdemo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

/**
 * Created by Heshan on 10/23/15.
 */
public class GPSTracker extends Service implements LocationListener {

    private Context mContext;
    private static LocationManager sLocationManager;
    private static boolean sIsGPSEnable = false;
    private static boolean sIsNetWorkEnable = false;
    private Location mLocation;

    public GPSTracker(Context context) {
        mContext = context;
    }

    /*get the current location geo-point*/
    public Location getCurrentLocation() {
        try{
            sLocationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
            sIsGPSEnable = sLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            sIsNetWorkEnable = sLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if(sIsGPSEnable) {
                sLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0, this);
                mLocation = sLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } else {
                if (sIsNetWorkEnable) {
                    sLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0, this);
                    mLocation = sLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mLocation;
    }


    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
