package com.fluxuous.barline;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class WaitTime extends AppCompatActivity {

    private String bar;
    private Integer id = -1, waitTime = 0, crowdingLevel = 0;
    private String adImg, adText, adLink;

    private Context thisContext;

    private boolean firstLoad = true;

    private CircleProgressBar progressBar;
    private Button btnShowWaitTimeSelected, btnShowWaitTimeNotSelected, btnCrowdingLevelSelected, btnCrowdingLevelNotSelected;
    private TextView lblEventDetails, lblProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wait_time);
        Fabric.with(this, new Crashlytics());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        thisContext = this;

        progressBar = (CircleProgressBar) findViewById(R.id.circularProgressBar);
        lblProgress = (TextView)findViewById(R.id.lblProgress);

        btnShowWaitTimeSelected = (Button)findViewById(R.id.btnShowWaitTimeSelected);
        btnShowWaitTimeSelected.setVisibility(View.VISIBLE);
        btnShowWaitTimeNotSelected = (Button)findViewById(R.id.btnShowWaitTimeNotSelected);
        btnShowWaitTimeNotSelected.setVisibility(View.GONE);
        btnCrowdingLevelSelected = (Button)findViewById(R.id.btnShowCrowdingLevelSelected);
        btnCrowdingLevelSelected.setVisibility(View.GONE);
        btnCrowdingLevelNotSelected = (Button)findViewById(R.id.btnShowCrowdingLevelNotSelected);
        btnCrowdingLevelNotSelected.setVisibility(View.VISIBLE);

        lblEventDetails = (TextView)findViewById(R.id.lblEventDetails);
        lblEventDetails.setVisibility(View.GONE); // hide until the ad can be loaded

        // get data about the province for the cities to be displayed
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            bar = extras.getString("bar");
            id = extras.getInt("barId");

            TextView lblBarName = (TextView)findViewById(R.id.lblBarName);
            lblBarName.setText(bar);

            findViewById(R.id.btnEdit).setEnabled(false); // disable until the id is loaded
            //refresh(false); // no need because it gets called in onResume
        }
    }

    // let them share a handler, so that when changing between wait time and crowding tabs, I can clear the handler callbacks (stop the animation update from the other progress bar)
    final Handler handler = new Handler();

    private void showWaitTimeProgress() {
        progressBar.setProgress(0);
        progressBar.setMax(60);
        progressBar.setProgressWithAnimation(waitTime);

        final Context context = this;

        // use a runnable to update the colour of the progress bar and the progress text while it's animating
        handler.removeCallbacksAndMessages(null); // remove the callbacks so that the other tab's animations don't interfere with this one
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    lblProgress.setText(Helper.getFormattedWaitTime((int)progressBar.getProgress()));
                    if (progressBar.getProgress() * 1.0 / progressBar.getMax() <= 1.0/3) {
                        progressBar.setColor(ContextCompat.getColor(context, R.color.progressGreen));
                    } else if (progressBar.getProgress() * 1.0 / progressBar.getMax() <= 2.0/3) {
                        progressBar.setColor(ContextCompat.getColor(context, R.color.progressYellow));
                    } else {
                        progressBar.setColor(ContextCompat.getColor(context, R.color.progressRed));
                    }
                }
                catch (Exception e) { }
                finally {
                    // keep calling it while the progress bar is not completed its animation
                    if (progressBar.getProgress() != waitTime) {
                        handler.postDelayed(this, 100);
                    }
                }
            }
        };
        handler.postDelayed(runnable, 0);

        lblProgress.setText(Helper.getFormattedWaitTime(waitTime));
    }
    private void showCrowdingLevelProgress() {
        progressBar.setProgress(0);
        progressBar.setMax(100);
        progressBar.setProgressWithAnimation(crowdingLevel);

        final Context context = this;

        // use a runnable to update the colour of the progress bar and the progress text while it's animating
        handler.removeCallbacksAndMessages(null); // remove the callbacks so that the other tab's animations don't interfere with this one
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {
                    lblProgress.setText(Helper.getFormattedCrowdingLevel((int)progressBar.getProgress()));
                    if (progressBar.getProgress() * 1.0 / progressBar.getMax() <= 1.0/3) {
                        progressBar.setColor(ContextCompat.getColor(context, R.color.progressGreen));
                    } else if (progressBar.getProgress() * 1.0 / progressBar.getMax() <= 2.0/3) {
                        progressBar.setColor(ContextCompat.getColor(context, R.color.progressYellow));
                    } else {
                        progressBar.setColor(ContextCompat.getColor(context, R.color.progressRed));
                    }
                }
                catch (Exception e) { }
                finally {
                    // keep calling it while the progress bar is not completed its animation
                    if (progressBar.getProgress() != crowdingLevel) {
                        handler.postDelayed(this, 100);
                    }
                }
            }
        };
        handler.postDelayed(runnable, 0);

        lblProgress.setText(Helper.getFormattedCrowdingLevel(crowdingLevel));
    }

    public void showWaitTime(View view) {

        if (btnShowWaitTimeSelected.getVisibility() != View.VISIBLE) { // not already selected
            btnShowWaitTimeSelected.setVisibility(View.VISIBLE);
            btnShowWaitTimeNotSelected.setVisibility(View.GONE);
            btnCrowdingLevelNotSelected.setVisibility(View.VISIBLE);
            btnCrowdingLevelSelected.setVisibility(View.GONE);
            showWaitTimeProgress();
        }
    }
    public void showCrowdingLevel(View view) {

        if (btnCrowdingLevelSelected.getVisibility() != View.VISIBLE) { // not already selected
            btnShowWaitTimeSelected.setVisibility(View.GONE);
            btnShowWaitTimeNotSelected.setVisibility(View.VISIBLE);
            btnCrowdingLevelNotSelected.setVisibility(View.GONE);
            btnCrowdingLevelSelected.setVisibility(View.VISIBLE);
            showCrowdingLevelProgress();
        }
    }

    public void refresh() {
        if (Helper.isOnline(this)) {
            TextView lblCurrentTime = (TextView)findViewById(R.id.lblCurrentTime);
            lblCurrentTime.setText(Helper.getCurrentTime());
            new ShowTimes().execute();
        } else {
            Helper.toast(getApplicationContext(), getResources().getString(R.string.no_internet_message), false);
        }
    }

    public void refresh(View v) {
        refresh();
    }

    private boolean cameFromAd = false;

    public void onResume() {
        super.onResume();
        if (!cameFromAd) { // don't refresh the data when they come back from an ad
            refresh();
        } else {
            // animate the progress bar though
            if (btnShowWaitTimeSelected.getVisibility() == View.VISIBLE) showWaitTimeProgress();
            else showCrowdingLevelProgress();
            cameFromAd = false;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // result came from the ad activity (ad was just closed)
        if (requestCode == Config.AD_REQUEST_CODE) {
            // don't care about what the result of the activity was
            cameFromAd = true;
        }
    }

    public void edit(View v) {

        final Dialog passwordDialog = new Dialog(thisContext);
        passwordDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        passwordDialog.setContentView(R.layout.auth);

        final EditText txtPassword = (EditText)passwordDialog.findViewById(R.id.password);
        txtPassword.setText(Helper.getPasswordPref(thisContext, id)); // fill in their remembered password

        final Button dialogButton = (Button)passwordDialog.findViewById(R.id.btnSubmit);

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Helper.isOnline(thisContext)) {
                    new AuthEdit(passwordDialog, txtPassword.getText().toString()).execute();
                } else {
                    Helper.toast(thisContext, getResources().getString(R.string.no_internet_message));
                }
            }
        });

        // automatically log them in if they have a password remembered
        if (Helper.getPasswordPref(thisContext, id) != "") {
            dialogButton.performClick();
        }

        passwordDialog.show();
    }

    public void eventDetails(View v) {
        new ShowAd(false).execute(); // false means to show the ad even if they have the password for this bar
    }

    private class ShowAd extends AsyncTask<String, Void, HashMap<String, Object>> {

        private boolean dontShowIfAuthorized;

        public ShowAd(boolean dontShowIfAuthorized) {
            this.dontShowIfAuthorized = dontShowIfAuthorized;
        }

        @Override
        protected HashMap<String, Object> doInBackground(String... params) {

            HashMap<String, Object> response = new HashMap<>();

            if (dontShowIfAuthorized) {

                try {

                    HashMap<String, String> urlParams = new HashMap<>();
                    urlParams.put("id", String.valueOf(id));

                    URL url = new URL(Config.getApiUrlAuth(urlParams));
                    HttpsURLConnection httpsURLConnection = (HttpsURLConnection)url.openConnection();
                    httpsURLConnection.addRequestProperty("Authorization", Helper.getB64Auth(String.valueOf(id), Helper.getPasswordPref(thisContext, id)));

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

                } catch (Exception e) {
                    e.printStackTrace();
                    Crashlytics.logException(e);

                    response.put("result", e.toString());
                    response.put("status", 500);
                    return response;
                }
            } else {
                // throw an error so the ad is shown
                response.put("result", "");
                response.put("status", 500);
                return response;
            }
        }

        private void showAd() {
            Intent intent = new Intent(WaitTime.this, Ad.class);
            intent.putExtra("adText", adText);
            intent.putExtra("adImg", adImg);
            intent.putExtra("adLink", adLink);
            intent.putExtra("barId", id);
            startActivityForResult(intent, Config.AD_REQUEST_CODE);
        }

        @Override
        protected void onPostExecute(HashMap<String, Object> response) {

            int status = (Integer)response.get("status");
            String result = (String)(response.get("result"));

            // show the ad if something goes wrong
            if (status >= 400) {
                showAd();
                return;
            }

            try {
                JSONObject jsonObject = new JSONObject(result);

                if (jsonObject.has("auth_status")) {
                    int authStatus = jsonObject.getInt("auth_status");

                    if (authStatus == 0) {
                        showAd();
                    }
                } else {
                    showAd();
                }
            } catch (JSONException je) {
                je.printStackTrace();
                Crashlytics.logException(je);
                showAd();
            }
        }
    }

    private class ShowTimes extends AsyncTask<String, Void, HashMap<String, Object>> {

        @Override
        protected void onPreExecute() {
            findViewById(R.id.loadingAnimation).setVisibility(View.VISIBLE);
            findViewById(R.id.btnRefresh).setEnabled(false);
            lblProgress.setText("");
            progressBar.setProgress(0);
        }

        @Override
        protected HashMap<String, Object> doInBackground(String... params) {

            HashMap<String, Object> response = new HashMap<>();

            try {
                HashMap<String, String> urlParams = new HashMap<>();
                urlParams.put("id", String.valueOf(id));

                URL url = new URL(Config.getApiUrlWaitTime(urlParams));
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

            try {
                JSONObject jsonObject = new JSONObject(result);

                id = jsonObject.getInt("id");
                findViewById(R.id.btnEdit).setEnabled(true); // enable when the id is loaded

                waitTime = jsonObject.getInt("wait_time");
                crowdingLevel = jsonObject.getInt("crowding_level");

                adImg = jsonObject.getString("ad_img");
                adLink = jsonObject.getString("ad_link");
                adText = jsonObject.getString("ad_text");

                // there is an ad to show and it has not been popped up before
                if ((!adImg.trim().equals("") || !adText.trim().equals("")) && firstLoad) {
                    firstLoad = false; // only load the ad once
                    lblEventDetails.setVisibility(View.VISIBLE);
                    new ShowAd(true).execute(); // true means to not show the ad if they have the password for this bar
                }

                if (btnShowWaitTimeSelected.getVisibility() == View.VISIBLE) showWaitTimeProgress();
                else showCrowdingLevelProgress();

                String lastUpdated = jsonObject.getString("last_updated"); // this is in PST
                TextView lblLastUpdated = (TextView)findViewById(R.id.lblLastUpdated);
                lblLastUpdated.setText("Last updated: " + Helper.getFormattedLastUpdated(lastUpdated));

            } catch (Exception je) {
                je.printStackTrace();
                Crashlytics.logException(je);
                Helper.toast(getApplicationContext(), String.valueOf(status) + " - " + getResources().getString(R.string.generic_error_message), true);
            }

            findViewById(R.id.loadingAnimation).setVisibility(View.GONE);
            findViewById(R.id.btnRefresh).setEnabled(true);
        }
    }

    private class AuthEdit extends AsyncTask<String, Void, HashMap<String, Object>> {

        private Dialog dialog;
        private String password;

        public AuthEdit(Dialog dialog, String password) {
            this.dialog = dialog;
            this.password = password;
        }

        @Override
        protected void onPreExecute() {
            this.dialog.findViewById(R.id.btnSubmit).setEnabled(false);
            this.dialog.findViewById(R.id.loadingAnimation).setVisibility(View.VISIBLE);
        }

        @Override
        protected HashMap<String, Object> doInBackground(String... params) {

            HashMap<String, Object> response = new HashMap<>();

            try {

                HashMap<String, String> urlParams = new HashMap<>();
                urlParams.put("id", String.valueOf(id));

                URL url = new URL(Config.getApiUrlAuth(urlParams));
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection)url.openConnection();
                httpsURLConnection.addRequestProperty("Authorization", Helper.getB64Auth(String.valueOf(id), password));

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
                    } else if (jsonObject.has("auth_status")) { // no error value, so probably incorrect password
                        Helper.toast(getApplicationContext(), jsonObject.getString("message"), false);
                    } else {
                        Helper.toast(getApplicationContext(), String.valueOf(status) + " - " + getResources().getString(R.string.generic_error_message), true);
                    }
                } catch (JSONException je) {
                    je.printStackTrace();
                    Crashlytics.logException(je);
                    Helper.toast(thisContext, String.valueOf(status) + " - " + getResources().getString(R.string.generic_error_message), true);
                }
                dialog.findViewById(R.id.loadingAnimation).setVisibility(View.GONE); // remove loading animation
                dialog.findViewById(R.id.btnSubmit).setEnabled(true);
                return;
            }

            int authStatus = 0;

            try {
                JSONObject jsonObject = new JSONObject(result);

                if (jsonObject.has("auth_status")) {
                    authStatus = jsonObject.getInt("auth_status");

                    if (authStatus == 0) {
                        Helper.toast(thisContext, jsonObject.getString("message"), true);
                    } else {
                        Helper.setPasswordPref(WaitTime.this, id, password);
                        Intent intent = new Intent(WaitTime.this, Edit.class);
                        intent.putExtra("id", id);
                        intent.putExtra("bar", bar);
                        intent.putExtra("password", password);
                        startActivity(intent);
                        overridePendingTransition(R.anim.right_to_left_enter, R.anim.right_to_left_exit);
                    }

                } else {
                    Helper.toast(thisContext, String.valueOf(status) + " - " + jsonObject.getString("error"), true);
                }

            } catch (JSONException je) {
                je.printStackTrace();
                Crashlytics.logException(je);
                Helper.toast(thisContext, String.valueOf(status) + " - " + getResources().getString(R.string.generic_error_message), true);
            }

            dialog.findViewById(R.id.loadingAnimation).setVisibility(View.GONE);
            dialog.findViewById(R.id.btnSubmit).setEnabled(true);
            if (authStatus == 1) dialog.dismiss(); // only dismiss the dialog if they were successfully logged in
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.left_to_right_enter, R.anim.left_to_right_exit);
    }

}
