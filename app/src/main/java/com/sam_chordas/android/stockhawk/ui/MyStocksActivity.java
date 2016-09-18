package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.model.DetailStockModel;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.DetailedStockTaskIntentService;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;
import com.sam_chordas.android.stockhawk.util.NetworkUtils;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, QuoteCursorAdapter.Callbacks{

    private static final int CURSOR_LOADER_ID = 0;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private QuoteCursorAdapter mCursorAdapter;
    boolean isConnected;

    private CardView mHiddenLayout;

    private Cursor mCursor = null;

    private static final String BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
    //private static final String SELECT_PREFIX = "select * from yahoo.finance.historicaldata where ";
    private static final String SELECT_PREFIX="%20select%20*%20from%20yahoo.finance.historicaldata%20where%20";
    private static final String SUFFIX_URL =
            "&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";
    private static final String SYMBOL_CLAUSE = "symbol = ";
    private static final String START_DATE_CLAUSE = " and startDate = ";
    private static final String END_DATE_CLAUSE = " and endDate = ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isConnected = NetworkUtils.isConnectedToNetwork(this);

        setContentView(R.layout.activity_my_stocks);

        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        Intent mServiceIntent = new Intent(this, StockIntentService.class);

        if (savedInstanceState == null){
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra(StockIntentService.KEY, StockIntentService.INIT);
            if (isConnected){
                startService(mServiceIntent);
            } else{
                NetworkUtils.buildNetworkFailureToast(this).show();
            }
        }

        mHiddenLayout = (CardView) findViewById(R.id.hidden_layout);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        recyclerView.setAdapter(mCursorAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {

//                if (mHiddenLayout.getVisibility() == View.VISIBLE) {
//                    mHiddenLayout.setVisibility(View.GONE);
//                } else if (mHiddenLayout.getVisibility() == View.GONE) {
//                    mHiddenLayout.setVisibility(View.VISIBLE);
//                }

                if (isConnected) {
                    NetworkUtils.buildAddStockSymbolDialog(MyStocksActivity.this).show();
                } else {
                    NetworkUtils.buildNetworkFailureToast(MyStocksActivity.this).show();
                }

            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();
        if (isConnected){
            final long period = 3600L;
            final long flex = 10L;
            final String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = buildPeriodicTask(period, flex, periodicTag);
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }

    }


    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
        IntentFilter filter = new IntentFilter(DetailedStockTaskIntentService.ACTION_COMPLETE);
        LocalBroadcastManager.getInstance(this).registerReceiver(launchDetailActivityReceiver, filter);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(launchDetailActivityReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
    }

    public void restoreActionBar() {

        if(getSupportActionBar() == null) {
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                break;
            case R.id.action_change_units:
                // this is for changing stock changes from percent value to dollar value
                Utils.showPercent = !Utils.showPercent;
                getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args){
        // This narrows the return to only the stocks that are most current.

        final String[] projection = {
                QuoteColumns._ID,
                QuoteColumns.SYMBOL,
                QuoteColumns.BIDPRICE,
                QuoteColumns.PERCENT_CHANGE,
                QuoteColumns.CHANGE,
                QuoteColumns.IS_UP
        };

        final String where = QuoteColumns.IS_CURRENT + "=?";
        final String[] whereArgs = { "1" };

        return new CursorLoader(
                this,
                QuoteProvider.Quotes.CONTENT_URI,
                projection,
                where,
                whereArgs,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data){
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader){
        mCursorAdapter.swapCursor(null);
    }

    private PeriodicTask buildPeriodicTask(long period, long flex, String periodicTag) {

        return new PeriodicTask.Builder()
                .setService(StockTaskService.class)
                .setPeriod(period)
                .setFlex(flex)
                .setTag(periodicTag)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setRequiresCharging(false)
                .build();
    }

    @Override
    public void onItemClick(int position) {

        mCursor = mCursorAdapter.getCursor();

        mCursor.moveToPosition(position);

        final String symbol = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
        final String bid = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE));
        final String percentChange = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
        final String change = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE));
        final boolean isUp = (mCursor.getInt(mCursor.getColumnIndex(QuoteColumns.IS_UP)) == 1);

        Intent launchDetailedService =
                new Intent(this, DetailedStockTaskIntentService.class);
        launchDetailedService.putExtra(DetailedStockTaskIntentService.KEY_SYMBOL, symbol);
        launchDetailedService.putExtra(DetailedStockTaskIntentService.KEY_BID, bid);
        launchDetailedService.putExtra(DetailedStockTaskIntentService.KEY_PERCENT_CHANGE, percentChange);
        launchDetailedService.putExtra(DetailedStockTaskIntentService.KEY_CHANGE, change);
        launchDetailedService.putExtra(DetailedStockTaskIntentService.KEY_IS_UP, isUp);

        startService(launchDetailedService);

    }

    private final BroadcastReceiver launchDetailActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final DetailStockModel model =
                    intent.getParcelableExtra(DetailedStockTaskIntentService.STOCK_DETAIL_EXTRA_KEY);
            Intent launchDetailActivity = new Intent(context, DetailStocksActivity.class);
            launchDetailActivity.putExtra(DetailedStockTaskIntentService.STOCK_DETAIL_EXTRA_KEY, model);
            startActivity(launchDetailActivity);
        }
    };

}
