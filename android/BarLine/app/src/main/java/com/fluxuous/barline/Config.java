package com.fluxuous.barline;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Config {

    public static final String API_BASE_URL = "https://fluxuous.com/barline/api/v1";

    public static final String API_URL_CITIES = API_BASE_URL + "/cities";
    public static final String API_URL_BARS = API_BASE_URL + "/bars";
    public static final String API_URL_WAIT_TIME = API_BASE_URL + "/wait_time";
    public static final String API_URL_AUTH = API_BASE_URL + "/auth";
    public static final String API_URL_STATUS = API_BASE_URL + "/status";

    public static final int AD_REQUEST_CODE = 1;

    public static String getRelativePathQuery(List<String> params) {
        try {
            StringBuilder queryStr = new StringBuilder();

            for (String value : params) {
                queryStr.append("/");
                queryStr.append(URLEncoder.encode(value, "UTF-8"));
            }

            return queryStr.toString();

        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
            return "";
        }
    }

    public static String getUrlParametersQuery(List<AbstractMap.SimpleEntry> params) {
        try {
            StringBuilder queryStr = new StringBuilder();
            boolean first = true;

            for (AbstractMap.SimpleEntry pair : params) {
                if (first) {
                    first = false;
                    queryStr.append("?");
                }
                else {
                    queryStr.append("&");
                }

                queryStr.append(URLEncoder.encode((String)pair.getKey(), "UTF-8"));
                queryStr.append("=");
                queryStr.append(URLEncoder.encode((String)pair.getValue(), "UTF-8"));
            }

            return queryStr.toString();

        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
            return "";
        }
    }

    public static String getApiUrlCities(List<AbstractMap.SimpleEntry> params) {
        return API_URL_CITIES + getUrlParametersQuery(params);
    }

    public static String getApiUrlBars(List<AbstractMap.SimpleEntry> params) {
        return API_URL_BARS + getUrlParametersQuery(params);
    }

    public static String getApiUrlAuth(HashMap<String, String> paramsMap) {

        List<String> paramsList = new ArrayList<>();
        if (paramsMap.containsKey("id")) {
            paramsList.add(paramsMap.get("id"));
        }

        return API_URL_AUTH + getRelativePathQuery(paramsList);
    }

    public static String getApiUrlWaitTime(HashMap<String, String> paramsMap) {

        List<String> paramsList = new ArrayList<>();
        if (paramsMap.containsKey("id")) {
            paramsList.add(paramsMap.get("id"));
        }

        return API_URL_WAIT_TIME + getRelativePathQuery(paramsList);
    }
}
