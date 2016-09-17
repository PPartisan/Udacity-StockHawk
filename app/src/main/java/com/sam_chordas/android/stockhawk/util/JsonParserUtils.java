package com.sam_chordas.android.stockhawk.util;

import com.sam_chordas.android.stockhawk.model.DetailStockModel;
import com.sam_chordas.android.stockhawk.model.DetailStockModel.Day;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public final class JsonParserUtils {

    private static final String KEY_DETAIL_QUERY = "query";

    private static final String KEY_DETAIL_RESULTS = "results";
    private static final String KEY_DETAIL_QUOTE = "quote";

    private static final String KEY_DETAIL_DATE = "Date";
    private static final String KEY_DETAIL_OPEN = "Open";

    private JsonParserUtils() { throw new AssertionError(); }

    public static DetailStockModel getDetailedStockModelFromJson(String jsonString) throws JSONException, ParseException {

        final JSONArray quote = new JSONObject(jsonString)
                .getJSONObject(KEY_DETAIL_QUERY)
                .getJSONObject(KEY_DETAIL_RESULTS)
                .getJSONArray(KEY_DETAIL_QUOTE);

        final int count = quote.length();

        DetailStockModel.Builder builder = new DetailStockModel.Builder(count);

        for (int i = 0; i < count; i++) {
            final JSONObject item = quote.getJSONObject(i);
            final String date = item.getString(KEY_DETAIL_DATE);
            final String open = item.getString(KEY_DETAIL_OPEN);
            builder.addDay(Day.buildDay(date, open));
        }

        return builder.build();

    }

}
