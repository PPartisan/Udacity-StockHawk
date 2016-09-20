package com.sam_chordas.android.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.db.chart.listener.OnEntryClickListener;
import com.db.chart.model.ChartSet;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.model.DetailStockModel;
import com.sam_chordas.android.stockhawk.model.DetailStockModel.Day;
import com.sam_chordas.android.stockhawk.service.DetailedStockTaskIntentService;
import com.sam_chordas.android.stockhawk.util.AccessibilityUtils;
import com.sam_chordas.android.stockhawk.util.NetworkUtils;
import com.sam_chordas.android.stockhawk.util.TypefaceUtils;
import com.sam_chordas.android.stockhawk.util.ViewUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetailStocksActivity extends AppCompatActivity implements OnEntryClickListener {

    private static final String SELECTED_ENTRY_KEY = "selected_entry_key";
    private static final String DETAILED_MODEL_KEY = "detail_model_key";

    private static final float POINT_RADIUS = ViewUtils.getDpAsPx(6);

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

    private SelectiveLineChartView mChartView;
    private TextView mDateTextView;
    private TextView mOpenTextView;

    private DetailStockModel mModel = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_line_graph);

        Intent i = getIntent();

        final String symbol = i.getStringExtra(DetailedStockTaskIntentService.KEY_SYMBOL).toUpperCase();
        final String bid = i.getStringExtra(DetailedStockTaskIntentService.KEY_BID);
        final String percentChange = i.getStringExtra(DetailedStockTaskIntentService.KEY_PERCENT_CHANGE);
        final String change = i.getStringExtra(DetailedStockTaskIntentService.KEY_CHANGE);
        final boolean isUp = i.getBooleanExtra(DetailedStockTaskIntentService.KEY_IS_UP, false);

        final String audibleSymbol = AccessibilityUtils.getStringAsAudibleCharacters(symbol);

        final Typeface robotoLightTypeface = TypefaceUtils.getRobotoLightTypeface(getAssets());

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(symbol);
        }

        mChartView = (SelectiveLineChartView) findViewById(R.id.linechart);
        mChartView.setXLabels(AxisController.LabelPosition.NONE);
        mChartView.setClickablePointRadius(POINT_RADIUS);
        mChartView.setTypeface(robotoLightTypeface);

        TextView title = (TextView) findViewById(R.id.alg_title);
        title.setTypeface(robotoLightTypeface);
        title.setFocusable(false);

        final TextView bidValue = (TextView) findViewById(R.id.alg_bid_value);
        final TextView changeValue = (TextView) findViewById(R.id.alg_change_value);
        final TextView percentChangeValue = (TextView) findViewById(R.id.alg_percent_change_value);

        bidValue.setText(bid);
        changeValue.setText(change);
        percentChangeValue.setText(percentChange);

        bidValue.setContentDescription(getString(R.string.ams_rvi_content_desc_bid, audibleSymbol, bid));
        changeValue.setContentDescription(getString(R.string.ams_rvi_content_desc_change, audibleSymbol, change));
        percentChangeValue.setContentDescription(
                getString(R.string.ams_rvi_content_des_change_per, audibleSymbol, percentChange)
        );

        final int pillResId = (isUp)
                ? R.drawable.percent_change_pill_green
                : R.drawable.percent_change_pill_red;

        ViewUtils.setViewBackgroundDrawable(changeValue, pillResId);
        ViewUtils.setViewBackgroundDrawable(percentChangeValue, pillResId);

        mDateTextView = (TextView) findViewById(R.id.alg_date_text);
        mOpenTextView = (TextView) findViewById(R.id.alg_open_text);

        final boolean isConnected = NetworkUtils.isConnectedToNetwork(this);

        if(savedInstanceState == null) {
            if (isConnected) {
                launchFetchDetailsService();
            } else {
                mChartView.setSelectedEntry(SelectiveLineChartView.NO_DATA);
            }
        } else {
            mModel = savedInstanceState.getParcelable(DETAILED_MODEL_KEY);
            if (mModel == null && isConnected) {
                launchFetchDetailsService();
            } else if (mModel == null) {
                mChartView.setSelectedEntry(SelectiveLineChartView.NO_DATA);
            } else {
                final int selection = savedInstanceState.getInt(SELECTED_ENTRY_KEY);
                initDetailedStockViews(mModel, selection);
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(DetailedStockTaskIntentService.ACTION_COMPLETE);
        LocalBroadcastManager.getInstance(this).registerReceiver(launchDetailActivityReceiver, filter);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(launchDetailActivityReceiver);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_ENTRY_KEY, mChartView.getSelectedEntry());
        outState.putParcelable(DETAILED_MODEL_KEY, mModel);
    }

    private void launchFetchDetailsService() {
        final String symbol = getIntent().getStringExtra(DetailedStockTaskIntentService.KEY_SYMBOL);
        Intent launchDetailedService = new Intent(this, DetailedStockTaskIntentService.class);
        launchDetailedService.putExtra(DetailedStockTaskIntentService.KEY_SYMBOL, symbol);
        startService(launchDetailedService);
    }

    private static double[] getMinMaxValues(List<Day> days) {

        double min, max;
        min = max = days.get(0).open;

        for (Day day : days) {
            if (day.open > max) {
                max = day.open;
            } else if(day.open < min) {
                min = day.open;
            }
        }

        if (min == max) max++;

        if (min > 1) min--;

        return new double[] { min, max };

    }

    private static int getStepCountFromMinMaxValues(double[] minMaxValues) {
        final int min = (int) Math.floor(minMaxValues[0]);
        final int max = (int) Math.ceil(minMaxValues[1]);
        return ((max - min) < 8) ? 1 : 8;

    }

    @Override
    public void onClick(int setIndex, int entryIndex, Rect rect) {
        mDateTextView.setText(getDateText(entryIndex));
        mOpenTextView.setText(getOpenText(entryIndex));
        mChartView.setSelectedEntry(entryIndex);
    }

    private String getDateText(int index) {
        return getString(R.string.alg_date, dateFormat.format(mModel.getDays().get(index).date));
    }

    private String getOpenText(int index) {
        return getString(R.string.alg_open, mModel.getDays().get(index).open);
    }

    private final BroadcastReceiver launchDetailActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final DetailStockModel model =
                    intent.getParcelableExtra(DetailedStockTaskIntentService.STOCK_DETAIL_EXTRA_KEY);
            initDetailedStockViews(model, 0);
        }
    };

    private void updateSelectedItems(int selection) {
        mChartView.setSelectedEntry(selection);
        mDateTextView.setText(getDateText(selection));
        mOpenTextView.setText(getOpenText(selection));
    }

    private void initDetailedStockViews(DetailStockModel model, int selection) {

        mModel = model;

        final Typeface robotoLightTypeface = TypefaceUtils.getRobotoLightTypeface(getAssets());

        final int colorBlue600 = ContextCompat.getColor(this, R.color.material_blue_600);
        final int colorBlue500 = ContextCompat.getColor(this, R.color.material_blue_500);
        final int colorGreen700 = ContextCompat.getColor(this, R.color.material_green_700);

        ArrayList<ChartSet> sets = new ArrayList<>();
        final int count = model.getDays().size();

        double[] minMax = getMinMaxValues(model.getDays());

        mChartView.setOnEntryClickListener(this);
        mChartView.setAxisBorderValues((int)Math.floor(minMax[0]), (int)Math.ceil(minMax[1]));
        mChartView.setStep(getStepCountFromMinMaxValues(minMax));

        String[] labels = new String[count];
        float[] values = new float[count];

        for (int i = 0; i < count; i++) {

            final Day day = model.getDays().get(i);

            labels[i] = dateFormat.format(day.date);
            values[i] = (float) day.open;

        }

        LineSet set = new LineSet(labels, values);
        set.setColor(colorBlue600);
        set.setGradientFill(new int[] { colorBlue600, colorBlue500 }, null);
        set.setThickness(1f);
        set.setDotsColor(colorGreen700);
        set.setDotsRadius(POINT_RADIUS);
        sets.add(set);

        mChartView.addData(sets);
        mChartView.show();

        TextView title = (TextView) findViewById(R.id.alg_title);
        title.setText(getString(R.string.alg_title, labels[0], labels[labels.length-1]));
        title.setContentDescription(getString(R.string.alg_title, labels[0], labels[labels.length-1]));
        title.setFocusable(true);
        title.setTypeface(robotoLightTypeface);

        if (selection == 0) selection = mModel.getDays().size() -1;
        updateSelectedItems(selection);

    }

}
