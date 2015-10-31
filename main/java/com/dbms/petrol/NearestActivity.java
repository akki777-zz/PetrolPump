package com.dbms.petrol;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class NearestActivity extends FragmentActivity implements OnMapReadyCallback {

    // Declaring a Location Manager
    protected LocationManager locationManager;
    TextToSpeech tts;
    ProgressDialog dialog;
    public ArrayList<String> placesList, coordinatesList, filteredList, placesNameList;
    HashMap<Integer, String> hashTitle;
    HashMap<String, Integer> hashCoord;

    String currentCoord,name_pp;
    Double currentLat, currentLong;

    String nearestPetrolName = null, nearestPetrolCoord = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        placesList = new ArrayList<String>(); //initialising ArrayList
        placesNameList = new ArrayList<String>();
        coordinatesList = new ArrayList<String>();
        filteredList = new ArrayList<String>();
        hashTitle = new HashMap<Integer, String>();
        hashCoord = new HashMap<String, Integer>();

        tts = new TextToSpeech(NearestActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.ENGLISH);
                }
            }
        });

        Button btnLoc = (Button) findViewById(R.id.btnLoc);
        btnLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) == false) {
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                } else {
                    getCurrentLoc();
                }
            }
        });


        Button btnGo = (Button) findViewById(R.id.btnGo);
        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + nearestPetrolCoord + "&mode=d");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);

            }
        });

        Button btnNearest = (Button) findViewById(R.id.btnNearest);
        btnNearest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog = ProgressDialog.show(NearestActivity.this, "Finding Petrol Pumps", "Please wait", true);
                Log.d("tag", currentCoord);
                new readFromGooglePlaceAPI()
                        .execute("https://maps.googleapis.com/maps/api/place/nearbysearch/json?"
                                + "location=" + currentCoord + "&radius=5000&sensor=true&"
                                + "key=AIzaSyCpM2W2hVW8TyNXR5sqVOU2jYQFh0xM6_w&types=gas_station");

            }
        });

        Button btnData = (Button) findViewById(R.id.btnData);
        btnData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (tts != null) {
                    tts.stop();
                    tts.shutdown();
                }

                Bundle b = new Bundle();
                Intent databaseIntent = new Intent(NearestActivity.this, DatabaseActivity.class);
                b.putString("curr_loc", currentCoord);
                b.putString("nearest", nearestPetrolCoord);
                b.putString("petrol_level", "3");
                b.putString("name_pp",name_pp);
                databaseIntent.putExtras(b);
                startActivity(databaseIntent);
            }
        });

    }

    public void showNearestOnMap() {
        dialog = ProgressDialog.show(NearestActivity.this, "Finding Min. Distance", "Please wait", true, false);

        String url = null;
        for (int k = 0; k < coordinatesList.size(); k++) {

            url = "https://maps.googleapis.com/maps/api/distancematrix/json?" +
                    "origins=" + coordinatesList.get(k).toString() + "&destinations=" + currentCoord +
                    "&key=AIzaSyDJulpVXmBXSDyOkTZmel-vmbujCtfmPmc";
            //System.out.println(url);

            callAsync(url, coordinatesList.get(k), k);

        }
    }

    public void callAsync(final String url, final String coordinates, final Integer index) {

        new AsyncTask<String, Void, String>() {

            @Override
            protected String doInBackground(String... param) {
                return readJSON(param[0]);
            }

            protected void onPostExecute(String str) {

                try {
                    JSONObject root = new JSONObject(str);
                    JSONArray rows = root.getJSONArray("rows");

                    for (int i = 0; i < rows.length(); i++) {
                        JSONObject arrayItemsRows = rows.getJSONObject(i);
                        JSONArray elements = arrayItemsRows.getJSONArray("elements");

                        for (int j = 0; j < elements.length(); j++) {
                            JSONObject arrayItemsElements = elements.getJSONObject(j);
                            JSONObject distance = arrayItemsElements.getJSONObject("distance");

                            String dValue = distance.getString("value"); //distance in metres

                            JSONObject duration = arrayItemsElements.getJSONObject("duration");
                            String dDuration = duration.getString("text");
                            if (Integer.valueOf(dValue) < 4000) {
                                filteredList.add(coordinates);
                                hashCoord.put(coordinates, Integer.valueOf(dValue));
                                placesNameList.add(hashTitle.get(index) + ":" + dDuration.toString());
                                Log.d("Distance", index + ")" + dValue + "::::" + dDuration);
                            }

                        }

                    }

                } catch (Exception e) {

                }
                System.out.println(index + "********************************************************************");
                if (index == (coordinatesList.size() - 1)) {
                    finished();
                }

            }

        }.execute(url);

    }

    public void finished() {
        dialog.dismiss();
        Log.d("TAG", "here");
        for (int i = 0; i < filteredList.size(); i++) {
            System.out.println("FL " + filteredList.get(i).toString());
        }

        Log.d("TAG", "intialiseMap");
        initialiseMap();
        findMinimum();
    }

    private void findMinimum() {
        int minDist = hashCoord.get(filteredList.get(0));
        int index = 0;
        for (int i = 0; i < hashCoord.size(); i++) {
            int distance = hashCoord.get(filteredList.get(i));
            System.out.println(distance);
            if (distance <= minDist) {
                minDist = distance;
                index = i;
            }
        }
        System.out.println("Min is " + minDist + " Coordinates are " + filteredList.get(index) + "Name is " + placesNameList.get(index));
        nearestPetrolCoord = filteredList.get(index);
        nearestPetrolName = placesNameList.get(index);
        String[] str= nearestPetrolName.split(":");
        name_pp = str[0];
        Log.d("NearestPump", name_pp);
    }

    public void initialiseMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(NearestActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        /*for (int i = 0; i < filteredList.size(); i++) {
            String[] coord = filteredList.get(i).split(",");
            LatLng marker = new LatLng(Double.valueOf(coord[0]), Double.valueOf(coord[1]));
            map.addMarker(new MarkerOptions().position(marker).icon(BitmapDescriptorFactory.fromResource(R.drawable.fillingstation))
                    .title(placesNameList.get(i) + filteredList.get(i)));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 12));
        }*/

        //showing only nearest petrol pump
        String[] coord = nearestPetrolCoord.split(",");
        LatLng marker = new LatLng(Double.valueOf(coord[0]), Double.valueOf(coord[1]));
        map.addMarker(new MarkerOptions()
                .title(nearestPetrolName)
                .position(marker)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.fillingstation)));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 14));

        map.addMarker(new MarkerOptions().position(new LatLng(currentLat, currentLong)).title("My Postion").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

        // MAP WORK FINISHES HERE : WE GOT THE NEAREST PETROL PUMP ON MAPS
        // NOW ASSISTANT WORK STARTS
        String toSpeak = "Nearest Petrol Pump is " + nearestPetrolName + "away.";
        tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void getCurrentLoc() {
        GPSTracker gps = new GPSTracker(NearestActivity.this);
        currentLat = gps.getLatitude();
        currentLong = gps.getLongitude();
        currentCoord = String.valueOf(currentLat.toString() + "," + currentLong.toString());
        Toast.makeText(getApplicationContext(), "Your Location is - " + currentCoord, Toast.LENGTH_LONG).show();
    }

    public class readFromGooglePlaceAPI extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... param) {
            return readJSON(param[0]);
        }

        protected void onPostExecute(String str) {
            //myArrayList = new ArrayList<GetterSetter>();
            try {
                JSONObject root = new JSONObject(str);
                JSONArray results = root.getJSONArray("results");
                Log.d("tag", String.valueOf(results.length()));
                for (int i = 0; i < results.length(); i++) {
                    JSONObject arrayItems = results.getJSONObject(i);
                    JSONObject geometry = arrayItems.getJSONObject("geometry");
                    JSONObject location = geometry.getJSONObject("location");

                    String pLati = location.getString("lat");
                    String pLongi = location.getString("lng");
                    String pName = arrayItems.getString("name").toString();
                    //Log.d("tag", pName + "--> " + pLati + "::::" + pLongi);

                    /*addValues = new GetterSetter();
                    addValues.setLat(pLati);
                    addValues.setLon(pLongi);
                    addValues.setName(pName);
                    myArrayList.add(addValues);*/

                    placesList.add(pName + ":" + pLati + "," + pLongi);

                    //Log.d("Before", myArrayList.toString());

                }

            } catch (Exception e) {

            }
            System.out.println("############################################################################");
            //Log.d("After:", myArrayList.toString());
            // adapter.notifyDataSetChanged();
            dialog.dismiss();
            splitString();
        }

    }

    public String readJSON(String URL) {
        StringBuilder sb = new StringBuilder();
        HttpGet httpGet = new HttpGet(URL);
        HttpClient client = new DefaultHttpClient();

        try {
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } else {
                Log.e("JSON", "Couldn't find JSON file");
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public void splitString() {

        for (int i = 0; i < placesList.size(); i++) {
            //Splitting the arrayList items into coordinates and names
            String listItem = placesList.get(i).toString();
            String[] str_array = listItem.split(":");
            String nName = str_array[0];
            String[] coord = str_array[1].split(",");
            String nLat = coord[0];
            String nLong = coord[1];
            Log.d("Strings", i + ")" + nName + nLat + nLong);

            hashTitle.put(i, nName);
            coordinatesList.add(nLat + "," + nLong);
        }

        //do the distance API work
        showNearestOnMap();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
