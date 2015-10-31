package com.dbms.petrol;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class DatabaseActivity extends ActionBarActivity {

    String currentLoc, nearestLoc, petrol_level,name_pp;

    // Progress Dialog
    private ProgressDialog pDialog;

    JSONParser jsonParser = new JSONParser();
    // JSON Node names
    private static final String TAG_SUCCESS = "success";


    private static String url_insert = "http://socketchat.tk/insert.php";  // url to insert data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);


        Button btnSend = (Button) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getBundle();
                new insertDetails().execute();
            }
        });


    }

    public class insertDetails extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(DatabaseActivity.this);
            pDialog.setMessage("Inserting into DB");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... args) {
            // Building Parameters
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("curr_loc", currentLoc));
            params.add(new BasicNameValuePair("nearest", nearestLoc));
            params.add(new BasicNameValuePair("petrol_level", petrol_level));
            params.add(new BasicNameValuePair("name_pp", name_pp));



            JSONObject json = jsonParser.makeHttpRequest(url_insert, "POST", params);

            // check log cat for response
           /* if(json!=null){
                Log.d("Create Response", json.toString());
            }else{
                Log.d("Create Response", "Error Occurred while creating JSON");
            }*/

           /* try{
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost =new HttpPost(url_insert);
                httpPost.setEntity(new UrlEncodedFormEntity(params));
                HttpResponse httpResponse = httpClient.execute(httpPost);

                HttpEntity httpEntity = httpResponse.getEntity();
            } catch(ClientProtocolException e){

            } catch (IOException e){

            }
            return "success";*/

            // check for success tag
            try {
                int success = json.getInt(TAG_SUCCESS);

                if (success == 1) {
                    // successfull
                    Log.d("Success", "Success");
                } else {
                    // failed
                    Log.d("Failed", "Failed");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            pDialog.dismiss();
        }
    }

    private void getBundle() {
        //getting bundle from MainActivity
        Intent in = getIntent();
        Bundle b = in.getExtras();
        currentLoc = b.getString("curr_loc");
        nearestLoc = b.getString("nearest");
        petrol_level = b.getString("petrol_level");
        name_pp = b.getString("name_pp");

        Log.d("db", currentLoc + "::" + nearestLoc + "::" + petrol_level.toString() + name_pp);

    }

}
