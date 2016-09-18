package com.sam_chordas.android.stockhawk.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public final class NetworkUtils {

    private static final String TAG = NetworkUtils.class.getSimpleName();

    private NetworkUtils() { throw new AssertionError(); }

    public static String getJsonString(String urlString) {

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        String resultString = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            InputStream inputStream = connection.getInputStream();
            if (inputStream == null) {
                return null;
            }
            StringBuilder builder = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }

            if (builder.length() == 0) {
                return null;
            }

            resultString = builder.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) connection.disconnect();
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing stream", e);
                }
            }
        }

        return resultString;

    }

    public static boolean isConnectedToNetwork(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());

    }

    public static MaterialDialog buildAddStockSymbolDialog(Context context) {

        final BuildAddStockSymbolDialogInputCallback callback =
                new BuildAddStockSymbolDialogInputCallback(context);

        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);

        final int backgroundColour = ContextCompat.getColor(context, R.color.dialog_background);

        builder.backgroundColor(backgroundColour);
        builder.title(R.string.asd_title);
        builder.content(R.string.asd_content);
        builder.inputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        builder.input(R.string.asd_hint, R.string.asd_prefill, callback);

        return builder.build();
    }

    private static Cursor getCursorFromSymbolQuery(ContentResolver contentResolver, String symbol) {

        final String[] projection = { QuoteColumns.SYMBOL };
        final String where = QuoteColumns.SYMBOL + "=?";
        final String[] whereArgs = { symbol };

        return contentResolver.query(
                QuoteProvider.Quotes.CONTENT_URI, projection, where, whereArgs, null
        );
    }

    private static void startStockIntentService(Context context, String symbol) {
        Intent serviceIntent = new Intent(context, StockIntentService.class);
        serviceIntent.putExtra(StockIntentService.KEY, StockIntentService.ADD);
        serviceIntent.putExtra("symbol", symbol);
        context.startService(serviceIntent);
    }

    public static Toast buildNetworkFailureToast(Context context){
        return  Toast.makeText(context, R.string.network_toast, Toast.LENGTH_SHORT);
    }

    private static class BuildAddStockSymbolDialogInputCallback implements MaterialDialog.InputCallback {

        private final Context mContext;

        private BuildAddStockSymbolDialogInputCallback(Context context) {
            mContext = context;
        }

        @Override
        public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

            final String symbol = input.toString();

            Cursor c = null;

            try {

                c = getCursorFromSymbolQuery(mContext.getContentResolver(), symbol);

                if (c.getCount() > 0) {
                    ViewUtils.makeLongToast(mContext, R.string.asd_stock_present_message).show();
                } else {
                    startStockIntentService(mContext, symbol);
                }

            } finally {
                if (c != null && !c.isClosed()) c.close();
            }

        }

    }

}
