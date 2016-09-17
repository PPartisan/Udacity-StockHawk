package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.DetailedStockTaskIntentService;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperAdapter;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperViewHolder;
import com.sam_chordas.android.stockhawk.util.TypefaceUtils;
import com.sam_chordas.android.stockhawk.util.ViewUtils;

/**
 * Created by sam_chordas on 10/6/15.
 *  Credit to skyfishjy gist:
 *    https://gist.github.com/skyfishjy/443b7448f59be978bc59
 * for the code structure
 */
public class QuoteCursorAdapter extends CursorRecyclerViewAdapter<QuoteCursorAdapter.ViewHolder>
        implements ItemTouchHelperAdapter{

    private final ContentResolver mContentResolver;
    private final Typeface mRobotoLightTypeface;
//    private boolean isPercent;

    public QuoteCursorAdapter(Context context, Cursor cursor){
        super(context, cursor);
        mContentResolver = context.getContentResolver();
        mRobotoLightTypeface = TypefaceUtils.getRobotoLightTypeface(context.getAssets());
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_quote, parent, false);
        return new ViewHolder(itemView, mRobotoLightTypeface);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final Cursor cursor){

        viewHolder.symbol.setText(cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL)));
        viewHolder.bidPrice.setText(cursor.getString(cursor.getColumnIndex(QuoteColumns.BIDPRICE)));

        final int isUp = cursor.getInt(cursor.getColumnIndex(QuoteColumns.IS_UP));
        final int pillResId = (isUp == 1)
                ? R.drawable.percent_change_pill_green
                : R.drawable.percent_change_pill_red;

        ViewUtils.setViewBackgroundDrawable(viewHolder.change, pillResId);

        if (Utils.showPercent){
            viewHolder.change.setText(cursor.getString(cursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE)));
        } else{
            viewHolder.change.setText(cursor.getString(cursor.getColumnIndex(QuoteColumns.CHANGE)));
        }

    }

    @Override public void onItemDismiss(int position) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        String symbol = c.getString(c.getColumnIndex(QuoteColumns.SYMBOL));
        mContentResolver.delete(QuoteProvider.Quotes.withSymbol(symbol), null, null);
        notifyItemRemoved(position);
    }

    @Override public int getItemCount() {
        return super.getItemCount();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements ItemTouchHelperViewHolder, View.OnClickListener {

        final TextView symbol;
        final TextView bidPrice;
        final TextView change;

        public ViewHolder(View itemView, Typeface robotoLight){
            super(itemView);
            symbol = (TextView) itemView.findViewById(R.id.stock_symbol);
            symbol.setTypeface(robotoLight);
            bidPrice = (TextView) itemView.findViewById(R.id.bid_price);
            change = (TextView) itemView.findViewById(R.id.change);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onItemSelected(){
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear(){
            itemView.setBackgroundColor(0);
        }

        @Override
        public void onClick(View v) {
            Log.d(getClass().getSimpleName(), "Click Registered at " + getAdapterPosition());
            Intent launchDetailedService = new Intent(v.getContext(), DetailedStockTaskIntentService.class);
            launchDetailedService.putExtra(DetailedStockTaskIntentService.KEY_SYMBOL, symbol.getText().toString());
            v.getContext().startService(launchDetailedService);
        }

    }

}
