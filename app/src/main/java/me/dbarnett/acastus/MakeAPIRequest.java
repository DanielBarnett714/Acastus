package me.dbarnett.acastus;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Author: Daniel Barnett
 */
public class MakeAPIRequest {

    /**
     * Fetch search results json object.
     *
     * @param searchQuery the search query
     * @return the json object
     * @throws IOException   the io exception
     * @throws JSONException the json exception
     */
    public JSONObject fetchSearchResults(String searchQuery) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        URL url = new URL(searchQuery);
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000 /* milliseconds */);
        urlConnection.setConnectTimeout(15000 /* milliseconds */);
        urlConnection.setDoOutput(true);
        urlConnection.connect();
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        String jsonString;
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();
        jsonString = sb.toString();
        return new JSONObject(jsonString);
    }

    /**
     * Is json valid boolean.
     *
     * @param checkObject the check object
     * @return the boolean
     */
    public static boolean isJSONValid(JSONObject checkObject) {
        try {
            JSONObject geocoding = checkObject.getJSONObject("geocoding");
            JSONObject engine = geocoding.getJSONObject("engine");
            String engineName = engine.getString("name");
            if (!engineName.equals("Pelias")){
                return false;
            }

        } catch (JSONException ex) {
            return false;
        }
        return true;
    }
}
