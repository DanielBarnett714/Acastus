package me.dbarnett.acastus;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Author: Daniel Barnett
 */

/**
 * The type Main activity.
 */
public class MainActivity extends AppCompatActivity {
    /**
     * The Labels.
     */
    private ArrayList<String> labels = new ArrayList<>();
    /**
     * The Recents.
     */
    private ArrayList<String> recents = new ArrayList<>();
    /**
     * The constant lookupList.
     */
    private ArrayList<ResultNode> lookupList = new ArrayList<>();
    /**
     * The Can nav.
     */
    private boolean canNav = false;
    /**
     * The Cur lat.
     */
    private double curLat;
    /**
     * The Cur lon.
     */
    private double curLon;
    /**
     * The Results.
     */
    private GetResults results = null;
    /**
     * The Map time.
     */
    private Boolean mapTime = false;
    /**
     * If there is a search query waiting
     */
    private boolean searching = false;
    private Intent intent;
    private String action;
    private String type;
    SharedPreferences prefs;
    /**
     * The Search text.
     */
    EditText searchText;
    /**
     * The Results list.
     */
    ListView resultsList;
    /**
     * The Toolbar.
     */
    Toolbar toolbar;
    /**
     * The Make request.
     */
    protected MakeAPIRequest makeRequest;
    /**
     * The Geo location.
     */
    protected GeoLocation geoLocation;
    /**
     * The Use location.
     */
    private Boolean useLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        useLocation = prefs.getBoolean("use_location", true);
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        makeRequest = new MakeAPIRequest();
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        getLocationProvider(locationManager);

