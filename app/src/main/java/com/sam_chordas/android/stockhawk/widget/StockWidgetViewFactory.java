package com.sam_chordas.android.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import static android.os.Binder.*;
import static com.sam_chordas.android.stockhawk.service.DetailedStockTaskIntentService.*;


class StockWidgetViewFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private Cursor mCursor;

    StockWidgetViewFactory(Context context) {
        mContext = context;
    }

    @Override
    public void onCreate() { }

    @Override
    public void onDataSetChanged() {

        if (mCursor != null) {
            mCursor.close();
        }

        final long identityToken = clearCallingIdentity();

        final String[] projection = {
                QuoteColumns._ID,
                QuoteColumns.SYMBOL,
                QuoteColumns.BIDPRICE,
                QuoteColumns.PERCENT_CHANGE,
                QuoteColumns.CHANGE,
                QuoteColumns.IS_UP,
                QuoteColumns.IS_CURRENT
        };

        final String selection = QuoteColumns.IS_CURRENT + "=?";
        final String[] selectionArgs = { "1" };
        final String orderBy = QuoteColumns._ID;

        mCursor = mContext.getContentResolver()
                .query(QuoteProvider.Quotes.CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        orderBy
                );

        restoreCallingIdentity(identityToken);
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        mContext = null;
    }

    @Override
    public int getCount() {
        if (mCursor == null) {
            return 0;
        }
        return mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int i) {

        if(i == AdapterView.INVALID_POSITION || mCursor == null || !mCursor.moveToPosition(i)) {
            return null;
        }

        final String symbol = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
        final String bid = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE));
        final String percentChange = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
        final String change = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE));
        final boolean isUp = (mCursor.getInt(mCursor.getColumnIndex(QuoteColumns.IS_UP)) == 1);

        final int textColor = (isUp)
                ? R.color.material_green_700
                : R.color.material_red_700;

        RemoteViews row = new RemoteViews(mContext.getPackageName(), R.layout.stock_widget_row);

        row.setTextViewText(R.id.sw_symbol, symbol);
        row.setTextViewText(R.id.sw_bid_price, bid);
        row.setTextViewText(R.id.sw_change, percentChange);
        row.setTextColor(R.id.sw_change, ContextCompat.getColor(mContext, textColor));

        Intent clickIntent = new Intent();
        clickIntent.putExtra(KEY_SYMBOL, symbol);
        clickIntent.putExtra(KEY_BID, bid);
        clickIntent.putExtra(KEY_PERCENT_CHANGE, percentChange);
        clickIntent.putExtra(KEY_CHANGE, change);
        clickIntent.putExtra(KEY_IS_UP, isUp);

        row.setOnClickFillInIntent(R.id.sw_row, clickIntent);

        return row;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        if(mCursor.moveToPosition(i)) {
            return mCursor.getLong(mCursor.getColumnIndex(QuoteColumns._ID));
        }
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

}
