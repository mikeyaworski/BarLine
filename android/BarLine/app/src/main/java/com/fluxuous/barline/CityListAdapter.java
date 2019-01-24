package com.fluxuous.barline;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class CityListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final ArrayList<String> cities;

    public CityListAdapter(Activity context, ArrayList<String> cities) {
        super(context, R.layout.city_list_item, cities);
        this.context = context;
        this.cities = cities;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.city_list_item, null, true);

        TextView lblCity = (TextView)rowView.findViewById(R.id.lblCity);
        lblCity.setText(this.cities.get(position));

        return rowView;
    }
}