package com.dbms.petrol;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class StatsActivity extends FragmentActivity implements OnMapReadyCallback {

    // Progress Dialog
    private ProgressDialog pDialog;
    ArrayList<String> coordList;
    ArrayList<String> placesNameList;

    private static String url_retrieve = "http://socketchat.tk/sendjson.php";  // url to retrieve data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        placesNameList = new ArrayList<String>();
        coordList = new ArrayList<String>();

        //fetch data
        new getDetails().execute();
    }

    @Override
    public void onMapReady(GoogleMap map) {

        for (int i = 0; i < placesNameList.size(); i++) {
            String[] coord = coordList.get(i).split(",");
            String lati = coord[0];
            String longi = coord[1];
            LatLng marker = new LatLng(Double.valueOf(lati),Double.valueOf(longi));
            map.addMarker(new MarkerOptions().position(marker).icon(BitmapDescriptorFactory.fromResource(R.drawable.fillingstation))
                    .title(placesNameList.get(i)));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 12));
        }
    }

    public void initialiseMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(StatsActivity.this);
    }

    public class getDetails extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(StatsActivity.this);
            pDialog.setMessage("Retreiving from DB");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... args) {

            // Creating new JSON Parser
            JSONParser jParser = new JSONParser();

            // Getting JSON from URL
            JSONObject json = jParser.getJSONFromUrl(url_retrieve);

            try {
                //JSONObject root = new JSONObject(args);
                JSONArray pid = json.getJSONArray("pid");
                JSONArray first = pid.getJSONArray(0);

                for(int i=0;i<first.length();i++){
                    JSONArray arrayItems = first.getJSONArray(i);
                    JSONArray second = first.getJSONArray(0);
                    String index = arrayItems.getString(0);
                    String date_time = arrayItems.getString(1);
                    String nearest_loc = arrayItems.getString(3);
                    String name_pp = arrayItems.getString(5);
                    placesNameList.add(name_pp + " " +  date_time);
                    coordList.add(nearest_loc);

                    Log.d("Retrieve", index + ":" + date_time + ":" + name_pp + ":" + nearest_loc);

                }


            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(String str) {
            super.onPostExecute(str);
            pDialog.dismiss();
            initialiseMap();
        }
    }
}