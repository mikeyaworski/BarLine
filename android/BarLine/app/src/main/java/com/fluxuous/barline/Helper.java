package com.fluxuous.barline;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.DateFormat;
import android.util.Base64;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Helper {

    public static boolean isOnline(Context context) {
        final ConnectivityManager conMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();

        if (activeNetwork != null && activeNetwork.isConnected()) {
            return true; // online
        } else {
            return false; // offline
        }
    }

    public static String getCurrentTime() {
        String delegate = "h:mm aaa";
        return (String)DateFormat.format(delegate, Calendar.getInstance().getTime());
    }
    public static String getFormattedWaitTime(int waitTime) {

        String minsStr = String.valueOf(waitTime) + " mins";

        // this assumes that the max wait time to display is 60 mins and then it will just say "60+ mins"
        if (waitTime >= 60) minsStr = "60+ mins";

        if (waitTime == 0) minsStr = "No wait!";

        return minsStr;
    }
    public static String getFormattedCrowdingLevel(int crowdingLevel) {
        if (crowdingLevel <= 0) return "Empty";
        else if (crowdingLevel >= 100) return "Full";
        return crowdingLevel + "%";
    }
    public static String getFormattedLastUpdated(String lastUpdated) {

        Calendar rightNow = Calendar.getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA);
        sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles")); // timezone of server

        try {
            Date parsed = sdf.parse(lastUpdated);

            long millis = rightNow.getTime().getTime() - parsed.getTime();
            if (millis < 0) return "just now";

            if (millis < 60000) {
                return "just now";
            } else if (millis < 3.6 * Math.pow(10, 6)) { // within hour
                long minutes = Math.round(millis / 60000.0);
                String minutesString = " minutes ago";
                if (minutes == 1) {
                    minutesString = " minute ago";
                }
                if (minutes == 60) { // possibly from rounding
                    return "1 hour ago";
                }
                return String.valueOf(minutes + minutesString);
            } else if (millis < 24 * 3.6 * Math.pow(10, 6)) { // within day
                long hours = Math.round(millis / (3.6 * Math.pow(10, 6)));
                String hoursString = " hours ago";
                if (hours == 1) {
                    hoursString = " hour ago";
                }
                return String.valueOf(hours + hoursString);
            } else {
                return "over a day ago";
            }

        } catch (ParseException pe) {
            pe.printStackTrace();
            return "";
        }
    }

    public static String getB64Auth(String login, String pass) {
        String source = login + ":" + pass;
        return "Basic " + Base64.encodeToString(source.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    public static String getPasswordPref(Context context, int barId) {
        SharedPreferences sp = context.getSharedPreferences("passwordPref", 0);
        return sp.getString(String.valueOf(barId), "");
    }
    public static void setPasswordPref(Context context, int barId, String password) {
        SharedPreferences.Editor editor = context.getSharedPreferences("passwordPref", 0).edit();
        editor.putString(String.valueOf(barId), password).commit();
    }

    public static void toast(Context context, Object obj, boolean isLong) {
        int length = isLong? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        Toast.makeText(context.getApplicationContext(), obj.toString(), length).show();
    }
    public static void toast(Context context, Object obj) {
        Toast.makeText(context.getApplicationContext(), obj.toString(), Toast.LENGTH_SHORT).show();
    }
}