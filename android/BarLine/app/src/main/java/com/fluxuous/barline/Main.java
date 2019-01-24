package com.fluxuous.barline;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class Main extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Fabric.with(this, new Crashlytics());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        TextView lblTitle = (TextView)findViewById(R.id.lblTitle);
        Button btnSearch = (Button)findViewById(R.id.btnSearch);
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/RockoFLF.ttf");
        lblTitle.setTypeface(font);
        btnSearch.setTypeface(font);
    }

    public void search(View v) {
        if (Helper.isOnline(this)) {
            new CheckApiStatus().execute();
        } else {
            Helper.toast(getApplicationContext(), getResources().getString(R.string.no_internet_message));
        }
    }

    private void goToCitySelection() {
        startActivity(new Intent(Main.this, SelectCity.class));
        //overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        overridePendingTransition(R.anim.right_to_left_enter, R.anim.right_to_left_exit);
    }

    private void launchMessageDialog(final String title, final String message, final boolean apiOutdated) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.message_dialog);

        TextView lblTitle = (TextView)dialog.findViewById(R.id.lblTitle);
        TextView lblMessage = (TextView)dialog.findViewById(R.id.lblMessage);

        if (title.trim().equals("")) {
            lblTitle.setVisibility(View.GONE);
        } else {
            lblTitle.setText(title);
        }

        if (message.trim().equals("")) {
            lblMessage.setVisibility(View.GONE);
        } else {
            lblMessage.setText(message);
        }

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // launch the city selection activity if the API status is ok, but still needed a message to be displayed
                if (!apiOutdated) {
                    goToCitySelection();
                }
            }
        });

        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);

        dialog.show();
    }

    // will launch a message dialog if the API status returns a message
    // and will launch the city selection activity otherwise
    private class CheckApiStatus extends AsyncTask<String, Void, HashMap<String, Object>> {

        @Override
        protected void onPreExecute() {
            findViewById(R.id.loadingAnimation).setVisibility(View.VISIBLE);
            findViewById(R.id.btnSearch).setEnabled(false);
        }

        @Override
        protected HashMap<String, Object> doInBackground(String... params) {

            HashMap<String, Object> response = new HashMap<>();

            try {

                URL url = new URL(Config.API_URL_STATUS);
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
                        Helper.toast(getApplicationContext(), String.valueOf(status) + " - " + "Something went wrong.", true);
                    }
                } catch (JSONException je) {
                    je.printStackTrace();
                    Crashlytics.logException(je);
                    Helper.toast(getApplicationContext(), String.valueOf(status) + " - " + "Something went wrong.", true);
                }
                findViewById(R.id.loadingAnimation).setVisibility(View.GONE); // remove loading animation
                findViewById(R.id.btnSearch).setEnabled(true);
                goToCitySelection();
                return;
            }

            try {
                JSONObject jsonObject = new JSONObject(result);
                String title = "", message = "";
                if (jsonObject.has("title")) {
                    title = jsonObject.getString("title");
                }
                if (jsonObject.has("message")) {
                    message = jsonObject.getString("message");
                }

                if (jsonObject.getString("status").equals("ok")) {
                    if (!message.trim().equals("")) {
                        // launch dialog to display the message, but also move to the next activity when the dialog is closed
                        launchMessageDialog(title, message, false);
                    } else {
                        // move to the next activity as usual
                        goToCitySelection();
                    }
                } else if (jsonObject.getString("status").equals("outdated")) {
                    if (!message.trim().equals("")) {
                        // launch activity for displaying the "out of date" message
                        launchMessageDialog(title, message, true);
                    } else {
                        Helper.toast(getApplicationContext(), "This app is out of date. Please update to use it.", true);
                    }
                } else {
                    goToCitySelection();
                }

            } catch (JSONException je) {
                je.printStackTrace();
                Crashlytics.logException(je);
                Helper.toast(getApplicationContext(), String.valueOf(status) + " - " + getResources().getString(R.string.generic_error_message), true);
                goToCitySelection();
            }

            findViewById(R.id.loadingAnimation).setVisibility(View.GONE);
            findViewById(R.id.btnSearch).setEnabled(true);
        }
    }
}
