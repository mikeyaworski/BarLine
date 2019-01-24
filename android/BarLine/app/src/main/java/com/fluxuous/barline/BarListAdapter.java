package com.fluxuous.barline;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;

public class BarListAdapter extends ArrayAdapter<HashMap<String,String>> {

    private final Activity activity;
    private final ArrayList<HashMap<String,String>> bars;

    public BarListAdapter(Activity activity, ArrayList<HashMap<String,String>> bars) {
        super(activity, R.layout.bar_list_item, bars);
        this.activity = activity;
        this.bars = bars;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = activity.getLayoutInflater();

        HashMap<String, String> bar = bars.get(position);
        if (bar.containsKey("itemType") && bar.get("itemType").equals("sectionHeader")) {
            View rowView = inflater.inflate(R.layout.letter_list_item, null, true);

            TextView lblLetter = (TextView)rowView.findViewById(R.id.lblLetter);
            lblLetter.setText(bar.get("header"));
            rowView.setTag(R.id.HEADER_TAG, true);
            return rowView;
        } else {
            View rowView = inflater.inflate(R.layout.bar_list_item, null, true);

            TextView lblCity = (TextView)rowView.findViewById(R.id.lblBar);
            lblCity.setText(this.bars.get(position).get("name"));
            rowView.setTag(R.id.HEADER_TAG, false);
            return rowView;
        }
    }
}