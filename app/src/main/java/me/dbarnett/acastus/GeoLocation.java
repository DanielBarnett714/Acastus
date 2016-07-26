package me.dbarnett.acastus;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Author: Daniel Barnett
 */
public class GeoLocation extends MainActivity{
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
        try {
            String bestProvider;
            Location location;
            Criteria criteria = new Criteria();
            try {
                bestProvider = locationManager.getBestProvider(criteria, false);
                location = locationManager.getLastKnownLocation(bestProvider);
            }catch (IllegalArgumentException e){
                return null;
            }
            Double lat, lon;
            Double[] coordinates = new Double[2];
            try {
                lat = location.getLatitude();
                lon = location.getLongitude();
                coordinates[0] = lat;
                coordinates[1] = lon;
                System.out.println("lat:" + lat + " , lon:" + lon);
                return coordinates;
            } catch (NullPointerException e) {
                Toast.makeText(GeoLocation.this, "Cannot get location.",
                        Toast.LENGTH_LONG).show();
                e.printStackTrace();
                return null;
            }
        }catch (SecurityException e){
            System.out.println("Cannot request location");
            return null;
        }
    }
}