        geoLocation = new GeoLocation(locationManager);
        getInputs();
        updateRecentsList();
        startTimer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    useLocation = false;
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settings = new Intent(this, SettingsActivity.class);
            startActivity(settings);
            searchText.setText("");
            return true;
        }
        if (id == R.id.action_share_location) {
            shareLocation();
            return true;
        }
        if (id == R.id.clear_recents) {
            clearRecents();
            return true;
        }
        if (id == R.id.donate) {
            Intent donateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DanielBarnett714/Acastus"));
            startActivity(donateIntent);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Get inputs.
     */
    void getLocationProvider(LocationManager locationManager){
        if (locationManager != null){
            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    // Called when a new location is found by the network location provider.
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }else {
                Criteria criteria = new Criteria();
                criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
                String bestProvider = locationManager.getBestProvider(criteria, false);
                locationManager.requestLocationUpdates(bestProvider, 0, 50, locationListener);

            }

        }
    }
    private void getInputs(){
        searchText = (EditText) findViewById(R.id.searchText);
        handleIntent();
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                startSearch();
            }
        });

        ImageButton navigate = (ImageButton) findViewById(R.id.imageButton);
        navigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (canNav) {
                    if (!lookupList.isEmpty()){
                        ResultNode tempNode = lookupList.get(0);
                        Intent searchResult = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + tempNode.lat + "," + tempNode.lon));
                        startActivity(searchResult);
                    }
                }
            }
        });
    }

    /**
     * Clear recents.
     */
    private void clearRecents(){
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("recents");
        editor.apply();
        recents.clear();
        String[] data = recents.toArray(new String[recents.size()]);  // terms is a List<String>
        updateList(data);
        resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Object o = resultsList.getItemAtPosition(position);
                EditText searchQuery = (EditText) findViewById(R.id.searchText);
                searchQuery.setText(o.toString());
                if(lookupList.isEmpty()){
                    return;
                }
                ResultNode tempNode = lookupList.get(position);
                setRecents(tempNode.name);
                searchText.setText("");

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + tempNode.lat + "," + tempNode.lon));
                try{
                    startActivity(browserIntent);
                }catch (ActivityNotFoundException e){
                    Toast.makeText(MainActivity.this, "Must have Maps/Navigation App Installed",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Share location.
     */
    private void shareLocation(){
        Double[] coordinates = null;
        try {
            coordinates = geoLocation.getLocation();
        }catch (IllegalArgumentException e){
            System.out.println("Could not get location");
        }
        if (coordinates != null){
            double lat = coordinates[0];
            double lon = coordinates[1];

            String uri = "geo:" + lat + "," +lon + "?q=" + lat + "," + lon;
            String shareBody = "My current location:\n" + uri + "\n\nThis service was provided by https://github.com/danielbarnett714/Acastus";
            Intent sharingLocation = new Intent(android.content.Intent.ACTION_SEND);
            sharingLocation.setType("text/plain");
            sharingLocation.putExtra(android.content.Intent.EXTRA_SUBJECT, "My Current Location");
            sharingLocation.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingLocation, "Share Your Location"));
        }
    }

    /**
     * Handle intent.
     */
    private void handleIntent(){
        intent = getIntent();
        action = intent.getAction();
        type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {

            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            }
        } else if (Intent.ACTION_VIEW.equals(action)) {
            handleActionView(intent);
        }
    }

    /**
     * Reset time.
     */
    private void resetTime(){
        mapTime = true;
        canNav = false;
        EditText searchQuery = (EditText) findViewById(R.id.searchText);
        String urlString = searchQuery.getText().toString();
        results = null;
        results = new GetResults();
        results.execute(urlString);
    }

    /**
     * Start timer.
     */
    private void startTimer(){
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                mapTime = false;
                if (searching == true){
                    resetTime();
                    searching = false;
                }
            }
        }, 0, 3334);
    }

    /**
     * Start search.
     */
    private void startSearch(){
        if (searchText.getText().toString().isEmpty()){
            updateRecentsList();
            return;
        }
        if (mapTime == false) {
            resetTime();
        }
        else {
            searching = true;
        }
    }

    /**
     * Handle send text.
     *
     * @param intent the intent
     */
    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            EditText searchQuery = (EditText) findViewById(R.id.searchText);
            searchQuery.setText(sharedText);
            startSearch();
        }
    }

    /**
     * Handle action view.
     *
     * @param intent the intent
     */
    void handleActionView(Intent intent) {
        try {
            URI uri = new URI(intent.getData().toString());
            String q = uri.getQuery();
            if (q != null) {
                EditText searchQuery = (EditText) findViewById(R.id.searchText);
                String addr = q.substring(q.indexOf("=") + 1).replace("\n",",");
                searchQuery.setText(addr);
                startSearch();
            }
        } catch (URISyntaxException e) {
            // Probably ought to put something here
        }
    }

    /**
     * Set recents.
     *
     * @param name the name
     */
    private void setRecents(String name){
        SharedPreferences.Editor editor = prefs.edit();
        if (prefs.getBoolean("store_recents", true) == false){
            return;
        }
        if (recents.contains(name)){
            recents.remove(name);
        }
        recents.add(0, name);
        JSONArray mJSONArray = new JSONArray(recents);
        editor.remove("recents");
        editor.apply();
        editor.putString("recents", mJSONArray.toString());
        editor.apply();
    }

    /**
     * Update list.
     *
     * @param data the data
     */
    private void updateList(String[] data){
        ArrayAdapter<?> adapter = new ArrayAdapter<Object>(this, android.R.layout.simple_selectable_list_item, data);
        resultsList = (ListView) findViewById(R.id.resultsList);
        resultsList.setAdapter(adapter);
        resultsList.setClickable(true);
    }

    /**
     * Update results list.
     */
    private void updateResultsList(){
        if (searchText.getText().toString().isEmpty()){
            updateRecentsList();
            return;
        }
        String[] data;
        data = labels.toArray(new String[labels.size()]);  // terms is a List<String>
        updateList(data);
        resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Object result = resultsList.getItemAtPosition(position);
                EditText searchQuery = (EditText) findViewById(R.id.searchText);
                searchQuery.setText(result.toString());
                if(lookupList.isEmpty()){
                    return;
                }
                ResultNode tempNode = lookupList.get(position);
                setRecents(tempNode.name);
                try {
                    Intent openInMaps = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + tempNode.lat + "," + tempNode.lon));
                    startActivity(openInMaps);
                    searchText.setText("");
                }catch (ActivityNotFoundException e){
                    Toast.makeText(MainActivity.this, "You must have a Maps/Navigation App installed.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Update recents list.
     */
    private void updateRecentsList(){
        String recentsStore = prefs.getString("recents", null);
        JSONArray mJSONArray = null;
        resultsList = (ListView) findViewById(R.id.resultsList);
        if (recentsStore != null){
            try {
                mJSONArray = new JSONArray(recentsStore);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            recents = null;
            recents = new ArrayList<>();
            if (mJSONArray != null){
                for (int i=0;i<mJSONArray.length();i++){
                    try {
                        recents.add(mJSONArray.get(i).toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            String[] data;
            data = recents.toArray(new String[recents.size()]);  // terms is a List<String>
            updateList(data);
            resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    Object result = resultsList.getItemAtPosition(position);
                    EditText searchQuery = (EditText) findViewById(R.id.searchText);
                    searchQuery.setText(result.toString());
                }
            });
        }else {
            resultsList = (ListView) findViewById(R.id.resultsList);
            resultsList.clearChoices();
        }
    }

    /**
     * Fill lists.
     *
     * @param object the object
     * @throws JSONException the json exception
     */
    void fillLists(JSONObject object) throws JSONException {
        JSONArray array = object.getJSONArray("features");
        lookupList.clear();
        labels.clear();
        double lat, lon;
        for (int i = 0; i < array.length(); i++) {
            JSONObject initialArray = array.getJSONObject(i);
            JSONObject geometry = initialArray.getJSONObject("geometry");
            JSONObject properties = initialArray.getJSONObject("properties");
            JSONArray coordinates = geometry.getJSONArray("coordinates");
            lat = coordinates.getDouble(1);
            lon = coordinates.getDouble(0);
            String name = properties.getString("label");
            ResultNode tempNode = new ResultNode();
            tempNode.lat = lat;
            tempNode.lon = lon;
            tempNode.name = name;
            if (useLocation) {
                Double distance = geoLocation.distance(curLat, lat, curLon, lon);
                labels.add(name + " : " + distance + " mi");
            }else {
                labels.add(name);
            }
            lookupList.add(tempNode);
        }
    }

    /**
     * Fetch search results.
     *
     * @param searchQuery the search query
     * @throws IOException   the io exception
     * @throws JSONException the json exception
     */
    private void fetchSearchResults(String searchQuery) throws IOException, JSONException {
        JSONObject object = makeRequest.fetchSearchResults(searchQuery);
        fillLists(object);
    }

    /**
     * Set search query string.
     *
     * @param input the input
     * @return the string
     */
    private String setSearchQuery(String input){
        Double[] coordinates = null;
        String serverAddress = prefs.getString("server_url", null);
        if (serverAddress == null){
            serverAddress = getResources().getString(R.string.server_url_setting);
        }
        useLocation = prefs.getBoolean("use_location", true);
        if (useLocation){
            coordinates = geoLocation.getLocation();
            if (coordinates != null){
                curLat = coordinates[0];
                curLon = coordinates[1];
            }
        }
        String searchQuery;
        if (useLocation && coordinates != null){
            searchQuery = serverAddress + "/v1/autocomplete?" + "focus.point.lat=" + curLat + "&focus.point.lon=" + curLon + "&text=" + input;
        }else {
            searchQuery = serverAddress + "/v1/autocomplete?text=" + input;
        }
        searchQuery = searchQuery.replace(' ', '+');

        return searchQuery;
    }

    private class GetResults extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            canNav = true;
            updateResultsList();
        }
        @Override
        protected String doInBackground(String... strings) {
            String searchQuery = setSearchQuery(strings[0]);
            if (searchQuery != null){
                try {
                    fetchSearchResults(searchQuery);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}