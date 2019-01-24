package com.fluxuous.barline;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class SelectCity extends AppCompatActivity {

    // defaults (for now)
    private String province = "Ontario";
    private String country = "Canada";
    private String countryCode = "CA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select);
        Fabric.with(this, new Crashlytics());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey("province")) {
                province = extras.getString("province");
                TextView lblProvince = (TextView)findViewById(R.id.lblSubtitle);
                lblProvince.setText(province);
            }
            if (extras.containsKey("country")) {
                country = extras.getString("country");
            }
            if (extras.containsKey("countryCode")) {
                countryCode = extras.getString("countryCode");
            }
        }
        TextView lblTitle = (TextView)findViewById(R.id.lblTitle);
        lblTitle.setText("Select a City" );

        refresh();
    }

    public void populateCities(final ArrayList<String> citiesList) {

        // populate the ListView
        CityListAdapter citiesCityListAdapter = new CityListAdapter(this, citiesList);
        ListView citiesListView = (ListView)findViewById(R.id.listView);
        citiesListView.setAdapter(citiesCityListAdapter);

        citiesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent (SelectCity.this, SelectBars.class);
                intent.putExtra("country", country);
                intent.putExtra("countryCode", countryCode);
                intent.putExtra("province", province);
                intent.putExtra("city", citiesList.get(position));
                startActivity(intent);
                overridePendingTransition(R.anim.right_to_left_enter, R.anim.right_to_left_exit);
            }
        });
    }

    private void refresh() {
        if (Helper.isOnline(this)) {
            new ShowCities().execute();
        } else {
            Helper.toast(getApplicationContext(), getResources().getString(R.string.no_internet_message));
        }
    }

    public void refresh(View v) {
        refresh();
    }

    private class ShowCities extends AsyncTask<String, Void, HashMap<String, Object>> {

        @Override
        protected void onPreExecute() {
            findViewById(R.id.loadingAnimation).setVisibility(View.VISIBLE);
            findViewById(R.id.btnRefresh).setEnabled(false);
        }

        @Override
        protected HashMap<String, Object> doInBackground(String... params) {

            HashMap<String, Object> response = new HashMap<>();

            try {

                List<AbstractMap.SimpleEntry> urlParams = new ArrayList<>();
                urlParams.add(new AbstractMap.SimpleEntry("countryCode", country));
                urlParams.add(new AbstractMap.SimpleEntry("country", country));
                urlParams.add(new AbstractMap.SimpleEntry("province", province));

                URL url = new URL(Config.getApiUrlCities(urlParams));
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection)url.openConnection();

                InputStream in;
                if (httpsURLConnection.getResponseCode() >= 400) in = new BufferedInputStream(httpsURLConnection.getErrorStream());
                else in = new BufferedInputStream(httpsURLConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, "iso-8859-1"), 8);

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line + "\n");
                }

                response.put("result", result.toString());
                response.put("status", httpsURLConnection.getResponseCode());

                in.close();
                httpsURLConnection.disconnect();

                return response;

            } catch(Exception e) {
                e.printStackTrace();
                Crashlytics.logException(e);

                response.put("result", e.toString());
                response.put("status", 500);
                return response;
            }
        }

        @Override
        protected void onPostExecute(HashMap<String, Object> response) {

            int status = (Integer)response.get("status");
            String result = (String)(response.get("result"));

            // throw an error if something goes wrong
            if (status >= 400) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    if (jsonObject.has("error")) {
                        Helper.toast(getApplicationContext(), String.valueOf(status) + " - " + jsonObject.getString("error"), true);
                    } else {
                        Helper.toast(getApplicationContext(), String.valueOf(status) + " - " + getResources().getString(R.string.generic_error_message), true);
                    }
                } catch (JSONException je) {
                    je.printStackTrace();
                    Crashlytics.logException(je);
                    Helper.toast(getApplicationContext(), String.valueOf(status) + " - " + getResources().getString(R.string.generic_error_message), true);
                }
                findViewById(R.id.loadingAnimation).setVisibility(View.GONE);
                findViewById(R.id.btnRefresh).setEnabled(true);
                return;
            }

            ArrayList<String> citiesList = new ArrayList<>();
            try {
                JSONArray jsonArray = new JSONArray(result);

                for (int i = 0; i < jsonArray.length(); i++) {
                    String city = jsonArray.getJSONObject(i).getString("name");
                    citiesList.add(city);
                }

            } catch (JSONException je) {
                je.printStackTrace();
                Crashlytics.logException(je);
                Helper.toast(getApplicationContext(), String.valueOf(status) + " - " + getResources().getString(R.string.generic_error_message), true);
            }

            findViewById(R.id.loadingAnimation).setVisibility(View.GONE);
            findViewById(R.id.btnRefresh).setEnabled(true);
            populateCities(citiesList);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //overridePendingTransition(android.R.anim.slide_out_right, android.R.anim.slide_in_left);
        //overridePendingTransition(R.anim.right_to_left_enter, R.anim.right_to_left_exit);
        overridePendingTransition(R.anim.left_to_right_enter, R.anim.left_to_right_exit);
    }
}
