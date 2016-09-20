package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperAdapter;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperViewHolder;
import com.sam_chordas.android.stockhawk.util.AccessibilityUtils;
import com.sam_chordas.android.stockhawk.util.TypefaceUtils;
import com.sam_chordas.android.stockhawk.util.ViewUtils;

/**
 * Created by sam_chordas on 10/6/15.
 *  Credit to skyfishjy gist:
 *    https://gist.github.com/skyfishjy/443b7448f59be978bc59
 * for the code structure
 */
public class QuoteCursorAdapter extends CursorRecyclerViewAdapter<QuoteCursorAdapter.ViewHolder>
        implements ItemTouchHelperAdapter {

    private final ContentResolver mContentResolver;
    private final Typeface mRobotoLightTypeface;
    private final Callbacks mCallbacks;
//    private boolean isPercent;

    public QuoteCursorAdapter(Context context, Cursor cursor){
        super(context, cursor);

        try {
            mCallbacks = (Callbacks) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    context.toString() + " must implement " + Callbacks.class.getSimpleName()
            );
        }

        mContentResolver = context.getContentResolver();
        mRobotoLightTypeface = TypefaceUtils.getRobotoLightTypeface(context.getAssets());
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_quote, parent, false);
        return new ViewHolder(itemView, mRobotoLightTypeface, mCallbacks);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final Cursor cursor){

        Resources res = viewHolder.itemView.getContext().getResources();

        final String symbol = cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL));
        final String bidPrice = cursor.getString(cursor.getColumnIndex(QuoteColumns.BIDPRICE));
        final String change = cursor.getString(cursor.getColumnIndex(QuoteColumns.CHANGE));
        final String percentChange = cursor.getString(cursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE));

        final String audibleSymbol = AccessibilityUtils.getStringAsAudibleCharacters(symbol);

        viewHolder.symbol.setText(symbol);
        viewHolder.symbol.setContentDescription(
                res.getString(R.string.ams_rvi_content_desc_symbol, audibleSymbol)
        );

        viewHolder.bidPrice.setText(bidPrice);
        viewHolder.bidPrice.setContentDescription(
                res.getString(R.string.ams_rvi_content_desc_bid, audibleSymbol, bidPrice)
        );

        final int isUp = cursor.getInt(cursor.getColumnIndex(QuoteColumns.IS_UP));
        final int pillResId = (isUp == 1)
                ? R.drawable.percent_change_pill_green
                : R.drawable.percent_change_pill_red;

        ViewUtils.setViewBackgroundDrawable(viewHolder.change, pillResId);

        if (Utils.showPercent){
            viewHolder.change.setText(percentChange);
            viewHolder.change.setContentDescription(
                    res.getString(R.string.ams_rvi_content_des_change_per, audibleSymbol, percentChange)
            );
        } else{
            viewHolder.change.setText(change);
            viewHolder.change.setContentDescription(
                    res.getString(R.string.ams_rvi_content_desc_change, audibleSymbol, change)
            );
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

    static class ViewHolder extends RecyclerView.ViewHolder
            implements ItemTouchHelperViewHolder, View.OnClickListener {

        final TextView symbol;
        final TextView bidPrice;
        final TextView change;

        private final Callbacks mCallbacks;

        ViewHolder(View itemView, Typeface robotoLight, Callbacks callbacks){
            super(itemView);
            symbol = (TextView) itemView.findViewById(R.id.stock_symbol);
            symbol.setTypeface(robotoLight);
            bidPrice = (TextView) itemView.findViewById(R.id.bid_price);
            change = (TextView) itemView.findViewById(R.id.change);

            mCallbacks = callbacks;

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
            mCallbacks.onItemClick(getAdapterPosition());
        }

    }

    public interface Callbacks {
        void onItemClick(int position);
    }

}
