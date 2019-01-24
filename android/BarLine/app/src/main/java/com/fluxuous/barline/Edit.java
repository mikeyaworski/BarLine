package com.fluxuous.barline;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class Edit extends AppCompatActivity {

    private String barName, password;
    private int id = -1;

    private SeekBar seekBarWaitTime, seekBarCrowdingLevel;

    private CircleProgressBar circularProgressBarWaitTime;
    private CircleProgressBar circularProgressBarCrowdingLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);
        Fabric.with(this, new Crashlytics());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        circularProgressBarWaitTime = (CircleProgressBar)findViewById(R.id.circularProgressBarWaitTime);
        circularProgressBarCrowdingLevel = (CircleProgressBar)findViewById(R.id.circularProgressBarCrowdingLevel);

        final TextView lblWaitTimeProgress = (TextView)findViewById(R.id.lblWaitTimeProgress);
        final TextView lblCrowdingLevelProgress = (TextView)findViewById(R.id.lblCrowdingLevelProgress);

        TextView lblBarName = (TextView)findViewById(R.id.lblBarName);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            id = extras.getInt("id");
            password = extras.getString("password");
            barName = extras.getString("bar");

            lblBarName.setText(barName);
            refresh();

            seekBarWaitTime = (SeekBar)findViewById(R.id.waitTimeProgress);
            seekBarCrowdingLevel = (SeekBar)findViewById(R.id.crowdingLevelProgress);

            // set increments by 1 and max to 12 and 20 (versus 60 and 100) so that sliding will slide a lot each time (simulate skipping 5 mins at a time)
            // can be multiplied by 5 when updating progress bars and submitting to API

            seekBarWaitTime.incrementProgressBy(1);
            seekBarWaitTime.setMax(12);

            seekBarCrowdingLevel.incrementProgressBy(1);
            seekBarCrowdingLevel.setMax(20);

            final Context context = this;

            seekBarWaitTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    final int mins = progress * 5;
                    lblWaitTimeProgress.setText(Helper.getFormattedWaitTime(mins));
                    circularProgressBarWaitTime.setProgressWithAnimation(mins);

                    // use a runnable to update the colour of the progress bar while it's animating
                    final Handler handler = new Handler();
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (circularProgressBarWaitTime.getProgress() * 1.0 / circularProgressBarWaitTime.getMax() <= 1.0/3) {
                                    circularProgressBarWaitTime.setColor(ContextCompat.getColor(context, R.color.progressGreen));
                                } else if (circularProgressBarWaitTime.getProgress() * 1.0 / circularProgressBarWaitTime.getMax() <= 2.0/3) {
                                    circularProgressBarWaitTime.setColor(ContextCompat.getColor(context, R.color.progressYellow));
                                } else {
                                    circularProgressBarWaitTime.setColor(ContextCompat.getColor(context, R.color.progressRed));
                                }
                            }
                            catch (Exception e) { }
                            finally {
                                // keep calling it while the progress bar is not completed its animation
                                if (circularProgressBarWaitTime.getProgress() != mins) {
                                    handler.postDelayed(this, 200);
                                }
                            }
                        }
                    };
                    handler.postDelayed(runnable, 0);
                }

                public void onStartTrackingTouch(SeekBar seekBar) { }
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });

            seekBarCrowdingLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    final int percent = progress * 5;
                    lblCrowdingLevelProgress.setText(Helper.getFormattedCrowdingLevel(percent));
                    circularProgressBarCrowdingLevel.setProgressWithAnimation(percent);

                    // use a runnable to update the colour of the progress bar while it's animating
                    final Handler handler = new Handler();
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (circularProgressBarCrowdingLevel.getProgress() * 1.0 / circularProgressBarCrowdingLevel.getMax() <= 1.0/3) {
                                    circularProgressBarCrowdingLevel.setColor(ContextCompat.getColor(context, R.color.progressGreen));
                                } else if (circularProgressBarCrowdingLevel.getProgress() * 1.0 / circularProgressBarCrowdingLevel.getMax() <= 2.0/3) {
                                    circularProgressBarCrowdingLevel.setColor(ContextCompat.getColor(context, R.color.progressYellow));
                                } else {
                                    circularProgressBarCrowdingLevel.setColor(ContextCompat.getColor(context, R.color.progressRed));
                                }
                            }
                            catch (Exception e) { }
                            finally {
                                // keep calling it while the progress bar is not completed its animation
                                if (circularProgressBarCrowdingLevel.getProgress() != percent) {
                                    handler.postDelayed(this, 200);
                                }
                            }
                        }
                    };
                    handler.postDelayed(runnable, 0);
                }

                public void onStartTrackingTouch(SeekBar seekBar) { }
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });
        }

        if (id == -1) {
            lblBarName.setText("");
            lblWaitTimeProgress.setText("");
            lblCrowdingLevelProgress.setText("");
            Helper.toast(getApplicationContext(), "The ID is unknown. Please go back and wait for the data to load before editing.", true);
        }
    }

    public void refresh() {
        if (Helper.isOnline(this)) {
            new ShowTimes().execute();
        } else {
            Helper.toast(getApplicationContext(), getResources().getString(R.string.no_internet_message), false);
        }
    }

    public void refresh(View v) {
        refresh();
    }

    public void save(View v) {
        if (Helper.isOnline(this)) {
            new SaveWaitTime(seekBarWaitTime.getProgress() * 5, seekBarCrowdingLevel.getProgress() * 5).execute();
        } else {
            Helper.toast(getApplicationContext(), getResources().getString(R.string.no_internet_message));
        }
    }

    private class ShowTimes extends AsyncTask<String, Void, HashMap<String, Object>> {

        @Override
        protected void onPreExecute() {
            findViewById(R.id.loadingAnimation).setVisibility(View.VISIBLE);
            findViewById(R.id.btnSave).setEnabled(false);
            findViewById(R.id.btnRefresh).setEnabled(false);
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
                findViewById(R.id.btnSave).setEnabled(true);
                findViewById(R.id.btnRefresh).setEnabled(true);
                return;
            }

            try {
                JSONObject jsonObject = new JSONObject(result);

                int waitTime = jsonObject.getInt("wait_time");
                int crowdingLevel = jsonObject.getInt("crowding_level");

                TextView lblWaitTimeProgress = (TextView)findViewById(R.id.lblWaitTimeProgress);
                TextView lblCrowdingLevelProgress = (TextView)findViewById(R.id.lblCrowdingLevelProgress);
                TextView lblLastUpdated = (TextView)findViewById(R.id.lblLastUpdated);

                lblWaitTimeProgress.setText(String.valueOf(waitTime));
                seekBarWaitTime.setProgress(waitTime / 5);
                lblCrowdingLevelProgress.setText(String.valueOf(crowdingLevel));
                seekBarCrowdingLevel.setProgress(crowdingLevel / 5);

                String lastUpdated = jsonObject.getString("last_updated"); // this is in PST

                String formattedWaitTime = Helper.getFormattedWaitTime(waitTime);
                String formattedCrowdingLevel = Helper.getFormattedCrowdingLevel(crowdingLevel);
                String formattedLastUpdated = Helper.getFormattedLastUpdated(lastUpdated);

                lblWaitTimeProgress.setText(formattedWaitTime);
                lblCrowdingLevelProgress.setText(formattedCrowdingLevel);
                lblLastUpdated.setText("Last updated: " + formattedLastUpdated);

            } catch (Exception je) {
                je.printStackTrace();
                Crashlytics.logException(je);
                Helper.toast(getApplicationContext(), getResources().getString(R.string.generic_error_message), true);
            }

            findViewById(R.id.loadingAnimation).setVisibility(View.GONE);
            findViewById(R.id.btnSave).setEnabled(true);
            findViewById(R.id.btnRefresh).setEnabled(true);
        }
    }

    private class SaveWaitTime extends AsyncTask<String, Void, HashMap<String, Object>> {

        private int waitTime;
        private int crowdingLevel;

        public SaveWaitTime(int waitTime, int crowdingLevel) {
            this.waitTime = waitTime;
            this.crowdingLevel = crowdingLevel;
        }

        @Override
        protected void onPreExecute() {
            findViewById(R.id.loadingAnimation).setVisibility(View.VISIBLE);
            findViewById(R.id.btnSave).setEnabled(false);
            findViewById(R.id.btnRefresh).setEnabled(false);
        }

        @Override
        protected HashMap<String, Object> doInBackground(String... params) {

            HashMap<String, Object> response = new HashMap<>();

            try {

                HashMap<String, String> urlParams = new HashMap<>();
                urlParams.put("id", String.valueOf(id));

                URL url = new URL(Config.getApiUrlWaitTime(urlParams));
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection)url.openConnection();
                httpsURLConnection.setReadTimeout(10000);
                httpsURLConnection.setConnectTimeout(15000);
                httpsURLConnection.setRequestMethod("PUT");
                httpsURLConnection.setDoInput(true);
                httpsURLConnection.setDoOutput(true);
                httpsURLConnection.setRequestProperty("Content-Type", "application/json");
                httpsURLConnection.setRequestProperty("Accept", "application/json");

                httpsURLConnection.addRequestProperty("Authorization", Helper.getB64Auth(String.valueOf(id), password));

                JSONObject payload = new JSONObject();
                payload.put("waitTime", waitTime);
                payload.put("crowdingLevel", crowdingLevel);
                payload.put("deviceName", android.os.Build.MODEL);

                OutputStream os = httpsURLConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(payload.toString());
                writer.flush();
                writer.close();
                os.close();

                httpsURLConnection.connect();

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
                    } else if (jsonObject.has("success_status")) {
                        Helper.toast(getApplicationContext(), jsonObject.getString("message"), true);
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
                findViewById(R.id.btnSave).setEnabled(true);
                return;
            }

            try {
                JSONObject jsonObject = new JSONObject(result);
                int success = jsonObject.getInt("success_status");

                String msg = jsonObject.getString("message");

                if (success == 1) {
                    Helper.setPasswordPref(Edit.this, id, password);
                    finish();
                }

                Helper.toast(getApplicationContext(), msg, false);

            } catch (JSONException je) {
                je.printStackTrace();
                Crashlytics.logException(je);
                Helper.toast(getApplicationContext(), String.valueOf(status) + " - " + getResources().getString(R.string.generic_error_message), true);
            }

            findViewById(R.id.loadingAnimation).setVisibility(View.GONE);
            findViewById(R.id.btnRefresh).setEnabled(true);
            findViewById(R.id.btnSave).setEnabled(true);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.left_to_right_enter, R.anim.left_to_right_exit);
    }
}
