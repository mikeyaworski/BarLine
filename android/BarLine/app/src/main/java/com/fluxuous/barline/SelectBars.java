package com.fluxuous.barline;

import android.content.Intent;
import android.graphics.Color;
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

public class SelectBars extends AppCompatActivity {

    private String province, city, country, countryCode;

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
            province = extras.getString("province");
            city = extras.getString("city");
            country = extras.getString("country");
            countryCode = extras.getString("countryCode");
            TextView lblCity = (TextView)findViewById(R.id.lblSubtitle);
            lblCity.setText(city);
        }
        TextView lblTitle = (TextView)findViewById(R.id.lblTitle);
        lblTitle.setText("Select a Bar");

        refresh();
    }

    public void populateBars(final ArrayList<HashMap<String,String>> barsList) {

        // set the sections headers for the bars before passing to the adapter
        char currentHeader = 'a';
        for (int i = 0; i < barsList.size(); i++) {
            HashMap<String, String> bar = barsList.get(i);

            if (bar.containsKey("name")) {
                char firstChar = bar.get("name").toLowerCase().charAt(0);

                // only occurs if there is no bar name that starts with an 'a'
                if (firstChar > currentHeader) {
                    currentHeader = firstChar;
                }

                if (firstChar == currentHeader) {
                    HashMap<String, String> sectionHeader = new HashMap<>();
                    sectionHeader.put("header", String.valueOf(currentHeader).toUpperCase());
                    sectionHeader.put("itemType", "sectionHeader");
                    barsList.add(i, sectionHeader);
                    i++; // increment twice to get over the sectionHeader just added
                    currentHeader++; // next character, so the following bars that start with the same letter won't be duplicated
                }
            }
        }

        // populate the ListView
        BarListAdapter barsCityListAdapter = new BarListAdapter(this, barsList);
        ListView barsListView = (ListView)findViewById(R.id.listView);
        barsListView.setAdapter(barsCityListAdapter);

        barsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
                // don't let them click on a header
                if ((Boolean)view.getTag(R.id.HEADER_TAG)) {
                    view.setBackgroundColor(Color.TRANSPARENT);
                    return;
                }

                Intent intent = new Intent (SelectBars.this, WaitTime.class);
                intent.putExtra("bar", barsList.get(position).get("name"));
                intent.putExtra("barId", Integer.parseInt(barsList.get(position).get("id")));
                startActivity(intent);
                overridePendingTransition(R.anim.right_to_left_enter, R.anim.right_to_left_exit);
            }
        });
    }

    private void refresh() {
        if (Helper.isOnline(this)) {
            new ShowBars().execute();
        } else {
            Helper.toast(getApplicationContext(), getResources().getString(R.string.no_internet_message));
        }
    }

    public void refresh(View v) {
        refresh();
    }

    private class ShowBars extends AsyncTask<String, Void, HashMap<String, Object>> {

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
                urlParams.add(new AbstractMap.SimpleEntry("city", city));

                URL url = new URL(Config.getApiUrlBars(urlParams));
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
                    JSONObject jObject = new JSONObject(result);
                    if (jObject.has("error")) {
                        Helper.toast(getApplicationContext(), String.valueOf(status) + " - " + jObject.getString("error"), true);
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

            ArrayList<HashMap<String,String>> barsList = new ArrayList<>();

            try {
                JSONArray jsonArray = new JSONArray(result);

                for (int i = 0; i < jsonArray.length(); i++) {
                    int barId = jsonArray.getJSONObject(i).getInt("id");
                    String barName = jsonArray.getJSONObject(i).getString("name");
                    HashMap<String,String> bar = new HashMap<>();
                    bar.put("id", String.valueOf(barId));
                    bar.put("name", barName);
                    barsList.add(bar);
                }

            } catch (JSONException je) {
                je.printStackTrace();
                Crashlytics.logException(je);
                Helper.toast(getApplicationContext(), String.valueOf(status) + " - " + getResources().getString(R.string.generic_error_message), true);
            }

            findViewById(R.id.loadingAnimation).setVisibility(View.GONE);
            findViewById(R.id.btnRefresh).setEnabled(true);
            populateBars(barsList);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.left_to_right_enter, R.anim.left_to_right_exit);
    }

}
