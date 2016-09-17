package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.util.Log;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sam_chordas on 10/8/15.
 */
public final class Utils {

    private static final String LOG_TAG = Utils.class.getSimpleName();

    private static final String KEY_QUERY = "query";
    private static final String KEY_COUNT = "count";

    private static final String KEY_RESULTS = "results";
    private static final String KEY_QUOTE = "quote";

    private static final String KEY_SYMBOL = "symbol";
    private static final String KEY_CHANGE = "Change";
    private static final String KEY_BID = "Bid";
    private static final String KEY_CHANGE_IN_PERCENT = "ChangeinPercent";

    private static final String NULL = "null";

    public static boolean showPercent = true;

    private Utils() { throw new AssertionError(); }

    public static ArrayList<ContentProviderOperation> quoteJsonToContentProviderOperations(String jsonString){

        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();

        try{

            final JSONObject queryObject = new JSONObject(jsonString).getJSONObject(KEY_QUERY);

            if (queryObject.length() < 1) {
                return null;
            }

            final int count = queryObject.getInt(KEY_COUNT);

            if (count == 1){
                final JSONObject quoteObject =
                        queryObject.getJSONObject(KEY_RESULTS).getJSONObject(KEY_QUOTE);

                ContentProviderOperation op = buildBatchOperation(quoteObject);

                //A null op signifies an incorrect "Symbol" provided by user
                if (op == null) {
                    return null;
                }

                batchOperations.add(op);

            } else {

                final JSONArray resultsArray =
                        queryObject.getJSONObject(KEY_RESULTS).getJSONArray(KEY_QUOTE);

                if (resultsArray != null && resultsArray.length() != 0){
                    for (int i = 0; i < resultsArray.length(); i++){
                        final JSONObject result = resultsArray.getJSONObject(i);
                        batchOperations.add(buildBatchOperation(result));
                    }
                }
            }

        } catch (JSONException e){
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }

        return batchOperations;

    }

    private static String truncateBidPrice(String bidPrice){
        bidPrice = String.format(Locale.getDefault(), "%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    private static String truncateChange(String change, boolean isPercentChange){
        String weight = change.substring(0,1);
        String ampersand = "";
        if (isPercentChange){
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format(Locale.getDefault(), "%.2f", round);
        StringBuilder changeBuilder = new StringBuilder(change);
        changeBuilder.insert(0, weight);
        changeBuilder.append(ampersand);
        change = changeBuilder.toString();
        return change;
    }

    /*
    A null return value indicates an invalid SYMBOL was initially supplied for query
     */
    private static ContentProviderOperation buildBatchOperation(JSONObject quote){

        ContentProviderOperation.Builder builder =
                ContentProviderOperation.newInsert(QuoteProvider.Quotes.CONTENT_URI);
        try {

            final String symbol = quote.getString(KEY_SYMBOL);
            final String bid = quote.getString(KEY_BID);
            final String percentChange = quote.getString(KEY_CHANGE_IN_PERCENT);
            final String change = quote.getString(KEY_CHANGE);

            if (isContainingNullResult(symbol, bid, percentChange, change)) {
                return null;
            }

            builder.withValue(QuoteColumns.SYMBOL, symbol);
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(bid));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(percentChange, true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.IS_CURRENT, 1);
            builder.withValue(QuoteColumns.IS_UP, isUp(change));

        } catch (JSONException e){
            e.printStackTrace();
        }
        return builder.build();
    }

    private static int isUp(String change) {
        return (change.charAt(0)=='-') ? 0 : 1;
    }

    private static boolean isContainingNullResult(String... results) {

        boolean isContainingNullResult = false;
        final int count = results.length;
        int index = 0;

        while((index < count) && !isContainingNullResult) {
            isContainingNullResult = results[index].equals(NULL);
            index++;
        }

        return isContainingNullResult;

    }

}
