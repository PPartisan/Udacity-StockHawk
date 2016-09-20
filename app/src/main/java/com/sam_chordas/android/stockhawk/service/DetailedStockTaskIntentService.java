package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.sam_chordas.android.stockhawk.model.DetailStockModel;
import com.sam_chordas.android.stockhawk.util.JsonParserUtils;
import com.sam_chordas.android.stockhawk.util.NetworkUtils;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DetailedStockTaskIntentService extends IntentService {

    private static final String TAG = DetailedStockTaskIntentService.class.getSimpleName();

    public static final String ACTION_COMPLETE = TAG + ".COMPLETE";

    public static final String STOCK_DETAIL_EXTRA_KEY = "stock_detail_extra_key";

    public static final String KEY_SYMBOL = "key_symbol";
    public static final String KEY_BID = "key_bid";
    public static final String KEY_PERCENT_CHANGE = "key_percent_change";
    public static final String KEY_CHANGE = "key_change";
    public static final String KEY_IS_UP = "key_is_up";

    private static final String BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
    private static final String SELECT_PREFIX = "select * from yahoo.finance.historicaldata where ";
    private static final String SUFFIX_URL =
            "&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org" +
                    "%2Falltableswithkeys&callback=";
    private static final String SYMBOL_CLAUSE = "symbol";
    private static final String START_DATE_CLAUSE = "startDate";
    private static final String END_DATE_CLAUSE = "endDate";

    //Note: Not thread-safe
    public static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public DetailedStockTaskIntentService() {
        super(TAG);
    }

    public DetailedStockTaskIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        final String symbol = intent.getStringExtra(KEY_SYMBOL);

        final String requestUrl = buildRequestString(symbol);

        final String resultString = NetworkUtils.getJsonString(requestUrl);

        try {

            DetailStockModel model =
                    JsonParserUtils.getDetailedStockModelFromJson(resultString, true);

            Intent resultIntent = new Intent(ACTION_COMPLETE);
            resultIntent.putExtra(STOCK_DETAIL_EXTRA_KEY, model);

            LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);

        } catch (JSONException | ParseException e) {
            e.printStackTrace();
        }

    }

    private static String buildRequestString(String symbol) {

        final String request = SELECT_PREFIX +
                SYMBOL_CLAUSE + " = " + withQuotes(symbol) + " and " +
                START_DATE_CLAUSE + " = " + withQuotes(getFormattedDayLessOneMonth()) + " and " +
                END_DATE_CLAUSE + " = " + withQuotes(getFormattedTodayDay());

        try {
            final String encodedRequest = URLEncoder.encode(request, "UTF-8");
            return BASE_URL + encodedRequest + SUFFIX_URL;
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }

    }

    private static String withQuotes(String string) {
        return "\"" + string + "\"";
    }

    private static String getFormattedTodayDay() {
        return DetailedStockTaskIntentService.DATE_FORMAT.format(Calendar.getInstance().getTime());
    }

    private static String getFormattedDayLessOneMonth() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -1);
        return DATE_FORMAT.format(c.getTime());
    }

}
