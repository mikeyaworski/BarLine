package com.fluxuous.barline;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.URL;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class Ad extends AppCompatActivity {

    private String adLink, adImg, adText;
    private int barId = -1;

    private ImageView imgView;
    private TextView lblAdText;
    private Button btnFindOutMore;

    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ad);
        Fabric.with(this, new Crashlytics());

        // Obtain the shared Tracker instance.
        BarLine application = (BarLine)getApplication();
        mTracker = application.getDefaultTracker();

        lblAdText = (TextView)findViewById(R.id.adText);
        imgView = (ImageView)findViewById(R.id.adImg);
        btnFindOutMore = (Button)findViewById(R.id.btnFindOutMore);

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            adText = extras.getString("adText");
            adImg = extras.getString("adImg");
            adLink = extras.getString("adLink");

            barId = extras.getInt("barId");

            // Log that this ad was opened to Google Analytics
            mTracker.setScreenName("Ad-" + barId);
            mTracker.send(new HitBuilders.ScreenViewBuilder().build());

            // setting the ad on the page
            if (!adText.trim().equals("")) {
                lblAdText.setVisibility(View.VISIBLE);
                lblAdText.setText(adText);
            } else {
                //lblAdText.setVisibility(View.GONE);
                lblAdText.setVisibility(View.INVISIBLE);
            }
            if (!adImg.trim().equals("")) {
                imgView.setVisibility(View.VISIBLE);
                if (Helper.isOnline(this)) {
                    new SetImageFromUrl().execute();
                } else {
                    finish();
                }
            } else {
                imgView.setVisibility(View.GONE);
            }
            if (adLink.trim().equals("")) {
                btnFindOutMore.setVisibility(View.GONE);
            }
        }
    }

    public void findOutMore(View v) {
        if (!adLink.trim().equals("")) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(adLink));
            startActivity(browserIntent);
        }

        // Build and send an Event to Google Analytics
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("ad")
                .setAction("Find Out More: " + barId)
                .setLabel("Text: " + adText + "\nImage: " + adImg + "\nLink: " + adLink)
                .build());
    }

    public void close(View v) {
        finish();
    }

    private class SetImageFromUrl extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            findViewById(R.id.loadingAnimation).setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                URL url = new URL(adImg);
                Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
                Crashlytics.logException(e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result == null) {
                imgView.setVisibility(View.GONE);
            } else {
                imgView.setImageBitmap(result);
            }
            findViewById(R.id.loadingAnimation).setVisibility(View.GONE);
        }
    }

}
