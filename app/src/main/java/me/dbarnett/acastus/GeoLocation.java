package me.dbarnett.acastus;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Author: Daniel Barnett
 */
public class GeoLocation extends MainActivity implements LocationListener{
    /**
     * The Location manager.
     */
    LocationManager locationManager;

    /**
     * Instantiates a new Geo location.
     *
     * @param locationManager the location manager
     */
    GeoLocation(LocationManager locationManager){
        this.locationManager = locationManager;
    }


    /**
     * Distance double.
     *
     * @param lat1 the lat 1
     * @param lat2 the lat 2
     * @param lon1 the lon 1
     * @param lon2 the lon 2
     * @return the double
     */
    public static double distance(double lat1, double lat2, double lon1, double lon2) {
        final int R = 6371; // Radius of the earth
        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        distance = Math.pow(distance, 2);
        return round(Math.sqrt(distance)/1609.344, 2);
    }

    /**
     * Round double.
     *
     * @param value  the value
     * @param places the places
     * @return the double
     */
    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Get location double [ ].
     *
     * @return the double [ ]
     */
    public Double[] getLocation() {
        Criteria criteria;
        String bestProvider;
        double latitude, longitude;
        try {
            if (MainActivity.isLocationEnabled()) {
                criteria = new Criteria();
                bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();

                Location location = locationManager.getLastKnownLocation(bestProvider);
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    Double[] coordinates = new Double[2];
                    coordinates[0]  = latitude;
                    coordinates[1] = longitude;
                    return coordinates;
                }
                else{
                    try{
                        locationManager.requestLocationUpdates(bestProvider, 0, 0, this);
                        Handler h = new Handler(Looper.getMainLooper());
                        h.post(new Runnable() {
                            public void run() {
                                Toast.makeText(context, R.string.accessing_location, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }catch (RuntimeException e){

                    }
                    return null;
                }
            }else {
                return null;
            }
        }catch (SecurityException e){
            Handler h = new Handler(Looper.getMainLooper());
            h.post(new Runnable() {
                public void run() {
                    Toast.makeText(context, R.string.location_not_enabled, Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            locationManager.removeUpdates(this);
        }catch (SecurityException e){

        }
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            locationManager.removeUpdates(this);

        }catch (SecurityException e){

        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {
      useLocation = true;
    }

    @Override
    public void onProviderDisabled(String s) {
        useLocation = false;
    }
}
