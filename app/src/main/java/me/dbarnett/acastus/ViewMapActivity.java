package me.dbarnett.acastus;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.mapzen.tangram.LngLat;
import com.mapzen.tangram.MapController;
import com.mapzen.tangram.MapView;
import com.mapzen.tangram.Marker;
import com.mapzen.tangram.TouchInput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * The type View map activity.
 */
public class ViewMapActivity extends AppCompatActivity implements MapView.OnMapReadyCallback{

    /**
     * The Map.
     */
    MapController map;

    /**
     * The Prefs.
     */
    protected SharedPreferences prefs;

    /**
     * The Map view.
     */
    MapView mapView;


    /**
     * The Point style.
     */
    String pointStyle = "{ style: 'points', color: '#00c853', size: [45px, 45px], order: 2000, collide: false }";
    /**
     * The Cur lat.
     */
    double curLat;
    /**
     * The Cur lon.
     */
    double curLon;

    /**
     * The Lookup list.
     */
    public JSONArray lookupList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("app_theme", false)){
            setTheme(R.style.DarkTheme);
        }
        setContentView(R.layout.activity_view_map);


        mapView = (MapView)findViewById(R.id.map);

        mapView.onCreate(savedInstanceState);

        Intent intent = getIntent();
        curLat = intent.getExtras().getDouble("latitude");
        curLon = intent.getExtras().getDouble("longitude");

        if (prefs.getBoolean("app_theme", false)){
            mapView.getMapAsync(this, "cinnabar-style-gh-pages/cinnabar-style-dark.yaml");
        }else {
            mapView.getMapAsync(this, "cinnabar-style-gh-pages/cinnabar-style.yaml");
        }
    }


    @Override
    public void onMapReady(MapController mapController) {
        map = mapController;
        addPoints();
    }



    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    /**
     * Add points.
     */
    public void addPoints(){
        try {
            lookupList = new JSONArray(getIntent().getStringExtra("lookup_list"));


            System.out.println(lookupList.toString());

            ArrayList<Marker> markers = new ArrayList<>();

            for (int i = 0; i < lookupList.length(); i++){
                Marker marker = map.addMarker();
                markers.add(marker);
            }

            for (int i = 0; i < lookupList.length(); i++){

                Marker marker = markers.get(i);
                marker.setStylingFromString(pointStyle);
                LngLat lngLat = new LngLat();
                JSONObject jsonObject = lookupList.getJSONObject(i);
                lngLat.set(jsonObject.getDouble("lon"), jsonObject.getDouble("lat"));
                marker.setPoint(lngLat);
                marker.setDrawable(getResources().getDrawable(R.mipmap.ic_marker));

            }
            JSONObject jsonObject = new JSONObject();
            String name = getResources().getString(R.string.my_current_location) + ", " + curLat + ", " + curLon;
            jsonObject.put("name", name);
            jsonObject.put("lat", curLat);
            jsonObject.put("lon", curLon);
            lookupList.put(jsonObject);
            Marker marker = map.addMarker();
            LngLat lngLat = new LngLat();
            lngLat.set(curLon, curLat);
            marker.setPoint(lngLat);
            marker.setStylingFromString(pointStyle);
            marker.setDrawable(getResources().getDrawable(R.mipmap.ic_my_position));
            map.requestRender();
            map.setPosition(lngLat);


        } catch (JSONException e) {
            e.printStackTrace();
        }

        map.setZoom(14);

        map.setTapResponder(new TouchInput.TapResponder() {
            @Override
            public boolean onSingleTapUp(float x, float y) {
                return false;
            }

            @Override
            public boolean onSingleTapConfirmed(float x, float y) {

                final LngLat tap = map.screenPositionToLngLat(new PointF(x, y));
                String minName = "";
                double min = 100000000;
                for (int i = 0; i < lookupList.length(); i++) {

                    try {
                        JSONObject jsonObject = lookupList.getJSONObject(i);
                        double distance = GeoLocation.distance(tap.latitude, jsonObject.getDouble("lat"), tap.longitude, jsonObject.getDouble("lon"), true);

                        if (distance < .1 && distance < min) {
                            TextView mLocationTitle = (TextView) findViewById(R.id.locationTitle);
                            mLocationTitle.setText(jsonObject.getString("name").substring(0, lookupList.getJSONObject(i).getString("name").indexOf(',')));
                            TextView mLocationDesc = (TextView) findViewById(R.id.locationDesc);
                            mLocationDesc.setText(jsonObject.getString("name").substring(lookupList.getJSONObject(i).getString("name").indexOf(',')+2, jsonObject.getString("name").length()));
                            min = distance;
                            minName = jsonObject.getString("name");
                            ImageButton imageButton = (ImageButton) findViewById(R.id.mapActions);
                            final String[] geoCoords = {addressString(tap.latitude, tap.longitude, minName)};
                            imageButton.setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {

                                    geoCoords[0] = geoCoords[0].replace(' ', '+');
                                    openInNavApp(geoCoords[0]);

                                }
                            });

                            imageButton.setOnLongClickListener(new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View v) {
                                    CharSequence list_options[] = new CharSequence[]{getResources().getString(R.string.navigate), getResources().getString(R.string.share_this_location), getResources().getString(R.string.copy_address_place), getResources().getString(R.string.copy_gps)};
                                    AlertDialog.Builder builder = new AlertDialog.Builder(ViewMapActivity.this);
                                    builder.setTitle("Choose option");

                                    builder.setItems(list_options, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (which == 0) {
                                                geoCoords[0] = geoCoords[0].replace(' ', '+');
                                                openInNavApp(geoCoords[0]);
                                            }

                                            if (which == 1) {
                                                String shareBody = "home" + "\n" + geoCoords[0];
                                                sharePlace(shareBody);
                                            }

                                            if (which == 2) {
                                                String copyBody = "Home";
                                                copyToClipboard(copyBody);
                                            }

                                            if (which == 3) {
                                                String copyBody = gpsString(tap.latitude, tap.longitude);
                                                copyToClipboard(copyBody);
                                            }
                                            return;
                                        }
                                    });
                                    builder.show();

                                    return true;
                                }

                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                System.out.println(minName);
                return false;
            }
        });

    }

    /**
     * Address string string.
     *
     * @param lat   the lat
     * @param lon   the lon
     * @param label the label
     * @return the string
     */
    protected String addressString(double lat, double lon, String label){
        String location;
        label = label.replace(" " , "+");
        label = label.replace("," , "+");
        label = label.replace("++" , "+");
        if (prefs.getBoolean("use_google", false) == true) {
            location = "http://maps.google.com/maps?q=+" + lat + "+" + lon;
            return location;
        }else {
            location = "geo:" + lat + "," + lon + "?q="+ lat + "+" + lon + "("+label+")";
            return location;
        }
    }

    /**
     * Gps string string.
     *
     * @param lat the lat
     * @param lon the lon
     * @return the string
     */
    protected String gpsString(double lat, double lon){
        String location;
        if (prefs.getBoolean("use_google", false) == true) {
            location = "http://maps.google.com/maps?q=" + lat + "+" + lon;
            return location;
        }else {
            location = "geo:" + lat + "," + lon + "?q=" + lat + "," + lon;
            return location;
        }
    }

    /**
     * Open in nav app.
     *
     * @param geoCoords the geo coords
     */
    void openInNavApp(String geoCoords){
        try {
            Intent openInMaps = new Intent(Intent.ACTION_VIEW, Uri.parse(geoCoords));
            startActivity(openInMaps);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ViewMapActivity.this, getResources().getString(R.string.need_nav_app),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Share place.
     *
     * @param shareBody the share body
     */
    void sharePlace(String shareBody) {
        Intent sharingLocation = new Intent(android.content.Intent.ACTION_SEND);
        sharingLocation.setType("text/plain");
        sharingLocation.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.shared_location));
        sharingLocation.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingLocation, getResources().getString(R.string.share_this_location)));
    }

    /**
     * Copy to clipboard.
     *
     * @param copyBody the copy body
     */
    void copyToClipboard(String copyBody){
        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setText(copyBody);
        Toast.makeText(ViewMapActivity.this, getResources().getString(R.string.copied_to_clipboard),
                Toast.LENGTH_LONG).show();
    }
}
